package nl.enjarai.showmeyourskin.mixin.hat;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.Equippable;
import nl.enjarai.showmeyourskin.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Hud.class)
public class HudMixin {
  @ModifyArg(
    method = "extractCameraOverlays",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/gui/Hud;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V",
      ordinal = 0
    ),
    index = 2
  )
  private float applyHeadEquipmentOpacity(float alpha, @Local Equippable equippable) {
    if (equippable.slot() != EquipmentSlot.HEAD) {
      return alpha;
    }

    var player = Minecraft.getInstance().player;
    if (player == null) {
      return alpha;
    }
    return alpha * ModConfig.INSTANCE.getApplicable(player.getUUID()).hat.baseAlpha();
  }
}
