package de.greensurvivors.headnseek.proxy;

import de.greensurvivors.greensocket.bungee.event.ReceivedPacketEvent;
import de.greensurvivors.headnseek.common.network.StringPacket;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ProxyPacketProcessor implements Listener {
    private final @NotNull ProxyHeadNSeek plugin;

    public ProxyPacketProcessor(final @NotNull ProxyHeadNSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPacketEvent(final @NotNull ReceivedPacketEvent event) {
        if (event.getPacket() instanceof StringPacket stringPacket) {
            final @NotNull BaseComponent component = TextComponent.fromLegacy(stringPacket.getMsg());

            for(final @NotNull Map.Entry<@NotNull String, @NotNull ServerInfo> entry : plugin.getProxy().getServersCopy().entrySet()) {
                if (plugin.getConfigManager().shouldMessageServer(entry.getKey())) {
                    for (final @NotNull ProxiedPlayer proxiedPlayer : entry.getValue().getPlayers()) {
                        if (proxiedPlayer.hasPermission(ProxyPermissionWrapper.RETRIEVE_BUNGEE_MSG.getPermission())) {
                            proxiedPlayer.sendMessage(component);
                        }
                    }
                }
            }
        }
    }
}
