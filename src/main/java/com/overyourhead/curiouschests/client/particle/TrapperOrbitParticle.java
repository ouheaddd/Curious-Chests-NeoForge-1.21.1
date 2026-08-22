package com.overyourhead.curiouschests.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/**
 * Soft magical mote replacing the Trapper Chest's Reverse Portal flashes.
 * The particle drifts gently so the occupied chest looks charged without the
 * heavy purple portal aesthetic.
 */
public final class TrapperOrbitParticle extends TextureSheetParticle {
    private final float peakAlpha;

    private TrapperOrbitParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.friction = 0.95F;
        this.gravity = -0.00015F;
        this.hasPhysics = false;
        this.speedUpWhenYMotionIsBlocked = false;
        this.xd = xSpeed * 0.45D;
        this.yd = ySpeed * 0.45D + 0.002D;
        this.zd = zSpeed * 0.45D;
        this.quadSize *= 0.32F + this.random.nextFloat() * 0.22F;
        this.lifetime = 18 + this.random.nextInt(12);
        this.peakAlpha = 0.56F + this.random.nextFloat() * 0.18F;
        this.alpha = 0.0F;
        this.rCol = 0.90F + this.random.nextFloat() * 0.05F;
        this.gCol = 0.82F + this.random.nextFloat() * 0.08F;
        this.bCol = 0.56F + this.random.nextFloat() * 0.05F;
        this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.oRoll = this.roll;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        this.roll += 0.04F;
        float progress = (float) this.age / (float) this.lifetime;
        if (progress < 0.18F) {
            this.alpha = peakAlpha * (progress / 0.18F);
        } else {
            float fade = 1.0F - (progress - 0.18F) / 0.82F;
            this.alpha = peakAlpha * Math.max(0.0F, fade * fade);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new TrapperOrbitParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
