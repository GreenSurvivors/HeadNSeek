package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.PermissionWrapper;
import de.greensurvivors.headnseek.paper.language.PlaceHolderKey;
import de.greensurvivors.headnseek.paper.language.TranslationKey;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ConfigureHeadCmd extends ACommand {
    private static final @NotNull DynamicCommandExceptionType ERROR_NOT_A_HEAD = new DynamicCommandExceptionType(key ->
        new LiteralMessage(((Key) key).asMinimalString() + " is not a player head!"));
    private static final @NotNull DynamicCommandExceptionType ERROR_NOT_AN_INT = new DynamicCommandExceptionType(input ->
        new LiteralMessage(input + " is not a valid integer!"));

    public ConfigureHeadCmd(final @NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public void buildSubCmd(final @NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {
        final @NotNull LiteralCommandNode<@NotNull CommandSourceStack> executingNode = Commands.literal(getLiteral()).
            requires(stack -> {
                final @NotNull CommandSender sender = stack.getSender();
                return sender instanceof Player && sender.hasPermission(PermissionWrapper.CMD_CONFIGURE_HEAD.getPermission());
            }).then(Commands.argument("head number", IntegerArgumentType.integer(1)).
                suggests((context, builder) -> {
                    try {
                        final int inputInt = Integer.parseInt(builder.getRemaining());
                        if (inputInt > 0) {
                            final long inputNextDigit = inputInt * 10L;

                            for (int i = 0; i <= 9; i++) {
                                final long nextSuggestion = inputNextDigit + i;
                                if (nextSuggestion > Integer.MAX_VALUE) {  // don't suggest Integer overflow
                                    break;
                                }

                                builder.suggest((int) nextSuggestion);
                            }
                        } else {
                            throw ERROR_NOT_AN_INT.create(inputInt);
                        }
                    } catch (NumberFormatException e) {
                        throw ERROR_NOT_AN_INT.create(builder.getRemaining());
                    }
                    return builder.buildFuture();
                }).executes(context -> {
                    final Player sender = (Player) context.getSource().getSender();
                    final ItemStack stack = sender.getInventory().getItemInMainHand();

                    if (stack.getType().asItemType() == ItemType.PLAYER_HEAD) {
                        final int number = IntegerArgumentType.getInteger(context, "head number");

                        if (plugin.getHeadManager().configureHead(number, stack).isEmpty()) {
                            plugin.getMessageManager().sendLang(sender, TranslationKey.CMD_CONFIGURE_HEAD_SUCCESS,
                                Formatter.number(PlaceHolderKey.NUMBER.getKey(), number));
                        } else {
                            plugin.getMessageManager().sendLang(sender, TranslationKey.CMD_CONFIGURE_HEAD_REPLACED,
                                Formatter.number(PlaceHolderKey.NUMBER.getKey(), number));
                        }
                    } else {
                        throw ERROR_NOT_A_HEAD.create(stack.getType().asItemType().key());
                    }

                    return Command.SINGLE_SUCCESS;
                })
            ).build();

        cmdBuilder.then(executingNode);
        cmdBuilder.then(LiteralArgumentBuilder.<CommandSourceStack>literal("cfgh").redirect(executingNode));
    }

    @Override
    protected @Nullable Permission getPermission() {
        return PermissionWrapper.CMD_CONFIGURE_HEAD.getPermission();
    }

    @Override
    protected @NotNull String getLiteral() {
        return "config_head";
    }
}
