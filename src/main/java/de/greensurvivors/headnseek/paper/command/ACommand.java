package de.greensurvivors.headnseek.paper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public abstract class ACommand {
    protected final @NotNull HeadNSeek plugin;

    protected ACommand(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;
    }

    protected abstract void buildSubCmd(final @NotNull LiteralArgumentBuilder<@NotNull CommandSourceStack> cmdBuilder);

    protected abstract @Nullable Permission getPermission();

    protected abstract @NotNull String getLiteral();

    @Override
    public boolean equals(final @Nullable Object other) {
        if (!super.equals(other)) {
            return false;
        }
        if (this.getClass() == other.getClass()) {
            final ACommand otherCmd = (ACommand) other;
            return this.plugin.equals(otherCmd.plugin) && this.getLiteral().equals(otherCmd.getLiteral());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(plugin, getLiteral(), getClass());
    }
}
