package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

final class Glacial {

    private static final double SNOWLINE_BASE = 0.34;
    private static final double SNOWLINE_JITTER = 0.06;

    private static final double LAPSE_SPAN = 620.0;
    private static final double LATITUDE_WEIGHT = 0.70;
    private static final double ELEV_WEIGHT = 0.44;

    private static final double FLOW_SMOOTH = 0.35;
    private static final double SCOUR_RATE = 0.55;
    private static final double OVERDEEPEN = 0.5;
    private static final double WIDEN_RADIUS = 2.6;

    private static final double CIRQUE_PLUCK = 4.5;
    private static final double FJORD_DEPTH = 18.0;

    private final GradientNoise jitter;
    private final double seaY;
    private final double poleZ;
    private final double polarColdness;
    private final double temperatureScale;

    Glacial(long seed, double seaY, double poleZ, double polarColdness, double temperatureScale) {
        this.jitter = new GradientNoise(seed ^ 0x517CC1B727220A95L);
        this.seaY = seaY;
        this.poleZ = poleZ;
        this.polarColdness = polarColdness;
        this.temperatureScale = temperatureScale;
    }

    void apply(float[] h, int n, double cell, double half, int iterations, double intensity) {
        double[] ice = new double[n * n];
        double[] flow = new double[n * n];
        float[] delta = new float[n * n];

        for (int it = 0; it < iterations; it++) {
            computeIce(h, n, cell, half, ice);
            routeIce(h, n, ice, flow);
            scour(h, n, cell, flow, delta, intensity);
        }
    }

    private void computeIce(float[] h, int n, double cell, double half, double[] ice) {
        for (int j = 0; j < n; j++) {
            double wz = (j + 0.5) * cell - half;
            double lat = clamp(Math.abs(wz) / poleZ, 0.0, 1.0);
            double latTemp = 1.0 - clamp(lat * polarColdness, 0.0, 1.0);
            for (int i = 0; i < n; i++) {
                double wx = (i + 0.5) * cell - half;
                int idx = j * n + i;
                double above = h[idx] - seaY;
                double lapse = clamp(above / LAPSE_SPAN, 0.0, 1.0);
                double t = latTemp * LATITUDE_WEIGHT - lapse * ELEV_WEIGHT + 0.5 * (1.0 - LATITUDE_WEIGHT);
                t = 0.5 + (t - 0.5) * temperatureScale;

                double snowline = SNOWLINE_BASE + SNOWLINE_JITTER * jitter.noise2(wx / 1800.0, wz / 1800.0);
                ice[idx] = t < snowline ? clamp((snowline - t) / 0.2, 0.0, 1.0) : 0.0;
            }
        }
    }

    private void routeIce(float[] h, int n, double[] ice, double[] flow) {
        for (int k = 0; k < flow.length; k++) flow[k] = ice[k];

        for (int pass = 0; pass < 3; pass++) {
            for (int j = 1; j < n - 1; j++) {
                for (int i = 1; i < n - 1; i++) {
                    int idx = j * n + i;
                    if (ice[idx] <= 0) continue;
                    double hc = h[idx];
                    int lowest = -1;
                    double drop = 0;
                    for (int d = 0; d < 8; d++) {
                        int ni = i + DX[d], nj = j + DZ[d];
                        int nidx = nj * n + ni;
                        double dd = hc - h[nidx];
                        if (dd > drop) { drop = dd; lowest = nidx; }
                    }
                    if (lowest >= 0) {
                        double move = flow[idx] * FLOW_SMOOTH;
                        flow[lowest] += move;
                    }
                }
            }
        }
    }

    private void scour(float[] h, int n, double cell, double[] flow, float[] delta, double intensity) {
        java.util.Arrays.fill(delta, 0f);
        int widen = (int) Math.ceil(WIDEN_RADIUS);

        for (int j = 1; j < n - 1; j++) {
            for (int i = 1; i < n - 1; i++) {
                int idx = j * n + i;
                double f = flow[idx];
                if (f <= 0) continue;

                double hc = h[idx];
                double slope = localSlope(h, n, cell, i, j);
                double headwall = clamp(slope / 0.6, 0.0, 1.0);

                double scour = SCOUR_RATE * f * (OVERDEEPEN + headwall) * intensity;
                if (hc < seaY) scour *= 1.0 + FJORD_DEPTH / Math.max(1.0, seaY - hc + FJORD_DEPTH);

                delta[idx] -= (float) scour;
                delta[idx] -= (float) (CIRQUE_PLUCK * headwall * f * intensity * 0.04);

                double shareTotal = 0;
                for (int dz = -widen; dz <= widen; dz++) {
                    for (int dx = -widen; dx <= widen; dx++) {
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > WIDEN_RADIUS || dist == 0) continue;
                        shareTotal += WIDEN_RADIUS - dist;
                    }
                }
                if (shareTotal <= 0) continue;
                double sidewall = scour * 0.4;
                for (int dz = -widen; dz <= widen; dz++) {
                    for (int dx = -widen; dx <= widen; dx++) {
                        int ni = i + dx, nj = j + dz;
                        if (ni < 0 || nj < 0 || ni >= n || nj >= n) continue;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > WIDEN_RADIUS || dist == 0) continue;
                        double w = (WIDEN_RADIUS - dist) / shareTotal;
                        if (h[nj * n + ni] > hc) {
                            delta[nj * n + ni] -= (float) (sidewall * w);
                        }
                    }
                }
            }
        }

        for (int k = 0; k < delta.length; k++) h[k] += delta[k];
    }

    private static double localSlope(float[] h, int n, double cell, int i, int j) {
        double dx = (h[j * n + (i + 1)] - h[j * n + (i - 1)]) / (2 * cell);
        double dz = (h[(j + 1) * n + i] - h[(j - 1) * n + i]) / (2 * cell);
        return Math.hypot(dx, dz);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static final int[] DX = { 0, 0, -1, 1, -1, 1, -1, 1 };
    private static final int[] DZ = { -1, 1, 0, 0, -1, -1, 1, 1 };
}
