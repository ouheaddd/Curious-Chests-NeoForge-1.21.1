package com.overyourhead.curiouschests.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.curiouschests.CuriousChestsMod;
import com.overyourhead.curiouschests.client.chest.archivist.ArchivistItemRenderer;
import com.overyourhead.curiouschests.client.chest.witch.WitchItemRenderer;
import com.overyourhead.curiouschests.client.model.BottomlessChestModel;
import com.overyourhead.curiouschests.client.model.BuildersChestModel;
import com.overyourhead.curiouschests.client.model.CollectorsChestModel;
import com.overyourhead.curiouschests.client.model.EnderDispatchChestModel;
import com.overyourhead.curiouschests.client.model.InfernalChestModel;
import com.overyourhead.curiouschests.client.model.ResonantChestModel;
import com.overyourhead.curiouschests.client.model.SculkSentinelChestModel;
import com.overyourhead.curiouschests.client.model.TrappersChestModel;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.item.SpecialChestBlockItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Draws Curious Chests block items with the same entity-style geometry used in-world.
 * The item JSONs only provide display transforms and opt into this custom renderer.
 */
public final class SpecialChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation[] TEXTURES = Arrays.stream(ChestKind.values())
            .map(kind -> ResourceLocation.fromNamespaceAndPath(
                    CuriousChestsMod.MOD_ID,
                    "textures/entity/chest/" + kind.id() + ".png"
            ))
            .toArray(ResourceLocation[]::new);

    private static final ResourceLocation TRAPPER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID, "textures/entity/chest/trappers_chest.png"
    );
    private static final ResourceLocation TRAPPER_ACTIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CuriousChestsMod.MOD_ID, "textures/entity/chest/trappers_chest_active.png"
    );

    private static final int SCULK_FRAME_COUNT = 10;
    private static final int SCULK_TICKS_PER_FRAME = 3;

    private ModelPart bottom;
    private ModelPart lid;
    private ModelPart lock;
    private ArchivistItemRenderer archivistItemRenderer;
    private BottomlessChestModel bottomlessModel;
    private BuildersChestModel buildersModel;
    private CollectorsChestModel collectorsModel;
    private EnderDispatchChestModel enderDispatchModel;
    private InfernalChestModel infernalModel;
    private ResonantChestModel resonantModel;
    private SculkSentinelChestModel sculkSentinelModel;
    private TrappersChestModel trapperModel;
    private WitchItemRenderer witchItemRenderer;
    private ItemRenderer itemRenderer;
    private boolean initialized;

    public SpecialChestItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private void ensureInitialized() {
        if (initialized) return;

        var models = Minecraft.getInstance().getEntityModels();
        ModelPart vanillaChest = models.bakeLayer(ModelLayers.CHEST);
        bottom = vanillaChest.getChild("bottom");
        lid = vanillaChest.getChild("lid");
        lock = vanillaChest.getChild("lock");
        archivistItemRenderer = new ArchivistItemRenderer(models);
        bottomlessModel = new BottomlessChestModel(models.bakeLayer(BottomlessChestModel.LAYER_LOCATION));
        buildersModel = new BuildersChestModel(models.bakeLayer(BuildersChestModel.LAYER_LOCATION));
        collectorsModel = new CollectorsChestModel(models.bakeLayer(CollectorsChestModel.LAYER_LOCATION));
        enderDispatchModel = new EnderDispatchChestModel(models.bakeLayer(EnderDispatchChestModel.LAYER_LOCATION));
        infernalModel = new InfernalChestModel(models.bakeLayer(InfernalChestModel.LAYER_LOCATION));
        resonantModel = new ResonantChestModel(models.bakeLayer(ResonantChestModel.LAYER_LOCATION));
        sculkSentinelModel = new SculkSentinelChestModel(models.bakeLayer(SculkSentinelChestModel.LAYER_LOCATION));
        trapperModel = new TrappersChestModel(models.bakeLayer(TrappersChestModel.LAYER_LOCATION));
        itemRenderer = Minecraft.getInstance().getItemRenderer();
        witchItemRenderer = new WitchItemRenderer(models, itemRenderer);
        initialized = true;
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!(stack.getItem() instanceof SpecialChestBlockItem chestItem)) return;
        ensureInitialized();

        // GUI / creative-tab icons were facing backwards relative to the viewer.
        // Flip the whole chest item around its local center only for item-display contexts
        // where a front-facing presentation is expected.
        poseStack.pushPose();
        if (displayContext == ItemDisplayContext.GUI
                || displayContext == ItemDisplayContext.FIXED
                || displayContext == ItemDisplayContext.GROUND) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }

        ChestKind kind = chestItem.kind();
        ResourceLocation texture = kind == ChestKind.TRAPPER
                ? (SpecialChestBlockEntity.getPackedTrapperEntityCount(stack) > 0
                    ? TRAPPER_ACTIVE_TEXTURE
                    : TRAPPER_TEXTURE)
                : textureFor(kind);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));

        switch (kind) {
            case BOTTOMLESS -> renderCustomModel(poseStack, () ->
                    bottomlessModel.render(poseStack, consumer, 0.0F, packedLight, packedOverlay));
            case BUILDERS -> renderCustomModel(poseStack, () ->
                    buildersModel.render(poseStack, consumer, 0.0F, packedLight, packedOverlay));
            case COLLECTORS -> renderCustomModel(poseStack, () ->
                    collectorsModel.render(poseStack, consumer, 0.0F, packedLight, packedOverlay));
            case ENDER_DISPATCH -> renderCustomModel(poseStack, () ->
                    enderDispatchModel.render(poseStack, consumer, 0.0F, packedLight, packedOverlay));
            case INFERNAL -> renderCustomModel(poseStack, () ->
                    infernalModel.render(poseStack, consumer, 0.0F, packedLight, packedOverlay));
            case SCULK_SENTINEL -> {
                ResourceLocation frameTexture = sculkFrame();
                VertexConsumer sculkConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(frameTexture));
                renderCustomModel(poseStack, () ->
                        sculkSentinelModel.render(poseStack, sculkConsumer, 0.0F, packedLight, packedOverlay));
            }
            case RESONANT -> renderResonant(
                    poseStack, bufferSource, consumer, texture, packedLight, packedOverlay, displayContext);
            case WITCH -> witchItemRenderer.render(poseStack, bufferSource, consumer, packedLight, packedOverlay);
            case TRAPPER -> renderCustomModel(poseStack, () ->
                    trapperModel.render(poseStack, consumer, 0.0F, packedLight, packedOverlay));
            case ARCHIVIST -> {
                renderVanillaChest(poseStack, consumer, packedLight, packedOverlay);
                archivistItemRenderer.render(poseStack, bufferSource, packedLight);
            }
            default -> renderVanillaChest(poseStack, consumer, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private static ResourceLocation textureFor(ChestKind kind) {
        return TEXTURES[kind.ordinal()];
    }

    private static void renderCustomModel(PoseStack poseStack, Runnable draw) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        draw.run();
        poseStack.popPose();
    }

    private void renderVanillaChest(
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay
    ) {
        lid.xRot = 0.0F;
        lock.xRot = 0.0F;
        lid.render(poseStack, consumer, packedLight, packedOverlay);
        lock.render(poseStack, consumer, packedLight, packedOverlay);
        bottom.render(poseStack, consumer, packedLight, packedOverlay);
    }

    private void renderResonant(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            VertexConsumer bodyConsumer,
            ResourceLocation texture,
            int packedLight,
            int packedOverlay,
            ItemDisplayContext displayContext
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        int bodyLight = boostedBodyLight(packedLight);
        resonantModel.renderMain(poseStack, bodyConsumer, 0.0F, bodyLight, packedOverlay);
        boolean guiCrystals = displayContext == ItemDisplayContext.GUI;
        VertexConsumer crystalConsumer = new ResonantCrystalVertexConsumer(
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture)),
                guiCrystals ? -1.0F : 1.0F
        );
        // The crossed zero-thickness crystal cards already use a fixed neutral normal.
        // In GUI lighting that still leaves them noticeably darker than the authored
        // Blockbench look, so only the inventory/GUI crystal pass receives full light.
        // World rendering and held/dropped item lighting remain unchanged.
        int crystalLight = guiCrystals
                ? LightTexture.FULL_BRIGHT
                : packedLight;
        resonantModel.renderCrystals(poseStack, crystalConsumer, 0.0F, crystalLight, packedOverlay);
        poseStack.popPose();
    }

    private static int boostedBodyLight(int packedLight) {
        int block = Math.max(LightTexture.block(packedLight), 10);
        int sky = Math.max(LightTexture.sky(packedLight), 10);
        return LightTexture.pack(block, sky);
    }



    private static ResourceLocation sculkFrame() {
        long gameTicks = Util.getMillis() / 50L;
        int frame = (int) ((gameTicks / SCULK_TICKS_PER_FRAME) % SCULK_FRAME_COUNT);
        return ResourceLocation.fromNamespaceAndPath(
                CuriousChestsMod.MOD_ID,
                "textures/entity/chest/sculk_sentinel_" + frame + ".png"
        );
    }


    private static final class ResonantCrystalVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float normalY;

        private ResonantCrystalVertexConsumer(VertexConsumer delegate, float normalY) {
            this.delegate = delegate;
            this.normalY = normalY;
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
            delegate.setNormal(0.0F, normalY, 0.0F);
            return this;
        }
    }
}
