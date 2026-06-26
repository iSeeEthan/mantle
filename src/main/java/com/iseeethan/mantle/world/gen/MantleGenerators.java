package com.iseeethan.mantle.world.gen;

import com.iseeethan.mantle.Mantle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class MantleGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Mantle.MOD_ID);

    public static final Supplier<MapCodec<? extends ChunkGenerator>> MANTLE =
            CHUNK_GENERATORS.register("mantle", () -> MantleChunkGenerator.CODEC);

    private MantleGenerators() {}

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
    }
}
