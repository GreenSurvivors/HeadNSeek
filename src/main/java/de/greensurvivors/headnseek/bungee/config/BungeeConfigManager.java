package de.greensurvivors.headnseek.bungee.config;

import de.greensurvivors.headnseek.bungee.BungeeHeadNSeek;
import de.greensurvivors.headnseek.common.config.ConfigOption;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BungeeConfigManager {
    protected final static @NotNull String CONFIG_FILE_NAME = "proxy-config.yml";

    protected final @NotNull BungeeHeadNSeek plugin;
    protected final @NotNull Path dataDirectoryPath;
    protected final @NotNull Path configFilePath;
    protected final @NotNull ConfigurationProvider configProvider;

    private final @NotNull ConfigOption<Set<String>> serverToMessage = new ConfigOption<>("serverToMessage", Collections.emptySet());

    protected @NotNull Configuration config;

    public BungeeConfigManager(final @NotNull BungeeHeadNSeek plugin) {
        this.plugin = plugin;
        dataDirectoryPath = plugin.getDataFolder().toPath();
        configFilePath = dataDirectoryPath.resolve(CONFIG_FILE_NAME);
        configProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);


        try (final @NotNull InputStream resourceStream = plugin.getResourceAsStream(CONFIG_FILE_NAME)) {
            config = configProvider.load(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
        } catch (final @NotNull IOException e) {
            plugin.getSLF4JLogger().error("Could not load default config", e);

            throw new RuntimeException(e);
        }
    }

    public void reload() {
        if (!Files.isRegularFile(configFilePath)) {

            try {
                Files.createDirectories(dataDirectoryPath);
                configProvider.save(config, configFilePath.toFile());
            } catch (final IOException e) {
                plugin.getSLF4JLogger().warn("Could not save default config", e);
            }
        } else {
            try {
                config = configProvider.load(configFilePath.toFile(), config);

                final @Nullable List<?> serverToMessageList = config.getList(serverToMessage.getPath(), null);
                final @Nullable Set<@NotNull String> serverToMessageSet;

                if (serverToMessageList != null && !serverToMessageList.isEmpty()) {
                    serverToMessageSet = new HashSet<>();

                    for (final @NotNull Object serverNameObj : serverToMessageList) {
                        if (serverNameObj instanceof String serverName) {
                            serverToMessageSet.add(serverName);
                        }
                    }
                } else {
                    serverToMessageSet = null;
                }

                if (serverToMessageSet != null && !serverToMessageSet.isEmpty()) {
                    serverToMessage.setValue(serverToMessageSet);
                }
            } catch (IOException e) {
                plugin.getSLF4JLogger().warn("Could not load config", e);
            }
        }
    }

    public boolean shouldMessageServer(final @NotNull String serverName) {
        return serverToMessage.getValueOrFallback().contains(serverName);
    }
}
