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
import com.overyourhead.curiouschests.common.chest.ChestKind;
import com.overyourhead.curiouschests.common.item.SpecialChestBlockItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

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

    private static final int SCULK_FRAME_COUNT = 10;
    private static final int SCULK_TICKS_PER_FRAME = 3;
    private static final int WITCH_LIQUID_FRAME_COUNT = 32;
    private static final int WITCH_LIQUID_TICKS_PER_FRAME = 4;

    private ModelPart bottom;
    private ModelPart lid;
    private ModelPart lock;
    private BookModel archivistBookModel;
    private BottomlessChestModel bottomlessModel;
    private BuildersChestModel buildersModel;
    private CollectorsChestModel collectorsModel;
    private EnderDispatchChestModel enderDispatchModel;
    private InfernalChestModel infernalModel;
    private ResonantChestModel resonantModel;
    private SculkSentinelChestModel sculkSentinelModel;
    private WitchsChestModel witchModel;
    private WitchLiquidModel witchLiquidModel;
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
        archivistBookModel = new BookModel(models.bakeLayer(ModelLayers.BOOK));
        bottomlessModel = new BottomlessChestModel(models.bakeLayer(BottomlessChestModel.LAYER_LOCATION));
        buildersModel = new BuildersChestModel(models.bakeLayer(BuildersChestModel.LAYER_LOCATION));
        collectorsModel = new CollectorsChestModel(models.bakeLayer(CollectorsChestModel.LAYER_LOCATION));
        enderDispatchModel = new EnderDispatchChestModel(models.bakeLayer(EnderDispatchChestModel.LAYER_LOCATION));
        infernalModel = new InfernalChestModel(models.bakeLayer(InfernalChestModel.LAYER_LOCATION));
        resonantModel = new ResonantChestModel(models.bakeLayer(ResonantChestModel.LAYER_LOCATION));
        sculkSentinelModel = new SculkSentinelChestModel(models.bakeLayer(SculkSentinelChestModel.LAYER_LOCATION));
        witchModel = new WitchsChestModel(models.bakeLayer(WitchsChestModel.LAYER_LOCATION));
        witchLiquidModel = new WitchLiquidModel(models.bakeLayer(WitchLiquidModel.LAYER_LOCATION));
        itemRenderer = Minecraft.getInstance().getItemRenderer();
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
        ResourceLocation texture = textureFor(kind);
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
            case WITCH -> renderWitch(poseStack, bufferSource, consumer, packedLight, packedOverlay);
            case ARCHIVIST -> {
                renderVanillaChest(poseStack, consumer, packedLight, packedOverlay);
                renderArchivistBook(poseStack, bufferSource, packedLight);
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

    private void renderWitch(
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

        witchModel.render(poseStack, chestConsumer, 0.0F, packedLight, packedOverlay);

        VertexConsumer liquidConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(witchLiquidFrame()));
        witchLiquidModel.render(poseStack, liquidConsumer, 0.0F, packedLight, packedOverlay);
        renderWitchPotions(poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderWitchPotions(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        for (int side = 0; side < 3; side++) {
            for (int marker = 0; marker < 3; marker++) {
                int slot = marker + side * 3;
                poseStack.pushPose();
                witchModel.applyPotionTransform(poseStack, side, marker);
                poseStack.translate(0.0F, 0.11F, 0.0F);
                if (side == 2) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                } else {
                    poseStack.mulPose(Axis.YP.rotationDegrees(side == 0 ? 90.0F : -90.0F));
                }
                poseStack.scale(0.45F, 0.45F, 0.45F);
                itemRenderer.renderStatic(
                        itemPotion(slot),
                        ItemDisplayContext.FIXED,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        bufferSource,
                        null,
                        slot
                );
                poseStack.popPose();
            }
        }
    }

    private static ItemStack itemPotion(int slot) {
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

    private void renderArchivistBook(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        // Same position language as the in-world Archivist, but static for an item icon.
        poseStack.translate(0.5F, 1.03F, 0.5F);
        // The vanilla enchanting-table BookModel has its own local orientation,
        // independent from the chest item. Turn it a quarter-turn so the open book
        // runs along the same visual axis as the Archivist chest in item displays.
        poseStack.mulPose(Axis.YP.rotationDegrees(285.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        poseStack.scale(0.92F, 0.92F, 0.92F);

        archivistBookModel.setupAnim(0.0F, 0.18F, 0.82F, 0.88F);
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

    private static ResourceLocation sculkFrame() {
        long gameTicks = Util.getMillis() / 50L;
        int frame = (int) ((gameTicks / SCULK_TICKS_PER_FRAME) % SCULK_FRAME_COUNT);
        return ResourceLocation.fromNamespaceAndPath(
                CuriousChestsMod.MOD_ID,
                "textures/entity/chest/sculk_sentinel_" + frame + ".png"
        );
    }

    private static ResourceLocation witchLiquidFrame() {
        long gameTicks = Util.getMillis() / 50L;
        int frame = (int) ((gameTicks / WITCH_LIQUID_TICKS_PER_FRAME) % WITCH_LIQUID_FRAME_COUNT);
        return ResourceLocation.fromNamespaceAndPath(
                CuriousChestsMod.MOD_ID,
                "textures/entity/chest/witch_liquid/witch_liquid_" + frame + ".png"
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
