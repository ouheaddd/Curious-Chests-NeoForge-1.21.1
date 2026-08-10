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

/** Blockbench model used only by the Builder's Chest block-entity renderer. */
public final class BuildersChestModel {
    private static final float CLOSED_X_ROT = (float) Math.PI;
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "builders_chest"),
            "main"
    );

    private final ModelPart knob;
    private final ModelPart lid;
    private final ModelPart base;

    public BuildersChestModel(ModelPart root) {
        this.knob = root.getChild("knob");
        this.lid = root.getChild("lid");
        this.base = root.getChild("base");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // Same hinge fix as the custom Ender chest, but preserving the author's
        // intentional +0.5 px lifted placement from Blockbench.
        root.addOrReplaceChild(
                "knob",
                CubeListBuilder.create()
                        .texOffs(16, 47)
                        .addBox(-1.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 14.5F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        PartDefinition lid = root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-7.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 14.5F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        lid.addOrReplaceChild(
                "bone",
                CubeListBuilder.create()
                        .texOffs(0, 43)
                        .addBox(-7.5F, 1.0F, 5.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 43)
                        .addBox(5.5F, 1.0F, 5.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 43)
                        .addBox(5.5F, 1.0F, -7.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(24, 43)
                        .addBox(-7.5F, 1.0F, -7.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 2.5F, 7.0F)
        );

        PartDefinition base = root.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -5.0F, -7.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 18.5F, 0.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        base.addOrReplaceChild(
                "bone2",
                CubeListBuilder.create()
                        .texOffs(32, 43)
                        .addBox(-7.5F, 1.0F, 5.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 43)
                        .addBox(5.5F, 1.0F, 5.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 47)
                        .addBox(5.5F, 1.0F, -7.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 47)
                        .addBox(-7.5F, 1.0F, -7.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -6.5F, 0.0F)
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
        float lidRotation = CLOSED_X_ROT - openness * MAX_LID_ANGLE;
        lid.xRot = lidRotation;
        knob.xRot = lidRotation;

        knob.render(poseStack, consumer, packedLight, packedOverlay);
        lid.render(poseStack, consumer, packedLight, packedOverlay);
        base.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
