import com.iseeethan.mantle.world.gen.ecology.Temperature;
import com.iseeethan.mantle.world.gen.ecology.registries.TemperatureConfig;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates a preview of the temperature noise map.
 * Colors range from blue (cold/poles) to red (hot/equator).
 * 
 * Args: seed out.png centerX centerZ spanBlocks pixelSize
 */
public final class TemperatureMap {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out = args.length > 1 ? args[1] : "verify/out/temperature.png";
        int cx = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int cz = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int span = args.length > 4 ? Integer.parseInt(args[4]) : PlateSim.WORLD;
        int px = args.length > 5 ? Integer.parseInt(args[5]) : 1000;

        // Use the default config for the preview
        Temperature tempSampler = new Temperature(seed, TemperatureConfig.DEFAULT, PlateSim.WORLD);
        
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        double step = (double) span / px;
        double originX = cx - (double) span / 2;
        double originZ = cz - (double) span / 2;

        for (int py = 0; py < px; py++) {
            for (int pxi = 0; pxi < px; pxi++) {
                int wx = (int) Math.round(originX + (pxi + 0.5) * step);
                int wz = (int) Math.round(originZ + (py + 0.5) * step);
                
                double t = tempSampler.getTemperature(wx, wz);
                img.setRGB(pxi, py, getTemperatureColor(t));
            }
        }

        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("Wrote temperature preview to " + out + " (Span: " + span + " blocks)");
    }

    /**
     * Converts a 0.0-1.0 temperature value into a thermal color ramp.
     */
    private static int getTemperatureColor(double t) {
        int r, g, b;
        
        // Multi-stop gradient: Blue -> Cyan -> Green -> Yellow -> Red
        if (t < 0.25) { // Blue to Cyan
            double u = t / 0.25;
            r = 0;
            g = (int) (255 * u);
            b = 255;
        } else if (t < 0.5) { // Cyan to Green
            double u = (t - 0.25) / 0.25;
            r = 0;
            g = 255;
            b = (int) (255 * (1 - u));
        } else if (t < 0.75) { // Green to Yellow
            double u = (t - 0.5) / 0.25;
            r = (int) (255 * u);
            g = 255;
            b = 0;
        } else { // Yellow to Red
            double u = (t - 0.75) / 0.25;
            r = 255;
            g = (int) (255 * (1 - u));
            b = 0;
        }

        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}