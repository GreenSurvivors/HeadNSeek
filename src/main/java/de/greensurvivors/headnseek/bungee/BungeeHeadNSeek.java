package de.greensurvivors.headnseek.bungee;

import de.greensurvivors.headnseek.bungee.config.BungeeConfigManager;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import org.jetbrains.annotations.NotNull;

public class BungeeHeadNSeek extends Plugin {
    private final @NotNull BungeeConfigManager configManager;
    private BungeeAudiences bungeeAudiences;

    @SuppressWarnings("unused") // used by proxy
    public BungeeHeadNSeek() {
        configManager = new BungeeConfigManager(this);
    }

    @SuppressWarnings("unused") // used by proxy
    protected BungeeHeadNSeek(ProxyServer proxy, PluginDescription description) {
        super(proxy, description);
        configManager = new BungeeConfigManager(this);
    }

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, new BungeePacketProcessor(this));
        bungeeAudiences = BungeeAudiences.create(this);
        configManager.reload();
    }

    @Override
    public void onDisable() {
        bungeeAudiences.close();
    }

    public @NotNull BungeeConfigManager getConfigManager() {
        return configManager;
    }

    public @NotNull BungeeAudiences getBungeeAudiences() throws IllegalStateException {
        if (this.bungeeAudiences == null) {
            throw new IllegalStateException("Cannot retrieve audience provider while plugin is not enabled");
        }
        return bungeeAudiences;
    }
}
