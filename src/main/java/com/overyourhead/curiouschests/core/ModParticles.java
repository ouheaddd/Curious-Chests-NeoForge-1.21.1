package com.overyourhead.curiouschests.core;

import com.overyourhead.curiouschests.CuriousChestsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(
            Registries.PARTICLE_TYPE,
            CuriousChestsMod.MOD_ID
    );

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WITCH_STEAM = PARTICLES.register(
            "witch_steam",
            () -> new SimpleParticleType(false)
    );

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WITCH_SPARK = PARTICLES.register(
            "witch_spark",
            () -> new SimpleParticleType(false)
    );

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WITCH_BURST = PARTICLES.register(
            "witch_burst",
            () -> new SimpleParticleType(false)
    );

    private ModParticles() {}
}
