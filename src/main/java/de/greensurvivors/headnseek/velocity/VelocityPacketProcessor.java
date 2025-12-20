package de.greensurvivors.headnseek.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import de.greensurvivors.greensocket.velocity.event.ReceivedPacketEvent;
import de.greensurvivors.headnseek.common.network.MessagePacket;
import de.greensurvivors.headnseek.proxy.ProxyPermissionWrapper;
import org.jetbrains.annotations.NotNull;

public class VelocityPacketProcessor {
    final @NotNull VelocityHeadNSeek plugin;

    public VelocityPacketProcessor(final @NotNull VelocityHeadNSeek plugin) {
        this.plugin = plugin;
    }


    @Subscribe
    public void onPacket(final @NotNull ReceivedPacketEvent event) {
        if (event.getPacket() instanceof MessagePacket messagePacket) {
            // all audience that are either a server themselves (as far as I know just the proxy we are on)
            // or a player with permission, and on a server that should be configured.
            plugin.getServer().filterAudience(audience ->
                !(audience instanceof Player player) ||
                    player.hasPermission(ProxyPermissionWrapper.RETRIEVE_BUNGEE_MSG.getPermission()) &&
                        player.getCurrentServer().
                            filter(serverConnection ->
                                plugin.getConfigManager().shouldMessageServer(serverConnection.getServerInfo().getName())
                            ).isPresent()
            ).sendMessage(messagePacket.getMsg());
        }
    }
}
