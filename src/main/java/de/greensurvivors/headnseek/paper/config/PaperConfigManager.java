package de.greensurvivors.headnseek.paper.config;

import de.greensurvivors.headnseek.common.config.ConfigOption;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import de.greensurvivors.headnseek.paper.socialadapter.ASocialAdapterType;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Locale;

public class PaperConfigManager {
    protected final @NotNull HeadNSeek plugin;

    protected final @NotNull ConfigOption<@NotNull ComparableVersion> dataVersion = new ConfigOption<>("dataVersion", new ComparableVersion("1.0.0"));

    protected final @NotNull ConfigOption<@NotNull Locale> localeConfigOption = new ConfigOption<>("language", Locale.ENGLISH);

    protected final @NotNull ConfigOption<@NotNull ASocialAdapterType> socialAdapterTypeOption = new ConfigOption<>("socialadapter.type", ASocialAdapterType.DUMMY);
    protected final @NotNull ConfigOption<@Nullable URI> socialAdapterURIOption = new ConfigOption<>("socialadapter.uri", null);

    public PaperConfigManager(@NotNull HeadNSeek plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        final FileConfiguration config = plugin.getConfig();

        final @Nullable String dataVersionStr = config.getString(dataVersion.getPath(), null);
        if (dataVersionStr == null) {
            plugin.getComponentLogger().warn("The data version in the config.yml file is missing. Ignoring it for now and assuming everything will go well. Be warned: There be dragons!");
        } else {
            final ComparableVersion foundVersion = new ComparableVersion(dataVersionStr);
            if (foundVersion.compareTo(dataVersion.getValueOrFallback()) > 0) {
                plugin.getComponentLogger().error("Trying to load config.yml and encountered newer version. Expected: \"{}\", got: \"{}\"", dataVersion.getValueOrFallback(), foundVersion);
            }
        }

        final @Nullable String localeStr = config.getString(localeConfigOption.getPath(), null);
        localeConfigOption.setValue(localeStr != null ? Locale.forLanguageTag(localeStr) : null);

        final @Nullable String socialAdapterTypeStr = config.getString(socialAdapterTypeOption.getPath(), null);
        socialAdapterTypeOption.setValue(socialAdapterTypeStr == null ? null : ASocialAdapterType.SOCIAL_ADAPTERS.get(socialAdapterTypeStr.toUpperCase(Locale.ENGLISH)));

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

    public @NotNull ASocialAdapterType getSocialAdapterType() {
        return socialAdapterTypeOption.getValueOrFallback();
    }

    public @Nullable URI getSocialAdapterUri() {
        return socialAdapterURIOption.getValueOrFallback();
    }
}
