package com.iseeethan.mantle.world.gen.tectonics;

final class Coastal {

    private static final int COAST_BAND = 3;
    private static final double WAVE_BASE = 28.0;
    private static final double CLIFF_RETREAT = 0.5;
    private static final double BEACH_BUILD = 0.4;
    private static final double SHELF_DEPTH = 8.0;

    private static final int FETCH_RADIUS = 10;
    private static final int FETCH_RAYS = 8;

    private Coastal() {}

    static void apply(float[] h, int n, double cell, float seaY, int passes) {
        boolean[] sea = new boolean[n * n];
        double[] exposure = new double[n * n];
        float[] delta = new float[n * n];

        for (int p = 0; p < passes; p++) {
            markSea(h, n, seaY, sea);
            computeExposure(sea, n, exposure);
            shape(h, n, seaY, sea, exposure, delta);
        }
    }

    private static void markSea(float[] h, int n, float seaY, boolean[] sea) {
        for (int k = 0; k < sea.length; k++) sea[k] = h[k] < seaY;
    }

    private static void computeExposure(boolean[] sea, int n, double[] exposure) {
        java.util.Arrays.fill(exposure, 0.0);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                if (sea[idx] || !nearSea(sea, n, i, j)) continue;

                double openRays = 0;
                for (int r = 0; r < FETCH_RAYS; r++) {
                    double ang = (Math.PI * 2 * r) / FETCH_RAYS;
                    double dx = Math.cos(ang), dz = Math.sin(ang);
                    int reach = 0;
                    for (int step = 1; step <= FETCH_RADIUS; step++) {
                        int si = (int) Math.round(i + dx * step);
                        int sj = (int) Math.round(j + dz * step);
                        if (si < 0 || sj < 0 || si >= n || sj >= n) break;
                        if (!sea[sj * n + si]) break;
                        reach = step;
                    }
                    openRays += (double) reach / FETCH_RADIUS;
                }
                exposure[idx] = openRays / FETCH_RAYS;
            }
        }
    }

    private static void shape(float[] h, int n, float seaY, boolean[] sea,
                              double[] exposure, float[] delta) {
        java.util.Arrays.fill(delta, 0f);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                if (sea[idx]) continue;
                if (!nearSea(sea, n, i, j)) continue;

                double exp = exposure[idx];

                if (exp > 0.45) {
                    double cut = WAVE_BASE * CLIFF_RETREAT * exp;
                    double floor = seaY + 1.0;
                    if (h[idx] - cut < floor) cut = h[idx] - floor;
                    if (cut > 0) delta[idx] -= (float) cut;
                } else {
                    double build = WAVE_BASE * BEACH_BUILD * (0.45 - exp);
                    double berm = seaY + 1.0;
                    if (h[idx] < berm) {
                        double fill = Math.min(build, berm - h[idx]);
                        delta[idx] += (float) fill;
                    }
                    spreadShelf(h, n, seaY, sea, i, j, build * 0.5, delta);
                }
            }
        }
        for (int k = 0; k < delta.length; k++) h[k] += delta[k];
    }

    private static void spreadShelf(float[] h, int n, float seaY, boolean[] sea,
                                    int i, int j, double amount, float[] delta) {
        if (amount <= 0) return;
        for (int d = 0; d < 8; d++) {
            int ni = i + DX[d], nj = j + DZ[d];
            if (ni < 0 || nj < 0 || ni >= n || nj >= n) continue;
            int k = nj * n + ni;
            if (!sea[k]) continue;
            double depth = seaY - h[k];
            if (depth <= 0 || depth > SHELF_DEPTH) continue;
            double fill = Math.min(amount * 0.25, depth);
            delta[k] += (float) fill;
        }
    }

    private static boolean nearSea(boolean[] sea, int n, int i, int j) {
        for (int dz = -COAST_BAND; dz <= COAST_BAND; dz++) {
            for (int dx = -COAST_BAND; dx <= COAST_BAND; dx++) {
                int ni = i + dx, nj = j + dz;
                if (ni < 0 || nj < 0 || ni >= n || nj >= n) continue;
                if (sea[nj * n + ni]) return true;
            }
        }
        return false;
    }

    private static final int[] DX = { 0, 0, -1, 1, -1, 1, -1, 1 };
    private static final int[] DZ = { -1, 1, 0, 0, -1, -1, 1, 1 };
}
