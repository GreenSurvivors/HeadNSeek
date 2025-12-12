package de.greensurvivors.headnseek.paper.socialadapter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.greensurvivors.headnseek.paper.HeadNSeek;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SlackAdapter extends ASocialAdapter implements AutoCloseable {
    private final @NotNull Gson gson = new Gson();
    private final @NotNull HttpClient httpClient = HttpClient.newHttpClient();

    protected SlackAdapter(final @NotNull HeadNSeek plugin) {
        super(plugin);
    }

    @Override
    public void sendMessage(final @NotNull Component message) {
        final @Nullable URI uri = plugin.getConfigManager().getSocialAdapterUri();

        if (uri == null) {
            plugin.getComponentLogger().warn("Could not send message to slack!");
            plugin.getComponentLogger().debug(message);

            return;
        }

        final @NotNull HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri);

        // header
        requestBuilder.header("Content-Type", "application/json; charset=UTF-8");

        // body
        final @NotNull JsonObject bodyJson = new JsonObject();
        bodyJson.add("text", new JsonPrimitive(PlainTextComponentSerializer.plainText().serialize(message)));

        // build
        final HttpRequest request = requestBuilder.
            POST(HttpRequest.BodyPublishers.ofString(gson.toJson(bodyJson), StandardCharsets.UTF_8)).
            build();

        // blast off
        // for now all we do is notify, we don't need any response body whatever it may be
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding()).
            // quick check response code
            thenAccept(response -> {
                if (response.statusCode()  != HttpURLConnection.HTTP_OK) {
                    plugin.getComponentLogger().warn("Got unexpected status code when notifying Slack: {}", response.statusCode());
                }
            });
    }

    @Override
    public @NotNull ASocialAdapterType getTyp() {
        return ASocialAdapterType.SLACK;
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
