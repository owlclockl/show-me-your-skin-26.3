package nl.enjarai.showmeyourskin.mixin.compat.wildfiregender;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import nl.enjarai.showmeyourskin.compat.wildfiregender.WildfireGenderCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Breast physics: Female Gender Mod damps physics based on the worn chestplate.
 */
@Pseudo
@Mixin(
  targets = "com.wildfire.client.physics.BothBreastsPhysics",
  remap = false
)
public class BothBreastsPhysicsMixin {
  @ModifyExpressionValue(
    method = "tick",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"
    ),
    require = 0
  )
  private ItemStack hideChestplate(ItemStack original, @Local(argsOnly = true) LivingEntity entity) {
    return WildfireGenderCompat.visibleChestplate(original, entity);
  }
}
