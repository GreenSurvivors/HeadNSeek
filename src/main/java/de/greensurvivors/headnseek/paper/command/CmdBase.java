package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CmdBase {
    protected final @NotNull HeadNSeek plugin;
    protected final @NotNull Map<@NotNull String, @NotNull ACommand> internalRegisteredCmds = new TreeMap<>();

    public CmdBase(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;

        registerCmdInternally(new ConfigureHeadCmd(plugin));
        registerCmdInternally(new GetCmd(plugin));
        registerCmdInternally(new DefineBoardCmd(plugin));
        registerCmdInternally(new ReloadCmd(plugin));
    }

    public final void registerCmdInternally(final @NotNull ACommand command) {
        internalRegisteredCmds.put(command.getLiteral(), command);
    }

    public void onCmdServerRegistration(final @NotNull ReloadableRegistrarEvent<Commands> event) {
        final @NotNull Collection<@NotNull ACommand> commands = internalRegisteredCmds.values();
        final @NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder = Commands.literal("headnseek").
            requires(stack -> {
                for (final @NotNull ACommand command : commands) {
                    if (command.getPermission() == null || stack.getSender().hasPermission(command.getPermission())) {
                        return true;
                    }
                }

                return false;
            });


        for (final @NotNull ACommand command : commands) {
            command.buildSubCmd(cmdBuilder);
        }

        event.registrar().register(cmdBuilder.build());
    }
}
