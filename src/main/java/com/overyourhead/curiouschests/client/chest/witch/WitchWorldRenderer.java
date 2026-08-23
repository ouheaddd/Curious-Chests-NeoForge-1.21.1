package com.overyourhead.curiouschests.client.chest.witch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.client.model.WitchLiquidModel;
import com.overyourhead.curiouschests.client.model.WitchsChestModel;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** World/model rendering specific to Witch Chest. */
public final class WitchWorldRenderer {
    private static final int LIQUID_FRAME_COUNT = 16;
    private static final int LIQUID_TICKS_PER_FRAME = 4;
    private static final ResourceLocation[] LIQUID_FRAMES = createLiquidFrames();

    private final WitchsChestModel model;
    private final WitchLiquidModel liquidModel;
    private final ItemRenderer itemRenderer;

    public WitchWorldRenderer(BlockEntityRendererProvider.Context context) {
        model = new WitchsChestModel(context.bakeLayer(WitchsChestModel.LAYER_LOCATION));
        liquidModel = new WitchLiquidModel(context.bakeLayer(WitchLiquidModel.LAYER_LOCATION));
        itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    public void render(
            SpecialChestBlockEntity chest,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            VertexConsumer chestConsumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        model.render(poseStack, chestConsumer, openness, packedLight, packedOverlay);
        VertexConsumer liquidConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(liquidFrame(chest)));
        liquidModel.render(poseStack, liquidConsumer, openness, packedLight, packedOverlay);
        renderPotions(chest, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderPotions(
            SpecialChestBlockEntity chest,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        for (int i = 0; i < 3; i++) {
            renderPotion(chest, poseStack, bufferSource, packedLight, packedOverlay, 0, i, createDisplayPotion(chest, i));
            renderPotion(chest, poseStack, bufferSource, packedLight, packedOverlay, 1, i, createDisplayPotion(chest, i + 3));
            renderPotion(chest, poseStack, bufferSource, packedLight, packedOverlay, 2, i, createDisplayPotion(chest, i + 6));
        }
    }

    private void renderPotion(
            SpecialChestBlockEntity chest,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            int side,
            int markerIndex,
            ItemStack stack
    ) {
        poseStack.pushPose();
        model.applyPotionTransform(poseStack, side, markerIndex);
        poseStack.translate(0.0F, 0.11F, 0.0F);
        if (side == 2) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(side == 0 ? 90.0F : -90.0F));
        }
        poseStack.scale(0.45F, 0.45F, 0.45F);

        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                chest.getLevel(),
                markerIndex + side * 3
        );
        poseStack.popPose();
    }

    private static ResourceLocation liquidFrame(SpecialChestBlockEntity chest) {
        long gameTime = chest.getLevel() == null ? 0L : chest.getLevel().getGameTime();
        int frame = (int) ((gameTime / LIQUID_TICKS_PER_FRAME) % LIQUID_FRAME_COUNT);
        return LIQUID_FRAMES[frame];
    }

    private static ResourceLocation[] createLiquidFrames() {
        ResourceLocation[] frames = new ResourceLocation[LIQUID_FRAME_COUNT];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = ResourceLocation.fromNamespaceAndPath(
                    CuriousChestsMod.MOD_ID,
                    "textures/entity/chest/witch_liquid/witch_liquid_" + i + ".png"
            );
        }
        return frames;
    }

    private static ItemStack createDisplayPotion(SpecialChestBlockEntity chest, int slotIndex) {
        if (slotIndex == 0) return createBasePotion(slotIndex, Items.POTION);
        if (slotIndex == 1) return createBasePotion(slotIndex, Items.LINGERING_POTION);
        if (slotIndex == 2) return createBasePotion(slotIndex, Items.POTION);

        int lingeringSlot = selectVariantSlot(chest, 0);
        int splashSlotA = selectVariantSlot(chest, 1);
        int splashSlotB = selectVariantSlot(chest, 2);
        if (slotIndex == lingeringSlot) return createBasePotion(slotIndex, Items.LINGERING_POTION);
        if (slotIndex == splashSlotA || slotIndex == splashSlotB) return createBasePotion(slotIndex, Items.SPLASH_POTION);
        return createBasePotion(slotIndex, Items.POTION);
    }

    private static int selectVariantSlot(SpecialChestBlockEntity chest, int variantIndex) {
        long seed = chest.getBlockPos().asLong() ^ 0x5F3759DFL;
        java.util.Random random = new java.util.Random(seed);
        int lingering = random.nextInt(9);
        int splashA;
        do { splashA = random.nextInt(9); } while (splashA == lingering);
        int splashB;
        do { splashB = random.nextInt(9); } while (splashB == lingering || splashB == splashA);
        return switch (variantIndex) {
            case 0 -> lingering;
            case 1 -> splashA;
            default -> splashB;
        };
    }

    private static ItemStack createBasePotion(int slotIndex, net.minecraft.world.item.Item item) {
        return switch (slotIndex) {
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
}
