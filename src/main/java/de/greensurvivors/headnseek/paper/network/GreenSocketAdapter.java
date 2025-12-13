package de.greensurvivors.headnseek.paper.network;

import de.greensurvivors.greensocket.spigot.SpigotSocketApi;
import de.greensurvivors.headnseek.common.network.StringPacket;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class GreenSocketAdapter extends AProxyAdapter {

    public GreenSocketAdapter(final @NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public boolean isProxyConnected() {
        return SpigotSocketApi.isRunning() && SpigotSocketApi.getServerName() != null;
    }

    @Override
    public void sendMessage(final @NotNull Component message) {
        if (isProxyConnected()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                SpigotSocketApi.sendPacketToBungee(
                    // unfortunately Waterfall/Bungeecord does NOT support adventure
                    new StringPacket(LegacyComponentSerializer.legacySection().serialize(message)))
            );
        }
    }
}
