package com.iseeethan.mantle.world.noise;

public final class SimpleNoise {
    private final long seed;

    public SimpleNoise(long seed) {
        this.seed = seed;
    }

    public double fbm(double x, double z, int octaves, double lacunarity, double gain) {
        double sum = 0.0;
        double amp = 1.0;
        double freq = 1.0;
        double norm = 0.0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * value(x * freq, z * freq);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return norm == 0.0 ? 0.0 : sum / norm;
    }

    public double value(double x, double z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        double fx = x - x0;
        double fz = z - z0;

        double n00 = lattice(x0, z0);
        double n10 = lattice(x0 + 1, z0);
        double n01 = lattice(x0, z0 + 1);
        double n11 = lattice(x0 + 1, z0 + 1);

        double u = fade(fx);
        double v = fade(fz);

        double nx0 = lerp(n00, n10, u);
        double nx1 = lerp(n01, n11, u);
        return lerp(nx0, nx1, v);
    }

    private double lattice(int x, int z) {
        long h = seed;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 29);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 32);

        return ((h >>> 11) / (double) (1L << 53)) * 2.0 - 1.0;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int fastFloor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }
}
