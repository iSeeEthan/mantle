import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

public final class CaveCheck {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        MantleWorld world = MantleWorld.forSeed(seed);

        int sampled = 0, withCaves = 0, totalAir = 0, totalSolid = 0;
        int n = 240;
        int span = 4000;
        java.util.Random rnd = new java.util.Random(99);
        for (int s = 0; s < n; s++) {
            int wx = rnd.nextInt(span * 2) - span;
            int wz = rnd.nextInt(span * 2) - span;
            if (world.isOcean(wx, wz)) { s--; continue; }
            int top = world.solidTopY(wx, wz);
            int air = 0, solid = 0;
            for (int y = MantleWorld.MIN_Y + 1; y <= top; y++) {
                if (world.caveAt(wx, y, wz, top)) air++; else solid++;
            }
            sampled++;
            totalAir += air; totalSolid += solid;
            if (air > 0) withCaves++;
        }
        double pct = 100.0 * totalAir / Math.max(1, totalAir + totalSolid);
        System.out.printf("seed=%d landColumns=%d withCaves=%d (%.1f%%) caveAirFraction=%.3f%%%n",
                seed, sampled, withCaves, 100.0 * withCaves / sampled, pct);
    }
}
