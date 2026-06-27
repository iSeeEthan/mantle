package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

public final class Soil {


    public enum Type {
        LOAM,
        SILT,
        CLAY,
        SANDY,
        STONY
    }

    public static final class Sample {
        public Type type;
        public int depth;
        public double wetness;
    }

    private static final int MAX_DEPTH = 8;
    private static final double VALLEY_ACCUM = 60.0;
    private static final double FLAT_SLOPE = 0.18;
    private static final double STEEP_SLOPE = 1.15;
    private static final int STONY_HEIGHT = 360;

    private final GradientNoise jitter;
    private final PlateSim sim;
    private final Climate climate;
    private final double seaY;

    Soil(long seed, PlateSim sim, Climate climate, double seaY) {
        this.jitter = new GradientNoise(seed ^ 0x2545F4914F6CDD1DL);
        this.sim = sim;
        this.climate = climate;
        this.seaY = seaY;
    }

    public Sample sample(double wx, double wz, double surfaceY, Sample out) {
        double slope = sim.macroSlope(wx, wz);
        double rain = climate.rainfall(wx, wz);
        int accum = sim.flowAccumAt(wx, wz);
        double flow = Math.min(1.0, accum / VALLEY_ACCUM);

        out.wetness = clamp(rain * 0.7 + flow * 0.5, 0.0, 1.0);
        out.depth = depth(slope, rain, flow, wx, wz);
        out.type = type(wx, wz, surfaceY, slope, rain, flow);
        return out;
    }

    private int depth(double slope, double rain, double flow, double wx, double wz) {
        double slopeFactor = clamp(1.0 - slope / STEEP_SLOPE, 0.0, 1.0);

        double accumBoost = flow * 3.0;
        double rainBoost = rain * 2.4;
        double base = 2.2 + rainBoost + accumBoost;

        double d = base * slopeFactor;
        d += 0.8 * jitter.noise2(wx / 70.0, wz / 70.0);

        int depth = (int) Math.round(d);
        if (depth < 0) depth = 0;
        if (depth > MAX_DEPTH) depth = MAX_DEPTH;
        return depth;
    }

    private Type type(double wx, double wz, double surfaceY, double slope, double rain, double flow) {
        if (surfaceY - seaY > STONY_HEIGHT && slope > 0.7) {
            return Type.STONY;
        }

        if (slope < FLAT_SLOPE && (flow > 0.35 || rain > 0.6)) {
            return Type.CLAY;
        }
        if (flow > 0.3 && slope < 0.4) {
            return Type.SILT;
        }
        if (rain < 0.32 || slope > 0.85) {
            return Type.SANDY;
        }
        return Type.LOAM;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
