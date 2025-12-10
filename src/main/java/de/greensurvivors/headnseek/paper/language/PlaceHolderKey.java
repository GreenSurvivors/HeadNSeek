package de.greensurvivors.headnseek.paper.language;

import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

public enum PlaceHolderKey {
    PLAYER("player"),
    NUMBER("number");

    private final @NotNull String key;

    PlaceHolderKey(@NotNull String key) {
        this.key = key;
    }

    @Subst("player")
    public @NotNull String getKey() {
        return key;
    }
}
