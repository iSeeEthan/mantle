package com.iseeethan.mantle.world.gen.ecology;

import com.iseeethan.mantle.world.gen.ecology.registries.TemperatureConfig;
import com.iseeethan.mantle.world.noise.GradientNoise;

public final class Temperature {
    private final GradientNoise noise;
    private final TemperatureConfig config;
    private final double radius;

    public Temperature(long seed, TemperatureConfig config, double worldSize) {
        this.noise = new GradientNoise(seed ^ 0x54454D504CL); // "TEMP" in hex
        this.config = config;
        this.radius = worldSize / 2.0;
    }

    /**
     * Returns a value from 0.0 (Arctic) to 1.0 (Equator).
     */
    public double getTemperature(double wx, double wz) {
        // 1. Normalized distance from the equator (0 at Z=0, 1 at Z=±5000)
        double lat = Math.abs(wz) / radius;

        //Domain warp
        double warp = noise.fbm(
                wx * config.warpFrequency(), 
                wz * config.warpFrequency(), 
                config.warpOctaves(), 
                config.warpLacunarity(), 
                config.warpGain()
        ) * config.warpStrength();

        double warpedLat = Math.clamp(lat + warp, 0, 1);


        double baseTemp = 1.0 - Math.pow(warpedLat, config.equatorFalloffPower());

        // 4. Subtle detail noise (microclimates)
        double detail = noise.fbm(
                wx * config.baseFrequency(), 
                wz * config.baseFrequency(), 
                config.baseOctaves(), 
                config.baseLacunarity(), 
                config.baseGain()
        ) * 0.1; // Limit variation to +/- 10%

        double finalTemp = baseTemp + detail;
        
        // Ensure we respect the min pole factor
        double min = config.minPoleTempFactor();
        return Math.clamp(finalTemp, min, 1.0);
    }
}