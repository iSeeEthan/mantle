package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

final class Boundary {

    private static final double RANGE_WIDTH = 700.0;
    private static final double RIFT_WIDTH = 420.0;
    private static final double SHEAR_WIDTH = 260.0;

    private final GradientNoise crest;
    private final GradientNoise grain;

    Boundary(long seed) {
        this.crest = new GradientNoise(seed ^ 0xC2B2AE3D27D4EB4FL);
        this.grain = new GradientNoise(seed ^ 0x165667B19E3779F9L);
    }

    double contribution(double sx, double sz, PlateField.Contact c) {
        switch (c.type) {
            case CONVERGENT: return convergent(sx, sz, c);
            case DIVERGENT:  return divergent(sx, sz, c);
            case TRANSFORM:  return transform(sx, sz, c);
            default:         return 0.0;
        }
    }

    private double convergent(double sx, double sz, PlateField.Contact c) {
        double closing = c.normal;
        boolean o1 = c.oceanA, o2 = c.oceanB;

        double uplift;
        if (!o1 && !o2)      uplift = closing * 2.4;
        else if (o1 && o2)   uplift = closing * 0.9;
        else {
            boolean thisContinental = !o1;
            uplift = thisContinental ? closing * 1.9 : -closing * 1.2;
        }

        double ramp = smoothstep(RANGE_WIDTH, 0.0, Math.abs(c.boundaryDist));
        double r = crest.ridged(sx / 1300.0, sz / 1300.0, 4, 2.05, 0.5);
        return uplift * ramp * (0.70 + 0.55 * r);
    }

    private double divergent(double sx, double sz, PlateField.Contact c) {
        double opening = -c.normal;
        double ramp = smoothstep(RIFT_WIDTH, 0.0, Math.abs(c.boundaryDist));

        double shoulder = grain.ridged(sx / 700.0, sz / 700.0, 3, 2.1, 0.5);
        double valley = -opening * ramp * (0.8 + 0.4 * shoulder);

        double flank = opening * smoothstep(RIFT_WIDTH * 2.2, RIFT_WIDTH, Math.abs(c.boundaryDist)) * 0.35;
        return valley + flank;
    }

    private double transform(double sx, double sz, PlateField.Contact c) {
        double shear = Math.abs(c.shear);
        double ramp = smoothstep(SHEAR_WIDTH, 0.0, Math.abs(c.boundaryDist));
        double side = Math.signum(c.boundaryDist);
        double ridge = grain.fbm(sx / 340.0, sz / 340.0, 4, 2.0, 0.5);
        return shear * ramp * side * (0.25 + 0.5 * ridge) * 0.6;
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
