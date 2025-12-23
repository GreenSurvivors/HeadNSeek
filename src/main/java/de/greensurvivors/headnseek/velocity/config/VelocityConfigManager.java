package de.greensurvivors.headnseek.velocity.config;

import de.greensurvivors.headnseek.common.Utils;
import de.greensurvivors.headnseek.common.config.ConfigOption;
import de.greensurvivors.headnseek.velocity.VelocityHeadNSeek;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class VelocityConfigManager {
    protected static final @NotNull String CONFIG_FILENAME = "proxy-config.yml";
    protected final @NotNull VelocityHeadNSeek plugin;
    protected final @NotNull Path configFilePath;

    private final @NotNull ConfigOption<Set<String>> serverToMessage = new ConfigOption<>("serverToMessage", Collections.emptySet());

    public VelocityConfigManager(final @NotNull VelocityHeadNSeek plugin) {
        this.plugin = plugin;
        configFilePath = plugin.getDataDirectoryPath().resolve(CONFIG_FILENAME);
    }

    public void reload() {
        // create data folder is not exists
        if (!Files.isDirectory(plugin.getDataDirectoryPath())) {
            try {
                Files.createDirectory(plugin.getDataDirectoryPath());
            } catch (final @NotNull IOException e) {
                plugin.getLogger().warn("Could not create data folder {}", plugin.getDataDirectoryPath(), e);
            }
        }

        // if configuration file does not exist, copy default configuration
        if (!Files.isRegularFile(configFilePath)) {
            try (final @Nullable InputStream stream = plugin.getClass().getClassLoader().getResourceAsStream(CONFIG_FILENAME)) {
                if (stream != null) {
                    Files.copy(stream, configFilePath);
                } else {
                    plugin.getLogger().error("Could find default configuration file, since it appears to be missing!");
                }
            } catch (final @NotNull IOException e) {
                plugin.getLogger().error("Could not copy default configuration file", e);
            }
        }

        // Load configuration
        try (final @NotNull Reader cfgReader = Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8)) {
            final @NotNull Map<?, ?> rawConfigMap = new Yaml().loadAs(cfgReader, Map.class);

            final @NotNull Map<@NotNull String, @NotNull Object> checkedConfigMap = Utils.checkMap(rawConfigMap, Object.class);

            if (checkedConfigMap.get(serverToMessage.getPath()) instanceof final @NotNull Collection<?> rawServerToMessage) {
                serverToMessage.setValue(Set.copyOf(Utils.checkCollection(rawServerToMessage, String.class)));
            }
        } catch (final @NotNull IOException e) {
            plugin.getLogger().error("Could not read config file!", e);
        }
    }

    public boolean shouldMessageServer(final @NotNull String serverName) {
        return serverToMessage.getValueOrFallback().contains(serverName);
    }
}
