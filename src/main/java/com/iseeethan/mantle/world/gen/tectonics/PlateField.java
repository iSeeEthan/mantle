package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

final class PlateField {

    private static final int GRID = 6;
    private static final int COUNT = GRID * GRID;
    private static final double OCEANIC_FRACTION = 0.55;

    private static final double JITTER = 0.85;

    private final int world;
    private final int half;

    private final double[] px = new double[COUNT];
    private final double[] pz = new double[COUNT];
    private final double[] pvx = new double[COUNT];
    private final double[] pvz = new double[COUNT];
    private final double[] pbase = new double[COUNT];
    private final boolean[] oceanic = new boolean[COUNT];

    PlateField(long seed, int world, int half) {
        this.world = world;
        this.half = half;
        init(seed);
    }

    private void init(long seed) {
        java.util.Random rnd = new java.util.Random(seed * 0x2545F4914F6CDD1DL + 1);
        double tile = (double) world / GRID;
        int k = 0;
        for (int gz = 0; gz < GRID; gz++) {
            for (int gx = 0; gx < GRID; gx++) {
                double cx = (gx + 0.5) * tile - half;
                double cz = (gz + 0.5) * tile - half;
                px[k] = cx + (rnd.nextDouble() - 0.5) * tile * JITTER;
                pz[k] = cz + (rnd.nextDouble() - 0.5) * tile * JITTER;

                double ang = rnd.nextDouble() * Math.PI * 2.0;
                double speed = 0.4 + rnd.nextDouble() * 0.6;
                pvx[k] = Math.cos(ang) * speed;
                pvz[k] = Math.sin(ang) * speed;

                boolean isOcean = rnd.nextDouble() < OCEANIC_FRACTION;
                oceanic[k] = isOcean;
                pbase[k] = isOcean
                        ? -0.62 + rnd.nextDouble() * 0.16
                        :  0.18 + rnd.nextDouble() * 0.30;
                k++;
            }
        }
    }

    void evaluate(double sx, double sz, Contact out) {
        int n1 = -1, n2 = -1;
        double d1 = Double.MAX_VALUE, d2 = Double.MAX_VALUE;
        for (int p = 0; p < COUNT; p++) {
            double dx = sx - px[p];
            double dz = sz - pz[p];
            double d = dx * dx + dz * dz;
            if (d < d1) { d2 = d1; n2 = n1; d1 = d; n1 = p; }
            else if (d < d2) { d2 = d; n2 = p; }
        }
        double dist1 = Math.sqrt(d1);
        double dist2 = Math.sqrt(d2);

        double w1 = 1.0 / (dist1 + 1.0);
        double w2 = 1.0 / (dist2 + 1.0);
        out.base = (pbase[n1] * w1 + pbase[n2] * w2) / (w1 + w2);

        out.boundaryDist = (dist2 - dist1) * 0.5;

        double nx = px[n2] - px[n1], nz = pz[n2] - pz[n1];
        double nlen = Math.hypot(nx, nz);
        if (nlen > 1e-6) { nx /= nlen; nz /= nlen; }

        double rvx = pvx[n1] - pvx[n2];
        double rvz = pvz[n1] - pvz[n2];

        double closing = rvx * nx + rvz * nz;
        double shear = rvx * (-nz) + rvz * nx;

        out.normal = closing;
        out.shear = shear;

        double mag = Math.hypot(closing, shear);
        if (mag < 1e-6) {
            out.type = BoundaryType.TRANSFORM;
        } else if (Math.abs(closing) >= Math.abs(shear)) {
            out.type = closing > 0 ? BoundaryType.CONVERGENT : BoundaryType.DIVERGENT;
        } else {
            out.type = BoundaryType.TRANSFORM;
        }

        out.oceanA = (out.boundaryDist > 0) ? oceanic[n1] : oceanic[n2];
        out.oceanB = (out.boundaryDist > 0) ? oceanic[n2] : oceanic[n1];
    }

    static final class Contact {
        double base;
        double boundaryDist;
        double normal;
        double shear;
        BoundaryType type;
        boolean oceanA;
        boolean oceanB;
    }
}
