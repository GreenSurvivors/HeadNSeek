package de.greensurvivors.headnseek.paper.socialadapter;

import de.greensurvivors.headnseek.paper.HeadNSeek;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class DummyAdapter extends ASocialAdapter {
    private final static @NotNull Component msgHead = Component.text("SocialMessage:");

    protected DummyAdapter(@NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public void sendMessage(final @NotNull Component message) {
        plugin.getComponentLogger().info(Component.text().append(msgHead).appendSpace().append(message).build());
    }

    @Override
    public @NotNull ASocialAdapterType getTyp() {
        return ASocialAdapterType.DUMMY;
    }
}
