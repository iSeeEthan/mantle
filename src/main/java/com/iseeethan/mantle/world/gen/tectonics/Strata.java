package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

public final class Strata {

    public enum Rock {

        SEDIMENTARY,

        IGNEOUS,

        METAMORPHIC,

        BASEMENT
    }

    private static final Rock[] LAYERS = {
            Rock.SEDIMENTARY,
            Rock.SEDIMENTARY,
            Rock.IGNEOUS,
            Rock.METAMORPHIC,
            Rock.BASEMENT,
    };

    private static final double[] THICK = { 28, 42, 70, 95 };

    private final GradientNoise fold;
    private final Folding folding;

    private final double seaY;

    public Strata(long seed, double seaY) {

        this.fold = new GradientNoise(seed ^ 0x7F4A7C159E3779B9L);
        this.folding = new Folding(seed, seaY);
        this.seaY = seaY;
    }

    private double structuralTop(double wx, double wz, double surfaceY) {

        double uplift = (surfaceY - seaY);

        double folds = fold.fbm(wx / 1500.0, wz / 1500.0, 4, 2.0, 0.5) * 120.0
                     + fold.fbm((wx + 5000) / 400.0, (wz - 5000) / 400.0, 3, 2.1, 0.5) * 45.0;

        double tectonicFold = folding.displacement(wx, wz, surfaceY) * 2.5;

        return seaY + uplift * 1.55 + folds + tectonicFold;
    }

    public Rock typeAt(double wx, double wz, double y, double surfaceY) {
        double top = structuralTop(wx, wz, surfaceY);
        double depth = top - y;
        if (depth <= 0) return LAYERS[0];

        double jitter = 1.0 + 0.25 * fold.noise2(wx / 220.0, wz / 220.0, 7);

        double base = 0;
        for (int i = 0; i < THICK.length; i++) {
            base += THICK[i] * jitter;
            if (depth < base) return LAYERS[i];
        }
        return Rock.BASEMENT;
    }

    public int contactCount() { return LAYERS.length; }

    public Rock layer(int i) { return LAYERS[Math.min(i, LAYERS.length - 1)]; }
}
