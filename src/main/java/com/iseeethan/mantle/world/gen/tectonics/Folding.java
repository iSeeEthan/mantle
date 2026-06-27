package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

final class Folding {

    private static final double BELT_SCALE = 2600.0;
    private static final double BELT_THRESHOLD = 0.40;

    private static final double AXIS_SCALE = 540.0;
    private static final double WARP_SCALE = 1400.0;
    private static final double WARP_AMOUNT = 220.0;

    private static final double MAX_AMPLITUDE = 28.0;

    private final GradientNoise belts;
    private final GradientNoise axes;
    private final GradientNoise warp;
    private final double seaY;

    Folding(long seed, double seaY) {
        this.belts = new GradientNoise(seed ^ 0x589965CD1B873593L);
        this.axes = new GradientNoise(seed ^ 0xCC9E2D51C2B2AE35L);
        this.warp = new GradientNoise(seed ^ 0x1B873593589965CDL);
        this.seaY = seaY;
    }

    void apply(float[] elev, int n, double cell, double half) {
        for (int j = 0; j < n; j++) {
            double wz = (j + 0.5) * cell - half;
            for (int i = 0; i < n; i++) {
                double wx = (i + 0.5) * cell - half;
                double cur = elev[j * n + i];
                double fold = displacement(wx, wz, cur);
                if (fold != 0.0) {
                    elev[j * n + i] = (float) (cur + fold);
                }
            }
        }
    }

    double ridgeStrength(double wx, double wz, double surfaceY) {
        return displacement(wx, wz, surfaceY);
    }

    double displacement(double wx, double wz, double surfaceY) {
        double belt = belts.fbm(wx / BELT_SCALE, wz / BELT_SCALE, 3, 2.0, 0.5);
        if (belt < BELT_THRESHOLD) return 0.0;
        double belt01 = (belt - BELT_THRESHOLD) / (1.0 - BELT_THRESHOLD);

        double field = ridges(wx, wz);

        double relief = clamp((surfaceY - seaY) / 220.0, 0.0, 1.0);
        double amplitude = MAX_AMPLITUDE * belt01 * (0.25 + 0.75 * relief);

        return field * amplitude;
    }

    private double ridges(double wx, double wz) {
        double sx = wx + warp.noise2(wx / WARP_SCALE, wz / WARP_SCALE) * WARP_AMOUNT;
        double sz = wz + warp.noise2((wx - 3000) / WARP_SCALE, (wz + 3000) / WARP_SCALE) * WARP_AMOUNT;
        double v = axes.noise2(sx / AXIS_SCALE, sz / AXIS_SCALE);
        return Math.sin(v * Math.PI);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
