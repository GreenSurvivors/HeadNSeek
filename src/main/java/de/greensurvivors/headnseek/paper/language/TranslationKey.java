package de.greensurvivors.headnseek.paper.language;

import org.jetbrains.annotations.NotNull;

public enum TranslationKey {
    PLUGIN_PREFIX("prefix", "<gold>[HeadNSeek]</gold>"),
    ACTION_FOUND("action.foundHead"),
    ACTION_PLACE_HEAD_BROADCAST("action.placeHead.broadcast"),
    ACTION_DEFINE_BOARD_SUCCESS("action.defineBoard.success"),
    ACTION_DEFINE_BOARD_REPLACED("action.defineBoard.replaced"),
    ACTION_DEFINE_BOARD_ERROR_OVERLAP_CANT_MERGE("action.defineBoard.error.overlap.cantMerge"),
    CMD_CONFIGURE_HEAD_SUCCESS("cmd.configureHead.success"),
    CMD_DEFINE_BOARD_START("cmd.defineBoard.start"),
    SOCIAL_MESSAGE_FOUND("social.message.found"),
    POXY_MESSAGE_FOUND("proxy.message.found"),
    ERROR_NO_PERMISSION("error.noPermission"),
    RELOAD_SUCCESS("reload.success");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    TranslationKey(final @NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    TranslationKey(final @NotNull String path, final @NotNull String defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getDefaultValue() {
        return defaultValue;
    }
}
