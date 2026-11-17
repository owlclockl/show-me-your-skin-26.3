package nl.enjarai.showmeyourskin.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Applies a per-piece alpha to model and item submissions made while rendering a hat or shield. */
public final class ArmorOpacitySubmitNodeCollector extends ArmorOpacitySubmitNodeCollectorBase
  implements SubmitNodeCollector {

  public ArmorOpacitySubmitNodeCollector(SubmitNodeCollector delegate, float alpha, boolean cull) {
    super(delegate, alpha, cull);
  }

  @Override
  public OrderedSubmitNodeCollector order(int order) {
    return new OrderedArmorOpacitySubmitNodeCollector(((SubmitNodeCollector) delegate).order(order), alpha, cull);
  }
}

/** Ordered view used when a renderer submits a feature with a local ordering offset. */
final class OrderedArmorOpacitySubmitNodeCollector extends ArmorOpacitySubmitNodeCollectorBase
  implements OrderedSubmitNodeCollector {

  OrderedArmorOpacitySubmitNodeCollector(OrderedSubmitNodeCollector delegate, float alpha, boolean cull) {
    super(delegate, alpha, cull);
  }
}

abstract class ArmorOpacitySubmitNodeCollectorBase implements OrderedSubmitNodeCollector {
  protected final OrderedSubmitNodeCollector delegate;
  protected final float alpha;
  protected final boolean cull;

  ArmorOpacitySubmitNodeCollectorBase(OrderedSubmitNodeCollector delegate, float alpha, boolean cull) {
    this.delegate = delegate;
    this.alpha = Math.max(0, Math.min(1, alpha));
    this.cull = cull;
  }

  @Override
  public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                              int lightCoords, int overlayCoords, int tintedColor,
                              @Nullable UvMapping uvMapping, int outlineColor) {
    RenderType translucentType = translucentModelType(renderType, uvMapping);
    delegate.submitModel(model, state, poseStack, translucentType, lightCoords, overlayCoords,
      ARGB.multiplyAlpha(tintedColor, alpha), uvMapping, ARGB.multiplyAlpha(outlineColor, alpha));
  }

  @Override
  public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, Identifier texture,
                              int lightCoords, int overlayCoords, int outlineColor) {
    RenderType renderType = cull ? RenderTypes.entityTranslucentCull(texture)
      : RenderTypes.entityTranslucent(texture);
    delegate.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords,
      ARGB.multiplyAlpha(0xFFFFFFFF, alpha), null, ARGB.multiplyAlpha(outlineColor, alpha));
  }

  @Override
  public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords,
                             int overlayCoords, @Nullable UvMapping uvMapping) {
    submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, uvMapping, 0xFFFFFFFF, 0);
  }

  @Override
  public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords,
                             int overlayCoords, @Nullable UvMapping uvMapping, int tintedColor) {
    submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, uvMapping, tintedColor, 0);
  }

  @Override
  public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords,
                             int overlayCoords, @Nullable UvMapping uvMapping, int tintedColor, int outlineColor) {
    delegate.submitModelPart(modelPart, poseStack, translucentModelType(renderType, uvMapping), lightCoords,
      overlayCoords, uvMapping, ARGB.multiplyAlpha(tintedColor, alpha), ARGB.multiplyAlpha(outlineColor, alpha));
  }

  @Override
  public void submitCrumblingOverlay(ModelPart modelPart, PoseStack poseStack, RenderType renderType,
                                     int lightCoords, int overlayCoords, int tintedColor,
                                     ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
    delegate.submitCrumblingOverlay(modelPart, poseStack, translucentModelType(renderType, null), lightCoords,
      overlayCoords, ARGB.multiplyAlpha(tintedColor, alpha), crumblingOverlay);
  }

  @Override
  public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack,
                                         RenderType renderType, int lightCoords, int overlayCoords,
                                         int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
    delegate.submitCrumblingOverlay(model, state, poseStack, translucentModelType(renderType, null), lightCoords,
      overlayCoords, ARGB.multiplyAlpha(tintedColor, alpha), crumblingOverlay);
  }

  @Override
  public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
    delegate.submitShadow(poseStack, radius, pieces);
  }

  @Override
  public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name,
                            boolean seeThrough, int lightCoords, CameraRenderState camera) {
    delegate.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, camera);
  }

  @Override
  public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow,
                         Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor,
                         int outlineColor) {
    delegate.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor,
      outlineColor);
  }

  @Override
  public void submitTextBackground(PoseStack poseStack, float minX, float minY, float maxX, float maxY,
                                  int color, Font.DisplayMode displayMode, int lightCoords) {
    delegate.submitTextBackground(poseStack, minX, minY, maxX, maxY, color, displayMode, lightCoords);
  }

  @Override
  public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
    delegate.submitFlame(poseStack, renderState, rotation);
  }

  @Override
  public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
    delegate.submitLeash(poseStack, leashState);
  }

  @Override
  public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
    delegate.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
  }

  @Override
  public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts,
                               int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
    delegate.submitBlockModel(poseStack, renderType, parts, tintLayers, lightCoords, overlayCoords, outlineColor);
  }

  @Override
  public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress,
                                       boolean shaded) {
    delegate.submitBreakingBlockModel(poseStack, parts, progress, shaded);
  }

  @Override
  public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color,
                                 float lineWidth, boolean depthTest) {
    delegate.submitShapeOutline(poseStack, shape, renderType, color, lineWidth, depthTest);
  }

  @Override
  public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords,
                         int outlineColor, int[] tintLayers, ItemQuads quads,
                         ItemStackRenderState.FoilType foilType) {
    if (alpha >= 1 || quads.isEmpty()) {
      delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads,
        foilType);
      return;
    }

    ItemSubmission submission = makeTranslucent(tintLayers, quads, alpha);
    delegate.submitItem(poseStack, displayContext, lightCoords, overlayCoords,
      ARGB.multiplyAlpha(outlineColor, alpha), submission.tintLayers(), submission.quads(), foilType);
  }

  @Override
  public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
                                   SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
    delegate.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
  }

  @Override
  public void submitQuadParticleGroup(QuadParticleRenderState particleRenderState) {
    delegate.submitQuadParticleGroup(particleRenderState);
  }

  @Override
  public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera,
                                    boolean depthTest) {
    delegate.submitGizmoPrimitives(group, camera, depthTest);
  }

  private RenderType translucentModelType(RenderType original, @Nullable UvMapping uvMapping) {
    // Patterned shield foil is a dedicated render type; retain its glint pipeline.
    if (original == RenderTypes.patternedShieldGlint()) {
      return original;
    }
    if (uvMapping instanceof TextureAtlasSprite sprite) {
      return cull ? RenderTypes.entityTranslucentCull(sprite.atlasLocation())
        : RenderTypes.entityTranslucent(sprite.atlasLocation());
    }
    return original;
  }

  private static ItemSubmission makeTranslucent(int[] tintLayers, ItemQuads quads, float alpha) {
    int[] sourceTints = tintLayers == null ? new int[0] : tintLayers;
    int[] translucentTints = Arrays.copyOf(sourceTints, sourceTints.length + 1);
    for (int i = 0; i < sourceTints.length; i++) {
      translucentTints[i] = ARGB.multiplyAlpha(sourceTints[i], alpha);
    }
    int alphaByte = Math.max(0, Math.min(255, Math.round(alpha * 255)));
    translucentTints[sourceTints.length] = alphaByte << 24 | 0x00FFFFFF;

    List<BakedQuad> translucent = new ArrayList<>(quads.all().size());
    for (BakedQuad quad : quads.all()) {
      var material = quad.materialInfo();
      int tintIndex = material.tintIndex();
      if (tintIndex < 0 || tintIndex >= sourceTints.length) {
        tintIndex = sourceTints.length;
      }

      var translucentMaterial = new BakedQuad.MaterialInfo(
        material.sprite(),
        material.layer(),
        RenderTypes.itemTranslucent(material.sprite().atlasLocation()),
        material.itemGlintRenderType(),
        material.itemGlintSpecialRenderType(),
        tintIndex,
        material.shadeDirectionOverride(),
        material.lightEmission()
      );
      translucent.add(new BakedQuad(
        quad.position0(), quad.position1(), quad.position2(), quad.position3(),
        quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
        quad.direction(), translucentMaterial
      ));
    }

    List<BakedQuad> immutableQuads = List.copyOf(translucent);
    return new ItemSubmission(translucentTints, new ItemQuads(immutableQuads, List.of(), immutableQuads));
  }

  private record ItemSubmission(int[] tintLayers, ItemQuads quads) {
  }
}
