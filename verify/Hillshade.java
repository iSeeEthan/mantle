import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class Hillshade {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out = args.length > 1 ? args[1] : "verify/out/hillshade.png";
        int cx = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int cz = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int win = args.length > 4 ? Integer.parseInt(args[4]) : 512;

        MantleWorld world = MantleWorld.forSeed(seed);
        int px = win;
        int x0 = cx - win / 2, z0 = cz - win / 2;

        int[][] h = new int[px + 1][px + 1];
        for (int j = 0; j <= px; j++)
            for (int i = 0; i <= px; i++)
                h[j][i] = world.solidTopY(x0 + i, z0 + j);

        double lx = -0.6, lz = -0.6, ly = 1.0;
        double ll = Math.sqrt(lx * lx + lz * lz + ly * ly);
        lx /= ll; lz /= ll; ly /= ll;

        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < px; j++)
            for (int i = 0; i < px; i++) {
                double dzdx = (h[j][i + 1] - h[j][i]);
                double dzdz = (h[j + 1][i] - h[j][i]);

                double nx = -dzdx, nz = -dzdz, ny = 1.0;
                double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                double shade = (nx * lx + ny * ly + nz * lz) / nl;
                shade = 0.25 + 0.75 * Math.max(0, shade);
                int hh = h[j][i];
                int base = hh < PlateSim.SEA_Y ? 0x3a6ea5 : 0x8a8a8a;
                int r = (int) (((base >> 16) & 0xff) * shade);
                int g = (int) (((base >> 8) & 0xff) * shade);
                int b = (int) ((base & 0xff) * shade);
                img.setRGB(i, j, (clamp(r) << 16) | (clamp(g) << 8) | clamp(b));
            }
        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("Wrote " + out + "  center=(" + cx + "," + cz + ") win=" + win);
    }

    private static int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
}
