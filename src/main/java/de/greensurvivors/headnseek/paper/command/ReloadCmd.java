package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.PermissionWrapper;
import de.greensurvivors.headnseek.paper.language.TranslationKey;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReloadCmd extends ACommand {
    protected ReloadCmd(@NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public void buildSubCmd(@NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder) {
        cmdBuilder.then(Commands.literal(getLiteral())
            .requires(stack ->
                stack.getSender().hasPermission(PermissionWrapper.CMD_RELOAD.getPermission())
            ).executes(context -> {
                plugin.reload();
                plugin.getMessageManager().sendLang(context.getSource().getSender(), TranslationKey.RELOAD_SUCCESS);

                return Command.SINGLE_SUCCESS;
            }));
    }

    @Override
    protected @Nullable Permission getPermission() {
        return PermissionWrapper.CMD_RELOAD.getPermission();
    }

    @Override
    protected @NotNull String getLiteral() {
        return "reload";
    }
}
