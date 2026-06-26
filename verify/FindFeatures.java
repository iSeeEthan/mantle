import com.iseeethan.mantle.world.gen.tectonics.Hydrology;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

public final class FindFeatures {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        PlateSim sim = new PlateSim(seed);
        Hydrology h = sim.hydrology();
        int n = h.gridSize();
        int[] accum = h.accum(), down = h.downstream();
        float[] wY = h.waterY();
        double cell = (double) PlateSim.WORLD / n, half = PlateSim.HALF;
        int RA = 400;

        int[] inflow = new int[n * n];
        for (int i = 0; i < n * n; i++) {
            if (accum[i] >= RA && wY[i] > PlateSim.SEA_Y && down[i] >= 0) inflow[down[i]]++;
        }

        int bestConf = -1; long bestA = 0;
        for (int i = 0; i < n * n; i++)
            if (inflow[i] >= 2 && accum[i] >= RA && wY[i] > PlateSim.SEA_Y + 20 && accum[i] > bestA) { bestA = accum[i]; bestConf = i; }
        print("CONFLUENCE", bestConf, n, cell, half, accum, wY);

        int bestLake = -1;
        for (int i = 0; i < n * n; i++) {
            if (!h.isLakeCell(i)) continue;
            boolean hasOut = down[i] >= 0 && accum[down[i]] >= RA;
            boolean hasIn = inflow[i] >= 1;
            if (hasOut && hasIn) { bestLake = i; break; }
        }
        print("LAKE(in+out)", bestLake, n, cell, half, accum, wY);

        int bestMouth = -1; long mA = 0;
        for (int i = 0; i < n * n; i++)
            if (accum[i] >= RA && wY[i] > PlateSim.SEA_Y && down[i] >= 0 && Float.isNaN(wY[down[i]]) && accum[i] > mA) { mA = accum[i]; bestMouth = i; }
        print("MOUTH", bestMouth, n, cell, half, accum, wY);

        int bestTrib = -1; long score = 0;
        for (int i = 0; i < n * n; i++) {
            if (h.isLakeCell(i) || accum[i] < RA || down[i] < 0) continue;
            int d = down[i];
            if (h.isLakeCell(d) || Float.isNaN(wY[d]) || wY[d] < PlateSim.SEA_Y + 15) continue;
            if (accum[i] >= RA && accum[i] < 3 * RA && accum[d] > 20 * RA) {
                long sc = accum[d];
                if (sc > score) { score = sc; bestTrib = i; }
            }
        }
        print("TRIBUTARY-JOIN", bestTrib, n, cell, half, accum, wY);
    }

    private static void print(String tag, int idx, int n, double cell, double half, int[] accum, float[] wY) {
        if (idx < 0) { System.out.println(tag + ": none found"); return; }
        int ci = idx % n, cj = idx / n;
        int wx = (int) Math.round((ci + 0.5) * cell - half);
        int wz = (int) Math.round((cj + 0.5) * cell - half);
        System.out.printf("%s: world (%d,%d) accum=%d waterY=%.1f%n", tag, wx, wz, accum[idx], wY[idx]);
    }
}
