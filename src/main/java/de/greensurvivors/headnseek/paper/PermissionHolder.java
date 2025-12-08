package de.greensurvivors.headnseek.paper;

import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public enum PermissionHolder {
    ADMIN ("*", Map.of());

    private final @NotNull Permission permission;

    PermissionHolder(final @NotNull String name, final @Nullable Map<@NotNull String, @NotNull Boolean> children) {
        this.permission = new Permission("headnseek." + name.toLowerCase(), children);
    }

    PermissionHolder(final @NotNull String name) {
        this(name, null);
    }

    public @NotNull Permission getPermission() {
        return permission;
    }
}
