package de.greensurvivors.headnseek.paper.socialadapter;

import de.greensurvivors.headnseek.paper.HeadNSeek;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public abstract class ASocialAdapter {
    protected final @NotNull HeadNSeek plugin;
    protected @Nullable URI uri = null;

    protected ASocialAdapter(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;
    }

    public void setUri (final @Nullable URI uri) {
        this.uri = uri;
    }

    public abstract void sendMessage (final @NotNull Component message);

    public abstract @NotNull SocialAdapterType getTyp();
}
