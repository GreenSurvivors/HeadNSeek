package de.greensurvivors.headnseek.paper.network;

import de.greensurvivors.headnseek.paper.HeadNSeek;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class DummyAdapter extends AProxyAdapter{
    private final @NotNull Component messageHead = Component.text("Message to proxy:");

    public DummyAdapter(final @NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public boolean isProxyConnected() {
        return false;
    }

    @Override
    public void sendMessage(final @NotNull Component message) {
        if (plugin.getComponentLogger().isDebugEnabled()) {
            plugin.getComponentLogger().info(Component.text().append(messageHead).appendSpace().append(message).build());
        }
    }
}
