package de.greensurvivors.headnseek.paper.socialadapter;

import de.greensurvivors.headnseek.paper.HeadNSeek;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public abstract class ASocialAdapterType {
    public final static @NotNull Map<@NotNull String, @NotNull ASocialAdapterType> SOCIAL_ADAPTERS = new HashMap<>();

    public final static ASocialAdapterType SLACK = new ASocialAdapterType() {
        @Override
        public @NotNull String getName() {
            return "SLACK";
        }

        @Override
        public @NotNull SlackAdapter createNew(@NotNull HeadNSeek plugin) {
            return new SlackAdapter(plugin);
        }
    };

    public final static ASocialAdapterType DUMMY = new ASocialAdapterType() {

        @Override
        public @NotNull String getName() {
            return "DUMMY";
        }

        @Override
        public @NotNull DummyAdapter createNew(@NotNull HeadNSeek plugin) {
            return new DummyAdapter(plugin);
        }
    };


    protected ASocialAdapterType() {
        SOCIAL_ADAPTERS.put(getName(), this);
    }

    public abstract @NotNull String getName();

    public abstract @NotNull ASocialAdapter createNew(final @NotNull HeadNSeek plugin);
}
