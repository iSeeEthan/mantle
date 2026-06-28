package com.iseeethan.mantle.world.gen.ecology.registries;

import com.iseeethan.mantle.Mantle;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

public record RainfallConfig(
        double baseFrequency,
        int baseOctaves,
        double baseLacunarity,
        double baseGain,
        double warpFrequency,
        int warpOctaves,
        double warpLacunarity,
        double warpGain,
        double warpStrength,
        double equatorFalloffPower,
        double minPoleTempFactor
) {
    public static final ResourceKey<Registry<RainfallConfig>> RAINFALL_CONFIGS_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Mantle.MOD_ID, "ecology/rainfall"));

    public static final DeferredRegister<RainfallConfig> RAINFALL_CONFIGS_DEFERRED_REGISTER =
            DeferredRegister.create(RAINFALL_CONFIGS_REGISTRY_KEY, Mantle.MOD_ID);



    // Default configuration, if you want to provide one
    public static final RainfallConfig DEFAULT = new RainfallConfig(
            1.0 / 2500.0, // baseFrequency (Large scale weather)
            5,            // baseOctaves (Keep it smooth)
            1.5,          // baseLacunarity
            1.5,         // baseGain (Low gain = solid bands)
            1.2 / 5000.0, // warpFrequency (Slow, continental wiggles)
            6,            // warpOctaves
            2.0,          // warpLacunarity
            0.55,          // warpGain
            0.6,         // warpStrength (Don't shred the bands)
            0.75,          // equatorFalloffPower (High = wide tropical belt)
            0.1          // minPoleTempFactor
    );

    public static final Codec<RainfallConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("base_frequency").forGetter(RainfallConfig::baseFrequency),
            Codec.INT.fieldOf("base_octaves").forGetter(RainfallConfig::baseOctaves),
            Codec.DOUBLE.fieldOf("base_lacunarity").forGetter(RainfallConfig::baseLacunarity),
            Codec.DOUBLE.fieldOf("base_gain").forGetter(RainfallConfig::baseGain),
            Codec.DOUBLE.fieldOf("warp_frequency").forGetter(RainfallConfig::warpFrequency),
            Codec.INT.fieldOf("warp_octaves").forGetter(RainfallConfig::warpOctaves),
            Codec.DOUBLE.fieldOf("warp_lacunarity").forGetter(RainfallConfig::warpLacunarity),
            Codec.DOUBLE.fieldOf("warp_gain").forGetter(RainfallConfig::warpGain),
            Codec.DOUBLE.fieldOf("warp_strength").forGetter(RainfallConfig::warpStrength),
            Codec.DOUBLE.fieldOf("equator_falloff_power").forGetter(RainfallConfig::equatorFalloffPower),
            Codec.DOUBLE.fieldOf("min_pole_temp_factor").forGetter(RainfallConfig::minPoleTempFactor)
    ).apply(instance, RainfallConfig::new));
}
