import com.iseeethan.mantle.world.gen.tectonics.Hydrology;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Block-resolution render of EXACTLY what the chunk generator sees:
 * for each block (wx,wz) it calls surfaceY + sampleRiver and paints
 *   - blue        : water placed (river/lake)
 *   - red         : a carved channel that ended up DRY (terrain notch, no water)
 *   - grey        : stone, height-shaded
 * This reproduces the in-game look so river bugs are visible without MC.
 *
 * Args: seed out.png centerX centerZ spanBlocks
 */
public final class BlockRiver {
    public static void main(String[] args) throws Exception {
        long seed   = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out  = args.length > 1 ? args[1] : "verify/out/blockriver.png";
        int cx      = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int cz      = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int span    = args.length > 4 ? Integer.parseInt(args[4]) : 600;

        long t0 = System.currentTimeMillis();
        PlateSim sim = new PlateSim(seed);
        System.out.println("build " + (System.currentTimeMillis() - t0) + "ms");

        int px = span; // 1 px per block
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        Hydrology.Sample s = new Hydrology.Sample();

        int dryChannels = 0, waterBlocks = 0;
        int x0 = cx - span / 2, z0 = cz - span / 2;

        for (int pz = 0; pz < px; pz++) {
            for (int pxx = 0; pxx < px; pxx++) {
                int wx = x0 + pxx, wz = z0 + pz;
                double terr = sim.surfaceY(wx, wz);
                sim.sampleRiver(wx, wz, terr, s);

                int rgb;
                if (s.water) {
                    // water column from bed..surf; depth shade
                    double depth = s.waterY - s.bedY;
                    int b = (int) Math.max(120, Math.min(255, 255 - depth * 12));
                    rgb = (30 << 16) | (70 << 8) | b;
                    waterBlocks++;
                } else if (s.bankOnly && s.bedY < terr - 0.5) {
                    // a channel was carved (bed below terrain) but no water -> dry notch
                    rgb = (200 << 16) | (40 << 8) | 40;
                    dryChannels++;
                } else {
                    double t = (terr - PlateSim.SEA_Y) / 300.0;
                    int g = (int) (70 + 150 * Math.min(1, Math.max(0, t)));
                    // cheap hillshade
                    double sl = sim.surfaceY(wx + 1, wz) - sim.surfaceY(wx - 1, wz);
                    g += (int) Math.max(-40, Math.min(40, -sl * 8));
                    g = Math.max(0, Math.min(255, g));
                    rgb = (g << 16) | (g << 8) | (int) (g * 0.85);
                }
                img.setRGB(pxx, pz, rgb);
            }
        }
        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("Wrote " + out + "  span=" + span
                + "  waterBlocks=" + waterBlocks + "  dryNotchBlocks=" + dryChannels);
    }
}
