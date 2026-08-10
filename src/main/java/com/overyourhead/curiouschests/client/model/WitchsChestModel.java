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

/** Blockbench model used only by the Witch's Chest block-entity renderer. */
public final class WitchsChestModel {
    private static final float CLOSED_X_ROT = (float) Math.PI;
    private static final float MAX_LID_ANGLE = (float) (Math.PI / 2.0);

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CuriousChestsMod.MOD_ID, "witchs_chest"),
            "main"
    );

    private final ModelPart knob;
    private final ModelPart lid;
    private final ModelPart base;
    private final ModelPart potionShelfLeft;
    private final ModelPart potionShelfRight;
    private final ModelPart potionShelfBack;
    private final ModelPart[] potionLeft;
    private final ModelPart[] potionRight;
    private final ModelPart[] potionBack;

    public WitchsChestModel(ModelPart root) {
        this.knob = root.getChild("knob");
        this.lid = root.getChild("lid");
        this.base = root.getChild("base");
        this.potionShelfLeft = this.base.getChild("potion_shelf_left");
        this.potionShelfRight = this.base.getChild("potion_shelf_right");
        this.potionLeft = new ModelPart[] {
                this.potionShelfLeft.getChild("potion_left_1"),
                this.potionShelfLeft.getChild("potion_left_2"),
                this.potionShelfLeft.getChild("potion_left_3")
        };
        this.potionRight = new ModelPart[] {
                this.potionShelfRight.getChild("potion_right_1"),
                this.potionShelfRight.getChild("potion_right_2"),
                this.potionShelfRight.getChild("potion_right_3")
        };
        this.potionShelfBack = this.base.getChild("potion_shelf_back");
        this.potionBack = new ModelPart[] {
                this.potionShelfBack.getChild("potion_back_1"),
                this.potionShelfBack.getChild("potion_back_2"),
                this.potionShelfBack.getChild("potion_back_3")
        };
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // Put the latch on the same rear hinge as the lid so it opens rigidly with it.
        root.addOrReplaceChild(
                "knob",
                CubeListBuilder.create()
                        .texOffs(56, 86)
                        .addBox(-1.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        // The original Blockbench lid pivot was in the center. These cube offsets preserve
        // the exact closed shape while moving the pivot to the rear hinge at (0, 15, 7).
        root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 22)
                        .addBox(-7.0F, 0.0F, 0.0F, 14.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 57)
                        .addBox(6.0F, 2.0F, 0.0F, 1.0F, 3.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(68, 57)
                        .addBox(-7.0F, 2.0F, 0.0F, 1.0F, 3.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 74)
                        .addBox(-6.0F, 2.0F, 0.0F, 12.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(58, 74)
                        .addBox(-6.0F, 2.0F, 13.0F, 12.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        PartDefinition base = root.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, 1.0F, 0.0F, 14.0F, 8.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 23.0F, 7.0F, CLOSED_X_ROT, 0.0F, 0.0F)
        );

        PartDefinition group = base.addOrReplaceChild(
                "group",
                CubeListBuilder.create()
                        .texOffs(44, 38)
                        .addBox(4.0F, -3.3571F, -8.0F, 3.0F, 3.0F, 16.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 55)
                        .addBox(-5.0F, -3.3571F, -8.0F, 3.0F, 3.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 2.3571F, 7.0F)
        );

        group.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(56, 0)
                        .addBox(-1.5F, -1.5F, -8.0F, 3.0F, 3.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, -1.8571F, -5.0F, 0.0F, -1.5708F, 0.0F)
        );
        group.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create()
                        .texOffs(56, 19)
                        .addBox(-1.5F, -1.5F, -8.0F, 3.0F, 3.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, -1.8571F, 5.0F, 0.0F, -1.5708F, 0.0F)
        );
        group.addOrReplaceChild(
                "cube_r3",
                CubeListBuilder.create()
                        .texOffs(4, 44)
                        .addBox(-5.0F, -0.5F, -8.0F, 8.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.7F, -2.3571F, 1.0F, 0.0F, -1.5708F, 0.0F)
        );

        PartDefinition potionShelfLeft = base.addOrReplaceChild(
                "potion_shelf_left",
                CubeListBuilder.create()
                        .texOffs(28, 93)
                        .addBox(-0.8F, -0.5F, -7.0F, 1.0F, 1.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offset(7.5F, 4.5F, 7.0F)
        );
        potionShelfLeft.addOrReplaceChild("potion_left_1", CubeListBuilder.create(), PartPose.offset(-0.3F, 0.0F, 4.5F));
        potionShelfLeft.addOrReplaceChild("potion_left_2", CubeListBuilder.create(), PartPose.offset(-0.3F, 0.0F, 0.0F));
        potionShelfLeft.addOrReplaceChild("potion_left_3", CubeListBuilder.create(), PartPose.offset(-0.3F, 0.0F, -4.5F));

        PartDefinition potionShelfRight = base.addOrReplaceChild(
                "potion_shelf_right",
                CubeListBuilder.create()
                        .texOffs(28, 93)
                        .addBox(-0.5F, -0.5F, -7.0F, 1.0F, 1.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-7.3F, 4.5F, 7.0F)
        );
        potionShelfRight.addOrReplaceChild("potion_right_1", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -4.5F));
        potionShelfRight.addOrReplaceChild("potion_right_2", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        potionShelfRight.addOrReplaceChild("potion_right_3", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 4.5F));

        // Latest Blockbench fix: the rear shelf now belongs to base, matching the side
        // shelves' coordinate space and vertical placement. Keep the proven side-shelf UV
        // because the authored 11,91 region in the current witch.png is fully transparent.
        PartDefinition potionShelfBack = base.addOrReplaceChild(
                "potion_shelf_back",
                CubeListBuilder.create()
                        .texOffs(28, 93)
                        .addBox(-0.5F, -0.5F, -7.3F, 1.0F, 1.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.3F, 4.5F, 0.0F, 0.0F, -1.5708F, 0.0F)
        );
        potionShelfBack.addOrReplaceChild("potion_back_1", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -4.5F));
        potionShelfBack.addOrReplaceChild("potion_back_2", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        potionShelfBack.addOrReplaceChild("potion_back_3", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 4.5F));

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

    /** Applies the authored empty marker transform for one decorative potion. */
    public void applyPotionTransform(PoseStack poseStack, int side, int index) {
        // All three shelves are children of base in the latest Blockbench export, so all
        // potion anchors inherit the exact same 180-degree X basis and Y placement.
        base.translateAndRotate(poseStack);
        if (side == 0) {
            potionShelfLeft.translateAndRotate(poseStack);
            potionLeft[index].translateAndRotate(poseStack);
        } else if (side == 1) {
            potionShelfRight.translateAndRotate(poseStack);
            potionRight[index].translateAndRotate(poseStack);
        } else {
            potionShelfBack.translateAndRotate(poseStack);
            potionBack[index].translateAndRotate(poseStack);
        }
    }
}
