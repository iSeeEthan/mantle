package com.iseeethan.mantle.client;

import com.iseeethan.mantle.world.gen.tectonics.PlateSim;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.atomic.AtomicReference;

public final class WorldPreview implements AutoCloseable {

    public static final int RES = 384;

    public enum Mode {
        COLOR("Color"),
        GRAYSCALE("Grayscale"),
        BIOMES("Biomes"),
        HIDE("Hidden");

        public final String label;
        Mode(String label) { this.label = label; }
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    private static final int FLOOR = PlateSim.FLOOR_Y;

    private volatile int seaY = PlateSim.SEA_Y;
    private volatile int peak = 760;

    private final AtomicReference<PlateSim.PreviewMaps> mapsRef = new AtomicReference<>();
    private volatile long pendingSeed;
    private volatile int pendingParamsHash;
    private volatile int builtParamsHash = Integer.MIN_VALUE;
    private volatile PlateSim.Params params = new PlateSim.Params();
    private volatile boolean building;
    private Thread worker;

    private DynamicTexture texture;
    private ResourceLocation textureId;
    private long uploadedSeed = Long.MIN_VALUE;
    private Mode uploadedMode = null;
    private volatile long builtSeed = Long.MIN_VALUE;

    private Mode mode = Mode.COLOR;
    private int displayVersion = 0;
    private int uploadedDisplayVersion = -1;
    private int uploadedParamsHash = Integer.MIN_VALUE;

    public void setMode(Mode m) { this.mode = m; }
    public Mode mode() { return mode; }

    public void setDisplay(int seaLevel, int peakHeight) {
        int p = Math.max(seaLevel + 16, peakHeight);
        if (seaLevel != seaY || p != peak) {
            seaY = seaLevel;
            peak = p;
            displayVersion++;
        }
    }

    public void setParams(PlateSim.Params p) {
        this.params = p;
    }

    private int paramsHash(PlateSim.Params p) {
        return java.util.Objects.hash(p.seaFraction, p.landCurve, p.mountainTopY,
                p.continentScale, p.erosionIntensity, p.riverDensity,
                p.temperatureScale, p.rainfallScale, p.polarColdness, p.continentalDryness);
    }

    public void request(long seed) {
        int ph = paramsHash(params);
        if (building && pendingSeed == seed && pendingParamsHash == ph) return;
        if (builtSeed == seed && builtParamsHash == ph && mapsRef.get() != null) return;
        pendingSeed = seed;
        pendingParamsHash = ph;
        building = true;
        PlateSim.Params snapshot = params;
        if (worker != null) worker.interrupt();
        worker = new Thread(() -> buildInto(seed, ph, snapshot), "mantle-preview");
        worker.setDaemon(true);
        worker.start();
    }

    public boolean needsRebuild(long seed) {
        return builtSeed != seed || builtParamsHash != paramsHash(params);
    }

    private void buildInto(long seed, int ph, PlateSim.Params snapshot) {
        try {
            PlateSim.PreviewMaps maps = PlateSim.previewMaps(seed, RES, snapshot);
            if (!Thread.currentThread().isInterrupted() && pendingSeed == seed && pendingParamsHash == ph) {
                mapsRef.set(maps);
                builtSeed = seed;
                builtParamsHash = ph;
            }
        } catch (Throwable ignored) {
        } finally {
            if (pendingSeed == seed && pendingParamsHash == ph) building = false;
        }
    }

    public ResourceLocation poll() {
        if (mode == Mode.HIDE) return null;
        PlateSim.PreviewMaps maps = mapsRef.get();
        if (maps == null) return textureId;
        if (uploadedSeed == builtSeed && uploadedMode == mode && uploadedDisplayVersion == displayVersion
                && uploadedParamsHash == builtParamsHash) return textureId;
        int[] px = paint(maps, mode);
        upload(px);
        uploadedSeed = builtSeed;
        uploadedMode = mode;
        uploadedDisplayVersion = displayVersion;
        uploadedParamsHash = builtParamsHash;
        return textureId;
    }

    private int[] paint(PlateSim.PreviewMaps maps, Mode mode) {
        int[] px = new int[RES * RES];
        for (int j = 0; j < RES; j++) {
            for (int i = 0; i < RES; i++) {
                int idx = j * RES + i;
                int y = Math.round(maps.height[idx]);
                int rgb;
                switch (mode) {
                    case GRAYSCALE: rgb = grayFor(y); break;
                    case BIOMES: rgb = biomeFor(y, maps.temperature[idx], maps.rainfall[idx]); break;
                    default: rgb = colorFor(y); break;
                }
                int r = (rgb >> 16) & 0xff, g = (rgb >> 8) & 0xff, b = rgb & 0xff;
                px[idx] = 0xFF000000 | (b << 16) | (g << 8) | r;
            }
        }
        return px;
    }

    private int grayFor(int y) {
        if (y < seaY) {
            double t = clamp01((double) (y - FLOOR) / (seaY - FLOOR));
            int v = (int) (20 + 60 * t);
            return (v << 16) | (v << 8) | v;
        }
        double t = clamp01((double) (y - seaY) / (peak - seaY));
        int v = (int) (90 + 165 * t);
        return (v << 16) | (v << 8) | v;
    }

    private int colorFor(int y) {
        if (y < seaY) {
            double t = clamp01((double) (y - FLOOR) / (seaY - FLOOR));
            return lerp(0x0a1a33, 0x2f6aa8, t);
        }
        double t = clamp01((double) (y - seaY) / (peak - seaY));
        if (t < 0.04) return lerp(0xd8cfa0, 0x6f9a5a, t / 0.04);
        if (t < 0.45) return lerp(0x6f9a5a, 0x4f7d44, (t - 0.04) / 0.41);
        if (t < 0.72) return lerp(0x4f7d44, 0x8a7d63, (t - 0.45) / 0.27);
        if (t < 0.9) return lerp(0x8a7d63, 0x9c9690, (t - 0.72) / 0.18);
        return lerp(0x9c9690, 0xf2f4f7, (t - 0.9) / 0.1);
    }

    private int biomeFor(int y, double t, double r) {
        if (y < seaY) {
            if (t < 0.16) return 0xcfe6f5;
            if (t > 0.66) return 0x2f8fb0;
            return 0x2a5f8f;
        }
        double elevAbove = y - seaY;
        if (elevAbove > 600) {
            if (t < 0.32) return 0xf2f4f7;
            return 0x8a8378;
        }
        if (elevAbove > 380) {
            if (t < 0.30) return 0xe8eef0;
            return 0x7d8a72;
        }
        if (t < 0.16) {
            if (r < 0.3) return 0xb8c4cc;
            if (r < 0.55) return 0xd8e4e8;
            return 0x6f8a6a;
        }
        if (t < 0.38) {
            if (r < 0.3) return 0x9bad7a;
            if (r < 0.52) return 0x4a6b54;
            return 0x3d5e44;
        }
        if (t < 0.62) {
            if (r < 0.22) return 0xc7b87a;
            if (r < 0.38) return 0x9bad5a;
            if (r < 0.5) return 0x7fa54a;
            if (r < 0.64) return 0x6f9a4a;
            if (r < 0.8) return 0x4f7d44;
            return 0x3a6e3a;
        }
        if (r < 0.2) return 0xd9c89a;
        if (r < 0.38) return 0xc7a85a;
        if (r < 0.55) return 0xa8b05a;
        if (r < 0.72) return 0x6f9a3a;
        return 0x2f7d33;
    }

    public boolean isBuilding() { return building; }
    public long readySeed() { return builtSeed; }

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

    @Override
    public void close() {
        if (worker != null) worker.interrupt();
        if (textureId != null) Minecraft.getInstance().getTextureManager().release(textureId);
        if (texture != null) texture.close();
        texture = null;
        textureId = null;
    }
}
