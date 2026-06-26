import com.iseeethan.mantle.world.gen.tectonics.PlateSim;
import com.iseeethan.mantle.world.gen.tectonics.Strata;
import com.iseeethan.mantle.world.MantleWorld;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

public final class StrataMap {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out = args.length > 1 ? args[1] : "verify/out/strata.png";
        int z = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int W = args.length > 3 ? Integer.parseInt(args[3]) : 4000;
        int startX = args.length > 4 ? Integer.parseInt(args[4]) : -PlateSim.HALF;

        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);
        Strata strata = world.strata();

        int imgW = 2000, imgH = (int) Math.round((double) imgW / W * (PlateSim.PEAK_Y - PlateSim.FLOOR_Y));
        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(135, 180, 235));
        g.fillRect(0, 0, imgW, imgH);

        int bot = PlateSim.FLOOR_Y, top = PlateSim.PEAK_Y;
        double yScale = (double) imgH / (top - bot);

        int seaPx = imgH - (int) ((PlateSim.SEA_Y - bot) * yScale);

        for (int sx = 0; sx < imgW; sx++) {
            int wx = startX + (int) ((double) sx / imgW * W);
            double surf = world.solidTopY(wx, z);
            int h = (int) Math.round(surf);

            if (h < PlateSim.SEA_Y) {
                int wpy = imgH - (int) ((PlateSim.SEA_Y - bot) * yScale);
                int hpy = imgH - (int) ((h - bot) * yScale);
                g.setColor(new Color(45, 95, 165));
                g.fillRect(sx, wpy, 1, hpy - wpy);
            }

            for (int py = imgH - 1; py >= 0; py--) {
                int y = bot + (int) ((imgH - 1 - py) / yScale);
                if (y > h) continue;
                Strata.Rock r = strata.typeAt(wx, z, y, surf);
                g.setColor(color(r));
                img.setRGB(sx, py, color(r).getRGB());
            }
        }

        g.setColor(new Color(120, 160, 230));
        g.drawLine(0, seaPx, imgW, seaPx);
        g.dispose();

        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("Wrote " + out);
    }

    private static Color color(Strata.Rock r) {
        switch (r) {
            case SEDIMENTARY: return new Color(208, 184, 130);
            case IGNEOUS:     return new Color(150, 90, 95);
            case METAMORPHIC: return new Color(110, 120, 140);
            case BASEMENT:    return new Color(70, 65, 80);
            default:          return Color.MAGENTA;
        }
    }
}
