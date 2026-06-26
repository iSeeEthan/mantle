import com.iseeethan.mantle.world.gen.tectonics.PlateSim;
import com.iseeethan.mantle.world.MantleWorld;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

public final class Profile {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out = args.length > 1 ? args[1] : "verify/out/profile.png";
        int z = args.length > 2 ? Integer.parseInt(args[2]) : 0;

        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);

        int winBlocks = args.length > 3 ? Integer.parseInt(args[3]) : 4000;
        int startX = args.length > 4 ? Integer.parseInt(args[4]) : -PlateSim.HALF;
        int W = winBlocks;
        int imgW = 2000, imgH = (int) Math.round((double) imgW / W * (PlateSim.PEAK_Y - PlateSim.FLOOR_Y));
        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(20, 24, 30));
        g.fillRect(0, 0, imgW, imgH);

        int top = PlateSim.PEAK_Y, bot = PlateSim.FLOOR_Y;
        double yScale = (double) imgH / (top - bot);

        int seaPx = imgH - (int) ((PlateSim.SEA_Y - bot) * yScale);
        g.setColor(new Color(60, 110, 200));
        g.fillRect(0, seaPx, imgW, imgH - seaPx);
        g.setColor(new Color(120, 160, 230));
        g.drawLine(0, seaPx, imgW, seaPx);

        g.setColor(new Color(50, 55, 62));
        for (int y = bot; y <= top; y += 128) {
            int py = imgH - (int) ((y - bot) * yScale);
            g.drawLine(0, py, imgW, py);
        }

        int maxY = Integer.MIN_VALUE, minLand = Integer.MAX_VALUE;
        int prevPx = -1, prevPy = -1;
        for (int sx = 0; sx < imgW; sx++) {
            int wx = startX + (int) ((double) sx / imgW * W);
            int h = world.solidTopY(wx, z);
            if (h >= PlateSim.SEA_Y) { maxY = Math.max(maxY, h); minLand = Math.min(minLand, h); }
            int py = imgH - (int) ((h - bot) * yScale);
            g.setColor(h >= PlateSim.SEA_Y ? new Color(110, 90, 70) : new Color(40, 70, 130));
            g.fillRect(sx, py, 1, imgH - py);
            if (prevPx >= 0) { g.setColor(new Color(230, 220, 200)); g.drawLine(prevPx, prevPy, sx, py); }
            prevPx = sx; prevPy = py;
        }
        g.dispose();
        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("Wrote " + out + "  maxLandY=" + maxY + " minLandY=" + minLand);
    }
}
