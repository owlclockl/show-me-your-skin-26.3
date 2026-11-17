package nl.enjarai.showmeyourskin.mixin.armor;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import nl.enjarai.showmeyourskin.ShowMeYourSkinClient;
import nl.enjarai.showmeyourskin.config.ArmorConfig;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {
  private static final String RENDER_LAYERS =
    "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;"
      + "Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;"
      + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I"
      + "Lnet/minecraft/resources/Identifier;II)V";
  private static final String SUBMIT_MODEL =
    "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;"
      + "Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;"
      + "Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V";

  @Inject(method = RENDER_LAYERS, at = @At("HEAD"))
  private void loadConfig(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> resourceKey,
                          Model<?> model, Object object, ItemStack itemStack, PoseStack poseStack,
                          SubmitNodeCollector submitNodeCollector, int light, @Nullable Identifier identifier,
                          int outlineColor, int order, CallbackInfo ci,
                          @Share("armor_config") LocalRef<ArmorConfig.PieceConfig> armorConfig) {
    if (!(object instanceof LivingEntityRenderState renderState)) {
      return;
    }

    var config = renderState.getData(ShowMeYourSkinClient.ARMOR_CONFIG_KEY);
    if (config == null) {
      return;
    }

    var equippable = itemStack.get(DataComponents.EQUIPPABLE);
    if (equippable == null) {
      return;
    }

    if (itemStack.is(Items.ELYTRA)) {
      armorConfig.set(config.elytra);
      return;
    }

    if (itemStack.is(ItemTags.TRIMMABLE_ARMOR)) {
      armorConfig.set(switch (equippable.slot()) {
        case FEET -> config.feet;
        case LEGS -> config.legs;
        case CHEST -> config.chest;
        case HEAD -> config.head;
        default -> null;
      });
      return;
    }

    if (equippable.slot() == EquipmentSlot.HEAD) {
      armorConfig.set(config.hat);
    }
  }

  @WrapWithCondition(method = RENDER_LAYERS, at = @At(value = "INVOKE", target = SUBMIT_MODEL, ordinal = 0))
  private boolean hideBase(OrderedSubmitNodeCollector instance, Model<?> model, Object state, PoseStack poseStack,
                           RenderType renderType, int light, int overlay, int color, @Nullable UvMapping uvMapping,
                           int order, @Share("armor_config") LocalRef<ArmorConfig.PieceConfig> armorConfig) {
    var config = armorConfig.get();
    return config == null || config.baseAlpha() > 0;
  }

  @WrapWithCondition(method = RENDER_LAYERS, at = @At(value = "INVOKE", target = SUBMIT_MODEL, ordinal = 1))
  private boolean hideTrim(OrderedSubmitNodeCollector instance, Model<?> model, Object state, PoseStack poseStack,
                           RenderType renderType, int light, int overlay, int color, @Nullable UvMapping uvMapping,
                           int order, @Share("armor_config") LocalRef<ArmorConfig.PieceConfig> armorConfig) {
    var config = armorConfig.get();
    return config == null || config.trim() && (!(config instanceof ArmorConfig.ArmorPieceConfig armorConfigPiece) || armorConfigPiece.trimAlpha() > 0);
  }

  @ModifyExpressionValue(method = RENDER_LAYERS, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z"))
  private boolean hideGlint(boolean original,
                            @Share("armor_config") LocalRef<ArmorConfig.PieceConfig> armorConfig) {
    var config = armorConfig.get();
    return original && (config == null || config.glint());
  }

  @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
  private RenderType useTranslucentBaseType(Identifier texture, Operation<RenderType> original,
                                             @Share("armor_config") LocalRef<ArmorConfig.PieceConfig> armorConfig) {
    var config = armorConfig.get();
    return config != null && config.baseAlpha() > 0 && config.baseAlpha() < 1
      ? RenderTypes.entityTranslucent(texture)
      : original.call(texture);
  }

  // In 26.3 the foil is baked into a separate armor render type. Its translucent replacement has no foil pass.
  @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCullGlint(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
  private RenderType useTranslucentGlintType(Identifier texture, Operation<RenderType> original,
                                              @Share("armor_config") LocalRef<ArmorConfig.PieceConfig> armorConfig) {
    var config = armorConfig.get();
    return config != null && config.baseAlpha() > 0 && config.baseAlpha() < 1
      ? RenderTypes.entityTranslucent(texture)
      : original.call(texture);
  }

  @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorTrim(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
  private RenderType useTranslucentTrimType(Identifier texture, boolean decal, Operation<RenderType> original,
                                              @Share("armor_config") LocalRef<ArmorConfig.PieceConfig> armorConfig) {
    var config = armorConfig.get();
    if (config instanceof ArmorConfig.ArmorPieceConfig armorConfigPiece &&
      armorConfigPiece.trimAlpha() > 0 && armorConfigPiece.trimAlpha() < 1) {
      return RenderTypes.entityTranslucent(texture);
    }
    return original.call(texture, decal);
  }

  @ModifyArg(method = RENDER_LAYERS, index = 6,
    at = @At(value = "INVOKE", target = SUBMIT_MODEL, ordinal = 0))
  private int applyBaseOpacity(int color,
                               @Share("armor_config") LocalRef<ArmorConfig.PieceConfig> armorConfig) {
    var config = armorConfig.get();
    return config == null ? color : multiplyAlpha(color, config.baseAlpha());
  }

  @ModifyArg(method = RENDER_LAYERS, index = 6,
    at = @At(value = "INVOKE", target = SUBMIT_MODEL, ordinal = 1))
  private int applyTrimOpacity(int color,
                               @Share("armor_config") LocalRef<ArmorConfig.PieceConfig> armorConfig) {
    var config = armorConfig.get();
    if (config instanceof ArmorConfig.ArmorPieceConfig armorConfigPiece) {
      return multiplyAlpha(color, armorConfigPiece.trimAlpha());
    }
    return color;
  }

  private static int multiplyAlpha(int color, float opacity) {
    if (opacity >= 1) {
      return color;
    }

    int alpha = Math.round((color >>> 24) * Math.max(0, opacity));
    return color & 0x00FFFFFF | alpha << 24;
  }
}
