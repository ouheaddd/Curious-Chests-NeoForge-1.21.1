package com.overyourhead.curiouschests.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Slow, low witch-vapor particle. Each spawned particle keeps one random sprite variant. */
public final class WitchSteamParticle extends TextureSheetParticle {
    private final float peakAlpha;

    private WitchSteamParticle(
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
        this.friction = 0.975F;
        this.gravity = -0.00035F;
        this.speedUpWhenYMotionIsBlocked = false;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize *= 0.52F + this.random.nextFloat() * 0.62F;
        this.lifetime = 62 + this.random.nextInt(38);
        this.peakAlpha = 0.52F + this.random.nextFloat() * 0.22F;
        this.alpha = 0.0F;
        this.rCol = 0.88F + this.random.nextFloat() * 0.05F;
        this.gCol = 0.82F + this.random.nextFloat() * 0.05F;
        this.bCol = 0.98F;

        // The 10 textures are variations, not animation frames. Pick one once so
        // the curl drifts slowly instead of rapidly morphing through the whole atlas.
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
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
            return new WitchSteamParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
