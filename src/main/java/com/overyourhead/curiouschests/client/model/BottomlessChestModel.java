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

/** Storage Chest model. The technical registry/model id remains bottomless_chest. */
public final class BottomlessChestModel {
    private static final float CLOSED_X_ROT = (float) Math.PI;
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "bottomless_chest"),
            "main"
    );

    private final ModelPart knob;
    private final ModelPart lid;
    private final ModelPart base;

    public BottomlessChestModel(ModelPart root) {
        this.knob = root.getChild("knob");
        this.lid = root.getChild("lid");
        this.base = root.getChild("base");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        root.addOrReplaceChild(
                "knob",
                CubeListBuilder.create()
                        .texOffs(48, 53)
                        .addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-8.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-8.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        PartDefinition base = root.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-8.0F, 24.0F, 8.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        base.addOrReplaceChild(
                "frame",
                CubeListBuilder.create()
                        .texOffs(54, 53).addBox(-5.0F, -1.0F, -0.55F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 0).addBox(4.0F, -1.0F, -0.55F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 43).addBox(-5.0F, -2.0F, -0.55F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 43).addBox(-5.0F, 5.0F, -0.55F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 43).addBox(-4.0F, -1.0F, -0.95F, 8.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, 3.0F, 15.05F)
        );

        PartDefinition bone = base.addOrReplaceChild(
                "bone",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(15.0F, 10.0F, 1.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        bone.addOrReplaceChild(
                "group4",
                CubeListBuilder.create()
                        .texOffs(40, 53).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(50, 45).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        bone.addOrReplaceChild(
                "group3",
                CubeListBuilder.create()
                        .texOffs(32, 53).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 50).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, -14.0F)
        );

        bone.addOrReplaceChild(
                "group",
                CubeListBuilder.create()
                        .texOffs(16, 53).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 45).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-14.0F, 0.0F, 0.0F)
        );

        bone.addOrReplaceChild(
                "group2",
                CubeListBuilder.create()
                        .texOffs(24, 53).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(34, 45).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-14.0F, 0.0F, -14.0F)
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

        knob.render(poseStack, consumer, packedLight, packedOverlay);
        lid.render(poseStack, consumer, packedLight, packedOverlay);
        base.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
