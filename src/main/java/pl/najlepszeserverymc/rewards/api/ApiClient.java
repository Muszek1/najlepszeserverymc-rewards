package pl.najlepszeserverymc.rewards.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import pl.najlepszeserverymc.rewards.RewardsPlugin;

public final class ApiClient {

  private final RewardsPlugin plugin;
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  public ApiClient(RewardsPlugin plugin) {
    this.plugin = plugin;
  }

  public List<PendingVote> fetchPending() throws IOException, InterruptedException {
    var config = plugin.pluginConfig();
    URI uri = URI.create(config.apiUrl() + "/votes/pending?limit=" + config.batchSize());
    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(15))
        .header("Authorization", "Bearer " + config.token())
        .header("Accept", "application/json")
        .header("User-Agent", "NajlepszeServeryMC-Rewards/" + plugin.getPluginMeta().getVersion())
        .GET()
        .build();

    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 401) {
      throw new IOException("Token odrzucony (401). Wygeneruj nowy w panelu strony.");
    }
    if (response.statusCode() == 429) {
      throw new IOException("Rate limit API (429). Zwiększ poll-interval-seconds.");
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("HTTP " + response.statusCode() + ": " + truncate(response.body()));
    }

    JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
    if (!root.has("ok") || !root.get("ok").getAsBoolean()) {
      String error = root.has("error") ? root.get("error").getAsString() : "nieznany błąd";
      throw new IOException("API: " + error);
    }

    List<PendingVote> votes = new ArrayList<>();
    JsonArray array = root.has("votes") && root.get("votes").isJsonArray()
        ? root.getAsJsonArray("votes")
        : new JsonArray();
    for (JsonElement element : array) {
      if (!element.isJsonObject()) {
        continue;
      }
      JsonObject row = element.getAsJsonObject();
      String id = text(row, "id");
      String username = text(row, "username");
      if (id.isEmpty() || username.isEmpty()) {
        continue;
      }
      votes.add(new PendingVote(id, username, text(row, "createdAt")));
    }
    return votes;
  }

  public int claim(Collection<String> ids) throws IOException, InterruptedException {
    if (ids.isEmpty()) {
      return 0;
    }

    JsonArray array = new JsonArray();
    for (String id : ids) {
      array.add(id);
    }
    JsonObject body = new JsonObject();
    body.add("ids", array);

    URI uri = URI.create(plugin.pluginConfig().apiUrl() + "/votes/claim");
    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(15))
        .header("Authorization", "Bearer " + plugin.pluginConfig().token())
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .header("User-Agent", "NajlepszeServeryMC-Rewards/" + plugin.getPluginMeta().getVersion())
        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
        .build();

    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("Claim HTTP " + response.statusCode() + ": " + truncate(response.body()));
    }

    JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
    if (!root.has("ok") || !root.get("ok").getAsBoolean()) {
      String error = root.has("error") ? root.get("error").getAsString() : "nieznany błąd";
      throw new IOException("Claim API: " + error);
    }
    return root.has("claimed") ? root.get("claimed").getAsInt() : ids.size();
  }

  public void logQuietFailure(String context, Exception exception) {
    plugin.getLogger().log(Level.WARNING, context + ": " + exception.getMessage());
  }

  private static String text(JsonObject object, String key) {
    if (!object.has(key) || object.get(key).isJsonNull()) {
      return "";
    }
    return object.get(key).getAsString().trim();
  }

  private static String truncate(String value) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    return trimmed.length() > 180 ? trimmed.substring(0, 180) + "…" : trimmed;
  }
}
