package de.greensurvivors.headnseek.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Utils {
    private Utils() {
    }

    public static <T> @NotNull Collection<@NotNull T> checkCollection(final @NotNull Collection<?> input, final @NotNull Class<T> clazz) {
        final @NotNull Collection<@NotNull T> result = new ArrayList<>(input.size());

        for (final @Nullable Object value : input) {
            if (clazz.isInstance(value)) {
                //noinspection unchecked
                result.add((T) value);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked") // it is checked, that's the whole point. Shut up!
    public static <T> @NotNull Map<@NotNull String, @NotNull T> checkMap(final @NotNull Map<?, ?> input, final @NotNull Class<T> clazz) {
        final @NotNull Map<@NotNull String, @NotNull T> result = new HashMap<>(input.size());

        for (final @NotNull Map.Entry<?, ?> entry : input.entrySet()) {
            if (entry.getKey() instanceof String key && clazz.isInstance(entry.getValue())) {
                result.put(key, (T) entry.getValue());
            }
        }

        return result;
    }

    public static int square(final int v) {
        return Math.multiplyExact(v, v);
    }
}
