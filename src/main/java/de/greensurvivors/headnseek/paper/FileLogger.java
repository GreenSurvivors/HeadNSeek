package de.greensurvivors.headnseek.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

// note: decided to go with nio instead of any logger framework since it seemed to be way easier
public class FileLogger implements Closeable {
    protected final @NotNull Path pathToFile;
    // using date or time doesn't matter, when using a custom subformat
    protected final @NotNull MessageFormat format = new MessageFormat("[{0,time,dd.MM.yyyy HH:mm:ss}] {1}\n");
    protected final @NotNull HeadNSeek plugin;
    protected @Nullable AsynchronousFileChannel fileChannel;
    protected final @NotNull AtomicLong bytePosition = new AtomicLong(0);
    protected final @NotNull LoggingCompletionHandler completionHandler = new LoggingCompletionHandler();

    public FileLogger(final @NotNull HeadNSeek plugin) {
        this.plugin = plugin;

        pathToFile = plugin.getDataPath().resolve("foundHeads.log");
        try {
            fileChannel = AsynchronousFileChannel.open(pathToFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            bytePosition.set(fileChannel.size());

        } catch (final @NotNull IOException e) {
            plugin.getComponentLogger().error("Couldn't open log file!", e);

            // this line is the reason, why we can't use final. Why java? if an exception is thrown while opening the channel, nothing is written to the variable,
            // that's the reason you cry about, when this line is missing.
            fileChannel = null;
        }
    }

    public void log(final @NotNull Component message) {
        // thanks java for not allowing modern time api in formats
        // note: you also could do System.currentTimeMillis() or Instant.now().toEpochMilli() - just wanted to keep time unit attached for no reason at all
        final @NotNull String formattedLine = format.format(new Object[] {Date.from(Instant.now()), PlainTextComponentSerializer.plainText().serialize(message)});

        if (fileChannel != null) {
            fileChannel.write(ByteBuffer.wrap(formattedLine.getBytes(StandardCharsets.UTF_8)), bytePosition.get(), formattedLine, completionHandler);
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

    protected class LoggingCompletionHandler implements CompletionHandler<@NotNull Integer, @NotNull String> {
        @Override
        public void completed(final @NotNull Integer bytesWritten, final @Nullable String lineToLog) {
            bytePosition.getAndAdd(bytesWritten);
        }

        @Override
        public void failed(final @NotNull Throwable exc, final @NotNull String lineToLog) {
            plugin.getComponentLogger().error("Couldn't write \"" + lineToLog + "\" to log file!", exc);
        }
    }
}
