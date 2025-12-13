package de.greensurvivors.headnseek.paper.network;

import de.greensurvivors.headnseek.paper.HeadNSeek;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public abstract class AProxyAdapter {
    protected final @NotNull HeadNSeek plugin;

    protected AProxyAdapter(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;
    }

    public abstract boolean isProxyConnected();

    public abstract void sendMessage(final @NotNull Component message);
}
