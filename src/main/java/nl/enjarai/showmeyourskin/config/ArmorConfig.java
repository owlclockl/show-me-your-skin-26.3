package nl.enjarai.showmeyourskin.config;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Map;

public class ArmorConfig {
  public static final ArmorConfig VANILLA_VALUES = new ArmorConfig();
  private static final Codec<ArmorConfig> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
    ArmorPieceConfig.CODEC.fieldOf("head").forGetter(c -> c.head),
    ArmorPieceConfig.CODEC.fieldOf("chest").forGetter(c -> c.chest),
    ArmorPieceConfig.CODEC.fieldOf("legs").forGetter(c -> c.legs),
    ArmorPieceConfig.CODEC.fieldOf("feet").forGetter(c -> c.feet),
    PieceConfig.CODEC.fieldOf("elytra").forGetter(c -> c.elytra),
    PieceConfig.CODEC.fieldOf("shield").forGetter(c -> c.shield),
    PieceConfig.CODEC.fieldOf("hat").forGetter(c -> c.hat),
    Codec.BOOL.fieldOf("show_in_combat").forGetter(c -> c.showInCombat),
    Codec.BOOL.fieldOf("show_name_tag").forGetter(c -> c.showNameTag),
    Codec.BOOL.fieldOf("force_elytra_when_flying").forGetter(c -> c.forceElytraWhenFlying)
  ).apply(instance, ArmorConfig::new));
  private static final Codec<ArmorConfig> LEGACY_CODEC = LegacyArmorConfig.CODEC.xmap(LegacyArmorConfig::toCurrent, ArmorConfig::toLegacy);
  public static final Codec<ArmorConfig> CODEC = Codec.either(CURRENT_CODEC, LEGACY_CODEC).xmap(
    either -> either.map(config -> config, config -> config),
    config -> Either.left(config)
  );
  public static final StreamCodec<ByteBuf, ArmorConfig> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

  public ArmorPieceConfig head = new ArmorPieceConfig();
  public ArmorPieceConfig chest = new ArmorPieceConfig();
  public ArmorPieceConfig legs = new ArmorPieceConfig();
  public ArmorPieceConfig feet = new ArmorPieceConfig();
  public PieceConfig elytra = new PieceConfig();
  public PieceConfig shield = new PieceConfig();
  public PieceConfig hat = new PieceConfig();
  public boolean showInCombat = true;
  public boolean showNameTag = true;
  public boolean forceElytraWhenFlying = true;

  public ArmorConfig() {
  }

  public ArmorConfig(ArmorPieceConfig head, ArmorPieceConfig chest, ArmorPieceConfig legs, ArmorPieceConfig feet,
                     PieceConfig elytra, PieceConfig shield, PieceConfig hat, boolean showInCombat,
                     boolean showNameTag, boolean forceElytraWhenFlying) {
    this.head = head;
    this.chest = chest;
    this.legs = legs;
    this.feet = feet;
    this.elytra = elytra;
    this.shield = shield;
    this.hat = hat;
    this.showInCombat = showInCombat;
    this.showNameTag = showNameTag;
    this.forceElytraWhenFlying = forceElytraWhenFlying;
  }

  public ArmorPieceConfig getEquipmentConfig(EquipmentSlot slot) {
    return switch (slot) {
      case HEAD -> head;
      case CHEST -> chest;
      case LEGS -> legs;
      case FEET -> feet;
      default -> throw new IllegalArgumentException("Unexpected value: " + slot);
    };
  }

  public void ensureValid() {
    head.ensureValid();
    chest.ensureValid();
    legs.ensureValid();
    feet.ensureValid();
    elytra.ensureValid();
    shield.ensureValid();
    hat.ensureValid();
  }

  /**
   * During the combat fade, blend each configured opacity back toward vanilla's fully visible value.
   */
  public void fadeToVanilla(float fade) {
    head.fadeToVanilla(fade);
    chest.fadeToVanilla(fade);
    legs.fadeToVanilla(fade);
    feet.fadeToVanilla(fade);
    elytra.fadeToVanilla(fade);
    shield.fadeToVanilla(fade);
    hat.fadeToVanilla(fade);

    if (fade > 0) {
      showNameTag = true;
      forceElytraWhenFlying = true;
    }
  }

  public ArmorConfig copy() {
    return new ArmorConfig(head.copy(), chest.copy(), legs.copy(), feet.copy(), elytra.copy(), shield.copy(), hat.copy(),
      showInCombat, showNameTag, forceElytraWhenFlying);
  }

  public void clear() {
    head = new ArmorPieceConfig();
    chest = new ArmorPieceConfig();
    legs = new ArmorPieceConfig();
    feet = new ArmorPieceConfig();
    elytra = new PieceConfig();
    shield = new PieceConfig();
    hat = new PieceConfig();
    showInCombat = true;
    showNameTag = true;
    forceElytraWhenFlying = true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArmorConfig that)) return false;

    return showInCombat == that.showInCombat && showNameTag == that.showNameTag &&
      forceElytraWhenFlying == that.forceElytraWhenFlying && head.equals(that.head) &&
      chest.equals(that.chest) && legs.equals(that.legs) && feet.equals(that.feet) &&
      elytra.equals(that.elytra) && shield.equals(that.shield) && hat.equals(that.hat);
  }

  @Override
  public int hashCode() {
    int result = head.hashCode();
    result = 31 * result + chest.hashCode();
    result = 31 * result + legs.hashCode();
    result = 31 * result + feet.hashCode();
    result = 31 * result + elytra.hashCode();
    result = 31 * result + shield.hashCode();
    result = 31 * result + hat.hashCode();
    result = 31 * result + Boolean.hashCode(showInCombat);
    result = 31 * result + Boolean.hashCode(showNameTag);
    result = 31 * result + Boolean.hashCode(forceElytraWhenFlying);
    return result;
  }

  private LegacyArmorConfig toLegacy() {
    return new LegacyArmorConfig(showInCombat, showNameTag, forceElytraWhenFlying,
      Map.of(
        "head", new LegacyOpacity(head.opacity),
        "chest", new LegacyOpacity(chest.opacity),
        "legs", new LegacyOpacity(legs.opacity),
        "feet", new LegacyOpacity(feet.opacity),
        "elytra", new LegacyOpacity(elytra.opacity),
        "shield", new LegacyOpacity(shield.opacity),
        "hat", new LegacyOpacity(hat.opacity)
      ),
      Map.of(
        "head", new LegacyOpacity(head.trimOpacity),
        "chest", new LegacyOpacity(chest.trimOpacity),
        "legs", new LegacyOpacity(legs.trimOpacity),
        "feet", new LegacyOpacity(feet.trimOpacity)
      ),
      Map.of(
        "head", new LegacyOpacity(head.glint ? 100 : 0),
        "chest", new LegacyOpacity(chest.glint ? 100 : 0),
        "legs", new LegacyOpacity(legs.glint ? 100 : 0),
        "feet", new LegacyOpacity(feet.glint ? 100 : 0),
        "elytra", new LegacyOpacity(elytra.glint ? 100 : 0),
        "shield", new LegacyOpacity(shield.glint ? 100 : 0),
        "hat", new LegacyOpacity(hat.glint ? 100 : 0)
      ));
  }

  private static int clampOpacity(int opacity) {
    return Math.max(0, Math.min(100, opacity));
  }

  /**
   * Decoder for persisted component data from the pre-26.3 format, which stored three maps of
   * byte-valued opacity settings instead of one config object per equipment slot.
   */
  private record LegacyArmorConfig(boolean showInCombat, boolean showNameTag, boolean forceElytraWhenFlying,
                                   Map<String, LegacyOpacity> pieces, Map<String, LegacyOpacity> trims,
                                   Map<String, LegacyOpacity> glints) {
    private static final Codec<LegacyArmorConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.BOOL.optionalFieldOf("showInCombat", true).forGetter(LegacyArmorConfig::showInCombat),
      Codec.BOOL.optionalFieldOf("showNameTag", true).forGetter(LegacyArmorConfig::showNameTag),
      Codec.BOOL.optionalFieldOf("forceElytraWhenFlying", true).forGetter(LegacyArmorConfig::forceElytraWhenFlying),
      Codec.unboundedMap(Codec.STRING, LegacyOpacity.CODEC).optionalFieldOf("pieces", Map.of()).forGetter(LegacyArmorConfig::pieces),
      Codec.unboundedMap(Codec.STRING, LegacyOpacity.CODEC).optionalFieldOf("trims", Map.of()).forGetter(LegacyArmorConfig::trims),
      Codec.unboundedMap(Codec.STRING, LegacyOpacity.CODEC).optionalFieldOf("glints", Map.of()).forGetter(LegacyArmorConfig::glints)
    ).apply(instance, LegacyArmorConfig::new));

    private ArmorConfig toCurrent() {
      var headOpacity = readOpacity(pieces, "head", 100);
      var chestOpacity = readOpacity(pieces, "chest", 100);
      var legsOpacity = readOpacity(pieces, "legs", 100);
      var feetOpacity = readOpacity(pieces, "feet", 100);
      var config = new ArmorConfig(
        armorPiece(headOpacity, readOpacity(trims, "head", 100), readOpacity(glints, "head", 100)),
        armorPiece(chestOpacity, readOpacity(trims, "chest", 100), readOpacity(glints, "chest", 100)),
        armorPiece(legsOpacity, readOpacity(trims, "legs", 100), readOpacity(glints, "legs", 100)),
        armorPiece(feetOpacity, readOpacity(trims, "feet", 100), readOpacity(glints, "feet", 100)),
        piece(readOpacity(pieces, "elytra", 100), readOpacity(glints, "elytra", 100)),
        piece(readOpacity(pieces, "shield", 100), readOpacity(glints, "shield", 100)),
        piece(readOpacity(pieces, "hat", 100), readOpacity(glints, "hat", 100)),
        showInCombat, showNameTag, forceElytraWhenFlying
      );
      config.ensureValid();
      return config;
    }

    private static ArmorPieceConfig armorPiece(int opacity, int trimOpacity, int glintOpacity) {
      return new ArmorPieceConfig(opacity > 0, glintOpacity > 0, trimOpacity > 0, opacity, trimOpacity);
    }

    private static PieceConfig piece(int opacity, int glintOpacity) {
      return new PieceConfig(opacity > 0, glintOpacity > 0, opacity);
    }

    private static int readOpacity(Map<String, LegacyOpacity> values, String id, int fallback) {
      var value = values.get(id);
      return clampOpacity(value == null ? fallback : value.transparency());
    }
  }

  private record LegacyOpacity(int transparency) {
    private static final Codec<LegacyOpacity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("transparency").forGetter(LegacyOpacity::transparency)
    ).apply(instance, LegacyOpacity::new));
  }

  private static int fadeOpacity(float opacity, float fade) {
    float clampedFade = Math.max(0, Math.min(1, fade));
    float clampedOpacity = Math.max(0, Math.min(1, opacity));
    return clampOpacity(Math.round((clampedOpacity + (1 - clampedOpacity) * clampedFade) * 100));
  }

  public static class ArmorPieceConfig extends PieceConfig {
    public static final Codec<ArmorPieceConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.BOOL.fieldOf("base").forGetter(c -> c.base),
      Codec.BOOL.fieldOf("glint").forGetter(c -> c.glint),
      Codec.BOOL.fieldOf("trim").forGetter(c -> c.trim),
      Codec.INT.optionalFieldOf("opacity", 100).forGetter(c -> c.opacity),
      Codec.INT.optionalFieldOf("trim_opacity", 100).forGetter(c -> c.trimOpacity)
    ).apply(instance, ArmorPieceConfig::new));
    public static final ArmorPieceConfig VANILLA_VALUES = new ArmorPieceConfig();

    public boolean trim = true;
    public int trimOpacity = 100;

    public ArmorPieceConfig(boolean base, boolean glint, boolean trim, int opacity, int trimOpacity) {
      super(base, glint, opacity);
      this.trim = trim;
      this.trimOpacity = trimOpacity;
    }

    public ArmorPieceConfig(boolean base, boolean glint, boolean trim) {
      this(base, glint, trim, 100, 100);
    }

    public ArmorPieceConfig() {
    }

    @Override
    public void setAll(boolean value) {
      super.setAll(value);
      this.trim = value;
      if (value && trimOpacity == 0) {
        trimOpacity = 100;
      }
    }

    public void setTrim(boolean value) {
      this.trim = value;
      if (value && trimOpacity == 0) {
        trimOpacity = 100;
      }
    }

    public void setTrimOpacity(int trimOpacity) {
      this.trimOpacity = clampOpacity(trimOpacity);
      this.trim = this.trimOpacity > 0;
    }

    public float trimAlpha() {
      return trim ? trimOpacity / 100f : 0f;
    }

    @Override
    public boolean trim() {
      return trim && trimOpacity > 0;
    }

    @Override
    public void ensureValid() {
      super.ensureValid();
      trimOpacity = clampOpacity(trimOpacity);
      if (trimOpacity == 0) {
        trim = false;
      }
    }

    @Override
    public void fadeToVanilla(float fade) {
      super.fadeToVanilla(fade);
      setTrimOpacity(fadeOpacity(trimAlpha(), fade));
      if (fade > 0) {
        glint = true;
      }
    }

    @Override
    public ArmorPieceConfig copy() {
      return new ArmorPieceConfig(base, glint, trim, opacity, trimOpacity);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ArmorPieceConfig that)) return false;

      return base == that.base && glint == that.glint && trim == that.trim &&
        opacity == that.opacity && trimOpacity == that.trimOpacity;
    }

    @Override
    public int hashCode() {
      int result = Boolean.hashCode(base);
      result = 31 * result + Boolean.hashCode(glint);
      result = 31 * result + Boolean.hashCode(trim);
      result = 31 * result + opacity;
      result = 31 * result + trimOpacity;
      return result;
    }
  }

  public static class PieceConfig {
    public static final Codec<PieceConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.BOOL.fieldOf("base").forGetter(c -> c.base),
      Codec.BOOL.fieldOf("glint").forGetter(c -> c.glint),
      Codec.INT.optionalFieldOf("opacity", 100).forGetter(c -> c.opacity)
    ).apply(instance, PieceConfig::new));
    public static final PieceConfig VANILLA_VALUES = new PieceConfig();

    public boolean base = true;
    public boolean glint = true;
    public int opacity = 100;

    public PieceConfig(boolean base, boolean glint, int opacity) {
      this.base = base;
      this.glint = glint;
      this.opacity = opacity;
    }

    public PieceConfig(boolean base, boolean glint) {
      this(base, glint, 100);
    }

    public PieceConfig() {
    }

    public void setAll(boolean value) {
      this.base = value;
      this.glint = value;
      if (value && opacity == 0) {
        opacity = 100;
      }
    }

    public void setBase(boolean value) {
      this.base = value;
      if (value && opacity == 0) {
        opacity = 100;
      }
    }

    public void setOpacity(int opacity) {
      this.opacity = clampOpacity(opacity);
      this.base = this.opacity > 0;
    }

    public float baseAlpha() {
      return base ? opacity / 100f : 0f;
    }

    public boolean base() {
      return base && opacity > 0;
    }

    public boolean trim() {
      return true;
    }

    public boolean glint() {
      return glint;
    }

    public void ensureValid() {
      opacity = clampOpacity(opacity);
      if (opacity == 0) {
        base = false;
      }
    }

    public void fadeToVanilla(float fade) {
      setOpacity(fadeOpacity(baseAlpha(), fade));
      if (fade > 0) {
        glint = true;
      }
    }

    public PieceConfig copy() {
      return new PieceConfig(base, glint, opacity);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof PieceConfig that)) return false;

      return base == that.base && glint == that.glint && opacity == that.opacity;
    }

    @Override
    public int hashCode() {
      int result = Boolean.hashCode(base);
      result = 31 * result + Boolean.hashCode(glint);
      result = 31 * result + opacity;
      return result;
    }
  }
}
