package com.iseeethan.mantle.client;

import com.iseeethan.mantle.Mantle;
import com.iseeethan.mantle.world.GenStatus;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;

@EventBusSubscriber(modid = Mantle.MOD_ID, value = Dist.CLIENT)
public final class MantleLoadingOverlay {

    private static final int CELL_PITCH = 2;
    private static final int MARGIN = 12;
    private static final int WHITE = 0xFFFFFFFF;

    private static Field listenerField;
    private static boolean lookupFailed;

    private MantleLoadingOverlay() {}

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof LevelLoadingScreen screen)) return;

        StoringChunkProgressListener listener = listener(screen);

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = screen.getMinecraft().font;

        int cx = screen.width / 2;
        int cy = screen.height / 2;
        int gridHalf = listener != null ? (listener.getDiameter() * CELL_PITCH) / 2 : 0;
        int y = cy + gridHalf + MARGIN;

        graphics.drawCenteredString(font, Component.literal(statusLine(listener)), cx, y, WHITE);
    }

    private static String statusLine(StoringChunkProgressListener listener) {
        if (GenStatus.isSimulating()) {
            String stage = GenStatus.stageText();
            return stage.isEmpty() ? "Building world..." : stage + "...";
        }
        if (listener != null) {
            return "Generating terrain: " + listener.getProgress() + "%";
        }
        return "Generating terrain";
    }

    private static StoringChunkProgressListener listener(LevelLoadingScreen screen) {
        Field f = resolveField();
        if (f == null) return null;
        try {
            Object value = f.get(screen);
            return value instanceof StoringChunkProgressListener s ? s : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Field resolveField() {
        if (listenerField != null) return listenerField;
        if (lookupFailed) return null;
        for (Field f : LevelLoadingScreen.class.getDeclaredFields()) {
            if (StoringChunkProgressListener.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                listenerField = f;
                return f;
            }
        }
        lookupFailed = true;
        return null;
    }
}
