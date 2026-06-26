import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.Hydrology;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

public final class FlowCheck {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);
        Hydrology h = sim.hydrology();
        int n = h.gridSize();
        int[] accum = h.accum();
        float[] wY = h.waterY();
        double cell = (double) PlateSim.WORLD / n, half = PlateSim.HALF;

        int best = -1; long bestA = 0;
        for (int i = 0; i < n * n; i++)
            if (!Float.isNaN(wY[i]) && !h.isLakeCell(i) && wY[i] > PlateSim.SEA_Y + 12 && accum[i] > bestA) { bestA = accum[i]; best = i; }

        MantleWorld.Column col = new MantleWorld.Column();
        int cur = best, steps = 0, prevW = Integer.MIN_VALUE, bigSteps = 0;
        System.out.println("cell  world(x,z)   stoneTop/waterTop  step  width");
        while (cur >= 0 && steps < 30) {
            if (Float.isNaN(wY[cur])) break;
            int ci = cur % n, cj = cur / n;
            int wx = (int) Math.round((ci + 0.5) * cell - half);
            int wz = (int) Math.round((cj + 0.5) * cell - half);
            world.column(wx, wz, col);
            int wt = col.hasWater ? col.waterTop : Integer.MIN_VALUE;

            int dsn = h.downstreamAt(cur);
            double fdx = dsn >= 0 ? (dsn % n) - ci : 1, fdz = dsn >= 0 ? (dsn / n) - cj : 0;
            double fl = Math.hypot(fdx, fdz); if (fl < 1e-6) fl = 1;
            double pxu = -fdz / fl, pzu = fdx / fl;
            int width = 0;
            for (int s = -8; s <= 8; s++) {
                MantleWorld.Column c2 = new MantleWorld.Column();
                world.column(wx + (int) Math.round(pxu * s), wz + (int) Math.round(pzu * s), c2);
                if (c2.hasWater && c2.waterTop > c2.stoneTop && c2.waterTop > PlateSim.SEA_Y) width++;
            }
            int step = prevW == Integer.MIN_VALUE ? 0 : prevW - wt;
            if (Math.abs(step) > 1) bigSteps++;
            System.out.printf("%5d (%6d,%6d)  %4d / %4d   %+3d   %d%n", steps, wx, wz, col.stoneTop, wt, step, width);
            prevW = wt; cur = h.downstreamAt(cur); steps++;
        }
        System.out.println("steps with >1 block surface drop: " + bigSteps);
    }
}
