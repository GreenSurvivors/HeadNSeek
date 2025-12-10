package de.greensurvivors.headnseek.paper;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public enum PermissionWrapper {
    ACTION_FIND_HEAD("action.find_head"),
    Admin("*", Map.of(
        ACTION_FIND_HEAD.permission.getName(), Boolean.TRUE
    ));

    private final @NotNull Permission permission;

    PermissionWrapper(final @NotNull String name) {
        this(name, Collections.emptyMap());
    }

    PermissionWrapper(final @NotNull String name, final @NotNull Map<@NotNull String, Boolean> children) {
        permission = new Permission(name, children);

        Bukkit.getPluginManager().addPermission(permission);
    }

    public @NotNull Permission getPermission() {
        return permission;
    }
}
