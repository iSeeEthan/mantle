import com.iseeethan.mantle.world.Locator;
import com.iseeethan.mantle.world.MantleWorld;

public final class LocateCheck {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        int fromX = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        int fromZ = args.length > 2 ? Integer.parseInt(args[2]) : 0;

        MantleWorld world = MantleWorld.forSeed(seed);
        Locator loc = new Locator(world);

        int basement = 0, igneous = 0, sed = 0, meta = 0, samples = 0;
        for (int x = -19000; x < 19000; x += 200) {
            for (int z = -19000; z < 19000; z += 200) {
                if (world.isOcean(x, z)) continue;
                int top = world.solidTopY(x, z);
                samples++;
                switch (world.rockAt(x, z, top - 1)) {
                    case BASEMENT: basement++; break;
                    case IGNEOUS: igneous++; break;
                    case METAMORPHIC: meta++; break;
                    case SEDIMENTARY: sed++; break;
                }
            }
        }
        System.out.printf("exposed rock over %d land samples: SED=%d IGN=%d META=%d BASE=%d%n",
                samples, sed, igneous, meta, basement);

        System.out.printf("seed=%d from=(%d,%d)%n", seed, fromX, fromZ);
        for (Locator.Target t : Locator.Target.values()) {
            long t0 = System.currentTimeMillis();
            Locator.Found f = loc.locate(t, fromX, fromZ);
            long ms = System.currentTimeMillis() - t0;
            if (f == null) {
                System.out.printf("  %-14s NOT FOUND (%dms)%n", t, ms);
            } else {
                int dist = (int) Math.round(Math.hypot(f.x - fromX, f.z - fromZ));
                System.out.printf("  %-14s [%d, %d, %d] dist=%d (%dms)%n", t, f.x, f.y, f.z, dist, ms);
            }
        }
    }
}
