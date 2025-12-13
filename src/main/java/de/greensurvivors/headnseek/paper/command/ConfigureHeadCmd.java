package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.PermissionWrapper;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
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
        MessageComponentSerializer.message().serialize(Component.text(((Key)key).asMinimalString() + " is not a player head!")));

    public ConfigureHeadCmd(final @NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public void buildSubCmd(final @NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {
        final @NotNull LiteralCommandNode<@NotNull CommandSourceStack> executingNode = Commands.literal(getLiteral()).
            requires(stack -> {
                final @NotNull CommandSender sender = stack.getSender();
                return sender instanceof Player && sender.hasPermission(PermissionWrapper.CMD_CONFIGURE_HEAD.getPermission());
            }).
            then(Commands.argument("head number", IntegerArgumentType.integer(0)).
                executes(context -> {
                    final Player sender = (Player) context.getSource().getSender();
                    final ItemStack stack = sender.getInventory().getItemInMainHand();

                    if (stack.getType().asItemType() == ItemType.PLAYER_HEAD) {
                        final int number = IntegerArgumentType.getInteger(context, "head number");

                        plugin.getHeadManager().setHead(number, stack);
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
