package de.greensurvivors.headnseek.proxy;

import de.greensurvivors.headnseek.proxy.config.ProxyConfigManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import org.jetbrains.annotations.NotNull;

public class ProxyHeadNSeek extends Plugin {
    private final @NotNull ProxyConfigManager configManager;

    public ProxyHeadNSeek() {
        configManager = new ProxyConfigManager(this);
    }

    protected ProxyHeadNSeek(ProxyServer proxy, PluginDescription description) {
        super(proxy, description);
        configManager = new ProxyConfigManager(this);
    }


    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, new ProxyPacketProcessor(this));
        configManager.reload();
    }

    public @NotNull ProxyConfigManager getConfigManager() {
        return configManager;
    }
}
