package de.greensurvivors.headnseek.paper.language;

import org.jetbrains.annotations.NotNull;

public enum TranslationKey {
    PLUGIN_PREFIX("prefix", "<gold>[HeadNSeek]</gold>"),
    SOCIAL_MESSAGE("social.message") // todo
    ;

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    TranslationKey(final @NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    TranslationKey(@NotNull String path, @NotNull String defaultValue) {
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
