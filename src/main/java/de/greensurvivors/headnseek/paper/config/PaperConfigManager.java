package de.greensurvivors.headnseek.paper.config;

import de.greensurvivors.headnseek.paper.HeadNSeek;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class PaperConfigManager {
    private final @NotNull HeadNSeek plugin;

    private final @NotNull ConfigOption<Locale> localeConfigOption = new ConfigOption<>("language", Locale.ENGLISH);

    public PaperConfigManager(@NotNull HeadNSeek plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        final @Nullable String localeStr = plugin.getConfig().getString(localeConfigOption.getPath(), null);
        localeConfigOption.setValue(localeStr != null ? Locale.forLanguageTag(localeStr) : null);
    }

    public @NotNull Locale getLocale() {
        return localeConfigOption.getValueOrFallback();
    }
}
