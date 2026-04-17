package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.PermissionWrapper;
import de.greensurvivors.headnseek.paper.language.PlaceHolderKey;
import de.greensurvivors.headnseek.paper.language.TranslationKey;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoveBoardCmd extends ACommand {
    private final static SimpleCommandExceptionType ERROR_NOT_A_PLAYER = new SimpleCommandExceptionType(new LiteralMessage("Sender is not a player!"));
    private static final @NotNull DynamicCommandExceptionType ERROR_NOT_A_DOUBLE = new DynamicCommandExceptionType(input ->
        new LiteralMessage(input + " is not a valid double!"));

    protected RemoveBoardCmd(@NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    protected void buildSubCmd(@NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {
        final @NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> subCmdRootBuilder = Commands.literal(getLiteral())
            .requires(stack -> stack.getSender().hasPermission(PermissionWrapper.CMD_REMOVE_BOARD.getPermission()));

        subCmdRootBuilder.then(Commands.literal("all")
            .executes(context -> {
                final int amountBoardsRemoved = plugin.getBoardManager().removeAllBoards();
                plugin.getMessageManager().sendLang(context.getSource().getSender(), TranslationKey.CMD_REMOVE_BOARD_ALL_SUCCESS,
                    Formatter.number(PlaceHolderKey.NUMBER.getKey(), amountBoardsRemoved));

                return Command.SINGLE_SUCCESS;
            })
        );

        subCmdRootBuilder.then(Commands.literal("world")
            .executes(context -> {
                if (context.getSource().getSender() instanceof Player sender) {
                    final @NotNull Key worldKey = sender.getWorld().getKey();
                    final int amountBoardsRemoved = plugin.getBoardManager().removeAllBoardsInWorld(worldKey);
                    plugin.getMessageManager().sendLang(context.getSource().getSender(), TranslationKey.CMD_REMOVE_BOARD_WORLD_SUCCESS,
                        Formatter.number(PlaceHolderKey.NUMBER.getKey(), amountBoardsRemoved),
                        Placeholder.unparsed(PlaceHolderKey.WORLD.getKey(), sender.getWorld().getName()));

                    return Command.SINGLE_SUCCESS;
                } else {
                    throw ERROR_NOT_A_PLAYER.create();
                }
            }).then(Commands.argument("worldKey", ArgumentTypes.namespacedKey()) //todo suggest but NOT require worlds currently loaded by the server. Allow deleted worlds to also get removed!
                .suggests((stack, builder) -> {

                    final @NotNull String @NotNull [] components = builder.getRemaining().split(":", 3); // todo precompile Pattern
                    final @NotNull String probablyValue; // value always not null but may be empty
                    final @Nullable String namespace; // namespace is never empty but may be null in case of no explicit given namespace -> in this case the typed in text may be either, we don't know yet

                    if (components.length > 2) {
                        return builder.buildFuture();
                    } else if (components.length == 1) {
                        // special case where components[0] could either be the namespace OR the value
                        namespace = null;//NamespacedKey.MINECRAFT_NAMESPACE;
                        probablyValue = components[0];

                        for (int i = 0, length = probablyValue.length(); i < length; i++) {
                            final char charAt = probablyValue.charAt(i);

                            if (!(Key.allowedInNamespace(charAt) || Key.allowedInValue(charAt))) {
                                return builder.buildFuture();
                            }
                        }
                    } else {
                        probablyValue = components[1];

                        // checkValue
                        for (int i = 0, length = probablyValue.length(); i < length; i++) {
                            if (!Key.allowedInValue(probablyValue.charAt(i))) {
                                return builder.buildFuture();
                            }
                        }

                        if (components[0].isEmpty()) {
                            namespace = NamespacedKey.MINECRAFT_NAMESPACE;
                        } else {
                            namespace = components[0];

                            // checkNamespace
                            for (int i = 0, length = namespace.length(); i < length; i++) {
                                if (!Key.allowedInNamespace(namespace.charAt(i))) {
                                    return builder.buildFuture();
                                }
                            }
                        }
                    }

                    for (final @NotNull World world : plugin.getServer().getWorlds()) {
                        final @NotNull Key worldKey = world.getKey();

                        if (namespace == null) {
                            if (worldKey.namespace().startsWith(probablyValue) || (
                                worldKey.value().startsWith(probablyValue) && worldKey.namespace().equals(Key.MINECRAFT_NAMESPACE))) {

                                builder.suggest(worldKey.namespace());
                                builder.suggest(worldKey.asString());
                            }
                        } else if (namespace.equals(worldKey.namespace()) &&
                                worldKey.value().startsWith(probablyValue)) {

                            builder.suggest(worldKey.namespace());
                            builder.suggest(worldKey.asString());
                        }
                    }
                    return builder.buildFuture();
                }).executes(context -> {
                    final @NotNull Key worldKey = context.getArgument("worldKey", NamespacedKey.class);
                    final int amountBoardsRemoved = plugin.getBoardManager().removeAllBoardsInWorld(worldKey);
                    plugin.getMessageManager().sendLang(context.getSource().getSender(), TranslationKey.CMD_REMOVE_BOARD_WORLD_SUCCESS,
                        Formatter.number(PlaceHolderKey.NUMBER.getKey(), amountBoardsRemoved),
                        Placeholder.unparsed(PlaceHolderKey.WORLD.getKey(), worldKey.asMinimalString()));

                    return Command.SINGLE_SUCCESS;
                })
            )
        );

        subCmdRootBuilder.then(Commands.literal("near")
            .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0, 200))
                .suggests((context, builder) -> {
                    try {
                        final double inputDouble = Double.parseDouble(builder.getRemaining());
                        if (inputDouble > 0 && inputDouble <= 200) {
                            final double inputNextDigit = inputDouble * 10L;

                            for (int i = 0; i <= 9; i++) {
                                final double nextSuggestion = inputNextDigit + i;
                                if (nextSuggestion > 200) {  // don't suggest over max
                                    break;
                                }

                                builder.suggest((int) nextSuggestion);
                            }
                        } else {
                            throw ERROR_NOT_A_DOUBLE.create(inputDouble);
                        }
                    } catch (NumberFormatException e) {
                        throw ERROR_NOT_A_DOUBLE.create(builder.getRemaining());
                    }

                    return builder.buildFuture();
                }).executes(context -> {
                    final @Nullable Location location = context.getSource().getLocation();
                    final double radius = DoubleArgumentType.getDouble(context, "radius");
                    final int amountBoardsRemoved = plugin.getBoardManager().removeAllBoardsNear(location, radius);
                    plugin.getMessageManager().sendLang(context.getSource().getSender(), TranslationKey.CMD_REMOVE_BOARD_NEAR_SUCCESS,
                        Formatter.number(PlaceHolderKey.NUMBER.getKey(), amountBoardsRemoved),
                        Formatter.number(PlaceHolderKey.RADIUS.getKey(), radius));

                    return Command.SINGLE_SUCCESS;
                })
            )
        );

        final @NotNull LiteralCommandNode<@NotNull CommandSourceStack> subCmdRoot = subCmdRootBuilder.build();

        cmdBuilder.then(subCmdRoot);
    }

    @Override
    protected @Nullable Permission getPermission() {
        return PermissionWrapper.CMD_REMOVE_BOARD.getPermission();
    }

    @Override
    protected @NotNull String getLiteral() {
        return "remove_board";
    }
}
