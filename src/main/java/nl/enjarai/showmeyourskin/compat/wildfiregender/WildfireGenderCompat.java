package nl.enjarai.showmeyourskin.compat.wildfiregender;

import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import nl.enjarai.showmeyourskin.config.ModConfig;
import nl.enjarai.showmeyourskin.pond.ArmorConfigContraband;

/**
 * Female Gender Mod changes breast shape and physics when a chestplate is worn.
 * When the chestplate is fully hidden, we tell it the chest slot is empty so it behaves as if nothing is worn.
 * <p>
 * The breast armor itself needs nothing extra: Female Gender Mod draws it through vanilla's
 * EquipmentLayerRenderer, which our armor mixin already handles.
 */
public final class WildfireGenderCompat {
  private WildfireGenderCompat() {
  }

  public static ItemStack visibleChestplate(ItemStack chestplate, LivingEntity entity) {
    if (!(entity instanceof Avatar)) {
      return chestplate;
    }

    var config = entity instanceof ArmorConfigContraband contraband && contraband.show_me_your_skin$getArmorConfig() != null ?
      contraband.show_me_your_skin$getArmorConfig() :
      ModConfig.INSTANCE.getApplicable(entity.getUUID());

    // Keep the armored shape whenever any chestplate or trim pixels remain visible, including partial alpha.
    return config.chest.baseAlpha() > 0 || config.chest.trimAlpha() > 0 ? chestplate : ItemStack.EMPTY;
  }
}
