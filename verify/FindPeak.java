import com.iseeethan.mantle.world.MantleWorld;

public final class FindPeak {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        MantleWorld world = MantleWorld.forSeed(seed);
        int best = Integer.MIN_VALUE, bx = 0, bz = 0;
        for (int z = -4900; z < 4900; z += 24)
            for (int x = -4900; x < 4900; x += 24) {
                int h = world.solidTopY(x, z);
                if (h > best) { best = h; bx = x; bz = z; }
            }
        System.out.println("peak Y=" + best + " at (" + bx + "," + bz + ")");
    }
}
