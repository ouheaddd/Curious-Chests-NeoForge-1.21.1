package com.overyourhead.curiouschests.client.chest.archivist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Static floating-book pass used by the Archivist Chest item icon. */
public final class ArchivistItemRenderer {
    private final BookModel model;

    public ArchivistItemRenderer(EntityModelSet models) {
        model = new BookModel(models.bakeLayer(ModelLayers.BOOK));
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.03F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(285.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        poseStack.scale(0.92F, 0.92F, 0.92F);

        model.setupAnim(0.0F, 0.18F, 0.82F, 0.88F);
        VertexConsumer bookConsumer = EnchantTableRenderer.BOOK_LOCATION.buffer(bufferSource, RenderType::entitySolid);
        model.render(poseStack, bookConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
