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

/** Blockbench model used only by the Sculk Sentinel Chest block-entity renderer. */
public final class SculkSentinelChestModel {
    private static final float KNOB_CLOSED_X_ROT = (float) Math.PI;
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "sculk_sentinel_chest"),
            "main"
    );

    private final ModelPart knob;
    private final ModelPart lid;
    private final ModelPart base;

    public SculkSentinelChestModel(ModelPart root) {
        this.knob = root.getChild("knob");
        this.lid = root.getChild("lid");
        this.base = root.getChild("base");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // The exported lid already has its pivot on the rear hinge, so keep its
        // closed geometry exactly as authored in Blockbench.
        root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(1.0F, -5.0F, -14.0F, 14.0F, 5.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 43)
                        .addBox(2.0F, -11.0F, -13.0F, 12.0F, 6.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-8.0F, 15.0F, 7.0F)
        );

        // Move the latch pivot to the same rear hinge as the lid while preserving
        // its exact closed position and UV orientation from the Blockbench export.
        // This makes it travel rigidly with the lid instead of "opening" by itself.
        root.addOrReplaceChild(
                "knob",
                CubeListBuilder.create()
                        .texOffs(48, 43)
                        .addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-8.0F, 15.0F, 7.0F, KNOB_CLOSED_X_ROT, 0.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -5.0F, -7.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, (float) Math.PI, 0.0F, 0.0F)
        );

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    public void render(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        float openAngle = openness * MAX_LID_ANGLE;
        lid.xRot = -openAngle;
        knob.xRot = KNOB_CLOSED_X_ROT - openAngle;

        knob.render(poseStack, consumer, packedLight, packedOverlay);
        lid.render(poseStack, consumer, packedLight, packedOverlay);
        base.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
