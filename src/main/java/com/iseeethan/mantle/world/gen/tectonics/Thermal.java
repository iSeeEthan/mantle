package com.iseeethan.mantle.world.gen.tectonics;

final class Thermal {

    private static final float SQRT2 = 1.41421356f;

    private Thermal() {}

    static void erode(float[] h, int n, int iterations,
                      float talusLow, float talusHigh, float hiRef) {
        erode(h, n, iterations, talusLow, talusHigh, hiRef, null);
    }

    static void erode(float[] h, int n, int iterations,
                      float talusLow, float talusHigh, float hiRef, float[] talusMul) {
        float[] delta = new float[n * n];
        for (int it = 0; it < iterations; it++) {
            java.util.Arrays.fill(delta, 0f);
            boolean anyMoved = false;
            for (int j = 0; j < n; j++) {
                int row = j * n;
                for (int i = 0; i < n; i++) {
                    int idx = row + i;
                    float hc = h[idx];
                    float frac = hiRef > 0 ? Math.min(1f, Math.max(0f, hc / hiRef)) : 0f;
                    float talus = talusLow + (talusHigh - talusLow) * frac;
                    if (talusMul != null) talus *= talusMul[idx];

                    float td = talus * SQRT2;
                    float total = 0f, maxDiff = 0f;
                    float dN  = diff(h, n, i, j, i,     j - 1, talus); total += dN;  if (dN  > maxDiff) maxDiff = dN;
                    float dS  = diff(h, n, i, j, i,     j + 1, talus); total += dS;  if (dS  > maxDiff) maxDiff = dS;
                    float dW  = diff(h, n, i, j, i - 1, j,     talus); total += dW;  if (dW  > maxDiff) maxDiff = dW;
                    float dE  = diff(h, n, i, j, i + 1, j,     talus); total += dE;  if (dE  > maxDiff) maxDiff = dE;
                    float dNW = diff(h, n, i, j, i - 1, j - 1, td);    total += dNW; if (dNW > maxDiff) maxDiff = dNW;
                    float dNE = diff(h, n, i, j, i + 1, j - 1, td);    total += dNE; if (dNE > maxDiff) maxDiff = dNE;
                    float dSW = diff(h, n, i, j, i - 1, j + 1, td);    total += dSW; if (dSW > maxDiff) maxDiff = dSW;
                    float dSE = diff(h, n, i, j, i + 1, j + 1, td);    total += dSE; if (dSE > maxDiff) maxDiff = dSE;
                    if (total <= 0f) continue;
                    anyMoved = true;

                    float move = maxDiff * 0.5f;
                    delta[idx] -= move;
                    if (dN  > 0) delta[(j - 1) * n + i]       += move * (dN  / total);
                    if (dS  > 0) delta[(j + 1) * n + i]       += move * (dS  / total);
                    if (dW  > 0) delta[row + (i - 1)]         += move * (dW  / total);
                    if (dE  > 0) delta[row + (i + 1)]         += move * (dE  / total);
                    if (dNW > 0) delta[(j - 1) * n + (i - 1)] += move * (dNW / total);
                    if (dNE > 0) delta[(j - 1) * n + (i + 1)] += move * (dNE / total);
                    if (dSW > 0) delta[(j + 1) * n + (i - 1)] += move * (dSW / total);
                    if (dSE > 0) delta[(j + 1) * n + (i + 1)] += move * (dSE / total);
                }
            }
            for (int k = 0; k < delta.length; k++) h[k] += delta[k];
            if (!anyMoved) break;
        }
    }

    private static float diff(float[] h, int n, int ai, int aj, int bi, int bj, float talus) {
        if (bi < 0 || bi >= n || bj < 0 || bj >= n) return 0f;
        float d = h[aj * n + ai] - h[bj * n + bi];
        return d > talus ? d - talus : 0f;
    }
}
