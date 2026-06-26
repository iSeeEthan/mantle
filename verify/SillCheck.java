import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

public final class SillCheck {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        int cx = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        int cz = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int span = args.length > 3 ? Integer.parseInt(args[3]) : 400;

        MantleWorld world = MantleWorld.forSeed(seed);
        int ox = cx - span / 2, oz = cz - span / 2;

        int[] waterTop = new int[span * span];
        int[] stoneTop = new int[span * span];
        boolean[] water = new boolean[span * span];
        MantleWorld.Column col = new MantleWorld.Column();
        for (int j = 0; j < span; j++)
            for (int i = 0; i < span; i++) {
                int wx = ox + i, wz = oz + j;
                if (!world.inWorld(wx, wz)) { continue; }
                world.column(wx, wz, col);
                int idx = j * span + i;
                stoneTop[idx] = col.stoneTop;
                water[idx] = col.hasWater && col.waterTop > col.stoneTop;
                waterTop[idx] = col.waterTop;
            }

        int sills = 0, examples = 0;
        for (int j = 1; j < span - 1; j++)
            for (int i = 1; i < span - 1; i++) {
                int idx = j * span + i;
                if (water[idx]) continue;

                int maxAdjW = Integer.MIN_VALUE;
                boolean wW = water[idx - 1], wE = water[idx + 1], wN = water[idx - span], wS = water[idx + span];
                if (wW) maxAdjW = Math.max(maxAdjW, waterTop[idx - 1]);
                if (wE) maxAdjW = Math.max(maxAdjW, waterTop[idx + 1]);
                if (wN) maxAdjW = Math.max(maxAdjW, waterTop[idx - span]);
                if (wS) maxAdjW = Math.max(maxAdjW, waterTop[idx + span]);
                if (maxAdjW == Integer.MIN_VALUE) continue;

                boolean surrounded = (wW && wE) || (wN && wS);
                if (surrounded && stoneTop[idx] > maxAdjW) {
                    sills++;
                    if (examples < 8) {
                        System.out.printf("  sill at (%d,%d) stoneTop=%d adjWater=%d%n",
                                ox + i, oz + j, stoneTop[idx], maxAdjW);
                        examples++;
                    }
                }
            }
        System.out.println("SILLS (stone poking through adjacent water): " + sills + "  in " + span + "x" + span);
    }
}
