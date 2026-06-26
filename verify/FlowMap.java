import com.iseeethan.mantle.world.gen.tectonics.Hydrology;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class FlowMap {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        String out = args.length > 1 ? args[1] : "verify/out/flow.png";

        int riverThresh = args.length > 2 ? Integer.parseInt(args[2]) : 1800;

        PlateSim sim = new PlateSim(seed);
        Hydrology h = sim.hydrology();
        int n = h.gridSize();

        int[] accum = h.accum();
        float[] filled = h.filled();

        boolean[] river = new boolean[n * n];
        for (int idx = 0; idx < n * n; idx++)
            river[idx] = filled[idx] > PlateSim.SEA_Y && accum[idx] >= riverThresh;
        float[] water = h.waterY();

        long maxAcc = 1;
        for (int a : accum) if (a > maxAcc) maxAcc = a;

        int[] field = new int[n * n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                int rgb;
                float surf = filled[idx];
                if (surf <= PlateSim.SEA_Y) {

                    double d = (PlateSim.SEA_Y - surf) / (double) (PlateSim.SEA_Y - PlateSim.FLOOR_Y);
                    int b = (int) (90 + 120 * (1 - Math.min(1, d)));
                    rgb = (20 << 16) | (50 << 8) | b;
                } else if (h.isLakeCell(idx)) {
                    rgb = (40 << 16) | (200 << 8) | 220;
                } else if (river[idx]) {

                    double t = Math.log(accum[idx] + 1) / Math.log(maxAcc + 1);
                    int c = (int) (120 + 135 * t);
                    rgb = (40 << 16) | (120 << 8) | c;
                } else {

                    double t = (surf - PlateSim.SEA_Y) / (double) (760 - PlateSim.SEA_Y);
                    int g = (int) (70 + 150 * Math.min(1, Math.max(0, t)));
                    rgb = (g << 16) | (g << 8) | (int) (g * 0.85);
                }
                field[idx] = rgb;
            }
        }

        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < n; j++)
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                int rgb = field[idx];
                if (filled[idx] > PlateSim.SEA_Y && !river[idx] && !h.isLakeCell(idx)) {

                    for (int d = 0; d < 8 && rgb == field[idx]; d++) {
                        int ni = i + (d==2||d==4||d==6?-1:(d==3||d==5||d==7?1:0));
                        int nj = j + (d==0||d==4||d==5?-1:(d==1||d==6||d==7?1:0));
                        if (ni>=0&&nj>=0&&ni<n&&nj<n && river[nj*n+ni]) rgb = (30<<16)|(90<<8)|180;
                    }
                }
                img.setRGB(i, j, rgb);
            }
        File f = new File(out);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);

        verifyMonotone(h, n);
        System.out.println("Wrote " + out + "  maxAccum=" + maxAcc);
    }

    private static void verifyMonotone(Hydrology h, int n) {
        float[] water = h.waterY();
        int violations = 0; float worst = 0;
        for (int idx = 0; idx < n * n; idx++) {
            if (Float.isNaN(water[idx])) continue;
            int ds = h.downstreamAt(idx);
            if (ds >= 0 && !Float.isNaN(water[ds])) {
                float rise = water[ds] - water[idx];
                if (rise > 0.001f) { violations++; if (rise > worst) worst = rise; }
            }
        }
        System.out.println("downhill check: " + violations + " upstream-higher violations (worst rise "
                + worst + ")  [expect 0]");
    }
}
