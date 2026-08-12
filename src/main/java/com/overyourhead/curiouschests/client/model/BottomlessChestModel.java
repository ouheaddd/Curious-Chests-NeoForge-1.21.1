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

/** Compression Chest model authored as the internal bottomless_chest asset. */
public final class BottomlessChestModel {
    private static final float CLOSED_X_ROT = (float) Math.PI;
    private static final float CLOSED_KNOB_Y = 15.0F;
    private static final float CLOSED_LID_Y = 12.5F;
    private static final float PRESS_TRAVEL = 4.0F;

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
                        .texOffs(6, 42)
                        .addBox(-1.0F, -1.0F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, CLOSED_KNOB_Y, -7.5F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-7.0F, -1.5F, -7.0F, 14.0F, 4.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, CLOSED_LID_Y, 0.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -5.0F, -7.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 42)
                        .addBox(-8.0F, -5.0F, -1.0F, 1.0F, 19.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 42)
                        .addBox(7.0F, -5.0F, -1.0F, 1.0F, 19.0F, 2.0F, new CubeDeformation(0.0F)),
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
        // Model-space Y is inverted by the standard custom-chest transform. Moving
        // lid/knob 4 px toward negative model Y lifts the press plate 4 px in-world.
        float lift = openness * PRESS_TRAVEL;
        lid.xRot = CLOSED_X_ROT;
        knob.xRot = CLOSED_X_ROT;
        lid.y = CLOSED_LID_Y - lift;
        knob.y = CLOSED_KNOB_Y - lift;

        knob.render(poseStack, consumer, packedLight, packedOverlay);
        lid.render(poseStack, consumer, packedLight, packedOverlay);
        base.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
