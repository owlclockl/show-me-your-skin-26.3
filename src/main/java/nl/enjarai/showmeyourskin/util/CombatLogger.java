package nl.enjarai.showmeyourskin.util;

import net.minecraft.util.Mth;
import nl.enjarai.showmeyourskin.config.ModConfig;

import java.util.HashMap;
import java.util.UUID;

public class CombatLogger {
  public static final CombatLogger INSTANCE = new CombatLogger();

  private final HashMap<UUID, Long> inCombat = new HashMap<>();

  public boolean isInCombat(UUID uuid) {
    var lastTrigger = inCombat.get(uuid);
    if (lastTrigger != null && lastTrigger > System.currentTimeMillis() - (long) (ModConfig.INSTANCE.combatCooldown * 1000)) {
      return true;
    }
    inCombat.remove(uuid);
    return false;
  }

  public float getFade(UUID uuid) {
    var lastTrigger = inCombat.get(uuid);
    if (lastTrigger == null) {
      return 0;
    }

    float fadeOutTime = ModConfig.INSTANCE.fadeOutTime;
    if (fadeOutTime <= 0) {
      return 1;
    }

    var fadeStart = lastTrigger + (long) (ModConfig.INSTANCE.combatCooldown * 1000) - (long) (fadeOutTime * 1000);
    var timeSinceFadeStart = System.currentTimeMillis() - fadeStart;

    // Fade from fully visible to the saved opacity during the final fadeOutTime seconds of combat.
    return Mth.clamp(1 - (timeSinceFadeStart / 1000f) / fadeOutTime, 0, 1);
  }

  public void triggerCombat(UUID uuid) {
    inCombat.put(uuid, System.currentTimeMillis());
  }
}
