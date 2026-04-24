package de.greensurvivors.headnseek.common.config;

import org.jetbrains.annotations.NotNull;

public interface IProxyConfigManager {
    void reload();

    /// if the server is configured or the wildchar * is present
    boolean shouldMessageServer(final @NotNull String serverName);
}
