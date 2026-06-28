import com.iseeethan.mantle.world.gen.ecology.Rainfall;
import com.iseeethan.mantle.world.gen.ecology.registries.RainfallConfig;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates a preview of the rainfall noise map.
 * Colors range from brown (arid) to blue (humid).
 *
 * Args: seed out.png centerX centerZ spanBlocks pixelSize
 */
public final class RainfallMap {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out = args.length > 1 ? args[1] : "verify/out/rainfall.png";
        int cx = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int cz = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int span = args.length > 4 ? Integer.parseInt(args[4]) : PlateSim.WORLD;
        int px = args.length > 5 ? Integer.parseInt(args[5]) : 1000;

        Rainfall rainSampler = new Rainfall(seed, RainfallConfig.DEFAULT, PlateSim.WORLD);

        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        double step = (double) span / px;
        double originX = cx - (double) span / 2;
        double originZ = cz - (double) span / 2;

        for (int py = 0; py < px; py++) {
            for (int pxi = 0; pxi < px; pxi++) {
                int wx = (int) Math.round(originX + (pxi + 0.5) * step);
                int wz = (int) Math.round(originZ + (py + 0.5) * step);

                double r = rainSampler.getRainfall(wx, wz);
                img.setRGB(pxi, py, getRainfallColor(r));
            }
        }

        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("Wrote rainfall preview to " + out + " (Span: " + span + " blocks)");
    }

    private static int getRainfallColor(double r) {
        int red, green, blue;
        // Multi-stop gradient: Brown (Arid) -> Yellow -> Green -> Blue (Humid)
        if (r < 0.25) { // Brown to Yellow
            double u = r / 0.25;
            red = (int) (139 + (255 - 139) * u);
            green = (int) (69 + (255 - 69) * u);
            blue = (int) (19 + (0 - 19) * u);
        } else if (r < 0.5) { // Yellow to Green
            double u = (r - 0.25) / 0.25;
            red = (int) (255 * (1 - u));
            green = 255;
            blue = 0;
        } else if (r < 0.75) { // Green to Cyan
            double u = (r - 0.5) / 0.25;
            red = 0;
            green = 255;
            blue = (int) (255 * u);
        } else { // Cyan to Blue
            double u = (r - 0.75) / 0.25;
            red = 0;
            green = (int) (255 * (1 - u));
            blue = 255;
        }
        return (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}