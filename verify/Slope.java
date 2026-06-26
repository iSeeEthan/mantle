import com.iseeethan.mantle.world.MantleWorld;

public final class Slope {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        int half = args.length > 1 ? Integer.parseInt(args[1]) : 2500;
        MantleWorld w = MantleWorld.forSeed(seed);
        int worst = 0; int wx0 = 0, wz0 = 0;
        long histPerBlock = 0, n = 0;
        int[] hist = new int[20];
        for (int z = -half; z < half; z += 1) {
            for (int x = -half; x < half; x += 1) {
                int h = w.solidTopY(x, z);
                int hx = w.solidTopY(x + 1, z);
                int d = Math.abs(h - hx);
                if (d > worst) { worst = d; wx0 = x; wz0 = z; }
                hist[Math.min(hist.length - 1, d)]++;
                histPerBlock += d; n++;
            }
        }
        double worstDeg = Math.toDegrees(Math.atan(worst));
        System.out.println("worst adjacent rise = " + worst + " blocks/block (" + (int) worstDeg + " deg) at (" + wx0 + "," + wz0 + ")");
        System.out.println("avg rise per block = " + (double) histPerBlock / n);

        long steep40 = 0, steep55 = 0;
        for (int i = 0; i < hist.length; i++) { if (i >= 1) steep40 += hist[i]; if (i >= 2) steep55 += hist[i]; }
        System.out.println("cells rise>=1 (~45deg+): " + steep40 + "   rise>=2 (~63deg+): " + steep55);
        StringBuilder sb = new StringBuilder("hist (rise:count): ");
        for (int i = 0; i < hist.length; i++) if (hist[i] > 0) sb.append(i).append(":").append(hist[i]).append("  ");
        System.out.println(sb);
    }
}
