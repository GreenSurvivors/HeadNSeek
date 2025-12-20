package de.greensurvivors.headnseek.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.greensurvivors.headnseek.velocity.config.VelocityConfigManager;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;

public class VelocityHeadNSeek {
    private final @NotNull ProxyServer server;
    private final @NotNull ComponentLogger logger;
    private final @NotNull Path dataDirectoryPath;

    private final @NotNull VelocityConfigManager configManager;

    // note to self, since currently it isn't documented well:
    // org.slf4j.Logger (NOT the same as the ComponentLogger!)
    // com.velocitypowered.api.plugin.PluginDescription,
    // com.velocitypowered.api.plugin.PluginContainer (own)
    // java.util.concurrent.ExecutorService
    // @Named("<plugin id>") com.velocitypowered.api.plugin.PluginContainer (any loaded plugin)
    //
    // com.velocitypowered.api.plugin.PluginManager
    // com.velocitypowered.api.event.EventManager
    // com.velocitypowered.api.command.CommandManager
    // are also possible injection options.
    // though I don't know why would you need the last three, since you can access them through the ProxyServer.
    @Inject
    public VelocityHeadNSeek(final @NotNull ProxyServer server, final @NotNull ComponentLogger logger,
                             final @DataDirectory @NonNull Path dataDirectoryPath) {
        this.server = server;
        this.logger = logger;
        this.dataDirectoryPath = dataDirectoryPath;
        configManager = new VelocityConfigManager(this);
    }

    @Subscribe
    public void onProxyInitialization(final @NotNull ProxyInitializeEvent event) {
        // Do some operation demanding access to the Velocity API here.
        // For instance, we could register an event:
        server.getEventManager().register(this, new VelocityPacketProcessor(this));
        configManager.reload();
    }


    public @NonNull ProxyServer getServer() {
        return server;
    }

    public @NonNull ComponentLogger getLogger() {
        return logger;
    }

    public @NotNull VelocityConfigManager getConfigManager() {
        return configManager;
    }

    public @NotNull Path getDataDirectoryPath() {
        return dataDirectoryPath;
    }
}
