package de.greensurvivors.headnseek.paper.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class ConfigOption <T> {
    private final @NotNull String path;
    private final @NotNull T fallback;
    private final @NotNull AtomicReference<@Nullable T> valueRefference = new AtomicReference<>(null);

    public ConfigOption(final @NotNull String path, final @NotNull T fallback) {
        this.path = path;
        this.fallback = fallback;
    }

    public @NotNull T getValueOrFallback(){
        final @Nullable T value = valueRefference.get();
        return value == null ? fallback : value;
    }

    public @NotNull String getPath(){
        return path;
    }

    public void  setValue(final @Nullable T newValue) {
        this.valueRefference.set(newValue);
    }
}
