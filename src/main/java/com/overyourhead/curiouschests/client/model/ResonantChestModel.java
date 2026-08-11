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

/** Blockbench model used only by the Resonant Chest block-entity renderer. */
public final class ResonantChestModel {
    private static final float CLOSED_X_ROT = (float) Math.PI;
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "resonant_chest"),
            "main"
    );

    private final ModelPart knob;
    private final ModelPart lid;
    private final ModelPart base;
    private final ModelPart lidCrystalCenter;
    private final ModelPart lidCrystalBack;
    private final ModelPart lidCrystalRight;
    private final ModelPart lidCrystalFront;
    private final ModelPart lidCrystalLeft;
    private final ModelPart baseCrystalRight;
    private final ModelPart baseCrystalBack;
    private final ModelPart baseCrystalLeft;

    public ResonantChestModel(ModelPart root) {
        this.knob = root.getChild("knob");
        this.lid = root.getChild("lid");
        this.base = root.getChild("base");
        this.lidCrystalCenter = this.lid.getChild("bone6");
        this.lidCrystalBack = this.lid.getChild("bone8");
        this.lidCrystalRight = this.lid.getChild("bone4");
        this.lidCrystalFront = this.lid.getChild("bone2");
        this.lidCrystalLeft = this.lid.getChild("bone");
        this.baseCrystalRight = this.base.getChild("bone5");
        this.baseCrystalBack = this.base.getChild("bone7");
        this.baseCrystalLeft = this.base.getChild("bone3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // The Blockbench export used a center pivot. Move lid + latch to the rear hinge
        // while shifting their authored local coordinates so the CLOSED geometry stays identical.
        root.addOrReplaceChild(
                "knob",
                CubeListBuilder.create()
                        .texOffs(0, 43)
                        .addBox(-1.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        PartDefinition lid = root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-7.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        PartDefinition bone6 = lid.addOrReplaceChild(
                "bone6",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(7.5F, 2.75F, 7.15F, 1.5708F, 1.5708F, 0.0F)
        );
        bone6.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create().texOffs(48, 51)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
        );
        bone6.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create().texOffs(48, 24)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F)
        );
        bone6.addOrReplaceChild(
                "cube_r3",
                CubeListBuilder.create().texOffs(48, 24)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
        );
        bone6.addOrReplaceChild(
                "cube_r4",
                CubeListBuilder.create().texOffs(48, 51)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F)
        );

        PartDefinition bone8 = lid.addOrReplaceChild(
                "bone8",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-2.5F, 2.75F, -1.85F, -1.5708F, 0.0F, 1.5708F)
        );
        bone8.addOrReplaceChild(
                "cube_r5",
                CubeListBuilder.create().texOffs(48, 51)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
        );
        bone8.addOrReplaceChild(
                "cube_r6",
                CubeListBuilder.create().texOffs(48, 51)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F)
        );

        PartDefinition bone4 = lid.addOrReplaceChild(
                "bone4",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-7.5F, 2.75F, 7.15F, -1.5708F, 1.5708F, 0.0F)
        );
        bone4.addOrReplaceChild(
                "cube_r7",
                CubeListBuilder.create().texOffs(48, 51)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.1F, 1.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
        );
        bone4.addOrReplaceChild(
                "cube_r8",
                CubeListBuilder.create().texOffs(48, 24)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F)
        );
        bone4.addOrReplaceChild(
                "cube_r9",
                CubeListBuilder.create().texOffs(48, 24)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
        );
        bone4.addOrReplaceChild(
                "cube_r10",
                CubeListBuilder.create().texOffs(48, 51)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.1F, 1.0F, 0.0F, 0.0F, 0.7854F, 0.0F)
        );

        PartDefinition bone2 = lid.addOrReplaceChild(
                "bone2",
                CubeListBuilder.create(),
                PartPose.offset(1.0F, 9.0F, 8.0F)
        );
        bone2.addOrReplaceChild(
                "cube_r11",
                CubeListBuilder.create().texOffs(48, 43)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, 0.0F, -4.4F, 0.0F, -0.7854F, 0.0F)
        );
        bone2.addOrReplaceChild(
                "cube_r12",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, 0.0F, -5.0F, 0.0F, -0.7854F, 0.0F)
        );
        bone2.addOrReplaceChild(
                "cube_r13",
                CubeListBuilder.create().texOffs(48, 24)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, 0.0F, 3.0F, 0.0F, -0.7854F, 0.0F)
        );
        bone2.addOrReplaceChild(
                "cube_r14",
                CubeListBuilder.create().texOffs(48, 51)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, 0.0F, 3.0F, 0.0F, -0.7854F, 0.0F)
        );
        bone2.addOrReplaceChild(
                "cube_r15",
                CubeListBuilder.create().texOffs(48, 51)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, 0.0F, 3.0F, 0.0F, 0.7854F, 0.0F)
        );
        bone2.addOrReplaceChild(
                "cube_r16",
                CubeListBuilder.create().texOffs(48, 43)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, 0.0F, -4.4F, 0.0F, 0.7854F, 0.0F)
        );
        bone2.addOrReplaceChild(
                "cube_r17",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, 0.0F, -5.0F, 0.0F, 0.7854F, 0.0F)
        );
        bone2.addOrReplaceChild(
                "cube_r18",
                CubeListBuilder.create().texOffs(48, 24)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, 0.0F, 3.0F, 0.0F, 0.7854F, 0.0F)
        );

        PartDefinition bone = lid.addOrReplaceChild(
                "bone",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 11.0F, 7.0F)
        );
        bone.addOrReplaceChild(
                "cube_r19",
                CubeListBuilder.create()
                        .texOffs(0, 52)
                        .addBox(-8.0F, -11.0F, 0.0F, 16.0F, 12.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 36)
                        .addBox(0.0F, -11.0F, -8.0F, 0.0F, 12.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
        );

        PartDefinition base = root.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -5.0F, -7.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        PartDefinition bone5 = base.addOrReplaceChild(
                "bone5",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(7.0F, -0.85F, 3.0F, 1.5708F, 1.5708F, 0.0F)
        );
        bone5.addOrReplaceChild(
                "cube_r20",
                CubeListBuilder.create().texOffs(48, 43)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(7.0F, 1.0F, -3.55F, 0.0F, -0.7854F, 0.0F)
        );
        bone5.addOrReplaceChild(
                "cube_r21",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 3.0F, -2.15F, 0.0F, -0.7854F, 0.0F)
        );
        bone5.addOrReplaceChild(
                "cube_r22",
                CubeListBuilder.create().texOffs(48, 43)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(7.0F, 1.0F, -3.55F, 0.0F, 0.7854F, 0.0F)
        );
        bone5.addOrReplaceChild(
                "cube_r23",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 3.0F, -2.15F, 0.0F, 0.7854F, 0.0F)
        );

        PartDefinition bone7 = base.addOrReplaceChild(
                "bone7",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.0F, 0.8F, -9.5F, -1.5708F, 0.0F, 0.7854F)
        );
        bone7.addOrReplaceChild(
                "cube_r24",
                CubeListBuilder.create().texOffs(48, 43)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.5F, -1.0F, -0.7F, 0.0F, -0.7854F, 0.0F)
        );
        bone7.addOrReplaceChild(
                "cube_r25",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.5F, 1.0F, 0.7F, 0.0F, -0.7854F, 0.0F)
        );
        bone7.addOrReplaceChild(
                "cube_r26",
                CubeListBuilder.create().texOffs(48, 43)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.5F, -1.0F, -0.7F, 0.0F, 0.7854F, 0.0F)
        );
        bone7.addOrReplaceChild(
                "cube_r27",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.5F, 1.0F, 0.7F, 0.0F, 0.7854F, 0.0F)
        );

        PartDefinition bone3 = base.addOrReplaceChild(
                "bone3",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-9.0F, 1.3F, -0.5F, -1.5708F, 1.5708F, 0.0F)
        );
        bone3.addOrReplaceChild(
                "cube_r28",
                CubeListBuilder.create().texOffs(48, 43)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.5F, -1.0F, -0.7F, 0.0F, -0.7854F, 0.0F)
        );
        bone3.addOrReplaceChild(
                "cube_r29",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.5F, 1.0F, 0.7F, 0.0F, -0.7854F, 0.0F)
        );
        bone3.addOrReplaceChild(
                "cube_r30",
                CubeListBuilder.create().texOffs(48, 43)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.5F, -1.0F, -0.7F, 0.0F, 0.7854F, 0.0F)
        );
        bone3.addOrReplaceChild(
                "cube_r31",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.5F, 1.0F, 0.7F, 0.0F, 0.7854F, 0.0F)
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
        renderMain(poseStack, consumer, openness, packedLight, packedOverlay);
        renderCrystals(poseStack, consumer, openness, packedLight, packedOverlay);
    }

    public void renderMain(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        setupAnimation(openness);
        setCrystalsVisible(false);
        try {
            knob.render(poseStack, consumer, packedLight, packedOverlay);
            lid.render(poseStack, consumer, packedLight, packedOverlay);
            base.render(poseStack, consumer, packedLight, packedOverlay);
        } finally {
            setCrystalsVisible(true);
        }
    }

    public void renderCrystals(
            PoseStack poseStack,
            VertexConsumer consumer,
            float openness,
            int packedLight,
            int packedOverlay
    ) {
        setupAnimation(openness);

        // Important: these parts are children of lid/base. Apply the parent transform
        // first, then render the exact authored child parts. This preserves the v2
        // positions, rotations and lid animation 1:1.
        poseStack.pushPose();
        lid.translateAndRotate(poseStack);
        lidCrystalCenter.render(poseStack, consumer, packedLight, packedOverlay);
        lidCrystalBack.render(poseStack, consumer, packedLight, packedOverlay);
        lidCrystalRight.render(poseStack, consumer, packedLight, packedOverlay);
        lidCrystalFront.render(poseStack, consumer, packedLight, packedOverlay);
        lidCrystalLeft.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        base.translateAndRotate(poseStack);
        baseCrystalRight.render(poseStack, consumer, packedLight, packedOverlay);
        baseCrystalBack.render(poseStack, consumer, packedLight, packedOverlay);
        baseCrystalLeft.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void setupAnimation(float openness) {
        float lidRotation = CLOSED_X_ROT - openness * MAX_LID_ANGLE;
        lid.xRot = lidRotation;
        knob.xRot = lidRotation;
    }

    private void setCrystalsVisible(boolean visible) {
        lidCrystalCenter.visible = visible;
        lidCrystalBack.visible = visible;
        lidCrystalRight.visible = visible;
        lidCrystalFront.visible = visible;
        lidCrystalLeft.visible = visible;
        baseCrystalRight.visible = visible;
        baseCrystalBack.visible = visible;
        baseCrystalLeft.visible = visible;
    }
}
