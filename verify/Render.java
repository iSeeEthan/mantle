import com.iseeethan.mantle.world.gen.tectonics.PlateSim;
import com.iseeethan.mantle.world.MantleWorld;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class Render {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out = args.length > 1 ? args[1] : "verify/out/map.png";
        int px = args.length > 2 ? Integer.parseInt(args[2]) : 1000;

        double cx = args.length > 4 ? Double.parseDouble(args[3]) : 0;
        double cz = args.length > 4 ? Double.parseDouble(args[4]) : 0;
        double span = args.length > 5 ? Double.parseDouble(args[5]) : PlateSim.WORLD;
        boolean micro = args.length > 6 && args[6].equals("micro");

        long t0 = System.currentTimeMillis();
        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);
        System.out.println("Sim built in " + (System.currentTimeMillis() - t0) + " ms, micro=" + micro);

        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        double step = span / px;
        double originX = cx - span / 2, originZ = cz - span / 2;
        double sea = PlateSim.SEA_Y;

        int land = 0;
        for (int py = 0; py < px; py++) {
            for (int pxi = 0; pxi < px; pxi++) {
                double wx = originX + (pxi + 0.5) * step;
                double wz = originZ + (py + 0.5) * step;
                double h = height(sim, world, micro, wx, wz);

                int rgb;
                if (h < sea) {
                    double depth = (sea - h) / (sea - PlateSim.FLOOR_Y);
                    int b = (int) (90 + 120 * (1 - depth));
                    int g = (int) (60 + 90 * (1 - depth));
                    rgb = ((Math.max(10, g - 40)) << 16) | (g << 8) | clamp(b);
                } else {
                    land++;

                    double o = 6.0;
                    double hx = height(sim, world, micro, wx + o, wz) - height(sim, world, micro, wx - o, wz);
                    double hz = height(sim, world, micro, wx, wz + o) - height(sim, world, micro, wx, wz - o);
                    double slope = (hx + hz) / (2 * o);

                    double shade = clampD(0.78 - slope * 2.2, 0.25, 1.35);

                    double t = Math.pow(clampD((h - sea) / (PlateSim.PEAK_Y - sea), 0, 1), 0.45);
                    int r, g, b;
                    if (t < 0.55) {
                        double u = t / 0.55;
                        r = (int) (70 + 110 * u);
                        g = (int) (135 - 45 * u);
                        b = (int) (60 - 20 * u);
                    } else if (t < 0.85) {
                        double u = (t - 0.55) / 0.30;
                        r = (int) (180 - 40 * u);
                        g = (int) (90 + 40 * u);
                        b = (int) (40 + 70 * u);
                    } else {
                        double u = (t - 0.85) / 0.15;
                        r = (int) (140 + 115 * u);
                        g = (int) (130 + 125 * u);
                        b = (int) (110 + 145 * u);
                    }
                    rgb = (clamp((int) (r * shade)) << 16) | (clamp((int) (g * shade)) << 8) | clamp((int) (b * shade));
                }
                img.setRGB(pxi, py, rgb);
            }
        }
        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("Wrote " + out + "  land=" + (100.0 * land / (px * (double) px)) + "%");
    }

    private static double height(PlateSim sim, MantleWorld world, boolean micro, double wx, double wz) {
        return micro ? world.solidTopY((int) Math.round(wx), (int) Math.round(wz)) : sim.surfaceY(wx, wz);
    }

    private static int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
    private static double clampD(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
