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
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class GetCmd extends ACommand {

    public GetCmd(final @NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public void buildSubCmd(final @NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {
        cmdBuilder.then(Commands.literal(getLiteral()))
            .requires(stack -> {
                    final @NotNull CommandSender sender = stack.getSender();
                    return sender instanceof Player && sender.hasPermission(PermissionWrapper.CMD_GET.getPermission());
            }).then(Commands.argument("head number", IntegerArgumentType.integer(0))
                .executes(context -> {
                    final int number = IntegerArgumentType.getInteger(context, "head number");

                    final @NotNull ItemStack stack = plugin.getHeadManager().getHead(number);
                    if (stack.isEmpty()) {
                        // todo
                    } else {
                        final @NotNull Player player = ((Player)context.getSource().getSender());

                        player.give(List.of(stack), true);
                        // todo message
                    }

                    return Command.SINGLE_SUCCESS;
                })
            );
    }

    @Override
    protected @Nullable Permission getPermission() {
        return PermissionWrapper.CMD_GET.getPermission();
    }

    @Override
    protected @NotNull String getLiteral() {
        return "get";
    }
}
