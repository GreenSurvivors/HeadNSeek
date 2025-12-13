package de.greensurvivors.headnseek.paper;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public enum PermissionWrapper {
    ACTION_FIND_HEAD("action.find_head"),
    ACTIONS("action.*", Map.of(
        ACTION_FIND_HEAD.permission.getName(), Boolean.TRUE
    )),
    CMD_CONFIGURE_HEAD("cmd.configure_head"),
    CMD_GET("cmd.get"),
    CMD_DEFINE_AREA("cmd.define_area"),
    CMD_RELOAD("cmd.reload"),
    COMMANDS("cmd.*", Map.of(
        CMD_CONFIGURE_HEAD.permission.getName(), Boolean.TRUE,
        CMD_GET.permission.getName(), Boolean.TRUE,
        CMD_DEFINE_AREA.permission.getName(), Boolean.TRUE,
        CMD_RELOAD.permission.getName(), Boolean.TRUE
    )),
    Admin("*", Map.of(
        ACTIONS.permission.getName(), Boolean.TRUE,
        COMMANDS.permission.getName(), Boolean.TRUE
    ));

    private final @NotNull Permission permission;

    PermissionWrapper(final @NotNull String name) {
        this(name, null);
    }

    PermissionWrapper(final @NotNull String name, final @Nullable Map<@NotNull String, Boolean> children) {
        permission = new Permission("headnseek." + name, children);

        Bukkit.getPluginManager().addPermission(permission);
    }

    public @NotNull Permission getPermission() {
        return permission;
    }
}
