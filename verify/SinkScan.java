import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.Strata;

public final class SinkScan {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        MantleWorld world = MantleWorld.forSeed(seed);

        int found = 0, sedLand = 0;
        int maxDropSeen = 0;
        for (int x = -19000; x < 19000; x += 8) {
            for (int z = -19000; z < 19000; z += 8) {
                if (world.isOcean(x, z)) continue;
                int c = world.solidTopY(x, z);
                if (c <= MantleWorld.SEA_Y + 2) continue;
                if (world.rockAt(x, z, c - 1) != Strata.Rock.SEDIMENTARY) continue;
                sedLand++;
                int ring = (world.solidTopY(x + 4, z) + world.solidTopY(x - 4, z)
                        + world.solidTopY(x, z + 4) + world.solidTopY(x, z - 4)) / 4;
                int drop = ring - c;
                if (drop > maxDropSeen) maxDropSeen = drop;
                if (drop >= 3) found++;
            }
        }
        System.out.printf("seed=%d sedLandPts=%d sinkholes(drop>=3)=%d maxDrop=%d%n",
                seed, sedLand, found, maxDropSeen);
    }
}
