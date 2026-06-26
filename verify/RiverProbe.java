import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.Hydrology;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

public final class RiverProbe {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);
        Hydrology h = sim.hydrology();
        int n = h.gridSize();
        int[] accum = h.accum();
        float[] wYg = h.waterY();

        int best = -1; long bestA = 0;
        for (int i = 0; i < n * n; i++)
            if (!Float.isNaN(wYg[i]) && !h.isLakeCell(i) && wYg[i] > PlateSim.SEA_Y + 12 && accum[i] > bestA) {
                bestA = accum[i]; best = i;
            }
        double cell = (double) PlateSim.WORLD / n, half = PlateSim.HALF;
        int ci = best % n, cj = best / n;
        int wx = (int) Math.round((ci + 0.5) * cell - half);
        int wz = (int) Math.round((cj + 0.5) * cell - half);
        System.out.println("Biggest river cell accum=" + bestA + " at world (" + wx + "," + wz + ") gridWaterY=" + wYg[best]);

        int ds = h.downstreamAt(best);
        double fdx = (ds >= 0 ? (ds % n) - ci : 0), fdz = (ds >= 0 ? (ds / n) - cj : 0);
        double fl = Math.hypot(fdx, fdz); if (fl < 1e-6) { fdx = 1; fdz = 0; fl = 1; }

        double pxu = -fdz / fl, pzu = fdx / fl;

        MantleWorld.Column col = new MantleWorld.Column();
        System.out.println("\nCross-section (perpendicular to flow):  offset : stoneTop / waterTop");
        Integer flatW = null; boolean flat = true;
        for (int s = -30; s <= 30; s++) {
            int sx = wx + (int) Math.round(pxu * s);
            int sz = wz + (int) Math.round(pzu * s);
            world.column(sx, sz, col);
            if (col.hasWater && col.waterTop > col.stoneTop && col.waterTop > PlateSim.SEA_Y) {
                System.out.printf("  %+4d : %4d / %4d (river)%n", s, col.stoneTop, col.waterTop);
                if (flatW == null) flatW = col.waterTop;
                else if (!flatW.equals(col.waterTop)) flat = false;
            }
        }
        System.out.println("  -> river water surface across width: " + (flat ? "FLAT ✓ (Y=" + flatW + ")" : "NOT FLAT ✗"));

        System.out.println("\nDownstream walk (grid waterY, must be non-increasing):");
        int cur = best; float prev = Float.MAX_VALUE; boolean mono = true; int steps = 0;
        StringBuilder sb = new StringBuilder("  ");
        while (cur >= 0 && steps < 40) {
            float y = wYg[cur];
            if (Float.isNaN(y)) break;
            if (y > prev + 0.001f) mono = false;
            sb.append((int) y).append(steps == 0 ? "" : "").append(" -> ");
            prev = y; cur = h.downstreamAt(cur); steps++;
        }
        System.out.println(sb.append("[outlet]"));
        System.out.println("  -> downstream monotone non-increasing: " + (mono ? "YES ✓ (always downhill)" : "NO ✗"));
    }
}
