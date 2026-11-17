package nl.enjarai.showmeyourskin.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import nl.enjarai.cicada.api.util.AbstractModConfig;
import nl.enjarai.showmeyourskin.Components;
import nl.enjarai.showmeyourskin.ShowMeYourSkin;
import nl.enjarai.showmeyourskin.ShowMeYourSkinClient;
import nl.enjarai.showmeyourskin.util.CombatLogger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

public class ModConfig extends AbstractModConfig {
  public static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve(ShowMeYourSkin.MODID + ".json");
  public static ModConfig INSTANCE = loadConfig();

  private static ModConfig loadConfig() {
    LegacyConfigMigration.migrateIfNeeded(CONFIG_FILE);
    return loadConfigFile(CONFIG_FILE, new ModConfig());
  }

  public boolean globalEnabled = true;
  public boolean overridesEnabledInServerMode = false;
  public float combatCooldown = 16;
  public float fadeOutTime = 2;
  public final ArmorConfig global = new ArmorConfig();
  public final HashMap<UUID, LocalOverride> overrides = new HashMap<>();


  public ArmorConfig getOverrideOrGlobal(UUID uuid) {
    var client = Minecraft.getInstance();

    boolean serverAvailable = ShowMeYourSkinClient.serverConfig != null && client.level != null;
    boolean useClientValues = overridesEnabledInServerMode || !serverAvailable;

    if (useClientValues) {
      var override = overrides.get(uuid);

      if (override != null) {
        return override.config();
      }
    }

    if (serverAvailable) {
      var player = client.level.getPlayerByUUID(uuid);

      if (player != null) {
        var config = player.getComponent(Components.ARMOR_CONFIG).getConfig();

        if (!config.equals(ArmorConfig.VANILLA_VALUES)) {
          return config;
        }
      }
    }

    if (useClientValues) {
      return global;
    }

    return ArmorConfig.VANILLA_VALUES;
  }

  public ArmorConfig getApplicable(UUID uuid) {
    if (!globalEnabled) {
      return ArmorConfig.VANILLA_VALUES;
    }

    var source = getOverrideOrGlobal(uuid);
    var applicable = source.copy();
    if (source.showInCombat && CombatLogger.INSTANCE.isInCombat(uuid)) {
      applicable.fadeToVanilla(CombatLogger.INSTANCE.getFade(uuid));
    }

    var level = Minecraft.getInstance().level;
    if (applicable.forceElytraWhenFlying && level != null) {
      var player = level.getPlayerByUUID(uuid);
      if (player != null && player.isFallFlying()) {
        applicable.elytra = ArmorConfig.PieceConfig.VANILLA_VALUES.copy();
      }
    }

    return applicable;
  }

  public ArmorConfig getOverride(UUID uuid) {
    var override = overrides.get(uuid);
    return override == null ? null : override.config();
  }

  public void deleteOverride(UUID uuid) {
    overrides.remove(uuid);
  }

  public void ensureValid() {
    if (combatCooldown < 0) {
      combatCooldown = 0;
    }
    if (fadeOutTime < 0) {
      fadeOutTime = 0;
    }
    if (fadeOutTime > combatCooldown) {
      fadeOutTime = combatCooldown;
    }

    global.ensureValid();
    // Remove incomplete entries from older or manually edited config files.
    overrides.entrySet().removeIf(entry -> {
      var value = entry.getValue();
      return value == null || value.config() == null || value.title() == null;
    });
    overrides.values().forEach(override -> override.config().ensureValid());

    save();
  }

  public record LocalOverride(String title, ArmorConfig config) {
  }
}
