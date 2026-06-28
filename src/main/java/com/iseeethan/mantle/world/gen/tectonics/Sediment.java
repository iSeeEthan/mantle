package com.iseeethan.mantle.world.gen.tectonics;

final class Sediment {

    private static final double EROSION_COEFF = 0.018;
    private static final double SLOPE_EXP = 1.3;
    private static final double ACCUM_EXP = 0.5;

    private static final double CARRY_CAPACITY = 0.9;
    private static final double FAN_SETTLE = 0.45;
    private static final double FLOOD_SETTLE = 0.12;

    private static final double DELTA_REACH = 6.0;
    private static final double DELTA_SETTLE = 0.6;
    private static final double DELTA_MAX_DEPTH = 26.0;

    private static final double MIN_CHANNEL_ACCUM = 24.0;

    private Sediment() {}

    static void apply(float[] h, int n, double cell, float seaY, int passes) {
        Drainage drain = new Drainage(n, seaY);
        double[] load = new double[n * n];

        for (int p = 0; p < passes; p++) {
            drain.solve(h);
            java.util.Arrays.fill(load, 0.0);
            transport(h, n, cell, seaY, drain, load);
        }
    }

    private static void transport(float[] h, int n, double cell, float seaY,
                                  Drainage drain, double[] load) {
        int[] order = drain.popOrder;
        int[] down = drain.downstream;
        int[] accum = drain.accum;
        double[] distDown = drain.distDown;

        for (int k = order.length - 1; k >= 0; k--) {
            int i = order[k];
            int j = down[i];
            if (j < 0) {
                depositAtMouth(h, n, cell, seaY, i, load[i]);
                continue;
            }

            double l = distDown[i] * cell;
            if (l <= 0) {
                load[j] += load[i];
                continue;
            }

            double slope = (h[i] - h[j]) / l;
            double a = accum[i];

            if (slope > 0) {
                double pickup = EROSION_COEFF * Math.pow(a, ACCUM_EXP) * Math.pow(slope, SLOPE_EXP);
                load[i] += pickup;
                h[i] -= (float) pickup;
            }

            double capacity = CARRY_CAPACITY * a * Math.max(0.0, slope);
            double excess = load[i] - capacity;

            if (excess > 0 && a >= MIN_CHANNEL_ACCUM) {
                double settleRate = slope <= 0 ? FAN_SETTLE : FLOOD_SETTLE;
                double settle = excess * settleRate;
                double room = h[j] - h[i];
                if (room > 0 && settle > room) settle = room;
                if (settle > 0) {
                    h[i] += (float) settle;
                    load[i] -= settle;
                }
            }

            load[j] += load[i];
        }
    }

    private static void depositAtMouth(float[] h, int n, double cell, float seaY, int idx, double carried) {
        if (carried <= 0) return;
        int ci = idx % n, cj = idx / n;

        double remaining = carried * DELTA_SETTLE;
        int reach = (int) Math.max(1, DELTA_REACH);

        for (int r = 0; r <= reach && remaining > 0; r++) {
            double ringShare = remaining / (reach - r + 1);
            int placed = 0;
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int ni = ci + dx, nj = cj + dz;
                    if (ni < 0 || nj < 0 || ni >= n || nj >= n) continue;
                    int k = nj * n + ni;
                    if (h[k] >= seaY) continue;
                    double depth = seaY - h[k];
                    if (depth > DELTA_MAX_DEPTH) continue;
                    double fill = Math.min(ringShare, depth);
                    if (fill <= 0) continue;
                    h[k] += (float) fill;
                    remaining -= fill;
                    placed++;
                }
            }
            if (placed == 0 && r > 0) break;
        }
    }
}
