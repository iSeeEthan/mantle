package com.iseeethan.mantle.client;

import com.iseeethan.mantle.world.GenStatus;
import com.iseeethan.mantle.world.gen.MantleChunkGenerator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldOptions;

import java.util.OptionalLong;

public final class MantleCustomizeScreen extends Screen {

    private static final Component TITLE = Component.translatable("mantle.customize.title");
    private static final Component MODE_LABEL = Component.translatable("mantle.customize.mode");
    private static final Component FLAVORED = Component.translatable("mantle.customize.flavored");
    private static final Component DEBUG = Component.translatable("mantle.customize.debug");
    private static final Component FLAVORED_DESC = Component.translatable("mantle.customize.flavored.desc");
    private static final Component DEBUG_DESC = Component.translatable("mantle.customize.debug.desc");
    private static final Component PREVIEW_LABEL = Component.translatable("mantle.customize.preview");
    private static final Component GENERATING = Component.translatable("mantle.customize.generating");

    private static final int PREVIEW_SIZE = 140;

    private final CreateWorldScreen lastScreen;
    private boolean debug;
    private final WorldPreview preview = new WorldPreview();

    public MantleCustomizeScreen(CreateWorldScreen lastScreen, WorldCreationContext context) {
        super(TITLE);
        this.lastScreen = lastScreen;
        ChunkGenerator gen = context.selectedDimensions().overworld();
        this.debug = gen instanceof MantleChunkGenerator m && m.isDebug();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        Button modeButton = Button.builder(modeLabel(), b -> {
            this.debug = !this.debug;
            b.setMessage(modeLabel());
        }).bounds(cx - 100, 80, 200, 20).build();
        addRenderableWidget(modeButton);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> apply())
                .bounds(cx - 100, this.height - 52, 98, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 2, this.height - 52, 98, 20).build());

        preview.request(currentSeed());
    }

    private long currentSeed() {
        String raw = lastScreen.getUiState().getSeed();
        OptionalLong parsed = WorldOptions.parseSeed(raw == null ? "" : raw);
        return parsed.orElse(0L);
    }

    private Component modeLabel() {
        return Component.empty().append(MODE_LABEL).append(": ").append(debug ? DEBUG : FLAVORED);
    }

    private void apply() {
        boolean newDebug = this.debug;
        lastScreen.getUiState().updateDimensions((registryAccess, dimensions) -> {
            ChunkGenerator current = dimensions.overworld();
            BiomeSource biomeSource = current.getBiomeSource();
            ChunkGenerator replacement = new MantleChunkGenerator(biomeSource, newDebug);
            return dimensions.replaceOverworldGenerator(registryAccess, replacement);
        });
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 28, 0xFFFFFF);

        Component desc = debug ? DEBUG_DESC : FLAVORED_DESC;
        g.drawCenteredString(this.font, desc, this.width / 2, 110, 0xA0A0A0);

        renderPreview(g);
    }

    private void renderPreview(GuiGraphics g) {
        int cx = this.width / 2;
        int top = 138;
        int side = PREVIEW_SIZE;
        int ox = cx - side / 2;

        g.drawCenteredString(this.font, PREVIEW_LABEL, cx, top - 12, 0xC0C0C0);

        ResourceLocation tex = preview.poll();
        long seed = currentSeed();
        if (preview.readySeed() != seed) {
            preview.request(seed);
        }

        g.fill(ox - 1, top - 1, ox + side + 1, top + side + 1, 0xFF000000);
        if (tex != null && preview.readySeed() == seed) {
            g.blit(tex, ox, top, 0, 0f, 0f, side, side, side, side);
        } else {
            g.fill(ox, top, ox + side, top + side, 0xFF0c1014);
            String stage = GenStatus.isSimulating() ? GenStatus.stageText() : "";
            g.drawCenteredString(this.font, GENERATING, cx, top + side / 2 - 9, 0xE0E0E0);
            if (stage != null && !stage.isEmpty()) {
                g.drawCenteredString(this.font, stage, cx, top + side / 2 + 3, 0x90A0B0);
            }
        }
    }

    @Override
    public void tick() {
        preview.poll();
    }

    @Override
    public void onClose() {
        preview.close();
        this.minecraft.setScreen(lastScreen);
    }
}
