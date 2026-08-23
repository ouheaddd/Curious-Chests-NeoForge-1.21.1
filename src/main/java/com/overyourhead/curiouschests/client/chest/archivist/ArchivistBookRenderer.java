package com.overyourhead.curiouschests.client.chest.archivist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Floating book renderer for Archivist Chest. */
public final class ArchivistBookRenderer {
    private final BookModel model;

    public ArchivistBookRenderer(BlockEntityRendererProvider.Context context) {
        model = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    public void render(
            SpecialChestBlockEntity chest,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float chestOpenness,
            int packedLight
    ) {
        poseStack.pushPose();
        float time = chest.getArchivistBookTime() + partialTick;
        float hover = 0.10F + Mth.sin(time * 0.1F) * 0.01F;
        float lidLift = chestOpenness * 0.16F;
        poseStack.translate(0.5F, 0.91F + hover + lidLift, 0.5F);

        float rotation = lerpRadians(partialTick, chest.getArchivistBookOldRot(), chest.getArchivistBookRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation * (180.0F / (float) Math.PI)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));

        float flip = Mth.lerp(partialTick, chest.getArchivistBookOldFlip(), chest.getArchivistBookFlip());
        float rightFlip = Mth.frac(flip + 0.25F) * 1.6F - 0.3F;
        float leftFlip = Mth.frac(flip + 0.75F) * 1.6F - 0.3F;
        float open = Mth.lerp(partialTick, chest.getArchivistBookOldOpen(), chest.getArchivistBookOpen());

        model.setupAnim(time, Mth.clamp(rightFlip, 0.0F, 1.0F), Mth.clamp(leftFlip, 0.0F, 1.0F), open);
        VertexConsumer bookConsumer = EnchantTableRenderer.BOOK_LOCATION.buffer(bufferSource, RenderType::entitySolid);
        model.render(poseStack, bookConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }

    private static float lerpRadians(float partialTick, float from, float to) {
        float delta = to - from;
        while (delta >= Math.PI) delta -= (float) (Math.PI * 2.0D);
        while (delta < -Math.PI) delta += (float) (Math.PI * 2.0D);
        return from + partialTick * delta;
    }
}
