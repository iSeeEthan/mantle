package com.iseeethan.mantle.world;

import com.iseeethan.mantle.world.gen.tectonics.PlateSim;
import com.iseeethan.mantle.world.gen.tectonics.Strata;

import java.util.concurrent.atomic.AtomicReference;

public final class MantleWorld {
    public static final int WORLD = PlateSim.WORLD;
    public static final int HALF = PlateSim.HALF;
    public static final int SEA_Y = PlateSim.SEA_Y;
    public static final int MIN_Y = PlateSim.FLOOR_Y;
    public static final int MAX_Y = PlateSim.PEAK_Y;

    public static final int HEIGHT = ((MAX_Y - MIN_Y + 15) / 16) * 16;

    public static final boolean DEBUG_NO_DETAIL = Boolean.getBoolean("mantle.nodetail");

    private static final AtomicReference<MantleWorld> CURRENT = new AtomicReference<>();

    private final PlateSim sim;
    private final Strata strata;

    private MantleWorld(long seed) {
        this.sim = new PlateSim(seed);
        this.strata = new Strata(seed, SEA_Y);
    }

    public Strata strata() { return strata; }

    public Strata.Rock rockAt(int wx, int wz, int y) {
        return strata.typeAt(wx, wz, y, solidTopY(wx, wz));
    }

    public static MantleWorld forSeed(long seed) {
        MantleWorld cur = CURRENT.get();
        if (cur != null && cur.sim.seed() == seed) {
            return cur;
        }
        MantleWorld built = new MantleWorld(seed);
        CURRENT.set(built);
        return built;
    }

    public static MantleWorld get() {
        MantleWorld cur = CURRENT.get();
        if (cur == null) {
            cur = forSeed(0L);
        }
        return cur;
    }

    public int solidTopY(int wx, int wz) {
        int top = (int) Math.round(sim.surfaceY(wx, wz));
        if (top > MAX_Y - 3) top = MAX_Y - 3;
        return top;
    }

    public static final class Column {

        public int stoneTop;

        public int waterTop;

        public boolean hasWater;
    }

    private final ThreadLocal<com.iseeethan.mantle.world.gen.tectonics.Hydrology.Sample> sampleTL =
            ThreadLocal.withInitial(com.iseeethan.mantle.world.gen.tectonics.Hydrology.Sample::new);

    public Column column(int wx, int wz, Column out) {
        int top = solidTopY(wx, wz);
        out.stoneTop = top;
        out.waterTop = top;
        out.hasWater = false;

        if (top < SEA_Y) {
            out.waterTop = SEA_Y;
            out.hasWater = true;
        }

        var s = sim.sampleRiver(wx, wz, top, sampleTL.get());
        if (s.bankOnly) {

            int shore = (int) Math.round(s.bedY);
            if (shore < out.stoneTop) {
                out.stoneTop = shore;
                if (!out.hasWater) out.waterTop = out.stoneTop;
            }

            int rim = (int) Math.floor(s.bankMinTop);
            if (rim > out.stoneTop) {
                out.stoneTop = rim;
                if (!out.hasWater) out.waterTop = out.stoneTop;
            }
        } else if (s.water) {
            if (s.isLake) {
                int surf = (int) Math.round(s.waterY);
                int bed = (int) Math.floor(s.bedY);
                if (bed >= surf) bed = surf - 1;
                if (bed < out.stoneTop) out.stoneTop = bed;
                if (surf > out.stoneTop) { out.waterTop = surf; out.hasWater = true; }
            } else {

                int surf = (int) Math.floor(s.waterY);
                int bed = (int) Math.floor(s.bedY);
                if (bed >= surf) bed = surf - 1;

                if (bed < out.stoneTop) out.stoneTop = bed;

                if (surf > out.stoneTop) { out.waterTop = surf; out.hasWater = true; }

            }
        }
        if (out.waterTop > MAX_Y - 3) out.waterTop = MAX_Y - 3;
        return out;
    }

    public boolean isOcean(int wx, int wz) {
        return sim.isOcean(wx, wz);
    }

    public boolean inWorld(int wx, int wz) {
        return wx >= -HALF && wx < HALF && wz >= -HALF && wz < HALF;
    }
}
