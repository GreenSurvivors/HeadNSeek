package de.greensurvivors.headnseek.common;

import org.jetbrains.annotations.NotNull;

public enum ProxyPermissionWrapper {
    RETRIEVE_PROXY_MSG("retrieve_proxy_msg"),
    ADMIN("*");

    private final @NotNull String permission;

    ProxyPermissionWrapper(final @NotNull String permission) {
        this.permission = "headnseek." + permission;
    }

    public @NotNull String getPermission() {
        return permission;
    }
}
