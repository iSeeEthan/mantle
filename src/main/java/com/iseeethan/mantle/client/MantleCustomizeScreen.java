package com.iseeethan.mantle.client;

import com.iseeethan.mantle.world.gen.MantleChunkGenerator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class MantleCustomizeScreen extends Screen {

    private static final Component TITLE = Component.translatable("mantle.customize.title");
    private static final Component MODE_LABEL = Component.translatable("mantle.customize.mode");
    private static final Component FLAVORED = Component.translatable("mantle.customize.flavored");
    private static final Component DEBUG = Component.translatable("mantle.customize.debug");
    private static final Component FLAVORED_DESC = Component.translatable("mantle.customize.flavored.desc");
    private static final Component DEBUG_DESC = Component.translatable("mantle.customize.debug.desc");

    private final CreateWorldScreen lastScreen;
    private boolean debug;

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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 28, 0xFFFFFF);
        Component desc = debug ? DEBUG_DESC : FLAVORED_DESC;
        graphics.drawCenteredString(this.font, desc, this.width / 2, 110, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(lastScreen);
    }
}
