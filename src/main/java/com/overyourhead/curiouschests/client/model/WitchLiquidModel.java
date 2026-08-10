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

/** Separate 16x16-textured surface for the animated Witch's Chest liquid. */
public final class WitchLiquidModel {
    private static final float CLOSED_X_ROT = (float) Math.PI;
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "witchs_chest_liquid"),
            "main"
    );

    private final ModelPart liquid;

    public WitchLiquidModel(ModelPart root) {
        this.liquid = root.getChild("liquid");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // This is the former 12x0x12 plane exported inside lid. It is split into its
        // own 16x16-textured model so the whole liquid frame maps cleanly to the surface.
        root.addOrReplaceChild(
                "liquid",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-6.0F, 4.0F, 1.0F, 12.0F, 0.01F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        return LayerDefinition.create(meshDefinition, 16, 16);
    }

    public void render(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        liquid.xRot = CLOSED_X_ROT - openness * MAX_LID_ANGLE;
        liquid.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
