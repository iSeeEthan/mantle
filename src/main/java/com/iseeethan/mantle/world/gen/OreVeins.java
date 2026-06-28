package com.iseeethan.mantle.world.gen;

import com.iseeethan.mantle.block.MantleBlocks;
import com.iseeethan.mantle.world.noise.GradientNoise;

public final class OreVeins {

    public static final class Band {
        final MantleBlocks.Ore ore;
        final int minY;
        final int maxY;
        final double frequency;
        final double threshold;
        final double richness;
        final GradientNoise field;
        final GradientNoise body;

        Band(long seed, int salt, MantleBlocks.Ore ore, int minY, int maxY,
             double frequency, double threshold, double richness) {
            this.ore = ore;
            this.minY = minY;
            this.maxY = maxY;
            this.frequency = frequency;
            this.threshold = threshold;
            this.richness = richness;
            this.field = new GradientNoise(seed ^ (0x9E3779B97F4A7C15L * (salt + 1)));
            this.body = new GradientNoise(seed ^ (0xC2B2AE3D27D4EB4FL * (salt + 1)));
        }
    }

    private final Band[] bands;
    private final double density;

    public OreVeins(long seed) {
        this(seed, 1.0);
    }

    public OreVeins(long seed, double density) {
        this.density = density < 0 ? 0 : density;
        this.bands = new Band[] {
            new Band(seed, 0, MantleBlocks.Ore.COAL,   -16,  128, 0.045, 0.70, 0.55),
            new Band(seed, 1, MantleBlocks.Ore.COPPER, -48,   96, 0.055, 0.72, 0.45),
            new Band(seed, 2, MantleBlocks.Ore.IRON,   -96,   64, 0.050, 0.72, 0.42),
            new Band(seed, 4, MantleBlocks.Ore.GOLD,  -128,    0, 0.065, 0.78, 0.24),
        };
    }

    public MantleBlocks.Ore oreAt(int wx, int y, int wz) {
        if (density <= 0.0) return null;
        for (Band b : bands) {
            if (y < b.minY || y > b.maxY) continue;

            double depthFade = depthWeight(y, b.minY, b.maxY);
            if (depthFade <= 0.0) continue;

            double presence = 0.5 + 0.5 * b.field.fbm(
                    wx * b.frequency * 0.35,
                    (wz + y * 1.7) * b.frequency * 0.35,
                    3, 2.0, 0.5);
            if (presence < 0.62) continue;

            double vein = 0.5 + 0.5 * b.body.fbm(
                    (wx + y * 0.6) * b.frequency,
                    (wz - y * 0.6) * b.frequency,
                    4, 2.1, 0.55);
            double jitter = 0.5 + 0.5 * b.body.noise2(wx * 0.9, (wz + y) * 0.9);

            double score = vein * 0.75 + presence * 0.15 + jitter * 0.10;
            double gate = b.threshold - b.richness * 0.05 * depthFade - (density - 1.0) * 0.06;

            if (score >= gate) {
                return b.ore;
            }
        }
        return null;
    }

    private static double depthWeight(int y, int minY, int maxY) {
        double span = maxY - minY;
        if (span <= 0) return 1.0;
        double t = (y - minY) / span;
        double edge = Math.min(t, 1.0 - t) * 2.0;
        return edge < 0 ? 0 : (edge > 1 ? 1 : edge);
    }
}
