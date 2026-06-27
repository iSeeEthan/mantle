package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

final class Erodibility {

    static final float SEDIMENTARY = 1.45f;
    static final float IGNEOUS = 0.70f;
    static final float METAMORPHIC = 0.55f;
    static final float BASEMENT = 0.40f;

    private static final double EXPOSURE_DEPTH = 60.0;

    private final GradientNoise grain;
    private final Strata strata;
    private final double seaY;

    Erodibility(long seed, Strata strata, double seaY) {
        this.grain = new GradientNoise(seed ^ 0x2545F4914F6CDD1DL);
        this.strata = strata;
        this.seaY = seaY;
    }

    float[] build(float[] surface, int n, double cell, double half) {
        float[] k = new float[n * n];
        for (int j = 0; j < n; j++) {
            double wz = (j + 0.5) * cell - half;
            for (int i = 0; i < n; i++) {
                double wx = (i + 0.5) * cell - half;
                k[j * n + i] = (float) sample(wx, wz, surface[j * n + i]);
            }
        }
        return k;
    }

    private double sample(double wx, double wz, double surfaceY) {
        double topRock = resistance(strata.typeAt(wx, wz, surfaceY, surfaceY));
        double deepRock = resistance(strata.typeAt(wx, wz, surfaceY - EXPOSURE_DEPTH, surfaceY));

        double exposure = clamp((surfaceY - seaY) / 300.0, 0.0, 1.0);
        double blended = topRock + (deepRock - topRock) * exposure;

        double grainVar = 1.0 + 0.18 * grain.fbm(wx / 380.0, wz / 380.0, 4, 2.0, 0.5);
        double v = blended * grainVar;
        return clamp(v, 0.30, 1.70);
    }

    private static double resistance(Strata.Rock rock) {
        switch (rock) {
            case SEDIMENTARY: return SEDIMENTARY;
            case IGNEOUS:     return IGNEOUS;
            case METAMORPHIC: return METAMORPHIC;
            case BASEMENT:    return BASEMENT;
            default:          return 1.0f;
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
