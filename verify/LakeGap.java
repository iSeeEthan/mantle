import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.Hydrology;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

public final class LakeGap {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);
        Hydrology h = sim.hydrology();
        int n = h.gridSize();
        int[] accum = h.accum(), down = h.downstream();
        float[] wY = h.waterY();
        double cell = (double) PlateSim.WORLD / n, half = PlateSim.HALF;
        int RA = 800;

        int worst = -1, worstGap = -1, worstInflow = -1;
        for (int i = 0; i < n * n; i++) {
            if (h.isLakeCell(i)) continue;
            if (Float.isNaN(wY[i]) || accum[i] < RA) continue;
            int ds = down[i];
            if (ds < 0 || !h.isLakeCell(ds)) continue;
            int ci = i % n, cj = i / n;
            double sx = (ci + 0.5) * cell - half, sz = (cj + 0.5) * cell - half;
            double lx = (ds % n + 0.5) * cell - half, lz = (ds / n + 0.5) * cell - half;
            MantleWorld.Column col = new MantleWorld.Column();
            int gap = 0, run = 0;
            int steps = 60;
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                int wx = (int) Math.round(sx + (lx - sx) * t);
                int wz = (int) Math.round(sz + (lz - sz) * t);
                world.column(wx, wz, col);
                if (col.hasWater && col.waterTop > col.stoneTop) { run = 0; }
                else { run++; if (run > gap) gap = run; }
            }
            if (gap > worstGap) { worstGap = gap; worst = i; worstInflow = ds; }
        }
        if (worst < 0) { System.out.println("no river->lake inflow found at RA=" + RA); return; }
        int ci = worst % n, cj = worst / n;
        double sx = (ci + 0.5) * cell - half, sz = (cj + 0.5) * cell - half;
        double lx = (worstInflow % n + 0.5) * cell - half, lz = (worstInflow / n + 0.5) * cell - half;
        System.out.println("Worst river->lake gap (blocks dry along line) = " + worstGap
                + "   accum=" + accum[worst] + "   dist=" + (int) Math.hypot(lx - sx, lz - sz));
        MantleWorld.Column col = new MantleWorld.Column();
        int steps = 40;
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            int wx = (int) Math.round(sx + (lx - sx) * t);
            int wz = (int) Math.round(sz + (lz - sz) * t);
            world.column(wx, wz, col);
            System.out.printf("  d=%4.0f  %4d/%4d %s%n", t * Math.hypot(lx - sx, lz - sz),
                    col.stoneTop, col.waterTop, (col.hasWater && col.waterTop > col.stoneTop) ? "WATER" : "dry");
        }
    }
}
