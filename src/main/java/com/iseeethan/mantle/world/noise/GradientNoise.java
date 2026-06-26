package com.iseeethan.mantle.world.noise;

public final class GradientNoise {
    private static final long PRIME_X = 0x5205402B9270C86FL;
    private static final long PRIME_Y = 0x598CD327003817B5L;
    private static final long HASH_MULTIPLIER = 0x53A3F72DEEC546F5L;

    private static final double ROOT2OVER2 = 0.7071067811865476;
    private static final double SKEW_2D = 0.366025403784439;
    private static final double UNSKEW_2D = -0.21132486540518713;

    private static final double NORMALIZER_2D = 0.05481866495625118;
    private static final int N_GRADS_2D_EXPONENT = 7;
    private static final int N_GRADS_2D = 1 << N_GRADS_2D_EXPONENT;

    private static final float[] GRADIENTS_2D;

    private final long seed;

    public GradientNoise(long seed) {
        this.seed = seed;
    }

    public double fbm(double x, double y, int octaves, double lacunarity, double gain) {
        double sum = 0, amp = 1, freq = 1, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * noise2(x * freq, y * freq, i);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return norm == 0 ? 0 : sum / norm;
    }

    public double ridged(double x, double y, int octaves, double lacunarity, double gain) {
        double sum = 0, amp = 1, freq = 1, norm = 0;
        for (int i = 0; i < octaves; i++) {
            double n = 1.0 - Math.abs(noise2(x * freq, y * freq, i));
            n *= n;
            sum += amp * n;
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return norm == 0 ? 0 : sum / norm;
    }

    public double noise2(double x, double y, int salt) {
        return noise2(this.seed + salt * 0x9E3779B97F4A7C15L, x, y);
    }

    public double noise2(double x, double y) {
        return noise2(this.seed, x, y);
    }

    private static double noise2(long seed, double x, double y) {

        double s = SKEW_2D * (x + y);
        double xs = x + s, ys = y + s;
        return noise2_UnskewedBase(seed, xs, ys);
    }

    private static double noise2_UnskewedBase(long seed, double xs, double ys) {
        int xsb = fastFloor(xs), ysb = fastFloor(ys);
        double xi = xs - xsb, yi = ys - ysb;

        long xsbp = xsb * PRIME_X, ysbp = ysb * PRIME_Y;

        double t = (xi + yi) * UNSKEW_2D;
        double dx0 = xi + t, dy0 = yi + t;

        double value = 0;

        double a0 = 2.0 / 3.0 - dx0 * dx0 - dy0 * dy0;
        if (a0 > 0) {
            value = (a0 * a0) * (a0 * a0) * grad(seed, xsbp, ysbp, dx0, dy0);
        }

        double a1 = (2 * (1 + 2 * UNSKEW_2D) * (1 / UNSKEW_2D + 2)) * t
                + ((-2 * (1 + 2 * UNSKEW_2D) * (1 + 2 * UNSKEW_2D)) + a0);
        if (a1 > 0) {
            double dx1 = dx0 - (1 + 2 * UNSKEW_2D);
            double dy1 = dy0 - (1 + 2 * UNSKEW_2D);
            value += (a1 * a1) * (a1 * a1) * grad(seed, xsbp + PRIME_X, ysbp + PRIME_Y, dx1, dy1);
        }

        if (dy0 > dx0) {
            double dx2 = dx0 - UNSKEW_2D;
            double dy2 = dy0 - (UNSKEW_2D + 1);
            double a2 = 2.0 / 3.0 - dx2 * dx2 - dy2 * dy2;
            if (a2 > 0) {
                value += (a2 * a2) * (a2 * a2) * grad(seed, xsbp, ysbp + PRIME_Y, dx2, dy2);
            }
        } else {
            double dx2 = dx0 - (UNSKEW_2D + 1);
            double dy2 = dy0 - UNSKEW_2D;
            double a2 = 2.0 / 3.0 - dx2 * dx2 - dy2 * dy2;
            if (a2 > 0) {
                value += (a2 * a2) * (a2 * a2) * grad(seed, xsbp + PRIME_X, ysbp, dx2, dy2);
            }
        }

        return value;
    }

    private static double grad(long seed, long xsvp, long ysvp, double dx, double dy) {

        long hash = seed + xsvp + ysvp;
        hash ^= hash >>> 33; hash *= 0xFF51AFD7ED558CCDL;
        hash ^= hash >>> 33; hash *= 0xC4CEB9FE1A85EC53L;
        hash ^= hash >>> 33;
        int gi = ((int) hash & (N_GRADS_2D - 1)) << 1;
        return GRADIENTS_2D[gi] * dx + GRADIENTS_2D[gi | 1] * dy;
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    static {
        GRADIENTS_2D = new float[N_GRADS_2D * 2];
        float[] grad2 = {
                0.38268343236509f, 0.923879532511287f,
                0.923879532511287f, 0.38268343236509f,
                0.923879532511287f, -0.38268343236509f,
                0.38268343236509f, -0.923879532511287f,
                -0.38268343236509f, -0.923879532511287f,
                -0.923879532511287f, -0.38268343236509f,
                -0.923879532511287f, 0.38268343236509f,
                -0.38268343236509f, 0.923879532511287f,
                0.130526192220052f, 0.99144486137381f,
                0.608761429008721f, 0.793353340291235f,
                0.793353340291235f, 0.608761429008721f,
                0.99144486137381f, 0.130526192220051f,
                0.99144486137381f, -0.130526192220051f,
                0.793353340291235f, -0.60876142900872f,
                0.608761429008721f, -0.793353340291235f,
                0.130526192220052f, -0.99144486137381f,
                -0.130526192220052f, -0.99144486137381f,
                -0.608761429008721f, -0.793353340291235f,
                -0.793353340291235f, -0.608761429008721f,
                -0.99144486137381f, -0.130526192220052f,
                -0.99144486137381f, 0.130526192220051f,
                -0.793353340291235f, 0.608761429008721f,
                -0.608761429008721f, 0.793353340291235f,
                -0.130526192220052f, 0.99144486137381f,
        };
        for (int i = 0; i < grad2.length; i++) {
            grad2[i] = (float) (grad2[i] / NORMALIZER_2D);
        }
        for (int i = 0, j = 0; i < GRADIENTS_2D.length; i++, j++) {
            if (j == grad2.length) j = 0;
            GRADIENTS_2D[i] = grad2[j];
        }
    }
}
