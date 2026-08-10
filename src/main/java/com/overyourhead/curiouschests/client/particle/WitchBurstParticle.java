package com.overyourhead.curiouschests.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** A short, soft reaction puff used only when new potions are added. */
public final class WitchBurstParticle extends TextureSheetParticle {
    private final float peakAlpha;

    private WitchBurstParticle(
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
        this.friction = 0.965F;
        this.gravity = -0.0002F;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize *= 0.62F + this.random.nextFloat() * 0.45F;
        this.lifetime = 34 + this.random.nextInt(22);
        this.peakAlpha = 0.62F + this.random.nextFloat() * 0.18F;
        this.alpha = 0.0F;
        this.rCol = 0.94F;
        this.gCol = 0.86F + this.random.nextFloat() * 0.08F;
        this.bCol = 1.0F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float progress = (float) this.age / (float) this.lifetime;
        if (progress < 0.10F) {
            this.alpha = peakAlpha * (progress / 0.10F);
        } else {
            float fade = 1.0F - (progress - 0.10F) / 0.90F;
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
            return new WitchBurstParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
