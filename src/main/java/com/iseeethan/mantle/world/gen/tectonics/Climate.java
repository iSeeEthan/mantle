package com.iseeethan.mantle.world.gen.tectonics;

import com.iseeethan.mantle.world.noise.GradientNoise;

public final class Climate {

    private static final double WIND_X = 0.86;
    private static final double WIND_Z = 0.51;
    private static final double UPWIND_STEP = 130.0;
    private static final int UPWIND_STEPS = 5;

    private static final double BASE_SCALE = 3200.0;
    private static final double DETAIL_SCALE = 760.0;

    private static final double POLE_Z = PlateSim.HALF;
    private static final double LATITUDE_WOBBLE = 900.0;
    private static final double LAPSE_SPAN = 620.0;
    private static final double LATITUDE_WEIGHT = 0.70;
    private static final double ELEV_WEIGHT = 0.44;

    private static final double CONT_FULL_DIST = 1500.0;
    private static final int CONT_GRID = 256;
    private static final double CONT_CELL = (double) (PlateSim.WORLD) / CONT_GRID;

    private volatile float[] contGrid;

    public static final class Sample {
        public double rainfall;
        public double temperature;
        public double continentality;
    }

    private final GradientNoise humidity;
    private final GradientNoise tempJitter;
    private final PlateSim sim;
    private final double seaY;

    private final PlateSim.Params params;

    Climate(long seed, PlateSim sim, double seaY, PlateSim.Params params) {
        this.humidity = new GradientNoise(seed ^ 0x68E31DA4FB1133A5L);
        this.tempJitter = new GradientNoise(seed ^ 0xD1B54A32D192ED03L);
        this.sim = sim;
        this.seaY = seaY;
        this.params = params;
    }

    public Sample sample(double wx, double wz, Sample out) {
        double cont = continentality(wx, wz);
        out.continentality = cont;
        out.rainfall = rainfall(wx, wz, cont);
        out.temperature = temperature(wx, wz, cont);
        return out;
    }

    public double rainfall(double wx, double wz) {
        return rainfall(wx, wz, continentality(wx, wz));
    }

    private double rainfall(double wx, double wz, double cont) {
        double base = 0.5 + 0.5 * humidity.fbm(wx / BASE_SCALE, wz / BASE_SCALE, 4, 2.0, 0.5);
        double detail = 0.5 + 0.5 * humidity.fbm((wx + 5000) / DETAIL_SCALE, (wz - 5000) / DETAIL_SCALE, 3, 2.1, 0.5);

        double orographic = orographicMoisture(wx, wz);

        double r = base * 0.52 + detail * 0.2 + 0.32 * orographic + 0.2 * (1.0 - cont);
        r -= 0.4 * cont * cont * params.continentalDryness;
        r = 0.5 + (r - 0.5) * params.rainfallScale;
        return clamp(r, 0.0, 1.0);
    }

    private double orographicMoisture(double wx, double wz) {
        double here = sim.surfaceY(wx, wz);
        double moisture = 0.0;
        double shadow = 0.0;
        double prev = sim.surfaceY(wx - WIND_X * UPWIND_STEP * UPWIND_STEPS,
                                   wz - WIND_Z * UPWIND_STEP * UPWIND_STEPS);
        for (int s = UPWIND_STEPS - 1; s >= 0; s--) {
            double sx = wx - WIND_X * UPWIND_STEP * s;
            double sz = wz - WIND_Z * UPWIND_STEP * s;
            double h = sim.surfaceY(sx, sz);
            double climb = (h - prev) / 120.0;
            if (climb > 0) {
                moisture += climb;
                shadow += climb;
            } else {
                shadow += climb * 0.6;
            }
            prev = h;
        }
        double lift = clamp((here - prev) / 110.0, -1.0, 1.0);
        return clamp(0.5 + 0.5 * lift + 0.25 * moisture - 0.42 * Math.max(0, shadow - lift * 1.4), -0.5, 1.0);
    }

    public double temperature(double wx, double wz) {
        return temperature(wx, wz, continentality(wx, wz));
    }

    private double temperature(double wx, double wz, double cont) {
        double wobble = tempJitter.fbm(wx / 2400.0, wz / 2400.0, 3, 2.0, 0.5) * LATITUDE_WOBBLE;
        double lat = clamp((Math.abs(wz + wobble)) / POLE_Z, 0.0, 1.0);
        double latTemp = 1.0 - clamp(lat * params.polarColdness, 0.0, 1.0);

        double above = sim.surfaceY(wx, wz) - seaY;
        double lapse = clamp(above / LAPSE_SPAN, 0.0, 1.0);

        double t = latTemp * LATITUDE_WEIGHT - lapse * ELEV_WEIGHT + 0.5 * (1.0 - LATITUDE_WEIGHT);

        double mid = 0.5;
        double swing = (t - mid) * (0.22 * cont);
        t += swing;

        t += 0.04 * tempJitter.noise2(wx / 520.0, wz / 520.0);
        t = 0.5 + (t - 0.5) * params.temperatureScale;
        return clamp(t, 0.0, 1.0);
    }

    public double continentality(double wx, double wz) {
        float[] g = contGrid;
        if (g == null) g = buildContGrid();
        double half = PlateSim.HALF;
        double fx = (wx + half) / CONT_CELL - 0.5;
        double fz = (wz + half) / CONT_CELL - 0.5;
        int ix = (int) Math.floor(fx), iz = (int) Math.floor(fz);
        double tx = fx - ix, tz = fz - iz;
        double a00 = contAt(g, ix, iz), a10 = contAt(g, ix + 1, iz);
        double a01 = contAt(g, ix, iz + 1), a11 = contAt(g, ix + 1, iz + 1);
        double top = a00 + (a10 - a00) * tx;
        double bot = a01 + (a11 - a01) * tx;
        return clamp(top + (bot - top) * tz, 0.0, 1.0);
    }

    private static double contAt(float[] g, int i, int j) {
        if (i < 0) i = 0; else if (i >= CONT_GRID) i = CONT_GRID - 1;
        if (j < 0) j = 0; else if (j >= CONT_GRID) j = CONT_GRID - 1;
        return g[j * CONT_GRID + i];
    }

    private synchronized float[] buildContGrid() {
        if (contGrid != null) return contGrid;
        double half = PlateSim.HALF;
        boolean[] ocean = new boolean[CONT_GRID * CONT_GRID];
        for (int j = 0; j < CONT_GRID; j++) {
            double wz = (j + 0.5) * CONT_CELL - half;
            for (int i = 0; i < CONT_GRID; i++) {
                double wx = (i + 0.5) * CONT_CELL - half;
                ocean[j * CONT_GRID + i] = sim.surfaceY(wx, wz) < seaY;
            }
        }
        float[] dist = new float[CONT_GRID * CONT_GRID];
        float far = CONT_GRID * 4f;
        for (int k = 0; k < dist.length; k++) dist[k] = ocean[k] ? 0f : far;
        for (int j = 0; j < CONT_GRID; j++)
            for (int i = 0; i < CONT_GRID; i++) {
                int k = j * CONT_GRID + i;
                if (i > 0) dist[k] = Math.min(dist[k], dist[k - 1] + 1);
                if (j > 0) dist[k] = Math.min(dist[k], dist[k - CONT_GRID] + 1);
                if (i > 0 && j > 0) dist[k] = Math.min(dist[k], dist[k - CONT_GRID - 1] + 1.41421f);
            }
        for (int j = CONT_GRID - 1; j >= 0; j--)
            for (int i = CONT_GRID - 1; i >= 0; i--) {
                int k = j * CONT_GRID + i;
                if (i < CONT_GRID - 1) dist[k] = Math.min(dist[k], dist[k + 1] + 1);
                if (j < CONT_GRID - 1) dist[k] = Math.min(dist[k], dist[k + CONT_GRID] + 1);
                if (i < CONT_GRID - 1 && j < CONT_GRID - 1) dist[k] = Math.min(dist[k], dist[k + CONT_GRID + 1] + 1.41421f);
            }
        for (int k = 0; k < dist.length; k++) {
            double blocks = dist[k] * CONT_CELL;
            dist[k] = (float) clamp(blocks / CONT_FULL_DIST, 0.0, 1.0);
        }
        contGrid = dist;
        return dist;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
