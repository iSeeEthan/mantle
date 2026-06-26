import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.Hydrology;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

public final class ProbeXY {
    public static void main(String[] args) {
        long seed = Long.parseLong(args[0]);
        int wx = Integer.parseInt(args[1]), wz = Integer.parseInt(args[2]);
        int rad = args.length > 3 ? Integer.parseInt(args[3]) : 3;
        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);
        Hydrology h = sim.hydrology();
        int n = h.gridSize();
        double cell = (double) PlateSim.WORLD / n, half = PlateSim.HALF;
        int ci = (int) Math.floor((wx + half) / cell - 0.5);
        int cj = (int) Math.floor((wz + half) / cell - 0.5);
        int[] accum = h.accum(); float[] wY = h.waterY(); float[] filled = h.filled();
        System.out.printf("center cell (%d,%d)  cell=%.2f blocks%n", ci, cj, cell);
        for (int dj = -rad; dj <= rad; dj++) {
            StringBuilder sb = new StringBuilder();
            for (int di = -rad; di <= rad; di++) {
                int ii = ci + di, jj = cj + dj;
                if (ii < 0 || jj < 0 || ii >= n || jj >= n) { sb.append("    ----    "); continue; }
                int idx = jj * n + ii;
                String tag = h.isLakeCell(idx) ? "L" : (!Float.isNaN(wY[idx]) ? "R" : ".");
                sb.append(String.format(" %s a=%-6d wY=%-3s ", tag, accum[idx],
                        Float.isNaN(wY[idx]) ? "-" : String.valueOf((int) wY[idx])));
            }
            System.out.println(sb);
        }
        MantleWorld.Column col = new MantleWorld.Column();
        world.column(wx, wz, col);
        System.out.printf("%nMantleWorld.column(%d,%d): stoneTop=%d waterTop=%d hasWater=%b%n",
                wx, wz, col.stoneTop, col.waterTop, col.hasWater);
    }
}
