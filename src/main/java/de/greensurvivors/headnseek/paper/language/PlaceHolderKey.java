package de.greensurvivors.headnseek.paper.language;

import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

public enum PlaceHolderKey {
    PLAYER_NAME("player_name"),
    PLAYER_UUID("player_uuid"),
    NUMBER("number");

    private final @NotNull String key;

    PlaceHolderKey(@NotNull String key) {
        this.key = key;
    }

    @Subst("player_name")
    public @NotNull String getKey() {
        return key;
    }
}
