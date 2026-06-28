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

    private static final int STREAM_STEPS = 70;
    private static final int STREAM_RECARVE_STEPS = 26;
    private static final int SEDIMENT_PASSES = 8;
    private static final int GLACIAL_ITERS = 4;
    private static final int COASTAL_PASSES = 3;
    private static final float TALUS_LOW = 0.008f;
    private static final float TALUS_HIGH = 0.022f;
    private static final float TALUS_HI_REF = 1.2f;

    private static final float MAX_STEP_BLOCKS = 6.5f;

    private static final double SEA_FRACTION = 0.52;
    private static final int SHORE_Y = SEA_Y + 4;

    private static final double LAND_CURVE = 1.9;

    public static final class Params {
        public double seaFraction = SEA_FRACTION;
        public double landCurve = LAND_CURVE;
        public int mountainTopY = MOUNTAIN_TOP_Y;
        public double continentScale = 1.0;
        public double erosionIntensity = 1.0;
        public double riverDensity = 1.0;
        public double temperatureScale = 1.0;
        public double rainfallScale = 1.0;
        public double polarColdness = 1.0;
        public double continentalDryness = 1.0;
        public double sedimentStrength = 1.0;
        public double glacialStrength = 1.0;
        public double coastalStrength = 1.0;
        public double ruggedness = 1.0;
    }

    private final Params params;

    private final long seed;
    private final GradientNoise warp;
    private final GradientNoise relief;
    private final GradientNoise detail;
    private final GradientNoise ridge;
    private final PlateField plates;
    private final Boundary boundary;
    private final Folding folding;
    private final Faulting faulting;
    private final Glacial glacial;
    private final Strata strata;
    private final Erodibility erodibility;
    private final Karst karst;
    private final Caves caves;
    private final Climate climate;
    private final Soil soil;
    private final Flora flora;

    private float[] raw;
    private final float[] elev;
    private float[] elevFine;
    private double seaThreshold;
    private Hydrology hydro;

    private static final int RIVER_ACCUM = 1600;

    public PlateSim(long seed) {
        this(seed, new Params());
    }

    public PlateSim(long seed, Params params) {
        this.params = params;
        this.seed = seed;
        this.warp = new GradientNoise(seed ^ 0x9E3779B97F4A7C15L);
        this.relief = new GradientNoise(seed ^ 0xC2B2AE3D27D4EB4FL);
        this.detail = new GradientNoise(seed ^ 0xA0761D6478BD642FL);
        this.ridge = new GradientNoise(seed ^ 0xE7037ED1A0B428DBL);
        this.plates = new PlateField(seed, WORLD, HALF);
        this.boundary = new Boundary(seed);
        this.folding = new Folding(seed, SEA_Y);
        this.faulting = new Faulting(seed, SEA_Y);
        this.glacial = new Glacial(seed, SEA_Y, HALF, params.polarColdness, params.temperatureScale);
        this.strata = new Strata(seed, SEA_Y);
        this.erodibility = new Erodibility(seed, strata, SEA_Y);
        this.karst = new Karst(seed, strata, SEA_Y, SHORE_Y);
        this.caves = new Caves(seed, strata, FLOOR_Y);
        this.climate = new Climate(seed, this, SEA_Y, params);
        this.soil = new Soil(seed, this, climate, SEA_Y);
        this.flora = new Flora(seed, SEA_Y);
        this.elev = new float[N * N];
        build();
    }

    private PlateSim(long seed, boolean previewOnly, Params params) {
        this.params = params;
        this.seed = seed;
        this.warp = new GradientNoise(seed ^ 0x9E3779B97F4A7C15L);
        this.relief = new GradientNoise(seed ^ 0xC2B2AE3D27D4EB4FL);
        this.detail = new GradientNoise(seed ^ 0xA0761D6478BD642FL);
        this.ridge = new GradientNoise(seed ^ 0xE7037ED1A0B428DBL);
        this.plates = new PlateField(seed, WORLD, HALF);
        this.boundary = new Boundary(seed);
        this.folding = null;
        this.faulting = null;
        this.glacial = null;
        this.strata = null;
        this.erodibility = null;
        this.karst = null;
        this.caves = null;
        this.climate = null;
        this.soil = null;
        this.flora = null;
        this.elev = null;
    }

    public static float[] previewRelief(long seed, int res) {
        return previewRelief(seed, res, new Params());
    }

    public static float[] previewRelief(long seed, int res, Params params) {
        PlateSim p = new PlateSim(seed, true, params);
        int n = res;
        double cell = (double) WORLD / n;
        float[] r = new float[n * n];
        PlateField.Contact contact = new PlateField.Contact();
        for (int j = 0; j < n; j++) {
            double wz = (j + 0.5) * cell - HALF;
            for (int i = 0; i < n; i++) {
                double wx = (i + 0.5) * cell - HALF;
                r[j * n + i] = (float) p.rawElevation(wx, wz, contact);
            }
        }

        float seaRaw = (float) p.seaThresholdRaw(r);
        float[] uplift = new float[r.length];
        double maxAbove = 1e-6;
        for (float v : r) if (v - seaRaw > maxAbove) maxAbove = v - seaRaw;
        double scale = UPLIFT_RATE / maxAbove;
        for (int i = 0; i < r.length; i++) {
            double above = r[i] - seaRaw;
            uplift[i] = above > 0 ? (float) (above * scale) : 0f;
        }
        int previewSteps = Math.max(1, (int) Math.round(24 * params.erosionIntensity));
        StreamPower.evolve(r, n, cell, seaRaw, uplift, previewSteps);

        return p.mapRawToWorldY(r, n);
    }

    public static final class PreviewMaps {
        public final int res;
        public final float[] height;
        public final float[] temperature;
        public final float[] rainfall;

        PreviewMaps(int res, float[] height, float[] temperature, float[] rainfall) {
            this.res = res;
            this.height = height;
            this.temperature = temperature;
            this.rainfall = rainfall;
        }
    }

    public static PreviewMaps previewMaps(long seed, int res, Params params) {
        float[] height = previewRelief(seed, res, params);
        int n = res;
        double cell = (double) WORLD / n;
        GradientNoise humidity = new GradientNoise(seed ^ 0x68E31DA4FB1133A5L);
        GradientNoise tempJitter = new GradientNoise(seed ^ 0xD1B54A32D192ED03L);

        float[] temp = new float[n * n];
        float[] rain = new float[n * n];
        double poleZ = HALF;

        for (int j = 0; j < n; j++) {
            double wz = (j + 0.5) * cell - HALF;
            for (int i = 0; i < n; i++) {
                double wx = (i + 0.5) * cell - HALF;
                double h = height[j * n + i];

                double cont = gridContinentality(height, n, i, j);

                double wobble = tempJitter.fbm(wx / 2400.0, wz / 2400.0, 3, 2.0, 0.5) * 900.0;
                double lat = clamp((Math.abs(wz + wobble)) / poleZ, 0.0, 1.0);
                double latTemp = 1.0 - clamp(lat * params.polarColdness, 0.0, 1.0);
                double above = h - SEA_Y;
                double lapse = clamp(above / 620.0, 0.0, 1.0);
                double t = latTemp * 0.70 - lapse * 0.44 + 0.5 * 0.30;
                t += (t - 0.5) * (0.22 * cont);
                t += 0.04 * tempJitter.noise2(wx / 520.0, wz / 520.0);
                t = 0.5 + (t - 0.5) * params.temperatureScale;
                temp[j * n + i] = (float) clamp(t, 0.0, 1.0);

                double base = 0.5 + 0.5 * humidity.fbm(wx / 3200.0, wz / 3200.0, 4, 2.0, 0.5);
                double det = 0.5 + 0.5 * humidity.fbm((wx + 5000) / 760.0, (wz - 5000) / 760.0, 3, 2.1, 0.5);
                double oro = gridOrographic(height, n, cell, i, j);
                double r = base * 0.52 + det * 0.2 + 0.32 * oro + 0.2 * (1.0 - cont);
                r -= 0.4 * cont * cont * params.continentalDryness;
                r = 0.5 + (r - 0.5) * params.rainfallScale;
                rain[j * n + i] = (float) clamp(r, 0.0, 1.0);
            }
        }
        return new PreviewMaps(n, height, temp, rain);
    }

    private static double gridContinentality(float[] height, int n, int i, int j) {
        int rad = Math.max(1, n / 16);
        double nearOcean = rad;
        for (int dj = -rad; dj <= rad; dj += Math.max(1, rad / 3)) {
            for (int di = -rad; di <= rad; di += Math.max(1, rad / 3)) {
                int ii = i + di, jj = j + dj;
                if (ii < 0 || jj < 0 || ii >= n || jj >= n) continue;
                if (height[jj * n + ii] < SEA_Y) {
                    double d = Math.sqrt(di * di + dj * dj);
                    if (d < nearOcean) nearOcean = d;
                }
            }
        }
        return clamp(nearOcean / rad, 0.0, 1.0);
    }

    private static double gridOrographic(float[] height, int n, double cell, int i, int j) {
        double wx = 0.86, wz = 0.51;
        int steps = 5;
        double stepCells = Math.max(1.0, 130.0 / cell);
        double here = sampleGrid(height, n, i, j);
        double prev = sampleGrid(height, n, (int) (i - wx * stepCells * steps), (int) (j - wz * stepCells * steps));
        double moisture = 0.0, shadow = 0.0;
        for (int s = steps - 1; s >= 0; s--) {
            double h = sampleGrid(height, n, (int) (i - wx * stepCells * s), (int) (j - wz * stepCells * s));
            double climb = (h - prev) / 120.0;
            if (climb > 0) { moisture += climb; shadow += climb; }
            else { shadow += climb * 0.6; }
            prev = h;
        }
        double lift = clamp((here - prev) / 110.0, -1.0, 1.0);
        return clamp(0.5 + 0.5 * lift + 0.25 * moisture - 0.42 * Math.max(0, shadow - lift * 1.4), -0.5, 1.0);
    }

    private static double sampleGrid(float[] g, int n, int i, int j) {
        if (i < 0) i = 0; else if (i >= n) i = n - 1;
        if (j < 0) j = 0; else if (j >= n) j = n - 1;
        return g[j * n + i];
    }

    private float[] mapRawToWorldY(float[] r, int n) {
        int len = r.length;
        float[] sorted = r.clone();
        java.util.Arrays.sort(sorted);
        int seaRank = (int) (params.seaFraction * (len - 1));
        int landCount = Math.max(1, (len - 1) - seaRank);
        float[] y = new float[len];
        for (int idx = 0; idx < len; idx++) {
            int rank = lowerBound(sorted, r[idx]);
            if (rank <= seaRank) {
                double t = 1.0 - (double) rank / Math.max(1, seaRank);
                t = Math.pow(t, 1.20);
                y[idx] = (float) (SEA_Y - t * (SEA_Y - FLOOR_Y));
            } else {
                double pr = (double) (rank - seaRank) / landCount;
                double t = Math.pow(pr, params.landCurve);
                y[idx] = (float) (SHORE_Y + t * (params.mountainTopY - SHORE_Y));
            }
        }
        return y;
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
        int streamSteps = Math.max(1, (int) Math.round(STREAM_STEPS * params.erosionIntensity));
        int thermalIters = Math.max(1, (int) Math.round(THERMAL_ITERS * params.erosionIntensity));
        StreamPower.evolve(raw, N, CELL, (float) seaThresholdRaw(raw), uplift, streamSteps);

        Thermal.erode(raw, N, thermalIters, TALUS_LOW, TALUS_HIGH, TALUS_HI_REF);

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

        if (params.sedimentStrength > 0) {
            GenStatus.stage("Depositing sediment");
            int sedimentPasses = Math.max(1, (int) Math.round(SEDIMENT_PASSES * params.erosionIntensity * params.sedimentStrength));
            Sediment.apply(elev, N, CELL, SEA_Y, sedimentPasses);
        }

        if (params.glacialStrength > 0) {
            GenStatus.stage("Grinding glaciers");
            int glacialIters = Math.max(1, (int) Math.round(GLACIAL_ITERS * params.glacialStrength));
            glacial.apply(elev, N, CELL, HALF, glacialIters, params.erosionIntensity * params.glacialStrength);
            Thermal.erode(elev, N, 8, MAX_STEP_BLOCKS, MAX_STEP_BLOCKS, 0f, reposeMul);
        }

        if (params.coastalStrength > 0) {
            GenStatus.stage("Shaping coastlines");
            int coastalPasses = Math.max(1, (int) Math.round(COASTAL_PASSES * params.coastalStrength));
            Coastal.apply(elev, N, CELL, SEA_Y, coastalPasses);
        }

        smoothGridResidual(elev, N, 8);
        raw = null;

        GenStatus.stage("Refining terrain");
        elevFine = upsampleElev();
        smoothGridResidual(elevFine, NH, 2);

        GenStatus.stage("Computing rivers and lakes");
        int riverAccum = params.riverDensity <= 0.0 ? Integer.MAX_VALUE
                : Math.max(64, (int) Math.round(RIVER_ACCUM / params.riverDensity));
        hydro = Hydrology.build(elevFine, NH, SEA_Y, riverAccum, CELLH, HALF);

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
        return s[(int) (params.seaFraction * (s.length - 1))];
    }

    private static final double UPLIFT_RATE = 0.0025;

    public Hydrology.Sample sampleRiver(double wx, double wz, double terrainY, Hydrology.Sample out) {
        return hydro.sample(wx, wz, terrainY, CELLH, HALF, out);
    }

    public int flowAccumAt(double wx, double wz) {
        int i = (int) ((wx + HALF) / CELLH);
        int j = (int) ((wz + HALF) / CELLH);
        if (i < 0) i = 0; else if (i >= NH) i = NH - 1;
        if (j < 0) j = 0; else if (j >= NH) j = NH - 1;
        return hydro.accumAt(j * NH + i);
    }

    public Climate climate() { return climate; }

    public Soil soil() { return soil; }

    public Flora flora() { return flora; }

    public double erodibilityAt(double wx, double wz) {
        return erodibility.sample(wx, wz, surfaceY(wx, wz));
    }

    public String boundaryTypeAt(double wx, double wz) {
        return boundaryType(wx, wz).name();
    }

    public double boundaryDistAt(double wx, double wz) {
        PlateField.Contact c = new PlateField.Contact();
        plates.evaluate(warpX(wx, wz), warpZ(wx, wz), c);
        return Math.abs(c.boundaryDist);
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

        double cs = params.continentScale;
        double landish = clamp((base + boundary + 0.35) * 1.6, 0.0, 1.0);
        double continental = clamp((base + 0.4) * 1.4, 0.0, 1.0);
        double regional = relief.fbm(wx / (2600.0 * cs), wz / (2600.0 * cs), 5, 2.0, 0.5);
        double hills    = relief.fbm((wx + 1234) / 900.0, (wz - 4321) / 900.0, 5, 2.05, 0.5);
        double rolling  = relief.fbm((wx - 5678) / 420.0, (wz + 8765) / 420.0, 5, 2.1, 0.5);
        double detail   = relief.fbm(wx / 220.0, wz / 220.0, 4, 2.2, 0.5);
        double ridge    = relief.ridged(wx / 1400.0, wz / 1400.0, 4, 2.0, 0.55) - 0.5;

        double lowland = clamp(1.0 - (continental - 0.18) / 0.55, 0.0, 1.0) * landish;

        double relief2 = regional * 0.62
                       + hills * 0.28 * (0.4 + 0.6 * continental)
                       + rolling * 0.22 * lowland
                       + detail * 0.06
                       + ridge * 0.34 * landish;

        relief2 *= (0.30 + 0.70 * landish);

        return base + boundary + relief2;
    }

    private void mapToWorldY(float[] r) {
        int n = r.length;

        float[] sorted = r.clone();
        java.util.Arrays.sort(sorted);
        seaThreshold = sorted[(int) (params.seaFraction * (n - 1))];

        int seaRank = (int) (params.seaFraction * (n - 1));
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
                double t = Math.pow(p, params.landCurve);
                y = SHORE_Y + t * (params.mountainTopY - SHORE_Y);
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

    private static final double DETAIL_SCALE = 38.0;
    private static final double DETAIL_FINE_SCALE = 14.0;
    private static final double RIDGE_SCALE = 120.0;
    private static final double LAND_BASE_AMP = 0.7;
    private static final double SLOPE_AMP = 13.0;
    private static final double RIDGE_AMP = 11.0;
    private static final double RIDGE_START = 140.0;
    private static final double RIDGE_FULL = 420.0;

    public double surfaceY(double wx, double wz) {
        double base = sampleGrid(elevFine, NH, CELLH, wx, wz);
        if (base < SEA_Y - 2) return base;
        return base + detailY(wx, wz, base);
    }

    private double detailY(double wx, double wz, double base) {
        double gslope = gridSlope(wx, wz);
        double above = base - SEA_Y;

        double shore = clamp((above + 2.0) / 8.0, 0.0, 1.0);
        double slopeGain = clamp(gslope / 0.55, 0.0, 1.0);

        double rough = relief.fbm(wx / DETAIL_SCALE, wz / DETAIL_SCALE, 4, 2.0, 0.5);
        double fine = detail.fbm(wx / DETAIL_FINE_SCALE, wz / DETAIL_FINE_SCALE, 3, 2.1, 0.5);
        double bumps = rough * 0.7 + fine * 0.3;

        double rug = params.ruggedness;
        double amp = (LAND_BASE_AMP + SLOPE_AMP * slopeGain) * rug;
        double d = bumps * amp * shore;

        double ridgeBlend = clamp((above - RIDGE_START) / (RIDGE_FULL - RIDGE_START), 0.0, 1.0);
        if (ridgeBlend > 0) {
            double rn = ridge.fbm(wx / RIDGE_SCALE, wz / RIDGE_SCALE, 5, 2.0, 0.5);
            double ridged = 1.0 - Math.abs(rn);
            ridged = ridged * ridged;
            d += (ridged - 0.5) * RIDGE_AMP * rug * ridgeBlend * (0.4 + 0.6 * slopeGain);
        }
        return d;
    }

    private double gridSlope(double wx, double wz) {
        double r = CELLH;
        double dx = (sampleGrid(elevFine, NH, CELLH, wx + r, wz) - sampleGrid(elevFine, NH, CELLH, wx - r, wz)) / (2 * r);
        double dz = (sampleGrid(elevFine, NH, CELLH, wx, wz + r) - sampleGrid(elevFine, NH, CELLH, wx, wz - r)) / (2 * r);
        return Math.hypot(dx, dz);
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
