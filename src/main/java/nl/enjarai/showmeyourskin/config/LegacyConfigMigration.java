package nl.enjarai.showmeyourskin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;

/** Converts the 1.21.x config file to the 26.3 equipment-slot schema before CICADA loads it. */
final class LegacyConfigMigration {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final String[] ARMOR_SLOTS = {"head", "chest", "legs", "feet"};

  private LegacyConfigMigration() {
  }

  static void migrateIfNeeded(Path configFile) {
    if (!Files.isRegularFile(configFile)) {
      return;
    }

    JsonObject root;
    try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
      var parsed = JsonParser.parseReader(reader);
      if (!parsed.isJsonObject()) {
        return;
      }
      root = parsed.getAsJsonObject();
    } catch (JsonParseException e) {
      // Let CICADA's loader handle invalid JSON and make its usual .bak copy.
      return;
    } catch (IOException e) {
      throw new RuntimeException("Could not inspect Show Me Your Skin config before migration: " + configFile, e);
    }

    JsonObject oldGlobal = objectOrNull(root.get("global"));
    boolean migrateGlobal = isLegacyArmorConfig(oldGlobal);
    JsonObject oldOverrides = objectOrNull(root.get("overrides"));
    JsonObject migratedOverrides = convertOverrides(oldOverrides);
    boolean migrateOverrides = oldOverrides != null && !oldOverrides.equals(migratedOverrides);

    if (!migrateGlobal && !migrateOverrides) {
      return;
    }

    JsonObject migrated = new JsonObject();
    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
      if (!entry.getKey().equals("global") && !entry.getKey().equals("overrides")) {
        migrated.add(entry.getKey(), entry.getValue().deepCopy());
      }
    }
    if (root.has("global")) {
      migrated.add("global", migrateGlobal ? convertArmorConfig(oldGlobal) : root.get("global").deepCopy());
    }
    migrated.add("overrides", migratedOverrides);

    Path backup = configFile.resolveSibling(configFile.getFileName() + ".pre-26.3.bak");
    Path temporary = null;
    try {
      if (!Files.exists(backup)) {
        Files.copy(configFile, backup);
      }

      Path parent = configFile.toAbsolutePath().getParent();
      temporary = Files.createTempFile(parent, configFile.getFileName().toString(), ".tmp");
      Files.writeString(temporary, GSON.toJson(migrated), StandardCharsets.UTF_8);
      try {
        Files.move(temporary, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not migrate Show Me Your Skin config; original is kept at " + configFile, e);
    } finally {
      if (temporary != null) {
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
          // The temporary file is harmless and can be removed manually if cleanup fails.
        }
      }
    }
  }

  private static JsonObject convertOverrides(JsonObject oldOverrides) {
    var migrated = new JsonObject();
    if (oldOverrides == null) {
      return migrated;
    }

    for (Map.Entry<String, JsonElement> entry : oldOverrides.entrySet()) {
      JsonObject value = objectOrNull(entry.getValue());
      if (isLegacyArmorConfig(value)) {
        // The previous format stored ArmorConfig directly under the UUID. Keep the UUID as a
        // fallback title until the player is seen online and a display name is available.
        var localOverride = new JsonObject();
        localOverride.addProperty("title", entry.getKey());
        localOverride.add("config", convertArmorConfig(value));
        migrated.add(entry.getKey(), localOverride);
      } else if (value != null && isLegacyArmorConfig(objectOrNull(value.get("config")))) {
        // Also handle a partially migrated entry whose LocalOverride wrapper already exists.
        JsonObject localOverride = value.deepCopy();
        localOverride.add("config", convertArmorConfig(objectOrNull(value.get("config"))));
        migrated.add(entry.getKey(), localOverride);
      } else {
        // Preserve nulls and any new-format entries verbatim.
        migrated.add(entry.getKey(), entry.getValue().deepCopy());
      }
    }

    return migrated;
  }

  private static JsonObject convertArmorConfig(JsonObject oldConfig) {
    var migrated = new JsonObject();

    for (String slot : ARMOR_SLOTS) {
      int opacity = opacity(oldConfig, "pieces", slot, 100);
      // Base armor and trims were independent settings in the old format; missing trim entries
      // therefore use the old default of fully visible, not the base armor's opacity.
      int trimOpacity = opacity(oldConfig, "trims", slot, 100);
      int glintOpacity = opacity(oldConfig, "glints", slot, 100);

      var piece = new JsonObject();
      piece.addProperty("base", opacity > 0);
      piece.addProperty("glint", glintOpacity > 0);
      piece.addProperty("trim", trimOpacity > 0);
      piece.addProperty("opacity", opacity);
      piece.addProperty("trimOpacity", trimOpacity);
      migrated.add(slot, piece);
    }

    for (String equipment : new String[]{"elytra", "shield", "hat"}) {
      int opacity = opacity(oldConfig, "pieces", equipment, 100);
      int glintOpacity = opacity(oldConfig, "glints", equipment, 100);

      var piece = new JsonObject();
      piece.addProperty("base", opacity > 0);
      piece.addProperty("glint", glintOpacity > 0);
      piece.addProperty("opacity", opacity);
      migrated.add(equipment, piece);
    }

    migrated.addProperty("showInCombat", bool(oldConfig, "showInCombat", true));
    migrated.addProperty("showNameTag", bool(oldConfig, "showNameTag", true));
    migrated.addProperty("forceElytraWhenFlying", bool(oldConfig, "forceElytraWhenFlying", true));
    return migrated;
  }

  private static int opacity(JsonObject config, String mapName, String key, int fallback) {
    JsonObject values = objectOrNull(config.get(mapName));
    if (values == null) {
      return clamp(fallback);
    }

    JsonElement value = values.get(key);
    if (value == null) {
      value = values.get(key.toUpperCase(Locale.ROOT));
    }
    if (value == null || value.isJsonNull()) {
      return clamp(fallback);
    }

    if (value.isJsonObject()) {
      JsonElement transparency = value.getAsJsonObject().get("transparency");
      if (transparency == null || transparency.isJsonNull()) {
        return clamp(fallback);
      }
      value = transparency;
    }

    try {
      return clamp(value.getAsInt());
    } catch (UnsupportedOperationException | NumberFormatException e) {
      return clamp(fallback);
    }
  }

  private static boolean bool(JsonObject object, String key, boolean fallback) {
    JsonElement value = object.get(key);
    if (value == null || value.isJsonNull()) {
      return fallback;
    }
    try {
      return value.getAsBoolean();
    } catch (UnsupportedOperationException | ClassCastException e) {
      return fallback;
    }
  }

  private static boolean isLegacyArmorConfig(JsonObject config) {
    return config != null && config.has("pieces") && !config.has("head");
  }

  private static int clamp(int value) {
    return Math.max(0, Math.min(100, value));
  }

  private static JsonObject objectOrNull(JsonElement element) {
    return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
  }
}
