package com.overyourhead.curiouschests.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/** Renders every special chest with the vanilla single-chest geometry. */
public final class SpecialChestRenderer implements BlockEntityRenderer<SpecialChestBlockEntity> {
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);
    private static final ResourceLocation[] TEXTURES = Arrays.stream(ChestKind.values())
            .map(kind -> ResourceLocation.fromNamespaceAndPath(
                    CuriousChestsMod.MOD_ID,
                    "textures/entity/chest/" + kind.id() + ".png"
            ))
            .toArray(ResourceLocation[]::new);

    private final ModelPart bottom;
    private final ModelPart lid;
    private final ModelPart lock;

    public SpecialChestRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(ModelLayers.CHEST);
        bottom = root.getChild("bottom");
        lid = root.getChild("lid");
        lock = root.getChild("lock");
    }

    @Override
    public void render(
            SpecialChestBlockEntity chest,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = chest.getBlockState();
        Direction facing = state.hasProperty(AbstractSpecialChestBlock.FACING)
                ? state.getValue(AbstractSpecialChestBlock.FACING)
                : Direction.SOUTH;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        float openness = chest.getOpenNess(partialTick);
        openness = 1.0F - openness;
        openness = 1.0F - openness * openness * openness;

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityCutout(TEXTURES[chest.kind().ordinal()])
        );
        renderParts(poseStack, consumer, openness, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderParts(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        lid.xRot = -(openness * MAX_LID_ANGLE);
        lock.xRot = lid.xRot;
        lid.render(poseStack, consumer, packedLight, packedOverlay);
        lock.render(poseStack, consumer, packedLight, packedOverlay);
        bottom.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
