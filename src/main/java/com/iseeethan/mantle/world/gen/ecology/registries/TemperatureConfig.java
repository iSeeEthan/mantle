package com.iseeethan.mantle.world.gen.ecology.registries;

import com.iseeethan.mantle.Mantle;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

public record TemperatureConfig(
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
    public static final ResourceKey<Registry<TemperatureConfig>> TEMPERATURE_CONFIGS_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Mantle.MOD_ID, "ecology/temperature"));

    public static final DeferredRegister<TemperatureConfig> TEMPERATURE_CONFIGS_DEFERRED_REGISTER =
            DeferredRegister.create(TEMPERATURE_CONFIGS_REGISTRY_KEY, Mantle.MOD_ID);


    // Default configuration, if you want to provide one
    public static final TemperatureConfig DEFAULT = new TemperatureConfig(
            1.0 / 2500.0, // baseFrequency (Large scale weather)
            5,            // baseOctaves (Keep it smooth)
            1.0,          // baseLacunarity
            1.0,         // baseGain (Low gain = solid bands)
            1.0 / 5000.0, // warpFrequency (Slow, continental wiggles)
            6,            // warpOctaves
            2.0,          // warpLacunarity
            0.55,          // warpGain
            0.25,         // warpStrength (Don't shred the bands)
            1.1,          // equatorFalloffPower (High = wide tropical belt)
            0.1          // minPoleTempFactor
    );

    public static final Codec<TemperatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("base_frequency").forGetter(TemperatureConfig::baseFrequency),
            Codec.INT.fieldOf("base_octaves").forGetter(TemperatureConfig::baseOctaves),
            Codec.DOUBLE.fieldOf("base_lacunarity").forGetter(TemperatureConfig::baseLacunarity),
            Codec.DOUBLE.fieldOf("base_gain").forGetter(TemperatureConfig::baseGain),
            Codec.DOUBLE.fieldOf("warp_frequency").forGetter(TemperatureConfig::warpFrequency),
            Codec.INT.fieldOf("warp_octaves").forGetter(TemperatureConfig::warpOctaves),
            Codec.DOUBLE.fieldOf("warp_lacunarity").forGetter(TemperatureConfig::warpLacunarity),
            Codec.DOUBLE.fieldOf("warp_gain").forGetter(TemperatureConfig::warpGain),
            Codec.DOUBLE.fieldOf("warp_strength").forGetter(TemperatureConfig::warpStrength),
            Codec.DOUBLE.fieldOf("equator_falloff_power").forGetter(TemperatureConfig::equatorFalloffPower),
            Codec.DOUBLE.fieldOf("min_pole_temp_factor").forGetter(TemperatureConfig::minPoleTempFactor)
    ).apply(instance, TemperatureConfig::new));
}
