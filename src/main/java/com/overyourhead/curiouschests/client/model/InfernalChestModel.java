package com.overyourhead.curiouschests.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.overyourhead.curiouschests.CuriousChestsMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Blockbench model used by the Infernal Chest block-entity and item renderers.
 * The latch is parented to the lid so it follows the full authored lid motion
 * instead of spinning around in place at the world origin.
 */
public final class InfernalChestModel {
    private static final float CLOSED_X_ROT = (float) Math.PI;
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "infernal_chest"),
            "main"
    );

    private final ModelPart knob;
    private final ModelPart lid;
    private final ModelPart base;

    public InfernalChestModel(ModelPart root) {
        this.knob = root.getChild("knob");
        this.lid = root.getChild("lid");
        this.base = root.getChild("base");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Match the proven Ender Dispatch hinge setup: lid and latch share the
        // exact same rear-edge pivot. The cube coordinates are compensated so the
        // CLOSED model remains pixel-for-pixel where it was in the Blockbench export.
        partdefinition.addOrReplaceChild(
                "knob",
                CubeListBuilder.create()
                        .texOffs(56, 8)
                        .addBox(-1.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        PartDefinition lid = partdefinition.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-7.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        lid.addOrReplaceChild(
                "bone5",
                CubeListBuilder.create()
                        .texOffs(54, 51).addBox(3.0F, -1.0F, 6.25F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 13).addBox(3.0F, -14.3F, 6.25F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 15).addBox(3.0F, -1.0F, -7.15F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 17).addBox(3.0F, -14.3F, -7.15F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-7.1F, -3.0F, 6.95F, 0.0F, 0.0F, 1.5708F)
        );

        lid.addOrReplaceChild(
                "bone",
                CubeListBuilder.create()
                        .texOffs(18, 53).addBox(-7.5F, -2.0F, 5.5F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 53).addBox(5.5F, -2.0F, 5.5F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(34, 53).addBox(5.5F, -2.0F, -7.5F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 53).addBox(-7.5F, -2.0F, -7.5F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 4.5F, 7.0F)
        );

        PartDefinition bone3 = lid.addOrReplaceChild(
                "bone3",
                CubeListBuilder.create()
                        .texOffs(0, 43).addBox(-5.5F, -1.0F, 5.5F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 43).addBox(-5.5F, -1.0F, -7.5F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 4.2F, 7.0F)
        );

        bone3.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(26, 47).addBox(-10.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-6.5F, 0.0F, 4.5F, 0.0F, -1.5708F, 0.0F)
        );

        bone3.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create()
                        .texOffs(0, 47).addBox(-10.0F, -1.0F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.5F, 0.0F, 4.5F, 0.0F, -1.5708F, 0.0F)
        );

        PartDefinition base = partdefinition.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.25F, -3.1F, -8.625F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 51).addBox(-1.75F, -1.65F, 3.525F, 7.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.75F, 20.9F, -1.625F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        base.addOrReplaceChild(
                "bone2",
                CubeListBuilder.create()
                        .texOffs(56, 0).addBox(-7.5F, -1.0F, 5.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 4).addBox(5.5F, -1.0F, 5.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(52, 47).addBox(5.5F, -1.0F, -7.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(50, 53).addBox(-7.5F, -1.0F, -7.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.75F, -2.1F, -1.625F)
        );

        base.addOrReplaceChild(
                "bone4",
                CubeListBuilder.create()
                        .texOffs(18, 51).addBox(-4.2F, -0.9F, 6.2F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(36, 51).addBox(-4.2F, -14.2F, 6.2F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(52, 45).addBox(-4.2F, -0.9F, -7.2F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(52, 43).addBox(-4.2F, -14.2F, -7.2F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.25F, 2.1F, -1.625F, 0.0F, 0.0F, 1.5708F)
        );

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    public void render(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        float lidRotation = CLOSED_X_ROT - openness * MAX_LID_ANGLE;
        lid.xRot = lidRotation;
        knob.xRot = lidRotation;

        knob.render(poseStack, consumer, packedLight, packedOverlay);
        lid.render(poseStack, consumer, packedLight, packedOverlay);
        base.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
