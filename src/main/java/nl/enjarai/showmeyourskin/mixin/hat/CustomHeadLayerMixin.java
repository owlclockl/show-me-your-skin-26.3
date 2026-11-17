package nl.enjarai.showmeyourskin.mixin.hat;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import nl.enjarai.showmeyourskin.ShowMeYourSkinClient;
import nl.enjarai.showmeyourskin.mixin.ItemStackRenderStateAccessor;
import nl.enjarai.showmeyourskin.util.ArmorOpacitySubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
  private static final String SUBMIT =
    "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
      + "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V";

  @Inject(method = SUBMIT, at = @At("HEAD"), cancellable = true)
  private void hideHat(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                      LivingEntityRenderState renderState, float yRot, float xRot, CallbackInfo ci) {
    var config = renderState.getData(ShowMeYourSkinClient.ARMOR_CONFIG_KEY);
    if (config == null) {
      return;
    }

    if (config.hat.baseAlpha() <= 0) {
      ci.cancel();
      return;
    }

    if (!config.hat.glint() && !renderState.headItem.isEmpty()) {
      var accessible = (ItemStackRenderStateAccessor) renderState.headItem;
      for (int i = 0; i < accessible.getActiveLayerCount(); i++) {
        accessible.getLayers()[i].setFoilType(ItemStackRenderState.FoilType.NONE);
      }
    }
  }

  @ModifyVariable(method = SUBMIT, at = @At("HEAD"), argsOnly = true, ordinal = 0)
  private SubmitNodeCollector applyHatOpacity(SubmitNodeCollector submitNodeCollector,
                                               @Local(argsOnly = true) LivingEntityRenderState renderState) {
    var config = renderState.getData(ShowMeYourSkinClient.ARMOR_CONFIG_KEY);
    if (config == null) {
      return submitNodeCollector;
    }

    float alpha = config.hat.baseAlpha();
    return alpha > 0 && alpha < 1
      ? new ArmorOpacitySubmitNodeCollector(submitNodeCollector, alpha, false)
      : submitNodeCollector;
  }
}
