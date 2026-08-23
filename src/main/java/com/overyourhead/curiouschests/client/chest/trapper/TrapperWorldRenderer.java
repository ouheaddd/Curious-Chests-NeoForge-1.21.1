package com.overyourhead.curiouschests.client.chest.trapper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.client.model.TrappersChestModel;
import com.overyourhead.curiouschests.common.block.TrapperChestBlock;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.level.block.state.BlockState;

/** World/model rendering specific to Trapper Chest. */
public final class TrapperWorldRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID, "textures/entity/chest/trappers_chest.png"
    );
    private static final ResourceLocation ACTIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID, "textures/entity/chest/trappers_chest_active.png"
    );

    private final TrappersChestModel model;

    public TrapperWorldRenderer(BlockEntityRendererProvider.Context context) {
        model = new TrappersChestModel(context.bakeLayer(TrappersChestModel.LAYER_LOCATION));
    }

    public ResourceLocation texture(SpecialChestBlockEntity chest) {
        BlockState state = chest.getBlockState();
        return state.hasProperty(TrapperChestBlock.OCCUPIED) && state.getValue(TrapperChestBlock.OCCUPIED)
                ? ACTIVE_TEXTURE
                : TEXTURE;
    }

    public void renderChest(PoseStack poseStack, VertexConsumer consumer, float openness, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        model.render(poseStack, consumer, openness, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public void renderPreview(
            SpecialChestBlockEntity chest,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        Entity entity = chest.getTrapperPreviewEntity();
        if (entity == null || chest.getLevel() == null) return;

        float maxDimension = Math.max(0.7F, Math.max(entity.getBbWidth(), entity.getBbHeight()));
        float scale = Mth.clamp(0.364F / maxDimension, 0.08F, 0.47F);
        long gameTime = chest.getLevel().getGameTime();
        freezePreviewPose(entity);

        float displayTime = (gameTime % 200L) + partialTick;
        float rotation = displayTime * 1.80F;
        float hover = Mth.sin((gameTime + partialTick) * 0.10F) * 0.025F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.355F + hover, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, -entity.getBbHeight() * 0.5F, 0.0F);

        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        dispatcher.render(
                entity,
                0.0D,
                0.0D,
                0.0D,
                0.0F,
                0.0F,
                poseStack,
                bufferSource,
                LightTexture.FULL_BRIGHT
        );
        dispatcher.setRenderShadow(true);
        poseStack.popPose();
    }

    private static void freezePreviewPose(Entity entity) {
        entity.tickCount = 0;
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        entity.setYRot(0.0F);
        entity.yRotO = 0.0F;
        entity.setXRot(0.0F);
        entity.xRotO = 0.0F;

        if (entity instanceof LivingEntity living) {
            living.yBodyRot = 0.0F;
            living.yBodyRotO = 0.0F;
            living.yHeadRot = 0.0F;
            living.yHeadRotO = 0.0F;
            living.attackAnim = 0.0F;
            living.oAttackAnim = 0.0F;
            living.hurtTime = 0;
            living.deathTime = 0;
            living.walkAnimation.setSpeed(0.0F);
        }
        if (entity instanceof Mob mob) mob.setTarget(null);
        if (entity instanceof NeutralMob neutralMob) neutralMob.stopBeingAngry();
    }
}
