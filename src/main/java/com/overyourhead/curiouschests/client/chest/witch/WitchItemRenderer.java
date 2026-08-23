package com.overyourhead.curiouschests.client.chest.witch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.client.model.WitchLiquidModel;
import com.overyourhead.curiouschests.client.model.WitchsChestModel;
import net.minecraft.Util;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Item-form rendering specific to Witch Chest. */
public final class WitchItemRenderer {
    private static final int LIQUID_FRAME_COUNT = 16;
    private static final int LIQUID_TICKS_PER_FRAME = 4;

    private final WitchsChestModel model;
    private final WitchLiquidModel liquidModel;
    private final ItemRenderer itemRenderer;

    public WitchItemRenderer(EntityModelSet models, ItemRenderer itemRenderer) {
        this.model = new WitchsChestModel(models.bakeLayer(WitchsChestModel.LAYER_LOCATION));
        this.liquidModel = new WitchLiquidModel(models.bakeLayer(WitchLiquidModel.LAYER_LOCATION));
        this.itemRenderer = itemRenderer;
    }

    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            VertexConsumer chestConsumer,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        model.render(poseStack, chestConsumer, 0.0F, packedLight, packedOverlay);
        VertexConsumer liquidConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(liquidFrame()));
        liquidModel.render(poseStack, liquidConsumer, 0.0F, packedLight, packedOverlay);
        renderPotions(poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderPotions(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (int side = 0; side < 3; side++) {
            for (int marker = 0; marker < 3; marker++) {
                int slot = marker + side * 3;
                poseStack.pushPose();
                model.applyPotionTransform(poseStack, side, marker);
                poseStack.translate(0.0F, 0.11F, 0.0F);
                if (side == 2) poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                else poseStack.mulPose(Axis.YP.rotationDegrees(side == 0 ? 90.0F : -90.0F));
                poseStack.scale(0.45F, 0.45F, 0.45F);
                itemRenderer.renderStatic(
                        potion(slot), ItemDisplayContext.FIXED, packedLight, packedOverlay,
                        poseStack, bufferSource, null, slot
                );
                poseStack.popPose();
            }
        }
    }

    private static ItemStack potion(int slot) {
        var item = switch (slot) {
            case 1 -> Items.LINGERING_POTION;
            case 3, 7 -> Items.SPLASH_POTION;
            default -> Items.POTION;
        };
        return switch (slot) {
            case 0 -> PotionContents.createItemStack(item, Potions.HEALING);
            case 1 -> PotionContents.createItemStack(item, Potions.SWIFTNESS);
            case 2 -> PotionContents.createItemStack(item, Potions.POISON);
            case 3 -> PotionContents.createItemStack(item, Potions.STRENGTH);
            case 4 -> PotionContents.createItemStack(item, Potions.REGENERATION);
            case 5 -> PotionContents.createItemStack(item, Potions.NIGHT_VISION);
            case 6 -> PotionContents.createItemStack(item, Potions.INVISIBILITY);
            case 7 -> PotionContents.createItemStack(item, Potions.FIRE_RESISTANCE);
            default -> PotionContents.createItemStack(item, Potions.WATER_BREATHING);
        };
    }

    private static ResourceLocation liquidFrame() {
        long gameTicks = Util.getMillis() / 50L;
        int frame = (int) ((gameTicks / LIQUID_TICKS_PER_FRAME) % LIQUID_FRAME_COUNT);
        return ResourceLocation.fromNamespaceAndPath(
                CuriousChestsMod.MOD_ID,
                "textures/entity/chest/witch_liquid/witch_liquid_" + frame + ".png"
        );
    }
}
