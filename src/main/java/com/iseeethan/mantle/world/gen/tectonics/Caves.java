package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

public final class Caves {

    private static final int FLOOR_MARGIN = 6;
    private static final int SURFACE_MARGIN = 6;

    private static final double TUNNEL_SCALE_XZ = 110.0;
    private static final double TUNNEL_SCALE_Y = 60.0;
    private static final double TUNNEL_RADIUS = 0.016;

    private static final double SECONDARY_SCALE_XZ = 72.0;
    private static final double SECONDARY_RADIUS = 0.012;

    private static final double CHEESE_SCALE = 30.0;
    private static final double CHEESE_THRESHOLD = 0.965;

    private static final double KARST_BAND = 70.0;

    private final GradientNoise tunnelA;
    private final GradientNoise tunnelB;
    private final GradientNoise cheese;
    private final GradientNoise warp;
    private final Strata strata;
    private final int floorY;

    Caves(long seed, Strata strata, int floorY) {
        this.tunnelA = new GradientNoise(seed ^ 0x27D4EB2F165667C5L);
        this.tunnelB = new GradientNoise(seed ^ 0x9E3779B185EBCA87L);
        this.cheese = new GradientNoise(seed ^ 0xC2B2AE3D27D4EB4FL);
        this.warp = new GradientNoise(seed ^ 0x165667B19E3779F9L);
        this.strata = strata;
        this.floorY = floorY;
    }

    public static final class Column {
        double sx, sz;
        double soluble;
        int solidTop;
        boolean trivial;
    }

    public Column column(int wx, int wz, int solidTop, double surfaceY, Column out) {
        out.solidTop = solidTop;
        out.trivial = solidTop - SURFACE_MARGIN <= floorY + FLOOR_MARGIN;
        if (out.trivial) return out;

        double wxd = wx, wzd = wz;
        double wpx = warp.fbm(wxd / 220.0, wzd / 220.0, 3, 2.0, 0.5) * 18.0;
        double wpz = warp.fbm((wxd + 4096) / 220.0, (wzd - 4096) / 220.0, 3, 2.0, 0.5) * 18.0;
        out.sx = wxd + wpx;
        out.sz = wzd + wpz;
        out.soluble = solubility(wx, wz, surfaceY);
        return out;
    }

    public boolean carved(Column ctx, int y) {
        if (ctx.trivial) return false;
        if (y <= floorY + FLOOR_MARGIN) return false;
        if (y >= ctx.solidTop - SURFACE_MARGIN) return false;

        double yd = y;
        double sx = ctx.sx, sz = ctx.sz;
        double soluble = ctx.soluble;

        double tunnelRadius = TUNNEL_RADIUS * (1.0 + 0.5 * soluble);
        double secondaryRadius = SECONDARY_RADIUS * (1.0 + 0.5 * soluble);

        double a = tunnelA.noise2(sx / TUNNEL_SCALE_XZ, yd / TUNNEL_SCALE_Y + sz / (TUNNEL_SCALE_XZ * 4));
        double a2 = tunnelA.noise2(sz / TUNNEL_SCALE_XZ + 31.7, yd / TUNNEL_SCALE_Y - sx / (TUNNEL_SCALE_XZ * 4));
        if (a * a + a2 * a2 < tunnelRadius) return true;

        double b = tunnelB.noise2(sx / SECONDARY_SCALE_XZ + 11.3, yd / (TUNNEL_SCALE_Y * 0.8));
        double b2 = tunnelB.noise2(sz / SECONDARY_SCALE_XZ - 7.1, yd / (TUNNEL_SCALE_Y * 0.8) + 5.0);
        if (b * b + b2 * b2 < secondaryRadius) return true;

        double depthFade = depthFade(y, ctx.solidTop);
        double cheeseThreshold = CHEESE_THRESHOLD - 0.05 * soluble - 0.05 * depthFade;
        double c = cheese.fbm(sx / CHEESE_SCALE, yd / (CHEESE_SCALE * 0.6), 3, 2.0, 0.5);
        double c2 = cheese.fbm(sz / CHEESE_SCALE + 19.0, yd / (CHEESE_SCALE * 0.6) - 13.0, 3, 2.0, 0.5);
        double cheeseVal = Math.max(Math.abs(c), Math.abs(c2));
        return cheeseVal > cheeseThreshold;
    }

    public boolean carved(int wx, int y, int wz, int solidTop, double surfaceY) {
        return carved(column(wx, wz, solidTop, surfaceY, new Column()), y);
    }

    private double solubility(int wx, int wz, double surfaceY) {
        double sampleY = surfaceY - KARST_BAND * 0.5;
        Strata.Rock rock = strata.typeAt(wx, wz, sampleY, surfaceY);
        return rock == Strata.Rock.SEDIMENTARY ? 1.0 : 0.0;
    }

    private double depthFade(int y, int solidTop) {
        double span = solidTop - floorY;
        if (span <= 0) return 0;
        double t = (double) (solidTop - y) / span;
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }
}
