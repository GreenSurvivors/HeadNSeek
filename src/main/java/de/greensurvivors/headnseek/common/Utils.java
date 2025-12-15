package de.greensurvivors.headnseek.common;

import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class Utils {
    private Utils(){
    }

    public static int getHorizontalLength(@NonNull BlockPosition upperLeft, @NonNull BlockPosition lowerRight) {
        return Math.abs(upperLeft.blockX() - lowerRight.blockX()) + Math.abs(upperLeft.blockZ() - lowerRight.blockZ());
    }

    public static @NotNull BlockPosition deserializePosition(final @NotNull Map<String, Object> serialized) throws IllegalArgumentException {
        if (serialized.get("x") instanceof Number x &&
            serialized.get("y") instanceof Number y &&
            serialized.get("z") instanceof Number z) {
            return Position.block(x.intValue(), y.intValue(), z.intValue());
        } else {
            throw new IllegalArgumentException(serialized + " is not a BlockPosition!");
        }
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

    public static <T> @NotNull Map<@NotNull String, @NotNull T> checkMap(final @NotNull Map<?, ?> input, final @NotNull Class<T> clazz) {
        final @NotNull Map<@NotNull String, @NotNull T> result = new HashMap<>(input.size());

        for (final @NotNull Map.Entry<?, ?> entry : input.entrySet()) {
            if (entry.getKey() instanceof String key && clazz.isInstance(entry.getValue())) {
                //noinspection unchecked
                result.put(key, (T)entry.getValue());
            }
        }

        return result;
    }

    public static @NotNull Map<@NotNull String, @NotNull Integer> serializePosition(final @NotNull Position position) {
        return Map.of(
            "x", position.blockX(),
            "y", position.blockY(),
            "z", position.blockZ()
        );
    }
}
