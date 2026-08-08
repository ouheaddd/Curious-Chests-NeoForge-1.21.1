package com.overyourhead.curiouschests.core;

import com.mojang.serialization.Codec;
import com.overyourhead.curiouschests.CuriousChestsMod;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;

/** Persistent stack data shared by Resonant Chests and their crystals. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(CuriousChestsMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> RESONANCE_ID =
            COMPONENTS.registerComponentType(
                    "resonance_id",
                    builder -> builder.persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString))
            );

    /** Storage slots that arrived through resonance and must not bounce back automatically. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Integer>>> RESONANCE_RECEIVED_SLOTS =
            COMPONENTS.registerComponentType(
                    "resonance_received_slots",
                    builder -> builder.persistent(Codec.INT.listOf())
            );

    private ModDataComponents() {}
}
