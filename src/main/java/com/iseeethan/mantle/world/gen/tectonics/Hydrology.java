package com.iseeethan.mantle.world.gen.tectonics;

import java.util.PriorityQueue;

public final class Hydrology {

    private static final int[] DX = {  0,  0, -1,  1, -1,  1, -1,  1 };
    private static final int[] DZ = { -1,  1,  0,  0, -1, -1,  1,  1 };

    private final int n;
    private final float seaY;

    private final float[] filled;

    private final int[] downstream;

    private final int[] accum;

    private final float[] waterY;

    private RiverRaster raster;

    private double rasterCell, rasterHalf;
    private int riverAccumThreshold;

    private Hydrology(int n, float seaY) {
        this.n = n;
        this.seaY = seaY;
        this.filled = new float[n * n];
        this.downstream = new int[n * n];
        this.accum = new int[n * n];
        this.waterY = new float[n * n];
    }

    public static Hydrology build(float[] elev, int n, float seaY, int riverAccum, double cell, double half) {
        Hydrology h = new Hydrology(n, seaY);
        h.rasterCell = cell;
        h.rasterHalf = half;
        h.riverAccumThreshold = riverAccum;
        h.priorityFlood(elev);
        h.flowAccumulation();
        h.assignWaterSurface(elev, riverAccum);

        h.raster = RiverRaster.build(h, cell, half, seaY, riverAccum);
        return h;
    }

    private static final class Node implements Comparable<Node> {
        final int idx; final float level; final int order;
        Node(int idx, float level, int order) { this.idx = idx; this.level = level; this.order = order; }
        public int compareTo(Node o) {
            int c = Float.compare(level, o.level);
            return c != 0 ? c : Integer.compare(order, o.order);
        }
    }

    private int[] popOrder;

    private void priorityFlood(float[] elev) {
        boolean[] closed = new boolean[n * n];
        PriorityQueue<Node> pq = new PriorityQueue<>();
        int orderSeq = 0;

        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                boolean edge = (i == 0 || j == 0 || i == n - 1 || j == n - 1);
                boolean ocean = elev[idx] <= seaY;
                if (edge || ocean) {
                    closed[idx] = true;
                    filled[idx] = Math.max(elev[idx], seaY);
                    downstream[idx] = -1;
                    pq.add(new Node(idx, filled[idx], orderSeq++));
                }
            }
        }

        popOrder = new int[n * n];
        int popped = 0;

        while (!pq.isEmpty()) {
            Node cur = pq.poll();
            int ci = cur.idx % n, cj = cur.idx / n;
            popOrder[popped++] = cur.idx;

            for (int d = 0; d < 8; d++) {
                int ni = ci + DX[d], nj = cj + DZ[d];
                if (ni < 0 || nj < 0 || ni >= n || nj >= n) continue;
                int nidx = nj * n + ni;
                if (closed[nidx]) continue;
                closed[nidx] = true;

                filled[nidx] = Math.max(elev[nidx], filled[cur.idx]);
                downstream[nidx] = cur.idx;
                pq.add(new Node(nidx, filled[nidx], orderSeq++));
            }
        }
    }

    private void flowAccumulation() {
        java.util.Arrays.fill(accum, 1);

        for (int k = popOrder.length - 1; k >= 0; k--) {
            int idx = popOrder[k];
            int ds = downstream[idx];
            if (ds >= 0) accum[ds] += accum[idx];
        }
    }

    private boolean[] lake;

    private void assignWaterSurface(float[] elev, int riverAccum) {
        java.util.Arrays.fill(waterY, Float.NaN);
        lake = new boolean[n * n];

        for (int k = 0; k < popOrder.length; k++) {
            int idx = popOrder[k];
            if (filled[idx] <= seaY) continue;

            boolean isLake = filled[idx] > elev[idx] + 0.01f;
            boolean isRiver = !isLake && accum[idx] >= riverAccum;
            if (!isLake && !isRiver) continue;
            lake[idx] = isLake;

            waterY[idx] = filled[idx];
        }
    }

    private static final double LAKE_MIN_CARVE = 3.0;
    private static final double LAKE_MAX_CARVE = 10.0;

    public boolean isLakeCell(int idx) { return lake != null && lake[idx]; }

    public static final class Sample {

        public boolean water;

        public double waterY;

        public double bedY;

        public boolean isLake;

        public boolean bankOnly;

        public double bankMinTop;
    }

    public Sample sample(double wx, double wz, double terrainY, double cell, double half, Sample out) {
        out.water = false; out.isLake = false; out.waterY = 0; out.bedY = 0; out.bankOnly = false; out.bankMinTop = 0;

        int bwx = (int) Math.floor(wx), bwz = (int) Math.floor(wz);

        double gx = (wx + half) / cell - 0.5;
        double gz = (wz + half) / cell - 0.5;
        int ci = (int) Math.floor(gx), cj = (int) Math.floor(gz);
        double lakeSurf = Double.NaN;
        for (int dj = -2; dj <= 2; dj++) {
            for (int di = -2; di <= 2; di++) {
                int ii = ci + di, jj = cj + dj;
                if (ii < 0 || jj < 0 || ii >= n || jj >= n) continue;
                int idx = jj * n + ii;
                if (lake == null || !lake[idx] || Float.isNaN(waterY[idx])) continue;
                double d2 = (gx - ii) * (gx - ii) + (gz - jj) * (gz - jj);
                if (d2 <= 2.25 && (Double.isNaN(lakeSurf) || waterY[idx] > lakeSurf)) lakeSurf = waterY[idx];
            }
        }

        float[] rr = riverScratch.get();
        boolean onRiver = raster != null && raster.lookup(bwx, bwz, rr);
        double riverSurf = onRiver ? rr[0] : Double.NaN;
        double riverBed = onRiver ? rr[1] : Double.NaN;
        boolean riverIsMouth = onRiver && rr[2] != 0f;

        if (!Double.isNaN(lakeSurf) && onRiver) {
            double surf = Math.max(lakeSurf, riverSurf);
            out.water = true; out.isLake = false;
            out.waterY = surf;
            out.bedY = Math.min(riverBed, surf - 1.0);
            if (terrainY < surf) out.bankMinTop = surf;
            return out;
        }

        if (!Double.isNaN(lakeSurf)) {
            out.water = true; out.isLake = true; out.waterY = lakeSurf;
            double natDepth = lakeSurf - terrainY;
            double carve = Math.min(LAKE_MAX_CARVE, Math.max(LAKE_MIN_CARVE, natDepth * 0.6));
            out.bedY = Math.min(terrainY, lakeSurf - carve);
            return out;
        }

        if (onRiver) {
            if (riverIsMouth) {

                if (terrainY >= seaY) {
                    out.water = true; out.isLake = false;
                    out.waterY = seaY;
                    out.bedY = Math.min(riverBed, seaY - 1.0);
                }
                return out;
            }
            double surf = riverSurf;
            if (surf > terrainY) surf = terrainY;
            if (surf < seaY) surf = seaY;
            if (terrainY <= seaY) return out;
            out.water = true; out.isLake = false;
            out.waterY = surf;
            out.bedY = Math.min(riverBed, surf - 1.0);
            return out;
        }

        double nearSurf = Double.NaN; int nearDist2 = Integer.MAX_VALUE;
        final int BANK = 3;
        for (int dz = -BANK; dz <= BANK; dz++) {
            for (int dx = -BANK; dx <= BANK; dx++) {
                if (dx == 0 && dz == 0) continue;
                if (raster != null && raster.lookup(bwx + dx, bwz + dz, rr)) {
                    int d2 = dx * dx + dz * dz;
                    double s = rr[2] != 0f ? seaY : rr[0];
                    if (d2 < nearDist2) { nearDist2 = d2; nearSurf = s; }
                }
            }
        }
        if (!Double.isNaN(nearSurf)) {
            double surf = nearSurf;
            if (surf < seaY) surf = seaY;
            if (terrainY <= seaY) return out;
            double dist = Math.sqrt(nearDist2);
            double bf = Math.max(0, Math.min(1, (dist - 1.0) / BANK));
            out.bankOnly = true;
            double shoreTarget = surf + (terrainY - surf) * (bf * bf);
            out.bedY = Math.min(shoreTarget, terrainY);
            if (terrainY < surf) {
                double rim = terrainY + (surf - terrainY) * (1.0 - bf);
                out.bankMinTop = Math.max(rim, terrainY);
            }
            return out;
        }
        return out;
    }

    private final ThreadLocal<float[]> riverScratch = ThreadLocal.withInitial(() -> new float[3]);

    public int gridSize() { return n; }
    public float filledAt(int idx) { return filled[idx]; }
    public int accumAt(int idx) { return accum[idx]; }
    public int downstreamAt(int idx) { return downstream[idx]; }
    public float waterYAt(int idx) { return waterY[idx]; }

    public float[] filled() { return filled; }
    public int[] accum() { return accum; }
    public int[] downstream() { return downstream; }
    public float[] waterY() { return waterY; }
}
