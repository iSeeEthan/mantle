import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

public final class Hash {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        PlateSim sim = new PlateSim(seed);
        int n = sim.hydrology().gridSize();

        long h = 0xcbf29ce484222325L;
        double half = PlateSim.HALF;
        double cell = (double) PlateSim.WORLD / n;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                double wx = (i + 0.5) * cell - half;
                double wz = (j + 0.5) * cell - half;
                long q = Math.round(sim.surfaceY(wx, wz) * 16.0);
                h ^= q; h *= 0x100000001b3L;
            }
        }
        System.out.printf("seed=%d hash=%016x%n", seed, h);
    }
}
