package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.GenStatus;
import com.iseeethan.mantle.world.noise.GradientNoise;

public final class PlateSim {

    public static final int WORLD = 10000;
    public static final int HALF = WORLD / 2;
    public static final int SEA_Y = 64;
    public static final int FLOOR_Y = -128;

    public static final int PEAK_Y = 1024;

    private static final int MOUNTAIN_TOP_Y = 760;

    public static final int N = 1280;
    private static final double CELL = (double) WORLD / N;

    private static final int HYDRO_MULT = 2;
    private static final int NH = N * HYDRO_MULT;
    private static final double CELLH = (double) WORLD / NH;

    private static final double WARP_BROAD = 520.0;
    private static final double WARP_FINE = 90.0;

    private static final int THERMAL_ITERS = 60;

    private static final int STREAM_STEPS = 40;
    private static final int STREAM_RECARVE_STEPS = 10;
    private static final float TALUS_LOW = 0.008f;
    private static final float TALUS_HIGH = 0.022f;
    private static final float TALUS_HI_REF = 1.2f;

    private static final float MAX_STEP_BLOCKS = 6.5f;

    private static final double SEA_FRACTION = 0.52;
    private static final int SHORE_Y = SEA_Y + 4;

    private static final double LAND_CURVE = 1.9;

    private final long seed;
    private final GradientNoise warp;
    private final GradientNoise relief;
    private final PlateField plates;
    private final Boundary boundary;
    private final Folding folding;
    private final Faulting faulting;
    private final Strata strata;
    private final Erodibility erodibility;
    private final Karst karst;
    private final Caves caves;

    private float[] raw;
    private final float[] elev;
    private float[] elevFine;
    private double seaThreshold;
    private Hydrology hydro;

    private static final int RIVER_ACCUM = 1600;

    public PlateSim(long seed) {
        this.seed = seed;
        this.warp = new GradientNoise(seed ^ 0x9E3779B97F4A7C15L);
        this.relief = new GradientNoise(seed ^ 0xC2B2AE3D27D4EB4FL);
        this.plates = new PlateField(seed, WORLD, HALF);
        this.boundary = new Boundary(seed);
        this.folding = new Folding(seed, SEA_Y);
        this.faulting = new Faulting(seed, SEA_Y);
        this.strata = new Strata(seed, SEA_Y);
        this.erodibility = new Erodibility(seed, strata, SEA_Y);
        this.karst = new Karst(seed, strata, SEA_Y, SHORE_Y);
        this.caves = new Caves(seed, strata, FLOOR_Y);
        this.elev = new float[N * N];
        build();
    }

    private void build() {
        GenStatus.begin();
        GenStatus.stage("Raising continents");
        raw = new float[N * N];
        final int threads = Math.max(1, Math.min(14, Runtime.getRuntime().availableProcessors()));
        Thread[] pool = new Thread[threads];
        for (int ti = 0; ti < threads; ti++) {
            final int t = ti;
            pool[t] = new Thread(() -> {
                PlateField.Contact contact = new PlateField.Contact();
                for (int j = t; j < N; j += threads) {
                    for (int i = 0; i < N; i++) {
                        double wx = (i + 0.5) * CELL - HALF;
                        double wz = (j + 0.5) * CELL - HALF;
                        raw[j * N + i] = (float) rawElevation(wx, wz, contact);
                    }
                }
            });
            pool[t].start();
        }
        join(pool);

        GenStatus.stage("Eroding terrain");
        float[] uplift = upliftField(raw);
        StreamPower.evolve(raw, N, CELL, (float) seaThresholdRaw(raw), uplift, STREAM_STEPS);

        Thermal.erode(raw, N, THERMAL_ITERS, TALUS_LOW, TALUS_HIGH, TALUS_HI_REF);

        mapToWorldY(raw);

        GenStatus.stage("Weathering slopes");
        float[] reposeMul = reposeVariation(N);
        talusGuarantee(elev, N, MAX_STEP_BLOCKS, reposeMul);

        GenStatus.stage("Folding strata");
        folding.apply(elev, N, CELL, HALF);

        GenStatus.stage("Fracturing faults");
        faulting.apply(elev, N, CELL, HALF);
        Thermal.erode(elev, N, 18, MAX_STEP_BLOCKS, MAX_STEP_BLOCKS, 0f, reposeMul);

        GenStatus.stage("Carving river valleys");
        float[] rockK = erodibility.build(elev, N, CELL, HALF);
        float[] flux = new float[N * N];
        StreamPower.evolve(elev, N, CELL, (float) SEA_Y, null, STREAM_RECARVE_STEPS, rockK, flux);

        Thermal.erode(elev, N, 12, MAX_STEP_BLOCKS, MAX_STEP_BLOCKS, 0f, reposeMul);

        smoothGridResidual(elev, N, 6);
        raw = null;

        GenStatus.stage("Refining terrain");
        elevFine = upsampleElev();
        smoothGridResidual(elevFine, NH, 2);

        GenStatus.stage("Computing rivers and lakes");
        hydro = Hydrology.build(elevFine, NH, SEA_Y, RIVER_ACCUM, CELLH, HALF);

        GenStatus.stage("Dissolving karst");
        karst.apply(elevFine, NH, CELLH, HALF);

        GenStatus.done();
    }

    public Hydrology hydrology() { return hydro; }

    public Strata strata() { return strata; }

    public Caves caves() { return caves; }

    private double warpX(double wx, double wz) {
        return wx + warp.fbm(wx / 1600.0, wz / 1600.0, 4, 2.0, 0.5) * WARP_BROAD
                  + warp.noise2(wx / 260.0, wz / 260.0) * WARP_FINE;
    }

    private double warpZ(double wx, double wz) {
        return wz + warp.fbm((wx + 7000) / 1600.0, (wz - 7000) / 1600.0, 4, 2.0, 0.5) * WARP_BROAD
                  + warp.noise2((wx - 4000) / 260.0, (wz + 4000) / 260.0) * WARP_FINE;
    }

    public boolean isFaultScarp(double wx, double wz) {
        double surf = surfaceY(wx, wz);
        if (surf < SEA_Y + 4) return false;
        return faulting.scarpStrength(wx, wz, surf) > 1.2;
    }

    public boolean isFoldRidge(double wx, double wz) {
        double surf = surfaceY(wx, wz);
        if (surf < SEA_Y + 4) return false;
        return folding.ridgeStrength(wx, wz, surf) > 8.0;
    }

    public boolean isRift(double wx, double wz) {
        return boundaryType(wx, wz) == BoundaryType.DIVERGENT && nearBoundary(wx, wz, 420.0);
    }

    public boolean isMountainBelt(double wx, double wz) {
        double surf = surfaceY(wx, wz);
        if (surf < SEA_Y + 60) return false;
        return boundaryType(wx, wz) == BoundaryType.CONVERGENT && nearBoundary(wx, wz, 700.0);
    }

    private BoundaryType boundaryType(double wx, double wz) {
        PlateField.Contact c = new PlateField.Contact();
        plates.evaluate(warpX(wx, wz), warpZ(wx, wz), c);
        return c.type;
    }

    private boolean nearBoundary(double wx, double wz, double width) {
        PlateField.Contact c = new PlateField.Contact();
        plates.evaluate(warpX(wx, wz), warpZ(wx, wz), c);
        return Math.abs(c.boundaryDist) < width;
    }

    private float[] upliftField(float[] r) {
        float seaRaw = (float) seaThresholdRaw(r);
        float[] u = new float[r.length];

        double maxAbove = 1e-6;
        for (float v : r) if (v - seaRaw > maxAbove) maxAbove = v - seaRaw;
        double scale = UPLIFT_RATE / maxAbove;
        for (int i = 0; i < r.length; i++) {
            double above = r[i] - seaRaw;
            u[i] = above > 0 ? (float) (above * scale) : 0f;
        }
        return u;
    }

    private double seaThresholdRaw(float[] r) {
        float[] s = r.clone();
        java.util.Arrays.sort(s);
        return s[(int) (SEA_FRACTION * (s.length - 1))];
    }

    private static final double UPLIFT_RATE = 0.0025;

    public Hydrology.Sample sampleRiver(double wx, double wz, double terrainY, Hydrology.Sample out) {
        return hydro.sample(wx, wz, terrainY, CELLH, HALF, out);
    }

    private static void smoothGridResidual(float[] e, int n, int passes) {
        float[] tmp = new float[n * n];
        for (int p = 0; p < passes; p++) {
            for (int j = 0; j < n; j++) {
                int jm = Math.max(0, j - 1), jp = Math.min(n - 1, j + 1);
                for (int i = 0; i < n; i++) {
                    int im = Math.max(0, i - 1), ip = Math.min(n - 1, i + 1);

                    float s = e[jm * n + im] + e[jm * n + ip] + e[jp * n + im] + e[jp * n + ip]
                            + 2 * (e[j * n + im] + e[j * n + ip] + e[jm * n + i] + e[jp * n + i])
                            + 4 * e[j * n + i];
                    tmp[j * n + i] = s / 16f;
                }
            }
            System.arraycopy(tmp, 0, e, 0, n * n);
        }
    }

    private double rawElevation(double wx, double wz, PlateField.Contact contact) {

        double sx = warpX(wx, wz);
        double sz = warpZ(wx, wz);

        plates.evaluate(sx, sz, contact);
        double base = contact.base;
        double boundary = this.boundary.contribution(sx, sz, contact);

        double landish = clamp((base + boundary + 0.35) * 1.6, 0.0, 1.0);
        double continental = clamp((base + 0.4) * 1.4, 0.0, 1.0);
        double regional = relief.fbm(wx / 2600.0, wz / 2600.0, 5, 2.0, 0.5);
        double hills    = relief.fbm((wx + 1234) / 900.0, (wz - 4321) / 900.0, 5, 2.05, 0.5);
        double detail   = relief.fbm(wx / 220.0, wz / 220.0, 4, 2.2, 0.5);
        double ridge    = relief.ridged(wx / 1400.0, wz / 1400.0, 4, 2.0, 0.55) - 0.5;

        double relief2 = regional * 0.62
                       + hills * 0.28 * (0.4 + 0.6 * continental)
                       + detail * 0.06
                       + ridge * 0.34 * landish;

        relief2 *= (0.30 + 0.70 * landish);

        return base + boundary + relief2;
    }

    private void mapToWorldY(float[] r) {
        int n = r.length;

        float[] sorted = r.clone();
        java.util.Arrays.sort(sorted);
        seaThreshold = sorted[(int) (SEA_FRACTION * (n - 1))];

        int seaRank = (int) (SEA_FRACTION * (n - 1));
        int landCount = Math.max(1, (n - 1) - seaRank);

        for (int idx = 0; idx < n; idx++) {
            double v = r[idx];

            int rank = lowerBound(sorted, (float) v);
            double y;
            if (rank <= seaRank) {

                double t = 1.0 - (double) rank / Math.max(1, seaRank);
                t = Math.pow(t, 1.20);
                y = SEA_Y - t * (SEA_Y - FLOOR_Y);
            } else {

                double p = (double) (rank - seaRank) / landCount;
                double t = Math.pow(p, LAND_CURVE);
                y = SHORE_Y + t * (MOUNTAIN_TOP_Y - SHORE_Y);
            }
            elev[idx] = (float) y;
        }
    }

    private void talusGuarantee(float[] e, int n, float maxStep, float[] reposeMul) {
        final int F = 4;
        int cn = n / F;
        float[] coarse = new float[cn * cn];
        float[] coarseMul = new float[cn * cn];
        for (int j = 0; j < cn; j++)
            for (int i = 0; i < cn; i++) {
                float s = 0, m = 0;
                for (int dj = 0; dj < F; dj++)
                    for (int di = 0; di < F; di++) {
                        int fi = (j * F + dj) * n + (i * F + di);
                        s += e[fi]; m += reposeMul[fi];
                    }
                coarse[j * cn + i] = s / (F * F);
                coarseMul[j * cn + i] = m / (F * F);
            }
        float[] before = coarse.clone();

        Thermal.erode(coarse, cn, 2500, maxStep * F, maxStep * F, 0f, coarseMul);

        float[] delta = new float[cn * cn];
        for (int k = 0; k < delta.length; k++) delta[k] = coarse[k] - before[k];

        for (int j = 0; j < n; j++) {
            double fj = (j + 0.5) / F - 0.5;
            int cj0 = (int) Math.floor(fj);
            double tj = smooth01(fj - cj0);
            for (int i = 0; i < n; i++) {
                double fi = (i + 0.5) / F - 0.5;
                int ci0 = (int) Math.floor(fi);
                double ti = smooth01(fi - ci0);
                double d00 = at(delta, cn, ci0, cj0);
                double d10 = at(delta, cn, ci0 + 1, cj0);
                double d01 = at(delta, cn, ci0, cj0 + 1);
                double d11 = at(delta, cn, ci0 + 1, cj0 + 1);
                double top = d00 + (d10 - d00) * ti;
                double bot = d01 + (d11 - d01) * ti;
                e[j * n + i] += (float) (top + (bot - top) * tj);
            }
        }

        Thermal.erode(e, n, 30, maxStep, maxStep, 0f, reposeMul);
    }

    private float[] reposeVariation(int n) {
        float[] mul = new float[n * n];
        for (int j = 0; j < n; j++) {
            double wz = (j + 0.5) * CELL - HALF;
            for (int i = 0; i < n; i++) {
                double wx = (i + 0.5) * CELL - HALF;

                double v = relief.fbm(wx / 600.0, wz / 600.0, 4, 2.0, 0.5)
                         + 0.4 * relief.noise2(wx / 140.0, wz / 140.0, 99);
                mul[j * n + i] = (float) (1.0 + 0.55 * v);
                if (mul[j * n + i] < 0.4f) mul[j * n + i] = 0.4f;
            }
        }
        return mul;
    }

    private static int lowerBound(float[] a, float key) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] < key) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    public double surfaceY(double wx, double wz) {
        return sampleGrid(elevFine, NH, CELLH, wx, wz);
    }

    private static double sampleGrid(float[] e, int n, double cell, double wx, double wz) {
        double fx = (wx + HALF) / cell - 0.5;
        double fz = (wz + HALF) / cell - 0.5;
        int ix = (int) Math.floor(fx);
        int iz = (int) Math.floor(fz);
        double tx = smooth01(fx - ix), tz = smooth01(fz - iz);

        double a00 = at(e, n, ix, iz),     a10 = at(e, n, ix + 1, iz);
        double a01 = at(e, n, ix, iz + 1), a11 = at(e, n, ix + 1, iz + 1);
        double top = a00 + (a10 - a00) * tx;
        double bot = a01 + (a11 - a01) * tx;
        return top + (bot - top) * tz;
    }

    private static double smooth01(double t) { return t * t * (3 - 2 * t); }

    public boolean isOcean(double wx, double wz) {
        return surfaceY(wx, wz) < SEA_Y;
    }

    public double macroSlope(double wx, double wz) {
        double r = 8.0;
        double dx = (surfaceY(wx + r, wz) - surfaceY(wx - r, wz)) / (2 * r);
        double dz = (surfaceY(wx, wz + r) - surfaceY(wx, wz - r)) / (2 * r);
        return Math.hypot(dx, dz);
    }

    private static double at(float[] e, int n, int i, int j) {
        if (i < 0) i = 0; else if (i >= n) i = n - 1;
        if (j < 0) j = 0; else if (j >= n) j = n - 1;
        return e[j * n + i];
    }

    private float[] upsampleElev() {
        float[] e = new float[NH * NH];
        final int threads = Math.max(1, Math.min(14, Runtime.getRuntime().availableProcessors()));
        Thread[] pool = new Thread[threads];
        for (int ti = 0; ti < threads; ti++) {
            final int t = ti;
            pool[t] = new Thread(() -> {
                for (int j = t; j < NH; j += threads) {
                    double wz = (j + 0.5) * CELLH - HALF;
                    for (int i = 0; i < NH; i++) {
                        double wx = (i + 0.5) * CELLH - HALF;
                        e[j * NH + i] = (float) sampleGrid(elev, N, CELL, wx, wz);
                    }
                }
            });
            pool[t].start();
        }
        join(pool);
        return e;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static void join(Thread[] pool) {
        for (Thread th : pool) {
            try { th.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public long seed() { return seed; }
}
