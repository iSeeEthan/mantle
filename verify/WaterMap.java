import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class WaterMap {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out = args.length > 1 ? args[1] : "verify/out/water.png";
        int cx = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int cz = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int span = args.length > 4 ? Integer.parseInt(args[4]) : 800;

        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);

        int px = span;
        int ox = cx - span / 2, oz = cz - span / 2;

        byte[] cls = new byte[px * px];
        int[] stoneTop = new int[px * px];
        MantleWorld.Column col = new MantleWorld.Column();
        for (int j = 0; j < px; j++) {
            for (int i = 0; i < px; i++) {
                int wx = ox + i, wz = oz + j;
                if (!world.inWorld(wx, wz)) { cls[j * px + i] = -1; continue; }
                world.column(wx, wz, col);
                stoneTop[j * px + i] = col.stoneTop;
                byte c = 0;
                if (col.hasWater && col.waterTop > col.stoneTop) {
                    if (col.waterTop <= PlateSim.SEA_Y && col.stoneTop < PlateSim.SEA_Y) c = 3;
                    else c = 1;

                }
                cls[j * px + i] = c;
            }
        }

        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        int trapped = 0;
        for (int j = 0; j < px; j++) {
            for (int i = 0; i < px; i++) {
                int c = cls[j * px + i];
                int rgb;
                if (c == -1) { img.setRGB(i, j, 0); continue; }
                if (c == 0) {

                    int w = 0;
                    if (i > 0 && cls[j * px + i - 1] == 1) w++;
                    if (i < px - 1 && cls[j * px + i + 1] == 1) w++;
                    if (j > 0 && cls[(j - 1) * px + i] == 1) w++;
                    if (j < px - 1 && cls[(j + 1) * px + i] == 1) w++;
                    if (w >= 3) { img.setRGB(i, j, 0xFF0000); trapped++; continue; }
                    int h = stoneTop[j * px + i];
                    int g = clamp(80 + (h - PlateSim.SEA_Y) / 3);
                    rgb = (g << 16) | (g << 8) | (g - 20 < 0 ? 0 : g - 20);
                } else if (c == 3) {
                    rgb = 0x0A2050;
                } else {
                    rgb = 0x3070E0;
                }
                img.setRGB(i, j, rgb);
            }
        }
        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("Wrote " + out + "  trapped(red)=" + trapped);
    }

    private static int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
}
