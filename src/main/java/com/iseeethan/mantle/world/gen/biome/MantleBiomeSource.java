package com.iseeethan.mantle.world.gen.biome;

import com.iseeethan.mantle.world.MantleWorld;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.EnumMap;
import java.util.stream.Stream;

public final class MantleBiomeSource extends BiomeSource {

    public static final MapCodec<MantleBiomeSource> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    RegistryOps.retrieveGetter(Registries.BIOME)
            ).apply(instance, MantleBiomeSource::new));

    private final HolderGetter<Biome> biomes;
    private final EnumMap<MantleBiomes, Holder<Biome>> holders;

    private volatile MantleBiomeClassifier classifier;
    private volatile long classifierSeed = Long.MIN_VALUE;

    public MantleBiomeSource(HolderGetter<Biome> biomes) {
        this.biomes = biomes;
        this.holders = new EnumMap<>(MantleBiomes.class);
        for (MantleBiomes b : MantleBiomes.values()) {
            holders.put(b, biomes.getOrThrow(b.key()));
        }
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return holders.values().stream();
    }

    private MantleBiomeClassifier classifier() {
        MantleWorld world = MantleWorld.get();
        long seed = world.sim().seed();
        MantleBiomeClassifier c = classifier;
        if (c == null || classifierSeed != seed) {
            c = new MantleBiomeClassifier(world.sim(), MantleWorld.SEA_Y, 4);
            classifier = c;
            classifierSeed = seed;
        }
        return c;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        int wx = qx << 2;
        int wz = qz << 2;
        MantleWorld world = MantleWorld.get();
        if (!world.inWorld(wx, wz)) {
            return holders.get(MantleBiomes.OCEAN);
        }
        MantleBiomes b = classifier().classify(wx, wz);
        Holder<Biome> h = holders.get(b);
        return h != null ? h : holders.get(MantleBiomes.GRASSLAND);
    }
}
