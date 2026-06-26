import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.Hydrology;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

public final class LakeConnect {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);
        Hydrology h = sim.hydrology();
        int n = h.gridSize();
        int[] accum = h.accum(), down = h.downstream();
        float[] wY = h.waterY();
        double cell = (double) PlateSim.WORLD / n, half = PlateSim.HALF;
        int RA = 400;

        int[] inflow = new int[n * n];
        for (int i = 0; i < n * n; i++)
            if (accum[i] >= RA && !Float.isNaN(wY[i]) && down[i] >= 0) inflow[down[i]]++;

        int lake = -1, inCell = -1;
        for (int i = 0; i < n * n && lake < 0; i++) {
            if (!h.isLakeCell(i)) continue;
            if (down[i] < 0 || accum[down[i]] < RA) continue;

            int ci = i % n, cj = i / n;
            for (int d = 0; d < 8 && inCell < 0; d++) {
                int[] DX = {0,0,-1,1,-1,1,-1,1}, DZ = {-1,1,0,0,-1,-1,1,1};
                int ii = ci + DX[d], jj = cj + DZ[d];
                if (ii < 0 || jj < 0 || ii >= n || jj >= n) continue;
                int ni = jj * n + ii;
                if (!h.isLakeCell(ni) && accum[ni] >= RA && down[ni] == i) { inCell = ni; lake = i; }
            }
        }
        if (lake < 0) { System.out.println("no in+out lake found"); return; }

        System.out.println("Lake cell gridWaterY=" + wY[lake]);

        int ci = inCell % n, cj = inCell / n;
        double sx = (ci + 0.5) * cell - half, sz = (cj + 0.5) * cell - half;
        double lx = (lake % n + 0.5) * cell - half, lz = (lake / n + 0.5) * cell - half;
        MantleWorld.Column col = new MantleWorld.Column();
        System.out.println("\nInflow walk (river cell -> lake center):  dist  stoneTop/waterTop  hasWater");
        int steps = 20;
        for (int s = 0; s <= steps; s++) {
            double t = (double) s / steps;
            int wx = (int) Math.round(sx + (lx - sx) * t);
            int wz = (int) Math.round(sz + (lz - sz) * t);
            world.column(wx, wz, col);
            System.out.printf("  %4.0f   %4d / %4d   %s%n", t * Math.hypot(lx - sx, lz - sz),
                    col.stoneTop, col.waterTop, col.hasWater ? "WATER" : "dry");
        }
    }
}
