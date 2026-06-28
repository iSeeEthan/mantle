package com.iseeethan.mantle.world.gen.ecology;

import com.iseeethan.mantle.world.gen.ecology.registries.RainfallConfig;
import com.iseeethan.mantle.world.noise.GradientNoise;

public final class Rainfall {
    private final GradientNoise noise;
    private final RainfallConfig config;
    private final double radius;

    public Rainfall(long seed, RainfallConfig config, double worldSize) {
        // Use a different seed salt than temperature (0x5241494E is "RAIN" in hex btw)
        this.noise = new GradientNoise(seed ^ 0x5241494EL);
        this.config = config;
        this.radius = worldSize / 2.0;
    }

    public double getRainfall(double wx, double wz) {
        double lat = Math.abs(wz) / radius;

        // Domain Warping (Independent of Temperature warping)
        double warp = noise.fbm(
                wx * config.warpFrequency(),
                wz * config.warpFrequency(),
                config.warpOctaves(),
                config.warpLacunarity(),
                config.warpGain()
        ) * config.warpStrength();

        double warpedLat = Math.clamp(lat + warp, 0, 1);

        // The "Hadley Cell" Curve:
        // High at 0 (Equator), Low at 0.3 (Deserts), High at 0.6 (Temperate), Low at 1.0 (Poles)
        // Formula: cos(lat * 2 * PI) creates two peaks.
        double baseRain = (Math.cos(warpedLat * Math.PI * 2.5) + 1.0) * 0.5;

        // Detail Noise (Micro-climates)
        double detail = noise.fbm(
                wx * config.baseFrequency(),
                wz * config.baseFrequency(),
                config.baseOctaves(),
                config.baseLacunarity(),
                config.baseGain()
        ) * 0.2;

        return Math.clamp(baseRain + detail, 0.0, 1.0);
    }
}