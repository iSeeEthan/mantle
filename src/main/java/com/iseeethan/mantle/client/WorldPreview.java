package com.iseeethan.mantle.client;

import com.iseeethan.mantle.world.gen.tectonics.PlateSim;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.atomic.AtomicReference;

public final class WorldPreview implements AutoCloseable {

    public static final int RES = 224;

    private static final int SEA_Y = PlateSim.SEA_Y;
    private static final int FLOOR = PlateSim.FLOOR_Y;
    private static final int PEAK = 760;

    private final AtomicReference<int[]> ready = new AtomicReference<>();
    private volatile long pendingSeed;
    private volatile boolean building;
    private Thread worker;

    private DynamicTexture texture;
    private ResourceLocation textureId;
    private long uploadedSeed = Long.MIN_VALUE;

    public void request(long seed) {
        if (building && pendingSeed == seed) return;
        if (uploadedSeed == seed && texture != null) return;
        pendingSeed = seed;
        building = true;
        ready.set(null);
        if (worker != null) worker.interrupt();
        worker = new Thread(() -> buildInto(seed), "mantle-preview");
        worker.setDaemon(true);
        worker.start();
    }

    private void buildInto(long seed) {
        try {
            float[] relief = PlateSim.previewRelief(seed, RES);
            int[] px = new int[RES * RES];

            int[][] h = new int[RES + 1][RES + 1];
            for (int j = 0; j <= RES; j++) {
                int sj = Math.min(RES - 1, j);
                for (int i = 0; i <= RES; i++) {
                    int si = Math.min(RES - 1, i);
                    h[j][i] = Math.round(relief[sj * RES + si]);
                }
            }

            double lx = -0.55, lz = -0.55, ly = 1.0;
            double ll = Math.sqrt(lx * lx + lz * lz + ly * ly);
            lx /= ll; lz /= ll; ly /= ll;

            for (int j = 0; j < RES; j++) {
                for (int i = 0; i < RES; i++) {
                    int hh = h[j][i];
                    double dzdx = h[j][i + 1] - h[j][i];
                    double dzdz = h[j + 1][i] - h[j][i];
                    double nx = -dzdx, nz = -dzdz, ny = 1.0;
                    double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    double shade = (nx * lx + ny * ly + nz * lz) / nl;
                    shade = 0.35 + 0.65 * Math.max(0, shade);

                    int rgb = colorFor(hh);
                    int r = (int) (((rgb >> 16) & 0xff) * shade);
                    int g = (int) (((rgb >> 8) & 0xff) * shade);
                    int b = (int) ((rgb & 0xff) * shade);
                    px[j * RES + i] = 0xFF000000 | (clamp(b) << 16) | (clamp(g) << 8) | clamp(r);
                }
            }
            if (!Thread.currentThread().isInterrupted() && pendingSeed == seed) {
                ready.set(px);
            }
        } catch (Throwable ignored) {
        } finally {
            if (pendingSeed == seed) building = false;
        }
    }

    private static int colorFor(int y) {
        if (y < SEA_Y) {
            double t = clamp01((double) (y - FLOOR) / (SEA_Y - FLOOR));
            return lerp(0x10243f, 0x2f6aa8, t);
        }
        double t = clamp01((double) (y - SEA_Y) / (PEAK - SEA_Y));
        if (t < 0.04) return lerp(0xd8cfa0, 0x6f9a5a, t / 0.04);
        if (t < 0.45) return lerp(0x6f9a5a, 0x4f7d44, (t - 0.04) / 0.41);
        if (t < 0.72) return lerp(0x4f7d44, 0x8a7d63, (t - 0.45) / 0.27);
        if (t < 0.9) return lerp(0x8a7d63, 0x9c9690, (t - 0.72) / 0.18);
        return lerp(0x9c9690, 0xf2f4f7, (t - 0.9) / 0.1);
    }

    public ResourceLocation poll() {
        int[] px = ready.getAndSet(null);
        if (px != null) {
            upload(px);
            uploadedSeed = pendingSeed;
        }
        return textureId;
    }

    public boolean isBuilding() {
        return building;
    }

    public long readySeed() {
        return uploadedSeed;
    }

    private void upload(int[] px) {
        if (texture == null) {
            NativeImage img = new NativeImage(RES, RES, false);
            texture = new DynamicTexture(img);
            textureId = Minecraft.getInstance().getTextureManager().register("mantle_preview", texture);
        }
        NativeImage img = texture.getPixels();
        if (img == null) return;
        for (int j = 0; j < RES; j++)
            for (int i = 0; i < RES; i++)
                img.setPixelRGBA(i, j, px[j * RES + i]);
        texture.upload();
    }

    private static int lerp(int a, int b, double t) {
        t = clamp01(t);
        int ar = (a >> 16) & 0xff, ag = (a >> 8) & 0xff, ab = a & 0xff;
        int br = (b >> 16) & 0xff, bg = (b >> 8) & 0xff, bb = b & 0xff;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    @Override
    public void close() {
        if (worker != null) worker.interrupt();
        if (textureId != null) Minecraft.getInstance().getTextureManager().release(textureId);
        if (texture != null) texture.close();
        texture = null;
        textureId = null;
    }
}
