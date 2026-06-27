package com.iseeethan.mantle.world;

import com.iseeethan.mantle.world.gen.tectonics.Hydrology;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;
import com.iseeethan.mantle.world.gen.tectonics.Strata;

public final class Locator {

    public enum Target {
        RIVER, LAKE, RIVER_MOUTH, COAST, OCEAN, PEAK, DEEP_OCEAN, PLAINS, CLIFF,
        CAVE, SINKHOLE, BASEMENT, IGNEOUS, METAMORPHIC, SEDIMENTARY, COASTAL_SPAWN
    }

    public static final class Found {
        public final int x, y, z;
        public Found(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    }

    private final MantleWorld world;
    private final PlateSim sim;
    private final Hydrology hydro;
    private final int n;
    private final double cell;
    private final int half;

    public Locator(MantleWorld world) {
        this.world = world;
        this.sim = world.sim();
        this.hydro = sim.hydrology();
        this.n = hydro.gridSize();
        this.cell = (double) PlateSim.WORLD / n;
        this.half = PlateSim.HALF;
    }

    public Found locate(Target target, int fromX, int fromZ) {
        switch (target) {
            case RIVER:        return scanGrid(fromX, fromZ, this::isRiverCell);
            case LAKE:         return scanGrid(fromX, fromZ, this::isLakeCell);
            case RIVER_MOUTH:  return scanGrid(fromX, fromZ, this::isMouthCell);
            case PEAK:         return findExtreme(fromX, fromZ, true, false);
            case DEEP_OCEAN:   return findExtreme(fromX, fromZ, false, true);
            case COAST:        return scanSurface(fromX, fromZ, this::isCoast);
            case OCEAN:        return scanSurface(fromX, fromZ, (x, z) -> world.isOcean(x, z));
            case PLAINS:       return scanSurface(fromX, fromZ, this::isPlains);
            case CLIFF:        return scanSurface(fromX, fromZ, this::isCliff);
            case CAVE:         return findCave(fromX, fromZ);
            case SINKHOLE:     return scanSurface(fromX, fromZ, this::isSinkhole);
            case BASEMENT:     return scanSurface(fromX, fromZ, (x, z) -> exposedRock(x, z) == Strata.Rock.BASEMENT);
            case IGNEOUS:      return scanSurface(fromX, fromZ, (x, z) -> exposedRock(x, z) == Strata.Rock.IGNEOUS);
            case METAMORPHIC:  return scanSurface(fromX, fromZ, (x, z) -> exposedRock(x, z) == Strata.Rock.METAMORPHIC);
            case SEDIMENTARY:  return scanSurface(fromX, fromZ, (x, z) -> exposedRock(x, z) == Strata.Rock.SEDIMENTARY);
            case COASTAL_SPAWN:return findCoastalSpawn();
            default:           return null;
        }
    }

    private interface CellTest { boolean test(int idx); }
    private interface SurfaceTest { boolean test(int wx, int wz); }

    private Found scanGrid(int fromX, int fromZ, CellTest test) {
        int ci = clampCell((fromX + half) / cell);
        int cj = clampCell((fromZ + half) / cell);
        int bestI = -1, bestJ = -1;
        long bestD = Long.MAX_VALUE;
        for (int r = 0; r < n; r++) {
            boolean any = false;
            for (int dj = -r; dj <= r; dj++) {
                int jj = cj + dj;
                if (jj < 0 || jj >= n) continue;
                int adj = Math.abs(dj);
                for (int di = -r; di <= r; di++) {
                    if (Math.abs(di) != r && adj != r) continue;
                    int ii = ci + di;
                    if (ii < 0 || ii >= n) continue;
                    any = true;
                    if (!test.test(jj * n + ii)) continue;
                    long d = (long) di * di + (long) dj * dj;
                    if (d < bestD) { bestD = d; bestI = ii; bestJ = jj; }
                }
            }
            if (bestI >= 0 && r * r > bestD) break;
            if (!any && bestI >= 0) break;
            if (!any && r > n) break;
        }
        if (bestI < 0) return null;
        return cellToFound(bestI, bestJ);
    }

    private Found scanSurface(int fromX, int fromZ, SurfaceTest test) {
        final int step = 16;
        for (int r = 0; r <= half; r += step) {
            Found f = ringSearch(fromX, fromZ, r, step, test);
            if (f != null) return f;
        }
        return null;
    }

    private Found ringSearch(int fromX, int fromZ, int r, int step, SurfaceTest test) {
        Found best = null;
        long bestD = Long.MAX_VALUE;
        if (r == 0) {
            if (world.inWorld(fromX, fromZ) && test.test(fromX, fromZ)) {
                return new Found(fromX, surfaceTop(fromX, fromZ), fromZ);
            }
            return null;
        }
        for (int a = -r; a <= r; a += step) {
            int[][] pts = {
                    { fromX + a, fromZ - r }, { fromX + a, fromZ + r },
                    { fromX - r, fromZ + a }, { fromX + r, fromZ + a }
            };
            for (int[] p : pts) {
                int x = p[0], z = p[1];
                if (!world.inWorld(x, z)) continue;
                if (!test.test(x, z)) continue;
                long d = (long) (x - fromX) * (x - fromX) + (long) (z - fromZ) * (z - fromZ);
                if (d < bestD) { bestD = d; best = new Found(x, surfaceTop(x, z), z); }
            }
        }
        return best;
    }

    private Found findExtreme(int fromX, int fromZ, boolean highest, boolean useSeabed) {
        int radius = 320;
        int ci = clampCell((fromX + half) / cell);
        int cj = clampCell((fromZ + half) / cell);
        int bestI = -1, bestJ = -1;
        double bestY = highest ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (int dj = -radius; dj <= radius; dj++) {
            int jj = cj + dj;
            if (jj < 0 || jj >= n) continue;
            for (int di = -radius; di <= radius; di++) {
                int ii = ci + di;
                if (ii < 0 || ii >= n) continue;
                double wx = (ii + 0.5) * cell - half;
                double wz = (jj + 0.5) * cell - half;
                double y = sim.surfaceY(wx, wz);
                if (highest ? y > bestY : y < bestY) { bestY = y; bestI = ii; bestJ = jj; }
            }
        }
        if (bestI < 0) return null;
        int wx = (int) Math.round((bestI + 0.5) * cell - half);
        int wz = (int) Math.round((bestJ + 0.5) * cell - half);
        if (useSeabed) {
            return new Found(wx, world.solidTopY(wx, wz), wz);
        }
        return cellToFound(bestI, bestJ);
    }

    private Found findCave(int fromX, int fromZ) {
        final int step = 16;
        for (int r = 0; r <= half; r += step) {
            for (int a = -r; a <= r; a += step) {
                int[][] pts = {
                        { fromX + a, fromZ - r }, { fromX + a, fromZ + r },
                        { fromX - r, fromZ + a }, { fromX + r, fromZ + a }
                };
                for (int[] p : pts) {
                    int x = p[0], z = p[1];
                    if (!world.inWorld(x, z)) continue;
                    int top = world.solidTopY(x, z);
                    for (int y = top - 8; y > MantleWorld.MIN_Y + 8; y--) {
                        if (world.caveAt(x, y, z, top)) {
                            return new Found(x, y, z);
                        }
                    }
                }
                if (r == 0) break;
            }
        }
        return null;
    }

    public Found findCoastalSpawn() {
        final int step = 64;
        Found best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (int x = -half + step; x < half; x += step) {
            for (int z = -half + step; z < half; z += step) {
                if (world.isOcean(x, z)) continue;
                int top = world.solidTopY(x, z);
                if (top < MantleWorld.SEA_Y + 1 || top > MantleWorld.SEA_Y + 8) continue;
                if (!oceanWithin(x, z, 160)) continue;
                double slope = sim.macroSlope(x, z);
                double score = (top - MantleWorld.SEA_Y) + slope * 40.0;
                if (score < bestScore) { bestScore = score; best = new Found(x, top, z); }
            }
        }
        return best;
    }

    private boolean oceanWithin(int x, int z, int radius) {
        for (int a = 0; a <= radius; a += 32) {
            if (world.isOcean(x + a, z) || world.isOcean(x - a, z)
                    || world.isOcean(x, z + a) || world.isOcean(x, z - a)) return true;
        }
        return false;
    }

    private boolean isRiverCell(int idx) {
        return !Float.isNaN(hydro.waterYAt(idx)) && !hydro.isLakeCell(idx) && hydro.filledAt(idx) > MantleWorld.SEA_Y;
    }

    private boolean isLakeCell(int idx) {
        return hydro.isLakeCell(idx) && !Float.isNaN(hydro.waterYAt(idx));
    }

    private boolean isMouthCell(int idx) {
        if (Float.isNaN(hydro.waterYAt(idx)) || hydro.isLakeCell(idx)) return false;
        int ds = hydro.downstreamAt(idx);
        if (ds < 0) return false;
        return hydro.filledAt(idx) > MantleWorld.SEA_Y && hydro.filledAt(ds) <= MantleWorld.SEA_Y + 0.5f;
    }

    private boolean isCoast(int wx, int wz) {
        int top = world.solidTopY(wx, wz);
        if (top < MantleWorld.SEA_Y || top > MantleWorld.SEA_Y + 3) return false;
        return oceanWithin(wx, wz, 48);
    }

    private boolean isPlains(int wx, int wz) {
        int top = world.solidTopY(wx, wz);
        if (top < MantleWorld.SEA_Y + 2 || top > MantleWorld.SEA_Y + 30) return false;
        return sim.macroSlope(wx, wz) < 0.06;
    }

    private boolean isCliff(int wx, int wz) {
        if (world.solidTopY(wx, wz) < MantleWorld.SEA_Y + 8) return false;
        return sim.macroSlope(wx, wz) > 0.85;
    }

    private boolean isSinkhole(int wx, int wz) {
        if (exposedRock(wx, wz) != Strata.Rock.SEDIMENTARY) return false;
        int c = world.solidTopY(wx, wz);
        if (c <= MantleWorld.SEA_Y + 2) return false;
        int maxDrop = 0;
        for (int rad = 4; rad <= 8; rad += 4) {
            int ring = (world.solidTopY(wx + rad, wz) + world.solidTopY(wx - rad, wz)
                    + world.solidTopY(wx, wz + rad) + world.solidTopY(wx, wz - rad)) / 4;
            maxDrop = Math.max(maxDrop, ring - c);
        }
        return maxDrop >= 3;
    }

    private Strata.Rock exposedRock(int wx, int wz) {
        int top = world.solidTopY(wx, wz);
        if (top < MantleWorld.SEA_Y) return null;
        return world.rockAt(wx, wz, top - 1);
    }

    private int surfaceTop(int wx, int wz) {
        int top = world.solidTopY(wx, wz);
        return Math.max(top + 1, MantleWorld.SEA_Y + 1);
    }

    private Found cellToFound(int ii, int jj) {
        int wx = (int) Math.round((ii + 0.5) * cell - half);
        int wz = (int) Math.round((jj + 0.5) * cell - half);
        return new Found(wx, surfaceTop(wx, wz), wz);
    }

    private int clampCell(double v) {
        int c = (int) Math.floor(v);
        return c < 0 ? 0 : (c >= n ? n - 1 : c);
    }
}
