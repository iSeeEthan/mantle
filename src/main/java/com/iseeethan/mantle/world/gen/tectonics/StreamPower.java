package com.iseeethan.mantle.world.gen.tectonics;

final class StreamPower {

    private StreamPower() {}

    static final double M = 0.5;
    static final double N = 1.0;

    static final double K = 0.45;

    static final double D = 0.18;

    static final double DT = 1.0;

    static void evolve(float[] h, int n, double cell, float seaY, float[] uplift, int steps) {
        Drainage drain = new Drainage(n, seaY);
        float[] tmp = new float[n * n];

        for (int s = 0; s < steps; s++) {

            if (uplift != null) {
                for (int i = 0; i < h.length; i++) h[i] += (float) (uplift[i] * DT);
            }

            drain.solve(h);

            int[] order = drain.popOrder;
            int[] down = drain.downstream;
            int[] accum = drain.accum;
            double[] distDown = drain.distDown;
            for (int k = 0; k < order.length; k++) {
                int i = order[k];
                int j = down[i];
                if (j < 0) continue;
                double a = accum[i];
                double l = distDown[i] * cell;
                if (l <= 0) continue;

                double f = K * Math.pow(a, M) * DT / l;
                double hj = h[j];
                double hi = h[i];
                if (hi <= hj) continue;
                double updated = (hi + f * hj) / (1.0 + f);
                if (updated < hj) updated = hj;
                h[i] = (float) updated;
            }

            diffuse(h, tmp, n);
        }
    }

    private static void diffuse(float[] h, float[] tmp, int n) {
        for (int j = 0; j < n; j++) {
            int jm = Math.max(0, j - 1), jp = Math.min(n - 1, j + 1);
            for (int i = 0; i < n; i++) {
                int im = Math.max(0, i - 1), ip = Math.min(n - 1, i + 1);
                float c = h[j * n + i];
                float lap = h[j * n + im] + h[j * n + ip] + h[jm * n + i] + h[jp * n + i] - 4f * c;
                tmp[j * n + i] = c + (float) (D * lap);
            }
        }
        System.arraycopy(tmp, 0, h, 0, n * n);
    }
}
