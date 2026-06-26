import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;

/**
 * 3D containment + lake-depth + connectivity checks over a window.
 *   spill   : a water column whose waterTop is ABOVE its own original terrain AND
 *             a 4-neighbour is open air at that level (water would flow out).
 *   maxLakeDepth : deepest (waterTop - stoneTop) among lake-ish columns.
 *   gaps    : river/lake water columns that have a NON-water, lower neighbour the
 *             water surface sits proud of with no wall (disconnection symptom).
 * Args: seed centerX centerZ span
 */
public final class Contain {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1337L;
        int cx = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        int cz = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int span = args.length > 3 ? Integer.parseInt(args[3]) : 600;

        PlateSim sim = new PlateSim(seed);
        MantleWorld world = MantleWorld.forSeed(seed);

        int x0 = cx - span / 2, z0 = cz - span / 2;
        int N = span;
        int[] stone = new int[N * N];
        int[] wtop  = new int[N * N];
        boolean[] has = new boolean[N * N];
        MantleWorld.Column col = new MantleWorld.Column();
        int maxDepth = 0, waterCols = 0;
        for (int j = 0; j < N; j++) for (int i = 0; i < N; i++) {
            int wx = x0 + i, wz = z0 + j;
            world.column(wx, wz, col);
            int idx = j * N + i;
            stone[idx] = col.stoneTop; wtop[idx] = col.waterTop; has[idx] = col.hasWater;
            if (col.hasWater && col.waterTop > col.stoneTop) {
                waterCols++;
                int d = col.waterTop - col.stoneTop;
                if (d > maxDepth) maxDepth = d;
            }
        }

        // spill: water column with a neighbour whose SOLID top is below this water
        // surface and that neighbour has no water as high -> water would pour out.
        // Spill severity = how far the water surface sits ABOVE the lowest neighbour
        // barrier (stone or its own water). 1 = a normal river step / shoreline,
        // >=2 means water is genuinely proud of an open side (image-3 sheeting).
        int[] dx = {1,-1,0,0}, dz = {0,0,1,-1};
        int spill1 = 0, spill2 = 0, spill3 = 0;
        boolean[] badMask = new boolean[N * N];
        for (int j = 1; j < N-1; j++) for (int i = 1; i < N-1; i++) {
            int idx = j*N+i;
            if (!has[idx] || wtop[idx] <= stone[idx]) continue;
            int ws = wtop[idx];
            int worst = 0;
            for (int d = 0; d < 4; d++) {
                int nidx = (j+dz[d])*N + (i+dx[d]);
                int nbarrier = Math.max(stone[nidx], has[nidx] ? wtop[nidx] : Integer.MIN_VALUE);
                int gap = ws - nbarrier;
                if (gap > worst) worst = gap;
            }
            if (worst >= 3) { spill3++; badMask[idx] = true; }
            else if (worst == 2) spill2++;
            else if (worst == 1) spill1++;
        }

        // largest contiguous blob of bad (>=3) spill — a real lateral sheet (image 3)
        // is one big blob; scattered cascade steps are many size-1 blobs.
        boolean[] seen = new boolean[N*N];
        int biggestBlob = 0;
        int[] stack = new int[N*N];
        for (int s0 = 0; s0 < N*N; s0++) {
            if (!badMask[s0] || seen[s0]) continue;
            int sp = 0, sz = 0; stack[sp++] = s0; seen[s0] = true;
            while (sp > 0) {
                int c = stack[--sp]; sz++;
                int ci = c % N, cj = c / N;
                for (int d = 0; d < 4; d++) {
                    int ni = ci + dx[d], nj = cj + dz[d];
                    if (ni<0||nj<0||ni>=N||nj>=N) continue;
                    int nc = nj*N+ni;
                    if (badMask[nc] && !seen[nc]) { seen[nc] = true; stack[sp++] = nc; }
                }
            }
            if (sz > biggestBlob) biggestBlob = sz;
        }
        System.out.println("window " + span + "x" + span + " @(" + cx + "," + cz + ")");
        System.out.println("  waterColumns = " + waterCols);
        System.out.println("  maxWaterDepth = " + maxDepth + "  (lakes should be > 3)");
        System.out.println("  spill  >=3 (bad) = " + spill3
                + "   ==2 (suspect) = " + spill2
                + "   ==1 (normal step/shore) = " + spill1);
        System.out.println("  biggest bad-spill blob = " + biggestBlob
                + "  (>~30 = a visible lateral sheet like image 3; <10 = cascade steps)");
    }
}
