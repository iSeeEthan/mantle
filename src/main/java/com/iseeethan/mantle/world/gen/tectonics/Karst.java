package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

final class Karst {

    private static final double FIELD_SCALE = 540.0;
    private static final double SOLUBILITY_THRESHOLD = 0.32;

    private static final double CELL_SCALE = 38.0;
    private static final double SITE_THRESHOLD = 0.62;

    private static final double MAX_DEPTH = 14.0;
    private static final double MIN_DEPTH = 3.0;

    private final GradientNoise field;
    private final GradientNoise sites;
    private final Strata strata;
    private final double seaY;
    private final double shoreY;

    Karst(long seed, Strata strata, double seaY, double shoreY) {
        this.field = new GradientNoise(seed ^ 0x1B873593CC9E2D51L);
        this.sites = new GradientNoise(seed ^ 0x85EBCA6BC2B2AE35L);
        this.strata = strata;
        this.seaY = seaY;
        this.shoreY = shoreY;
    }

    void apply(float[] elev, int n, double cell, double half) {
        for (int j = 0; j < n; j++) {
            double wz = (j + 0.5) * cell - half;
            for (int i = 0; i < n; i++) {
                double wx = (i + 0.5) * cell - half;
                double surf = elev[j * n + i];
                double depth = sinkholeDepth(wx, wz, surf);
                if (depth > 0) {
                    float lowered = (float) (surf - depth);
                    if (lowered < shoreY + 1) lowered = (float) (shoreY + 1);
                    elev[j * n + i] = lowered;
                }
            }
        }
    }

    private double sinkholeDepth(double wx, double wz, double surfaceY) {
        if (surfaceY <= shoreY + 2) return 0;

        Strata.Rock here = strata.typeAt(wx, wz, surfaceY, surfaceY);
        if (here != Strata.Rock.SEDIMENTARY) return 0;

        double solubility = field.fbm(wx / FIELD_SCALE, wz / FIELD_SCALE, 4, 2.0, 0.5);
        if (solubility < SOLUBILITY_THRESHOLD) return 0;

        double site = sites.ridged(wx / CELL_SCALE, wz / CELL_SCALE, 3, 2.1, 0.55);
        if (site < SITE_THRESHOLD) return 0;

        double intensity = (site - SITE_THRESHOLD) / (1.0 - SITE_THRESHOLD);
        double field01 = (solubility - SOLUBILITY_THRESHOLD) / (1.0 - SOLUBILITY_THRESHOLD);
        double depth = (MIN_DEPTH + (MAX_DEPTH - MIN_DEPTH) * intensity) * (0.4 + 0.6 * field01);

        double cap = (surfaceY - shoreY) * 0.5;
        return Math.min(depth, cap);
    }
}
