package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

final class Faulting {

    private static final double NETWORK_SCALE = 1900.0;
    private static final double WARP_SCALE = 700.0;
    private static final double WARP_AMOUNT = 260.0;

    private static final double SCARP_HALF_WIDTH = 22.0;
    private static final double MAX_THROW = 11.0;
    private static final double ACTIVITY_THRESHOLD = 0.45;

    private final GradientNoise lines;
    private final GradientNoise warp;
    private final GradientNoise activity;
    private final GradientNoise throwField;
    private final double seaY;

    Faulting(long seed, double seaY) {
        this.lines = new GradientNoise(seed ^ 0x27D4EB2F165667C5L);
        this.warp = new GradientNoise(seed ^ 0x9E3779B185EBCA87L);
        this.activity = new GradientNoise(seed ^ 0xD6E8FEB86659FD93L);
        this.throwField = new GradientNoise(seed ^ 0xA0761D6478BD642FL);
        this.seaY = seaY;
    }

    void apply(float[] elev, int n, double cell, double half) {
        for (int j = 0; j < n; j++) {
            double wz = (j + 0.5) * cell - half;
            for (int i = 0; i < n; i++) {
                double wx = (i + 0.5) * cell - half;
                double cur = elev[j * n + i];
                double offset = throwAt(wx, wz, cur);
                if (offset != 0.0) {
                    elev[j * n + i] = (float) (cur + offset);
                }
            }
        }
    }

    double scarpStrength(double wx, double wz, double surfaceY) {
        return Math.abs(throwAt(wx, wz, surfaceY));
    }

    private double throwAt(double wx, double wz, double surfaceY) {
        double act = activity.fbm(wx / 3200.0, wz / 3200.0, 3, 2.0, 0.5);
        if (act < ACTIVITY_THRESHOLD) return 0.0;

        double sx = wx + warp.noise2(wx / WARP_SCALE, wz / WARP_SCALE) * WARP_AMOUNT;
        double sz = wz + warp.noise2((wx + 4000) / WARP_SCALE, (wz - 4000) / WARP_SCALE) * WARP_AMOUNT;

        double field = lines.noise2(sx / NETWORK_SCALE, sz / NETWORK_SCALE);
        double grad = NETWORK_SCALE * 0.5;
        double dx = (lines.noise2((sx + grad) / NETWORK_SCALE, sz / NETWORK_SCALE)
                   - lines.noise2((sx - grad) / NETWORK_SCALE, sz / NETWORK_SCALE)) / (2.0 * grad);
        double dz = (lines.noise2(sx / NETWORK_SCALE, (sz + grad) / NETWORK_SCALE)
                   - lines.noise2(sx / NETWORK_SCALE, (sz - grad) / NETWORK_SCALE)) / (2.0 * grad);
        double slope = Math.hypot(dx, dz);
        if (slope < 1e-9) return 0.0;

        double signedDist = field / slope;
        double absDist = Math.abs(signedDist);
        if (absDist > SCARP_HALF_WIDTH * 3.0) return 0.0;

        double activity01 = (act - ACTIVITY_THRESHOLD) / (1.0 - ACTIVITY_THRESHOLD);
        double magnitude = MAX_THROW * activity01
                * (0.4 + 0.6 * Math.abs(throwField.noise2(wx / 480.0, wz / 480.0)));

        double relief = clamp((surfaceY - seaY) / 200.0, 0.0, 1.0);
        magnitude *= 0.35 + 0.65 * relief;

        double side = Math.signum(signedDist);
        double profile = Math.exp(-(absDist * absDist) / (2.0 * SCARP_HALF_WIDTH * SCARP_HALF_WIDTH));
        return side * magnitude * profile;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
