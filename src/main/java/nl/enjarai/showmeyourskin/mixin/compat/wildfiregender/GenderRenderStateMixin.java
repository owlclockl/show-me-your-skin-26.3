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
 * Breast shape: Female Gender Mod picks the armored shape from the render state's chest slot.
 */
@Pseudo
@Mixin(
  targets = "com.wildfire.client.render.GenderRenderState",
  remap = false
)
public class GenderRenderStateMixin {
  @ModifyExpressionValue(
    method = "<init>",
    at = @At(
      value = "FIELD",
      target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;chestEquipment:Lnet/minecraft/world/item/ItemStack;"
    ),
    require = 0
  )
  private ItemStack hideChestplate(ItemStack original, @Local(argsOnly = true) LivingEntity entity) {
    return WildfireGenderCompat.visibleChestplate(original, entity);
  }
}
