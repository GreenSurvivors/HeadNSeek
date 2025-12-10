package de.greensurvivors.headnseek.paper.socialadapter;

import de.greensurvivors.headnseek.paper.HeadNSeek;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public interface SocialAdapterType {
    @NotNull Map<@NotNull String, @NotNull SocialAdapterType> SOCIAL_ADAPTERS = new HashMap<>();

    SocialAdapterType SLACK = new SlackAdapterType();


    @NotNull String getName();
    @NotNull SlackAdapter createNew(final @NotNull HeadNSeek plugin);



    class SlackAdapterType implements SocialAdapterType {
        public SlackAdapterType() {
            SOCIAL_ADAPTERS.put(getName(), this);
        }

        @Override
        public @NotNull String getName() {
            return "SLACK";
        }

        @Override
        public @NotNull SlackAdapter createNew(@NotNull HeadNSeek plugin) {
            return new SlackAdapter(plugin);
        }
    }
}
