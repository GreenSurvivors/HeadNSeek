package de.greensurvivors.headnseek.proxy;

import org.jetbrains.annotations.NotNull;

public enum ProxyPermissionWrapper {
    RETRIEVE_BUNGEE_MSG("retrieve_bungee_msg");

    private final @NotNull String permission;

    ProxyPermissionWrapper(final @NotNull String permission) {
        this.permission = "headnseek." + permission;
    }

    public @NotNull String getPermission() {
        return permission;
    }
}
