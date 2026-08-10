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

/** Blockbench model used only by the Collector's Chest block-entity renderer. */
public final class CollectorsChestModel {
    private static final float CLOSED_X_ROT = (float) Math.PI;
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "collectors_chest"),
            "main"
    );

    private final ModelPart base;
    private final ModelPart lid;
    private final ModelPart knob;

    public CollectorsChestModel(ModelPart root) {
        this.base = root.getChild("base");
        this.lid = root.getChild("lid");
        this.knob = root.getChild("knob");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        root.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-8.0F, 24.0F, 8.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(1.0F, 0.0F, 0.0F, 14.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 40)
                        .addBox(13.0F, 2.0F, 2.0F, 2.0F, 3.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(24, 40)
                        .addBox(1.0F, 2.0F, 2.0F, 2.0F, 3.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 40)
                        .addBox(1.0F, 2.0F, 0.0F, 14.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 45)
                        .addBox(1.0F, 2.0F, 12.0F, 14.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-8.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "knob",
                CubeListBuilder.create()
                        .texOffs(48, 50)
                        .addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-8.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        return LayerDefinition.create(meshDefinition, 128, 128);
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

        lid.render(poseStack, consumer, packedLight, packedOverlay);
        knob.render(poseStack, consumer, packedLight, packedOverlay);
        base.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
