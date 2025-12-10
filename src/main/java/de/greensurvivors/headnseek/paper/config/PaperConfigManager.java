package de.greensurvivors.headnseek.paper.config;

import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.socialadapter.SocialAdapterType;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Locale;

public class PaperConfigManager {
    private final @NotNull HeadNSeek plugin;

    private final @NotNull ConfigOption<@NotNull Locale> localeConfigOption = new ConfigOption<>("language", Locale.ENGLISH);

    private final @NotNull ConfigOption<@NotNull SocialAdapterType> socialAdapterTypeOption = new ConfigOption<>("socialadapter.type", SocialAdapterType.SLACK);
    private final @NotNull ConfigOption<@Nullable URI> socialAdapterURIOption = new ConfigOption<>("socialadapter.uri", null);

    public PaperConfigManager(@NotNull HeadNSeek plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        final FileConfiguration config = plugin.getConfig();

        final @Nullable String localeStr = config.getString(localeConfigOption.getPath(), null);
        localeConfigOption.setValue(localeStr != null ? Locale.forLanguageTag(localeStr) : null);

        final @Nullable String socialAdapterTypeStr = config.getString(socialAdapterTypeOption.getPath(), null);
        socialAdapterTypeOption.setValue(socialAdapterTypeStr == null ? null : SocialAdapterType.SOCIAL_ADAPTERS.get(socialAdapterTypeStr.toUpperCase(Locale.ENGLISH)));

        final @Nullable String socialAdapterUriStr = config.getString(socialAdapterURIOption.getPath(), null);
        if (socialAdapterUriStr == null) {
            socialAdapterURIOption.setValue(null);
        } else {
            try {
                socialAdapterURIOption.setValue(URI.create(socialAdapterUriStr));
            } catch (final @NotNull IllegalArgumentException e) {
                plugin.getComponentLogger().error("Couldn't read social adapter uri, ignoring it. NOTE: We can't notify the");

                socialAdapterURIOption.setValue(null);
            }
        }
    }

    public @NotNull Locale getLocale() {
        return localeConfigOption.getValueOrFallback();
    }

    public @NotNull SocialAdapterType getSocialAdapterType () {
        return socialAdapterTypeOption.getValueOrFallback();
    }

    public @Nullable URI getSocialAdapterUri () {
        return socialAdapterURIOption.getValueOrFallback();
    }
}
