package com.overyourhead.curiouschests.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.client.model.BuildersChestModel;
import com.overyourhead.curiouschests.client.model.CollectorsChestModel;
import com.overyourhead.curiouschests.client.model.EnderDispatchChestModel;
import com.overyourhead.curiouschests.client.model.SculkSentinelChestModel;
import com.overyourhead.curiouschests.client.model.WitchLiquidModel;
import com.overyourhead.curiouschests.client.model.WitchsChestModel;
import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
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

    private static final int SCULK_SENTINEL_FRAME_COUNT = 10;
    private static final int SCULK_SENTINEL_TICKS_PER_FRAME = 3;
    private static final ResourceLocation[] SCULK_SENTINEL_FRAMES = createSculkSentinelFrames();

    private static final int WITCH_LIQUID_FRAME_COUNT = 32;
    private static final int WITCH_LIQUID_TICKS_PER_FRAME = 4;
    private static final ResourceLocation[] WITCH_LIQUID_FRAMES = createWitchLiquidFrames();

    private final ModelPart bottom;
    private final ModelPart lid;
    private final ModelPart lock;
    private final BuildersChestModel buildersModel;
    private final CollectorsChestModel collectorsModel;
    private final EnderDispatchChestModel enderDispatchModel;
    private final SculkSentinelChestModel sculkSentinelModel;
    private final WitchsChestModel witchModel;
    private final WitchLiquidModel witchLiquidModel;
    private final ItemRenderer itemRenderer;

    public SpecialChestRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(ModelLayers.CHEST);
        bottom = root.getChild("bottom");
        lid = root.getChild("lid");
        lock = root.getChild("lock");
        buildersModel = new BuildersChestModel(context.bakeLayer(BuildersChestModel.LAYER_LOCATION));
        collectorsModel = new CollectorsChestModel(context.bakeLayer(CollectorsChestModel.LAYER_LOCATION));
        enderDispatchModel = new EnderDispatchChestModel(context.bakeLayer(EnderDispatchChestModel.LAYER_LOCATION));
        sculkSentinelModel = new SculkSentinelChestModel(context.bakeLayer(SculkSentinelChestModel.LAYER_LOCATION));
        witchModel = new WitchsChestModel(context.bakeLayer(WitchsChestModel.LAYER_LOCATION));
        witchLiquidModel = new WitchLiquidModel(context.bakeLayer(WitchLiquidModel.LAYER_LOCATION));
        itemRenderer = Minecraft.getInstance().getItemRenderer();
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

        ResourceLocation texture = chest.kind() == ChestKind.SCULK_SENTINEL
                ? sculkSentinelFrame(chest)
                : TEXTURES[chest.kind().ordinal()];
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        if (chest.kind() == ChestKind.BUILDERS) {
            renderBuilders(poseStack, consumer, openness, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.COLLECTORS) {
            renderCollectors(poseStack, consumer, openness, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.ENDER_DISPATCH) {
            renderEnderDispatch(poseStack, consumer, openness, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.SCULK_SENTINEL) {
            renderSculkSentinel(poseStack, consumer, openness, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.WITCH) {
            renderWitch(chest, poseStack, bufferSource, consumer, openness, packedLight, packedOverlay);
        } else {
            renderParts(poseStack, consumer, openness, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private void renderBuilders(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        // The authored Builder model already sits 0.5 px higher via its ModelPart offsets,
        // so we keep the standard custom-chest world transform here.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        buildersModel.render(poseStack, consumer, openness, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderCollectors(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        // Blockbench's Modded Entity export uses entity-model coordinates.
        // This transform maps those coordinates back into the same 1x1x1 block space
        // used by the vanilla chest renderer, without changing collision or placement.
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        // The custom Blockbench model faces the opposite Z direction from ModelLayers.CHEST.
        // Rotate only this model 180 degrees so the block's FACING property stays untouched.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collectorsModel.render(poseStack, consumer, openness, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderEnderDispatch(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        // Same Blockbench coordinate conversion as the Collector model.
        // The exported model faces opposite to ModelLayers.CHEST, so only this
        // custom model gets an extra 180-degree yaw.
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        enderDispatchModel.render(poseStack, consumer, openness, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderSculkSentinel(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        // Same Blockbench coordinate conversion/orientation used by the other custom chests.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        sculkSentinelModel.render(poseStack, consumer, openness, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderWitch(
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

        witchModel.render(poseStack, chestConsumer, openness, packedLight, packedOverlay);

        ResourceLocation liquidTexture = witchLiquidFrame(chest);
        VertexConsumer liquidConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(liquidTexture));
        witchLiquidModel.render(poseStack, liquidConsumer, openness, packedLight, packedOverlay);

        renderWitchPotions(chest, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderWitchPotions(
            SpecialChestBlockEntity chest,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        for (int i = 0; i < 3; i++) {
            renderWitchPotion(chest, poseStack, bufferSource, packedLight, packedOverlay, 0, i, createWitchDisplayPotion(chest, i));
            renderWitchPotion(chest, poseStack, bufferSource, packedLight, packedOverlay, 1, i, createWitchDisplayPotion(chest, i + 3));
            renderWitchPotion(chest, poseStack, bufferSource, packedLight, packedOverlay, 2, i, createWitchDisplayPotion(chest, i + 6));
        }
    }

    private void renderWitchPotion(
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
        witchModel.applyPotionTransform(poseStack, side, markerIndex);

        // The authored marker is centered on the 1px-thick shelf. Keep all three
        // shelf directions using the same item scale and a tiny lift from the wood.
        poseStack.translate(0.0F, 0.11F, 0.0F);
        if (side == 2) {
            // The rear shelf is now a child of base, just like left/right. It already
            // inherits base's X orientation, so only cancel its authored -90-degree Y
            // shelf turn. This leaves the flat potion upright and facing out the back.
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

    private static ResourceLocation witchLiquidFrame(SpecialChestBlockEntity chest) {
        long gameTime = chest.getLevel() == null ? 0L : chest.getLevel().getGameTime();
        int frame = (int) ((gameTime / WITCH_LIQUID_TICKS_PER_FRAME) % WITCH_LIQUID_FRAME_COUNT);
        return WITCH_LIQUID_FRAMES[frame];
    }

    private static ResourceLocation[] createWitchLiquidFrames() {
        ResourceLocation[] frames = new ResourceLocation[WITCH_LIQUID_FRAME_COUNT];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = ResourceLocation.fromNamespaceAndPath(
                    CuriousChestsMod.MOD_ID,
                    "textures/entity/chest/witch_liquid/witch_liquid_" + i + ".png"
            );
        }
        return frames;
    }

    private static ItemStack createWitchDisplayPotion(SpecialChestBlockEntity chest, int slotIndex) {
        // Guarantee visual subtype variety on the left shelf without changing any colors,
        // positions, scales, or other placement details.
        if (slotIndex == 0) {
            return createBaseWitchPotion(slotIndex, Items.POTION);
        }
        if (slotIndex == 1) {
            return createBaseWitchPotion(slotIndex, Items.LINGERING_POTION);
        }
        if (slotIndex == 2) {
            return createBaseWitchPotion(slotIndex, Items.POTION);
        }

        int lingeringSlot = selectWitchVariantSlot(chest, 0);
        int splashSlotA = selectWitchVariantSlot(chest, 1);
        int splashSlotB = selectWitchVariantSlot(chest, 2);

        if (slotIndex == lingeringSlot) {
            return createBaseWitchPotion(slotIndex, Items.LINGERING_POTION);
        }
        if (slotIndex == splashSlotA || slotIndex == splashSlotB) {
            return createBaseWitchPotion(slotIndex, Items.SPLASH_POTION);
        }
        return createBaseWitchPotion(slotIndex, Items.POTION);
    }

    private static int selectWitchVariantSlot(SpecialChestBlockEntity chest, int variantIndex) {
        long seed = chest.getBlockPos().asLong() ^ 0x5F3759DFL;
        java.util.Random random = new java.util.Random(seed);
        int lingering = random.nextInt(9);
        int splashA;
        do {
            splashA = random.nextInt(9);
        } while (splashA == lingering);
        int splashB;
        do {
            splashB = random.nextInt(9);
        } while (splashB == lingering || splashB == splashA);

        return switch (variantIndex) {
            case 0 -> lingering;
            case 1 -> splashA;
            default -> splashB;
        };
    }

    private static ItemStack createBaseWitchPotion(int slotIndex, net.minecraft.world.item.Item item) {
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

    private static ResourceLocation sculkSentinelFrame(SpecialChestBlockEntity chest) {
        long gameTime = chest.getLevel() == null ? 0L : chest.getLevel().getGameTime();
        int frame = (int) ((gameTime / SCULK_SENTINEL_TICKS_PER_FRAME) % SCULK_SENTINEL_FRAME_COUNT);
        return SCULK_SENTINEL_FRAMES[frame];
    }

    private static ResourceLocation[] createSculkSentinelFrames() {
        ResourceLocation[] frames = new ResourceLocation[SCULK_SENTINEL_FRAME_COUNT];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = ResourceLocation.fromNamespaceAndPath(
                    CuriousChestsMod.MOD_ID,
                    "textures/entity/chest/sculk_sentinel_" + i + ".png"
            );
        }
        return frames;
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
