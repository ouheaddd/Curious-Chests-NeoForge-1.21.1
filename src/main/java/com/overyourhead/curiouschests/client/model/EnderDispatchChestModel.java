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

/** Blockbench model used only by the Ender Dispatch Chest block-entity renderer. */
public final class EnderDispatchChestModel {
    private static final float CLOSED_X_ROT = (float) Math.PI;
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "ender_dispatch_chest"),
            "main"
    );

    private final ModelPart knob;
    private final ModelPart lid;
    private final ModelPart base;

    public EnderDispatchChestModel(ModelPart root) {
        this.knob = root.getChild("knob");
        this.lid = root.getChild("lid");
        this.base = root.getChild("base");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // The latch uses the SAME hinge pivot as the lid. That makes it travel with
        // the lid instead of rotating in place around its own center.
        root.addOrReplaceChild(
                "knob",
                CubeListBuilder.create()
                        .texOffs(32, 43)
                        .addBox(-1.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        // Pivot the lid on its rear edge, matching the working Collector/vanilla chest motion.
        // The cube is translated relative to that hinge so the CLOSED geometry stays
        // in exactly the same place as the Blockbench export.
        PartDefinition lid = root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-7.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        // Keep the raised center piece attached to the lid. Its local transform is
        // adjusted only to preserve its original closed position after moving the hinge.
        lid.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(0, 43)
                        .addBox(-4.0F, -1.5F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 4.5F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -5.0F, -7.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, CLOSED_X_ROT, 0.0F, 0.0F)
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
