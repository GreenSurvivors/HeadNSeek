package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@SuppressWarnings("UnstableApiUsage")
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
                    final @NotNull String worldName = sender.getWorld().getName();
                    final int amountBoardsRemoved = plugin.getBoardManager().removeAllBoardsInWorld(worldName);
                    plugin.getMessageManager().sendLang(context.getSource().getSender(), TranslationKey.CMD_REMOVE_BOARD_WORLD_SUCCESS,
                        Formatter.number(PlaceHolderKey.NUMBER.getKey(), amountBoardsRemoved),
                        Placeholder.unparsed(PlaceHolderKey.WORLD.getKey(), worldName));

                    return Command.SINGLE_SUCCESS;
                } else {
                    throw ERROR_NOT_A_PLAYER.create();
                }
            }).then(Commands.argument("worldName", StringArgumentType.string()) // suggest but NOT require worlds currently loaded by the server. Allow deleted worlds to also get removed!
                .suggests((stack, builder) -> {
                    for (final @NotNull World world : plugin.getServer().getWorlds()) {
                        final @NotNull String worldName = world.getName();
                        if (worldName.toLowerCase(Locale.ENGLISH).startsWith(builder.getRemaining())) {
                            builder.suggest(worldName);
                        }
                    }
                    return builder.buildFuture();
                }).executes(context -> {
                    final @NotNull String worldName = StringArgumentType.getString(context, "worldName");
                    final int amountBoardsRemoved = plugin.getBoardManager().removeAllBoardsInWorld(worldName);
                    plugin.getMessageManager().sendLang(context.getSource().getSender(), TranslationKey.CMD_REMOVE_BOARD_WORLD_SUCCESS,
                        Formatter.number(PlaceHolderKey.NUMBER.getKey(), amountBoardsRemoved),
                        Placeholder.unparsed(PlaceHolderKey.WORLD.getKey(), worldName));

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
