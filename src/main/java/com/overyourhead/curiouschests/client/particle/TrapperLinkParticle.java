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
 * Short, readable replacement for Vault Connection used by the Trapper Chest.
 * It keeps the same spawn contract (position + velocity / offset vector), but
 * renders with our own small cyan-gold sprites for 1.20 portability.
 */
public final class TrapperLinkParticle extends TextureSheetParticle {
    private final float peakAlpha;

    private TrapperLinkParticle(
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
        this.friction = 0.92F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.speedUpWhenYMotionIsBlocked = false;
        this.xd = xSpeed * 0.12D;
        this.yd = ySpeed * 0.12D;
        this.zd = zSpeed * 0.12D;
        this.quadSize *= 0.18F + this.random.nextFloat() * 0.10F;
        this.lifetime = 12 + this.random.nextInt(8);
        this.peakAlpha = 0.78F + this.random.nextFloat() * 0.18F;
        this.alpha = 0.0F;
        this.rCol = 0.82F + this.random.nextFloat() * 0.08F;
        this.gCol = 0.91F + this.random.nextFloat() * 0.06F;
        this.bCol = 0.99F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float progress = (float) this.age / (float) this.lifetime;
        if (progress < 0.22F) {
            this.alpha = peakAlpha * (progress / 0.22F);
        } else {
            float fade = 1.0F - (progress - 0.22F) / 0.78F;
            this.alpha = peakAlpha * Math.max(0.0F, fade);
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
            return new TrapperLinkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
