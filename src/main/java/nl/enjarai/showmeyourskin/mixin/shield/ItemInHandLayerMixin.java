package nl.enjarai.showmeyourskin.mixin.shield;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.enjarai.showmeyourskin.ShowMeYourSkinClient;
import nl.enjarai.showmeyourskin.mixin.ItemStackRenderStateAccessor;
import nl.enjarai.showmeyourskin.util.ArmorOpacitySubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
  @Inject(
    method = "submitArmWithItem",
    at = @At("HEAD"),
    cancellable = true
  )
  private void hideShield(ArmedEntityRenderState armedEntityRenderState, ItemStackRenderState itemStackRenderState,
                          ItemStack itemStack, HumanoidArm humanoidArm, PoseStack poseStack,
                          SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
    var config = armedEntityRenderState.getData(ShowMeYourSkinClient.ARMOR_CONFIG_KEY);
    if (config == null || !itemStack.is(Items.SHIELD)) {
      return;
    }

    if (!config.shield.base()) {
      ci.cancel();
    }

    if (!config.shield.glint() && !itemStackRenderState.isEmpty()) {
      var accessible = (ItemStackRenderStateAccessor) itemStackRenderState;

      for (int i = 0; i < accessible.getActiveLayerCount(); i++) {
        accessible.getLayers()[i].setFoilType(ItemStackRenderState.FoilType.NONE);
      }
    }
  }

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
                                  @Local(argsOnly = true) ArmedEntityRenderState renderState,
                                  @Local(argsOnly = true) ItemStack itemStack) {
    var config = renderState.getData(ShowMeYourSkinClient.ARMOR_CONFIG_KEY);
    if (config == null || !itemStack.is(Items.SHIELD) || config.shield.baseAlpha() >= 1) {
      original.call(itemState, poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
      return;
    }

    float alpha = config.shield.baseAlpha();
    if (alpha <= 0) {
      return;
    }
    original.call(itemState, poseStack,
      new ArmorOpacitySubmitNodeCollector(submitNodeCollector, alpha, true),
      lightCoords, overlayCoords, outlineColor);
  }
}
