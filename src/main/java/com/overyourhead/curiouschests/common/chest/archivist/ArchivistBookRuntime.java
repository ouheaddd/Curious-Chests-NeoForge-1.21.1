package com.overyourhead.curiouschests.common.chest.archivist;

import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Client-side animation state for the floating Archivist book. */
public final class ArchivistBookRuntime {
    private final SpecialChestBlockEntity owner;
    private int time;
    private float flip;
    private float oldFlip;
    private float flipTarget;
    private float flipVelocity;
    private float open;
    private float oldOpen;
    private float rot;
    private float oldRot;
    private float targetRot;

    public ArchivistBookRuntime(SpecialChestBlockEntity owner) {
        this.owner = owner;
    }

    public int time() { return time; }
    public float flip() { return flip; }
    public float oldFlip() { return oldFlip; }
    public float open() { return open; }
    public float oldOpen() { return oldOpen; }
    public float rot() { return rot; }
    public float oldRot() { return oldRot; }

    public void clientTick(Level level, BlockPos pos) {
        oldOpen = open;
        oldRot = rot;

        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        Player player = level.getNearestPlayer(centerX, centerY, centerZ, 3.0D, false);

        if (player != null) {
            double dx = player.getX() - centerX;
            double dz = player.getZ() - centerZ;
            targetRot = (float) Mth.atan2(dz, dx);
            open += 0.1F;

            if (open < 0.5F || level.random.nextInt(40) == 0) {
                float previousTarget = flipTarget;
                do {
                    flipTarget += level.random.nextInt(4) - level.random.nextInt(4);
                } while (previousTarget == flipTarget);
            }
        } else {
            targetRot += 0.02F;
            open -= 0.1F;
        }

        while (rot >= Math.PI) rot -= (float) (Math.PI * 2.0D);
        while (rot < -Math.PI) rot += (float) (Math.PI * 2.0D);
        while (targetRot >= Math.PI) targetRot -= (float) (Math.PI * 2.0D);
        while (targetRot < -Math.PI) targetRot += (float) (Math.PI * 2.0D);

        float rotationDelta = targetRot - rot;
        while (rotationDelta >= Math.PI) rotationDelta -= (float) (Math.PI * 2.0D);
        while (rotationDelta < -Math.PI) rotationDelta += (float) (Math.PI * 2.0D);
        rot += rotationDelta * 0.4F;

        open = Mth.clamp(open, 0.0F, 1.0F);
        oldFlip = flip;
        float flipDelta = (flipTarget - flip) * 0.4F;
        flipDelta = Mth.clamp(flipDelta, -0.2F, 0.2F);
        flipVelocity += (flipDelta - flipVelocity) * 0.9F;
        flip += flipVelocity;
        time++;
    }
}
