package de.greensurvivors.headnseek.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// note: decided to go with nio instead of any logger framework since it seemed to be way easier
public class FileLogger implements Closeable {
    protected final @NotNull Path pathToFile;
    // using date or time doesn't matter, when using a custom subformat
    protected final @NotNull MessageFormat format = new MessageFormat("[{0,time,dd.MM.yyyy HH:mm:ss}] {1}\n");
    protected final @NotNull HeadNSeek plugin;
    protected @Nullable AsynchronousFileChannel fileChannel;

    public FileLogger(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;

        pathToFile = plugin.getDataPath().resolve("foundHeads.log");
        try {
            fileChannel = AsynchronousFileChannel.open(pathToFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final @NotNull IOException e) {
            plugin.getComponentLogger().error("Couldn't open log file!", e);

            // this line is the reason, why we can't use final. Why java? if an exception is thrown while opening the channel, nothing is written to the variable,
            // that's the reason you cry about, when this line is missing.
            fileChannel = null;
        }
    }

    public void log(final @NotNull Component message) {
        final @NotNull String formattedLine = format.format(new Object[] {Instant.now(), PlainTextComponentSerializer.plainText().serialize(message)});

        if (fileChannel != null) {
            try {
                fileChannel.write(ByteBuffer.wrap(formattedLine.getBytes(StandardCharsets.UTF_8)), 0).get(1, TimeUnit.MINUTES);
            } catch (final @NotNull InterruptedException | ExecutionException | TimeoutException e) {
                plugin.getComponentLogger().error("Could not log line \"{}\" to file {}", message, pathToFile, e);
            }
        } else {
            plugin.getComponentLogger().warn(formattedLine);
        }
    }

    public void close() {
        if (fileChannel != null) {
            try {
                fileChannel.close();
            } catch (final @NotNull IOException e) {
                plugin.getComponentLogger().error("Could not close log file writer for file {}", pathToFile, e);
            }
        }
    }
}
