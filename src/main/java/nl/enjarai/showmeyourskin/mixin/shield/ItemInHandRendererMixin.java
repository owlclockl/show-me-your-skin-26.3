package nl.enjarai.showmeyourskin.mixin.shield;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.enjarai.showmeyourskin.config.ModConfig;
import nl.enjarai.showmeyourskin.mixin.ItemStackRenderStateAccessor;
import nl.enjarai.showmeyourskin.util.ArmorOpacitySubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class ItemInHandRendererMixin {
  @WrapOperation(
    method = "submitArmWithItem",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
    )
  )
  private void applyShieldOpacity(ItemStackRenderState itemState, PoseStack poseStack,
                                  SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords,
                                  int outlineColor, Operation<Void> original,
                                  @Local(argsOnly = true) ItemStack itemStack) {
    var player = Minecraft.getInstance().player;
    if (!itemStack.is(Items.SHIELD) || player == null) {
      original.call(itemState, poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
      return;
    }

    var config = ModConfig.INSTANCE.getApplicable(player.getUUID());
    if (!config.shield.glint()) {
      var accessible = (ItemStackRenderStateAccessor) itemState;
      for (int i = 0; i < accessible.getActiveLayerCount(); i++) {
        accessible.getLayers()[i].setFoilType(ItemStackRenderState.FoilType.NONE);
      }
    }

    if (!config.shield.base()) {
      return;
    }

    float alpha = config.shield.baseAlpha();
    if (alpha >= 1) {
      original.call(itemState, poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
    } else if (alpha > 0) {
      original.call(itemState, poseStack,
        new ArmorOpacitySubmitNodeCollector(submitNodeCollector, alpha, true),
        lightCoords, overlayCoords, outlineColor);
    }
  }
}
