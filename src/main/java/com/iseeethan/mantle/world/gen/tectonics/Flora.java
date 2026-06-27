package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

public final class Flora {

    public enum Biome {
        DESERT,
        BARREN,
        SAVANNA,
        STEPPE,
        GRASSLAND,
        SHRUBLAND,
        TEMPERATE_FOREST,
        RAINFOREST,
        TAIGA,
        BOREAL_FOREST,
        ALPINE_TUNDRA
    }

    public enum TreeKind {
        NONE,
        BROADLEAF,
        BIRCH,
        CONIFER,
        JUNGLE,
        ACACIA,
        SCRUB
    }

    public static final class Cover {
        public Biome biome;
        public TreeKind tree;
        public double treeDensity;
        public double bushDensity;
        public double grassDensity;
        public double fernDensity;
        public double flowerDensity;
        public double canopyScale;
        public double vigor;
        public int soilDepth;
        public double rainfall;
    }

    private static final double TREE_MIN_DEPTH = 3.0;
    private static final double CLIFF_SLOPE = 1.25;

    private final GradientNoise patch;
    private final double seaY;

    Flora(long seed, double seaY) {
        this.patch = new GradientNoise(seed ^ 0x7A5F1C39B6D2E481L);
        this.seaY = seaY;
    }

    public Cover cover(double wx, double wz, double surfaceY,
                       Soil.Sample soil, double rain, double temp, double slope, double flow, Cover out) {
        double wetness = soil.wetness;
        double depth = soil.depth;
        double elevAbove = surfaceY - seaY;

        double local = 0.5 + 0.5 * patch.fbm(wx / 340.0, wz / 340.0, 3, 2.0, 0.5);
        double r = clamp(rain * 0.78 + wetness * 0.22 + (local - 0.5) * 0.18, 0.0, 1.0);
        double t = clamp(temp + (patch.noise2(wx / 800.0, wz / 800.0) * 0.05), 0.0, 1.0);

        Biome biome = classify(t, r, elevAbove, slope, flow, soil.type);
        out.biome = biome;

        double vigor = clamp(r * 0.6 + clamp(depth / 6.0, 0, 1) * 0.3 + t * 0.1, 0.0, 1.0);
        vigor *= clamp(1.0 - (slope - 0.5) / (CLIFF_SLOPE - 0.5), 0.2, 1.0);
        out.vigor = vigor;

        applyDensities(out, biome, r, t, depth, slope, flow, vigor);
        out.tree = treeKind(biome);
        if (depth < TREE_MIN_DEPTH) {
            out.treeDensity *= clamp((depth - 1.0) / (TREE_MIN_DEPTH - 1.0), 0.0, 1.0) * 0.4;
        }
        if (slope > 0.9) out.treeDensity *= clamp((CLIFF_SLOPE - slope) / 0.35, 0.0, 1.0);
        out.canopyScale = clamp(0.55 + r * 0.5 + clamp(depth / 8.0, 0, 1) * 0.3 - elevAbove / 1600.0, 0.4, 1.4);
        out.soilDepth = soil.depth;
        out.rainfall = r;
        return out;
    }

    private Biome classify(double t, double r, double elevAbove, double slope, double flow, Soil.Type soil) {
        if (elevAbove > 560 && t < 0.35) return Biome.ALPINE_TUNDRA;

        if (t < 0.18) {
            return r < 0.32 ? Biome.ALPINE_TUNDRA : Biome.TAIGA;
        }
        if (t < 0.4) {
            if (r < 0.22) return Biome.STEPPE;
            if (r < 0.5) return Biome.TAIGA;
            return Biome.BOREAL_FOREST;
        }
        if (t < 0.62) {
            if (r < 0.22) return Biome.STEPPE;
            if (r < 0.42) return Biome.GRASSLAND;
            if (r < 0.66) return soil == Soil.Type.STONY ? Biome.SHRUBLAND : Biome.TEMPERATE_FOREST;
            return Biome.TEMPERATE_FOREST;
        }
        if (r < 0.2) return soil == Soil.Type.SANDY ? Biome.DESERT : Biome.BARREN;
        if (r < 0.42) return Biome.SAVANNA;
        if (r < 0.68) return Biome.SHRUBLAND;
        return Biome.RAINFOREST;
    }

    private void applyDensities(Cover out, Biome b, double r, double t, double depth,
                                double slope, double flow, double vigor) {
        switch (b) {
            case DESERT:
                out.treeDensity = 0.004; out.bushDensity = 0.02;
                out.grassDensity = 0.01; out.fernDensity = 0; out.flowerDensity = 0.002;
                break;
            case BARREN:
                out.treeDensity = 0; out.bushDensity = 0.015;
                out.grassDensity = 0.03; out.fernDensity = 0; out.flowerDensity = 0.004;
                break;
            case SAVANNA:
                out.treeDensity = 0.012; out.bushDensity = 0.05;
                out.grassDensity = 0.55; out.fernDensity = 0; out.flowerDensity = 0.03;
                break;
            case STEPPE:
                out.treeDensity = 0.002; out.bushDensity = 0.04;
                out.grassDensity = 0.45; out.fernDensity = 0; out.flowerDensity = 0.04;
                break;
            case GRASSLAND:
                out.treeDensity = 0.006; out.bushDensity = 0.05;
                out.grassDensity = 0.7; out.fernDensity = 0.02; out.flowerDensity = 0.09;
                break;
            case SHRUBLAND:
                out.treeDensity = 0.02; out.bushDensity = 0.16;
                out.grassDensity = 0.4; out.fernDensity = 0.04; out.flowerDensity = 0.06;
                break;
            case TEMPERATE_FOREST:
                out.treeDensity = 0.16; out.bushDensity = 0.07;
                out.grassDensity = 0.35; out.fernDensity = 0.12; out.flowerDensity = 0.05;
                break;
            case RAINFOREST:
                out.treeDensity = 0.24; out.bushDensity = 0.1;
                out.grassDensity = 0.3; out.fernDensity = 0.2; out.flowerDensity = 0.04;
                break;
            case TAIGA:
                out.treeDensity = 0.1; out.bushDensity = 0.09;
                out.grassDensity = 0.25; out.fernDensity = 0.14; out.flowerDensity = 0.02;
                break;
            case BOREAL_FOREST:
                out.treeDensity = 0.18; out.bushDensity = 0.06;
                out.grassDensity = 0.18; out.fernDensity = 0.16; out.flowerDensity = 0.015;
                break;
            case ALPINE_TUNDRA:
                out.treeDensity = 0.002; out.bushDensity = 0.03;
                out.grassDensity = 0.12; out.fernDensity = 0.01; out.flowerDensity = 0.02;
                break;
            default:
                out.treeDensity = 0; out.bushDensity = 0;
                out.grassDensity = 0; out.fernDensity = 0; out.flowerDensity = 0;
        }
        out.treeDensity *= 0.7 + 0.3 * vigor;
        out.grassDensity *= 0.6 + 0.4 * vigor;
    }

    private static TreeKind treeKind(Biome b) {
        switch (b) {
            case TEMPERATE_FOREST: return TreeKind.BROADLEAF;
            case GRASSLAND:
            case STEPPE: return TreeKind.BIRCH;
            case RAINFOREST: return TreeKind.JUNGLE;
            case SAVANNA: return TreeKind.ACACIA;
            case TAIGA:
            case BOREAL_FOREST:
            case ALPINE_TUNDRA: return TreeKind.CONIFER;
            case SHRUBLAND:
            case DESERT: return TreeKind.SCRUB;
            default: return TreeKind.NONE;
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
