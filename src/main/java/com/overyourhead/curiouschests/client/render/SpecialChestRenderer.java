package com.overyourhead.curiouschests.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.client.model.BottomlessChestModel;
import com.overyourhead.curiouschests.client.model.BuildersChestModel;
import com.overyourhead.curiouschests.client.model.CollectorsChestModel;
import com.overyourhead.curiouschests.client.model.EnderDispatchChestModel;
import com.overyourhead.curiouschests.client.model.InfernalChestModel;
import com.overyourhead.curiouschests.client.model.ResonantChestModel;
import com.overyourhead.curiouschests.client.model.SculkSentinelChestModel;
import com.overyourhead.curiouschests.client.model.WitchLiquidModel;
import com.overyourhead.curiouschests.client.model.WitchsChestModel;
import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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

    private static final int WITCH_LIQUID_FRAME_COUNT = 16;
    private static final int WITCH_LIQUID_TICKS_PER_FRAME = 4;
    private static final ResourceLocation[] WITCH_LIQUID_FRAMES = createWitchLiquidFrames();

    private static final int INFERNAL_FRAME_COUNT = 3;
    private static final int INFERNAL_TICKS_PER_FRAME = 8;
    private static final ResourceLocation[] INFERNAL_FRAMES = createInfernalFrames();

    private final ModelPart bottom;
    private final ModelPart lid;
    private final ModelPart lock;
    private final BookModel archivistBookModel;
    private final BottomlessChestModel bottomlessModel;
    private final BuildersChestModel buildersModel;
    private final CollectorsChestModel collectorsModel;
    private final EnderDispatchChestModel enderDispatchModel;
    private final InfernalChestModel infernalModel;
    private final ResonantChestModel resonantModel;
    private final SculkSentinelChestModel sculkSentinelModel;
    private final WitchsChestModel witchModel;
    private final WitchLiquidModel witchLiquidModel;
    private final ItemRenderer itemRenderer;

    public SpecialChestRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(ModelLayers.CHEST);
        bottom = root.getChild("bottom");
        lid = root.getChild("lid");
        lock = root.getChild("lock");
        archivistBookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
        bottomlessModel = new BottomlessChestModel(context.bakeLayer(BottomlessChestModel.LAYER_LOCATION));
        buildersModel = new BuildersChestModel(context.bakeLayer(BuildersChestModel.LAYER_LOCATION));
        collectorsModel = new CollectorsChestModel(context.bakeLayer(CollectorsChestModel.LAYER_LOCATION));
        enderDispatchModel = new EnderDispatchChestModel(context.bakeLayer(EnderDispatchChestModel.LAYER_LOCATION));
        infernalModel = new InfernalChestModel(context.bakeLayer(InfernalChestModel.LAYER_LOCATION));
        resonantModel = new ResonantChestModel(context.bakeLayer(ResonantChestModel.LAYER_LOCATION));
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
        if (chest.kind() == ChestKind.TRAPPER) {
            renderTrapperPreview(chest, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

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

        ResourceLocation texture = switch (chest.kind()) {
            case SCULK_SENTINEL -> sculkSentinelFrame(chest);
            case INFERNAL -> infernalFrame(chest);
            default -> TEXTURES[chest.kind().ordinal()];
        };
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        if (chest.kind() == ChestKind.BOTTOMLESS) {
            renderBottomless(poseStack, consumer, openness, packedLight, packedOverlay);
            renderStorageDisplayItem(chest, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.BUILDERS) {
            renderBuilders(poseStack, consumer, openness, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.COLLECTORS) {
            renderCollectors(poseStack, consumer, openness, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.ENDER_DISPATCH) {
            renderEnderDispatch(poseStack, consumer, openness, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.INFERNAL) {
            renderInfernal(poseStack, consumer, openness, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.SCULK_SENTINEL) {
            renderSculkSentinel(poseStack, consumer, openness, packedLight, packedOverlay);
        } else if (chest.kind() == ChestKind.RESONANT) {
            renderResonant(poseStack, bufferSource, consumer, openness, packedLight, packedOverlay, texture);
        } else if (chest.kind() == ChestKind.WITCH) {
            renderWitch(chest, poseStack, bufferSource, consumer, openness, packedLight, packedOverlay);
        } else {
            renderParts(poseStack, consumer, openness, packedLight, packedOverlay);
        }
        poseStack.popPose();

        if (chest.kind() == ChestKind.ENDER_DISPATCH) {
            renderEnderDispatchPreview(chest, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
        if (chest.kind() == ChestKind.ARCHIVIST) {
            renderArchivistBook(chest, partialTick, poseStack, bufferSource, openness, packedLight);
        }
    }


    private void renderTrapperPreview(
            SpecialChestBlockEntity chest,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Entity entity = chest.getTrapperPreviewEntity();
        if (entity == null || chest.getLevel() == null) return;

        float maxDimension = Math.max(0.7F, Math.max(entity.getBbWidth(), entity.getBbHeight()));
        float scale = Mth.clamp(0.56F / maxDimension, 0.12F, 0.72F);
        long gameTime = chest.getLevel().getGameTime();
        float rotation = (gameTime + partialTick) * 2.2F;
        float hover = Mth.sin((gameTime + partialTick) * 0.10F) * 0.025F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.48F + hover, 0.5F);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, -entity.getBbHeight() * 0.5F, 0.0F);

        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        dispatcher.render(
                entity,
                0.0D,
                0.0D,
                0.0D,
                rotation,
                partialTick,
                poseStack,
                bufferSource,
                LightTexture.FULL_BRIGHT
        );
        dispatcher.setRenderShadow(true);
        poseStack.popPose();
    }

    private void renderEnderDispatchPreview(
            SpecialChestBlockEntity chest,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack preview = chest.getDispatchPreviewStack();
        if (preview.isEmpty()) return;

        poseStack.pushPose();

        long gameTime = chest.getLevel() == null ? 0L : chest.getLevel().getGameTime();
        float time = gameTime + partialTick;
        float hover = Mth.sin(time * 0.18F) * 0.025F;
        poseStack.translate(0.5F, 1.24F + hover, 0.5F);

        // Keep flat 2D items camera-facing so they remain readable.
        // 3D items instead use a fixed chest-relative display so they feel
        // like a levitating pedestal preview rather than flipping when viewed
        // from above or odd angles.
        var model = itemRenderer.getModel(
                preview,
                chest.getLevel(),
                null,
                (int) chest.getBlockPos().asLong()
        );

        if (model.isGui3d()) {
            Direction facing = chest.getBlockState().hasProperty(AbstractSpecialChestBlock.FACING)
                    ? chest.getBlockState().getValue(AbstractSpecialChestBlock.FACING)
                    : Direction.SOUTH;
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
            poseStack.translate(0.0F, -0.03F, 0.0F);
            poseStack.scale(0.70F, 0.70F, 0.70F);
        } else {
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(0.525F, 0.525F, 0.525F);
        }

        itemRenderer.renderStatic(
                preview,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                chest.getLevel(),
                (int) chest.getBlockPos().asLong()
        );
        poseStack.popPose();
    }

    private void renderArchivistBook(
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

        archivistBookModel.setupAnim(
                time,
                Mth.clamp(rightFlip, 0.0F, 1.0F),
                Mth.clamp(leftFlip, 0.0F, 1.0F),
                open
        );
        VertexConsumer bookConsumer = EnchantTableRenderer.BOOK_LOCATION.buffer(
                bufferSource,
                RenderType::entitySolid
        );
        archivistBookModel.render(
                poseStack,
                bookConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
        poseStack.popPose();
    }

    private static float lerpRadians(float partialTick, float from, float to) {
        float delta = to - from;
        while (delta >= Math.PI) delta -= (float) (Math.PI * 2.0D);
        while (delta < -Math.PI) delta += (float) (Math.PI * 2.0D);
        return from + partialTick * delta;
    }

    private void renderStorageDisplayItem(
            SpecialChestBlockEntity chest,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack displayed = chest.getStorageDisplayItem();
        if (displayed.isEmpty()) return;

        poseStack.pushPose();
        // The authored frame is centered on the lower front panel. This transform is
        // in block-local space after the chest FACING rotation, so it follows the
        // front of the Storage Chest automatically.
        poseStack.translate(0.5F, 0.3125F, 0.965F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.30F, 0.30F, 0.30F);
        itemRenderer.renderStatic(
                displayed,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                chest.getLevel(),
                (int) chest.getBlockPos().asLong()
        );
        poseStack.popPose();
    }

    private void renderBottomless(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        bottomlessModel.render(poseStack, consumer, openness, packedLight, packedOverlay);
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

    private void renderInfernal(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        infernalModel.render(poseStack, consumer, openness, packedLight, packedOverlay);
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

    private void renderResonant(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay,
            ResourceLocation texture
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        // Resonant crystals already match the authored Blockbench look closely. The
        // remaining mismatch is the chest body reading too dark under normal world
        // lighting, so give only the body a gentle minimum light floor instead of
        // making it emissive/fullbright.
        int bodyLight = boostedBodyLight(packedLight);
        resonantModel.renderMain(poseStack, consumer, openness, bodyLight, packedOverlay);

        // Draw the exact same crystal geometry with its parent transforms intact, but
        // fold all face normals to one neutral upward normal. This removes the harsh
        // opposite-facing card shading without changing UVs, pivots or positions.
        VertexConsumer crystalConsumer = new ResonantCrystalVertexConsumer(
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture))
        );
        resonantModel.renderCrystals(poseStack, crystalConsumer, openness, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private static int boostedBodyLight(int packedLight) {
        int block = Math.max(LightTexture.block(packedLight), 10);
        int sky = Math.max(LightTexture.sky(packedLight), 10);
        return LightTexture.pack(block, sky);
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

    private static ResourceLocation infernalFrame(SpecialChestBlockEntity chest) {
        long gameTime = chest.getLevel() == null ? 0L : chest.getLevel().getGameTime();
        int frame = (int) ((gameTime / INFERNAL_TICKS_PER_FRAME) % INFERNAL_FRAME_COUNT);
        return INFERNAL_FRAMES[frame];
    }

    private static ResourceLocation[] createInfernalFrames() {
        return new ResourceLocation[]{
                ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "textures/entity/chest/infernal.png"),
                ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "textures/entity/chest/infernal2.png"),
                ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "textures/entity/chest/infernal3.png")
        };
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
    /**
     * Delegates every vertex attribute unchanged except the normal. Resonant crystals
     * are crossed zero-thickness cards, so opposite card normals can receive wildly
     * different directional shading. A single neutral normal keeps those cards equally
     * readable while leaving their actual geometry and texture untouched.
     */
    private static final class ResonantCrystalVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;

        private ResonantCrystalVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(0.0F, 1.0F, 0.0F);
            return this;
        }
    }

}
