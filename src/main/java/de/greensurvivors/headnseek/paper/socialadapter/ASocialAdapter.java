package de.greensurvivors.headnseek.paper.socialadapter;

import de.greensurvivors.headnseek.paper.HeadNSeek;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public abstract class ASocialAdapter {
    protected final @NotNull HeadNSeek plugin;

    protected ASocialAdapter(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;
    }

    public abstract void sendMessage(final @NotNull Component message);

    public abstract @NotNull SocialAdapterType getTyp();
}
