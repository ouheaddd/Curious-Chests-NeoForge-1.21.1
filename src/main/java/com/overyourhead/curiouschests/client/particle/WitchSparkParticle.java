package com.overyourhead.curiouschests.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Small slow-moving magical mote. Uses one random sprite variation per particle. */
public final class WitchSparkParticle extends TextureSheetParticle {
    private final float peakAlpha;

    private WitchSparkParticle(
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
        this.friction = 0.955F;
        this.gravity = -0.00015F;
        this.speedUpWhenYMotionIsBlocked = false;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize *= 0.24F + this.random.nextFloat() * 0.22F;
        this.lifetime = 34 + this.random.nextInt(26);
        this.peakAlpha = 0.82F + this.random.nextFloat() * 0.16F;
        this.alpha = 0.0F;
        this.rCol = 0.96F;
        this.gCol = 0.91F + this.random.nextFloat() * 0.07F;
        this.bCol = 1.0F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float progress = (float) this.age / (float) this.lifetime;
        if (progress < 0.12F) {
            this.alpha = peakAlpha * (progress / 0.12F);
        } else {
            this.alpha = peakAlpha * Math.max(0.0F, 1.0F - progress);
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
            return new WitchSparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
