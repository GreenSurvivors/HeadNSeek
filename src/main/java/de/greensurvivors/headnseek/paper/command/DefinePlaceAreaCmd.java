package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.PermissionWrapper;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class DefinePlaceAreaCmd extends ACommand {

    public DefinePlaceAreaCmd(final @NotNull HeadNSeek plugin) {
        super(plugin);
    }
    @Override
    public void buildSubCmd(@NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {
        cmdBuilder.then(Commands.literal(getLiteral()))
            .requires(stack -> {
                final @NotNull CommandSender sender = stack.getSender();
                return sender instanceof Player && sender.hasPermission(PermissionWrapper.CMD_DEFINE_AREA.getPermission());
            });
    }

    @Override
    protected @Nullable Permission getPermission() {
        return PermissionWrapper.CMD_DEFINE_AREA.getPermission();
    }

    @Override
    protected @NotNull String getLiteral() {
        return "define_display";
    }
}
