import com.iseeethan.mantle.world.noise.GradientNoise;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class NoiseTest {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out = args.length > 1 ? args[1] : "verify/out/noise.png";
        int px = 800;
        double scale = 1.0 / 40.0;
        GradientNoise g = new GradientNoise(seed);
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < px; y++)
            for (int x = 0; x < px; x++) {
                double v = g.fbm(x * scale, y * scale, 4, 2.0, 0.5);
                int c = (int) ((v * 0.5 + 0.5) * 255);
                c = Math.max(0, Math.min(255, c));
                img.setRGB(x, y, (c << 16) | (c << 8) | c);
            }
        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("Wrote " + out);
    }
}
