import com.iseeethan.mantle.world.MantleWorld;

public final class PondCount {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        MantleWorld world = MantleWorld.forSeed(seed);

        int waterAboveSea = 0, landSamples = 0;
        MantleWorld.Column col = new MantleWorld.Column();
        for (int x = -19000; x < 19000; x += 12) {
            for (int z = -19000; z < 19000; z += 12) {
                if (world.isOcean(x, z)) continue;
                landSamples++;
                world.column(x, z, col);
                if (col.hasWater && col.waterTop > MantleWorld.SEA_Y) waterAboveSea++;
            }
        }
        System.out.printf("seed=%d landSamples=%d aboveSeaWaterCells=%d (%.2f%%)%n",
                seed, landSamples, waterAboveSea, 100.0 * waterAboveSea / landSamples);
    }
}
