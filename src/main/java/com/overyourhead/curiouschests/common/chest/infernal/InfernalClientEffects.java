package com.overyourhead.curiouschests.common.chest.infernal;

import com.overyourhead.curiouschests.common.block.AbstractSpecialChestBlock;
import com.overyourhead.curiouschests.common.blockentity.SpecialChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Stateless client ambience for Infernal Chest. */
public final class InfernalClientEffects {
    private InfernalClientEffects() {}

    public static void tick(Level level, BlockPos pos, SpecialChestBlockEntity chest) {
        if (level.random.nextDouble() < 0.10D) {
            level.playLocalSound(
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    SoundEvents.BLASTFURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS,
                    0.62F,
                    1.0F,
                    false
            );
        }
        if (level.random.nextFloat() < 0.12F) spawnParticles(level, pos, chest.getBlockState());
    }

    private static void spawnParticles(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.hasProperty(AbstractSpecialChestBlock.FACING)
                ? state.getValue(AbstractSpecialChestBlock.FACING)
                : Direction.NORTH;

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 2.0D / 16.0D + level.random.nextDouble() * 6.0D / 16.0D;
        double z = pos.getZ() + 0.5D;
        double tangent = level.random.nextDouble() * 0.6D - 0.3D;
        double forward = 0.52D;

        switch (facing) {
            case WEST -> { x -= forward; z += tangent; }
            case EAST -> { x += forward; z += tangent; }
            case NORTH -> { x += tangent; z -= forward; }
            case SOUTH -> { x += tangent; z += forward; }
            default -> { x += tangent; z -= forward; }
        }

        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        if (level.random.nextFloat() < 0.65F) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
}
