package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.PermissionWrapper;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class ConfigureCmd implements ICommand {
    private final @NotNull HeadNSeek plugin;

    public ConfigureCmd(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public void buildSubCmd(@NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {
        cmdBuilder.then(Commands.literal("set").
            requires(stack -> {
                final @NotNull CommandSender sender = stack.getSender();
                return sender instanceof Player && sender.hasPermission(PermissionWrapper.CMD_SET.getPermission());
            }).
            then(Commands.argument("head number", IntegerArgumentType.integer(0, 8)). // todo make upper bound configurable
                executes(context -> {
                    final Player sender = (Player) context.getSource().getSender();
                    final ItemStack stack = sender.getInventory().getItemInMainHand();

                    if (stack.getType().asItemType() == ItemType.PLAYER_HEAD) {
                        int number = IntegerArgumentType.getInteger(context, "head number");

                        plugin.getHeadManager().setHead(number, stack);
                    } else {
                        // todo throw CmdException
                    }

                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }
}
