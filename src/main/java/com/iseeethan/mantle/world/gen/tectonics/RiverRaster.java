package com.iseeethan.mantle.world.gen.tectonics;

import java.util.Arrays;

final class RiverRaster {

    private long[] keys;
    private float[] surf;
    private float[] bed;
    private float[] nearDist;
    private boolean[] isLakeEdge;
    private int mask;
    private int size;
    private static final long EMPTY = Long.MIN_VALUE;

    private RiverRaster(int capacityPow2) {
        keys = new long[capacityPow2];
        surf = new float[capacityPow2];
        bed = new float[capacityPow2];
        nearDist = new float[capacityPow2];
        isLakeEdge = new boolean[capacityPow2];
        Arrays.fill(keys, EMPTY);
        mask = capacityPow2 - 1;
    }

    static RiverRaster build(Hydrology h, double cell, double half, float seaY, int riverAccum) {
        int n = h.gridSize();
        int[] accum = h.accum();
        int[] down = h.downstream();
        float[] waterY = h.waterY();

        long est = 0;
        for (int i = 0; i < n * n; i++) {
            if (!Float.isNaN(waterY[i]) && !h.isLakeCell(i) && accum[i] >= riverAccum) {
                est += (long) (cell * 4);
            }
        }
        int cap = 1 << Math.max(12, 64 - Long.numberOfLeadingZeros(Math.max(16, est * 2)));
        RiverRaster r = new RiverRaster(cap);

        int[] surfI = new int[n * n];
        for (int i = 0; i < n * n; i++) surfI[i] = Float.isNaN(waterY[i]) ? Integer.MIN_VALUE : Math.round(waterY[i]);

        for (int idx = 0; idx < n * n; idx++) {
            if (Float.isNaN(waterY[idx]) || h.isLakeCell(idx) || accum[idx] < riverAccum) continue;
            int ds = down[idx];
            if (ds >= 0 && surfI[ds] != Integer.MIN_VALUE && surfI[idx] < surfI[ds]) surfI[idx] = surfI[ds];
        }

        for (int idx = 0; idx < n * n; idx++) {
            if (Float.isNaN(waterY[idx]) || h.isLakeCell(idx)) continue;
            if (accum[idx] < riverAccum) continue;

            int ci = idx % n, cj = idx / n;
            int ds = down[idx];

            double ax = (ci + 0.5) * cell - half;
            double az = (cj + 0.5) * cell - half;
            double bx, bz;
            int aSurfI = surfI[idx], bSurfI;
            boolean toOcean = false, toLake = false;
            if (ds >= 0) {
                bx = (ds % n + 0.5) * cell - half;
                bz = (ds / n + 0.5) * cell - half;
                if (Float.isNaN(waterY[ds])) { toOcean = true; bSurfI = Math.round(seaY); }
                else if (h.isLakeCell(ds)) { toLake = true; bSurfI = Math.round(waterY[ds]); }
                else { bSurfI = surfI[ds]; }
            } else {
                bx = ax; bz = az; bSurfI = aSurfI;
            }

            double segLenBlocks = Math.hypot(bx - ax, bz - az);
            double drop = Math.max(0, aSurfI - bSurfI);
            double grad = segLenBlocks > 1e-6 ? drop / segLenBlocks : 0;
            double a = accum[idx];
            double steepShrink = 1.0 / (1.0 + 2.5 * grad);
            double halfW = (1.0 + 0.7 * Math.sqrt(a) / 30.0) * steepShrink;
            if (halfW > 4.0) halfW = 4.0;
            if (toOcean) halfW = Math.max(halfW, 3.5);
            double overlapFloor = 0.55 * cell;
            if (halfW < overlapFloor) halfW = overlapFloor;

            double maxDepth = Math.min(3.0, 0.6 + 0.012 * Math.sqrt(a)) * steepShrink;

            if ((toOcean || toLake) && segLenBlocks > 1e-6) {
                double ex = (bx - ax) / segLenBlocks, ez = (bz - az) / segLenBlocks;
                double over = toLake ? 3.0 * cell : 3 * cell;
                bx = ax + ex * (segLenBlocks + over);
                bz = az + ez * (segLenBlocks + over);
            }

            r.stampSegment(ax, az, aSurfI, bx, bz, bSurfI, halfW, maxDepth, seaY, toOcean);
        }
        return r;
    }

    private void stampSegment(double ax, double az, int aSurf, double bx, double bz, int bSurf,
                              double halfW, double maxDepth, double seaY, boolean toOcean) {
        double vx = bx - ax, vz = bz - az;
        double len2 = vx * vx + vz * vz;
        int x0 = (int) Math.floor(Math.min(ax, bx) - halfW - 1);
        int x1 = (int) Math.ceil(Math.max(ax, bx) + halfW + 1);
        int z0 = (int) Math.floor(Math.min(az, bz) - halfW - 1);
        int z1 = (int) Math.ceil(Math.max(az, bz) + halfW + 1);

        for (int wz = z0; wz <= z1; wz++) {
            for (int wx = x0; wx <= x1; wx++) {
                double t = len2 > 1e-9 ? ((wx - ax) * vx + (wz - az) * vz) / len2 : 0;
                t = Math.max(0, Math.min(1, t));
                double px = ax + t * vx, pz = az + t * vz;
                double dist = Math.hypot(wx - px, wz - pz);
                if (dist > halfW) continue;

                float surfY = (float) (aSurf + (bSurf - aSurf) * t);

                int loSurf = Math.min(aSurf, bSurf);
                double rel = halfW > 1e-6 ? dist / halfW : 1.0;
                double depth = maxDepth * (1.0 - rel * rel);
                double bedY = loSurf - depth - 1.0;

                boolean edgeBody = toOcean;
                if (toOcean) {
                    surfY = (float) seaY;
                    bedY = seaY - 1.0 - 2.0 * (1.0 - rel * rel);
                }

                if (!toOcean && surfY < seaY) surfY = (float) seaY;

                put(wx, wz, surfY, (float) bedY, (float) dist, edgeBody);
            }
        }
    }

    private static long key(int wx, int wz) {

        return ((long) wx << 32) | (wz & 0xFFFFFFFFL);
    }

    private static int hash(long k) {
        k *= 0xff51afd7ed558ccdL; k ^= k >>> 33;
        return (int) k;
    }

    private void put(int wx, int wz, float surfY, float bedY, float dist, boolean edgeBody) {
        if ((size + 1) * 4 >= keys.length * 3) rehash();
        long k = key(wx, wz);
        int i = hash(k) & mask;
        while (keys[i] != EMPTY) {
            if (keys[i] == k) {
                if (bedY < bed[i]) bed[i] = bedY;

                if (edgeBody) {
                    if (!isLakeEdge[i] || surfY < surf[i]) surf[i] = surfY;
                    isLakeEdge[i] = true; nearDist[i] = dist;
                } else if (!isLakeEdge[i] && surfY < surf[i]) {
                    surf[i] = surfY; nearDist[i] = dist;
                }
                return;
            }
            i = (i + 1) & mask;
        }
        keys[i] = k; surf[i] = surfY; bed[i] = bedY; nearDist[i] = dist; isLakeEdge[i] = edgeBody; size++;
    }

    private void rehash() {
        long[] ok = keys; float[] os = surf, ob = bed, od = nearDist; boolean[] oe = isLakeEdge;
        int ncap = keys.length << 1;
        keys = new long[ncap]; surf = new float[ncap]; bed = new float[ncap]; nearDist = new float[ncap];
        isLakeEdge = new boolean[ncap];
        Arrays.fill(keys, EMPTY); mask = ncap - 1; size = 0;
        for (int j = 0; j < ok.length; j++) {
            if (ok[j] != EMPTY) put((int) (ok[j] >> 32), (int) ok[j], os[j], ob[j], od[j], oe[j]);
        }
    }

    boolean lookup(int wx, int wz, float[] out) {
        long k = key(wx, wz);
        int i = hash(k) & mask;
        while (keys[i] != EMPTY) {
            if (keys[i] == k) { out[0] = surf[i]; out[1] = bed[i]; out[2] = isLakeEdge[i] ? 1f : 0f; return true; }
            i = (i + 1) & mask;
        }
        return false;
    }

    int size() { return size; }
}
