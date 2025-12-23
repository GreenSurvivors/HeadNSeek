package de.greensurvivors.headnseek.bungee;

import de.greensurvivors.greensocket.bungee.event.ReceivedPacketEvent;
import de.greensurvivors.headnseek.common.network.MessagePacket;
import de.greensurvivors.headnseek.proxy.ProxyPermissionWrapper;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

public class BungeePacketProcessor implements Listener {
    private final @NotNull BungeeHeadNSeek plugin;

    public BungeePacketProcessor(final @NotNull BungeeHeadNSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPacketEvent(final @NotNull ReceivedPacketEvent event) {
        if (event.getPacket() instanceof MessagePacket msgPacket) {
            ProxyServer.getInstance().broadcast();

            plugin.getBungeeAudiences().filter(commandSender ->
                // all audience that are either a server themselves (as far as I know just the proxy we are on)
                // or a player with permission, and on a server that should be configured.
                !(commandSender instanceof ProxiedPlayer player) ||
                    ((commandSender.hasPermission(ProxyPermissionWrapper.RETRIEVE_PROXY_MSG.getPermission()) ||
                        commandSender.hasPermission(ProxyPermissionWrapper.ADMIN.getPermission())) &&
                        plugin.getConfigManager().shouldMessageServer(player.getServer().getInfo().getName()))
            ).sendMessage(MiniMessage.miniMessage().deserialize(msgPacket.getMsg()));
        }
    }
}
