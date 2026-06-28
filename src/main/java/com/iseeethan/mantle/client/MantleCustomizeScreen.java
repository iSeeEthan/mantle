package com.iseeethan.mantle.client;

import com.iseeethan.mantle.world.gen.MantleChunkGenerator;
import com.iseeethan.mantle.world.gen.MantleGenConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class MantleCustomizeScreen extends Screen {

    private static final Component TITLE = Component.translatable("mantle.customize.title");

    private enum Tab { TERRAIN, SURFACE, CLIMATE, WORLD, ORES, PRESETS }

    private final CreateWorldScreen lastScreen;
    private final MantleGenConfig cfg;
    private final WorldPreview preview = new WorldPreview();

    private Tab tab = Tab.TERRAIN;
    private final List<AbstractWidget> tabContent = new ArrayList<>();
    private EditBox presetName;
    private String status = "";

    private int panelLeft;
    private int panelWidth;
    private int previewLeft;
    private int previewSize;

    public MantleCustomizeScreen(CreateWorldScreen lastScreen, WorldCreationContext context) {
        super(TITLE);
        this.lastScreen = lastScreen;
        ChunkGenerator gen = context.selectedDimensions().overworld();
        MantleGenConfig c = new MantleGenConfig();
        if (gen instanceof MantleChunkGenerator m) {
            c = m.config().copy();
        }
        this.cfg = c;
    }

    @Override
    protected void init() {
        previewSize = Math.min(260, this.height - 150);
        previewLeft = this.width - previewSize - 24;
        panelLeft = 24;
        panelWidth = previewLeft - panelLeft - 20;

        int tabW = Math.min(96, (panelWidth - 8) / Tab.values().length);
        int tx = panelLeft;
        for (Tab t : Tab.values()) {
            Tab target = t;
            addRenderableWidget(Button.builder(Component.literal(tabTitle(t)), b -> {
                this.tab = target;
                rebuildTab();
            }).bounds(tx, 44, tabW - 2, 18).build());
            tx += tabW;
        }

        addRenderableWidget(Button.builder(Component.literal("View: " + preview.mode().label), b -> {
            preview.setMode(preview.mode().next());
            b.setMessage(Component.literal("View: " + preview.mode().label));
        }).bounds(previewLeft, 44, previewSize, 18).build());

        presetName = new EditBox(this.font, panelLeft, this.height - 78, 150, 18, Component.literal("preset"));
        presetName.setHint(Component.literal("preset name"));
        presetName.setMaxLength(48);
        addRenderableWidget(presetName);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> apply())
                .bounds(this.width - 218, this.height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(this.width - 112, this.height - 30, 100, 20).build());

        rebuildTab();
        preview.request(currentSeed());
    }

    private static String tabTitle(Tab t) {
        switch (t) {
            case TERRAIN: return "Terrain";
            case SURFACE: return "Surface";
            case CLIMATE: return "Climate";
            case WORLD: return "World";
            case ORES: return "Ores";
            default: return "Presets";
        }
    }

    private void rebuildTab() {
        for (AbstractWidget w : tabContent) {
            removeWidget(w);
        }
        tabContent.clear();

        boolean presets = tab == Tab.PRESETS;
        presetName.visible = presets;
        presetName.active = presets;
        if (!presets) presetName.setFocused(false);

        int x = panelLeft;
        int y = 74;
        int w = panelWidth;
        int rowH = 24;

        switch (tab) {
            case TERRAIN:
                addToggle(x, y, w, "Debug (flat stone)", cfg.debug, v -> cfg.debug = v); y += rowH;
                addIntSlider(x, y, w, "Sea Level", 0, 200, () -> cfg.seaLevel, v -> cfg.seaLevel = v); y += rowH;
                addIntSlider(x, y, w, "Mountain Height", 120, 1000, () -> cfg.mountainHeight, v -> cfg.mountainHeight = v); y += rowH;
                addSlider(x, y, w, "Land Curve", 0.5, 4.0, () -> cfg.landCurve, v -> cfg.landCurve = v); y += rowH;
                addSlider(x, y, w, "Sea Fraction", 0.1, 0.9, () -> cfg.seaFraction, v -> cfg.seaFraction = v); y += rowH;
                addSlider(x, y, w, "Continent Scale", 0.3, 3.0, () -> cfg.continentScale, v -> cfg.continentScale = v); y += rowH;
                addSlider(x, y, w, "Erosion Intensity", 0.0, 3.0, () -> cfg.erosionIntensity, v -> cfg.erosionIntensity = v); y += rowH;
                addSlider(x, y, w, "Ruggedness", 0.0, 3.0, () -> cfg.ruggedness, v -> cfg.ruggedness = v); y += rowH;
                break;
            case SURFACE:
                addToggle(x, y, w, "Sediment & Deltas", cfg.enableSediment, v -> cfg.enableSediment = v); y += rowH;
                addSlider(x, y, w, "Sediment Strength", 0.0, 3.0, () -> cfg.sedimentStrength, v -> cfg.sedimentStrength = v); y += rowH;
                addToggle(x, y, w, "Glaciers", cfg.enableGlaciers, v -> cfg.enableGlaciers = v); y += rowH;
                addSlider(x, y, w, "Glacial Strength", 0.0, 3.0, () -> cfg.glacialStrength, v -> cfg.glacialStrength = v); y += rowH;
                addToggle(x, y, w, "Coastlines", cfg.enableCoasts, v -> cfg.enableCoasts = v); y += rowH;
                addSlider(x, y, w, "Coastal Strength", 0.0, 3.0, () -> cfg.coastalStrength, v -> cfg.coastalStrength = v); y += rowH;
                break;
            case CLIMATE:
                addSlider(x, y, w, "Temperature Scale", 0.2, 3.0, () -> cfg.temperatureScale, v -> cfg.temperatureScale = v); y += rowH;
                addSlider(x, y, w, "Rainfall Scale", 0.2, 3.0, () -> cfg.rainfallScale, v -> cfg.rainfallScale = v); y += rowH;
                addSlider(x, y, w, "Polar Coldness", 0.0, 2.0, () -> cfg.polarColdness, v -> cfg.polarColdness = v); y += rowH;
                addSlider(x, y, w, "Continental Dryness", 0.0, 2.0, () -> cfg.continentalDryness, v -> cfg.continentalDryness = v); y += rowH;
                break;
            case WORLD:
                addToggle(x, y, w, "Oceans", cfg.enableOceans, v -> cfg.enableOceans = v); y += rowH;
                addToggle(x, y, w, "Mountains", cfg.enableMountains, v -> cfg.enableMountains = v); y += rowH;
                addToggle(x, y, w, "Caves", cfg.enableCaves, v -> cfg.enableCaves = v); y += rowH;
                addToggle(x, y, w, "Rivers", cfg.enableRivers, v -> cfg.enableRivers = v); y += rowH;
                addSlider(x, y, w, "River Density", 0.0, 3.0, () -> cfg.riverDensity, v -> cfg.riverDensity = v); y += rowH;
                break;
            case ORES:
                addSlider(x, y, w, "Ore Density", 0.0, 4.0, () -> cfg.oreDensity, v -> cfg.oreDensity = v); y += rowH;
                addIntSlider(x, y, w, "Deepslate Level", -64, 128, () -> cfg.oreDeepslateLevel, v -> cfg.oreDeepslateLevel = v); y += rowH;
                break;
            case PRESETS:
                buildPresetTab(x, y, w);
                break;
        }
    }

    private void buildPresetTab(int x, int y, int w) {
        int half = (w - 4) / 2;
        addContent(Button.builder(Component.literal("Open folder"), b -> MantleConfigPresets.openFolder())
                .bounds(x, y, half, 18).build());
        addContent(Button.builder(Component.literal("Refresh"), b -> {
            status = "Rescanned presets folder";
            rebuildTab();
        }).bounds(x + half + 4, y, w - half - 4, 18).build());
        y += 24;

        addContent(Button.builder(Component.literal("Reset to defaults"), b -> {
            copyInto(new MantleGenConfig(), cfg);
            status = "Reset to defaults";
            rebuildTab();
        }).bounds(x, y, w, 18).build());
        y += 24;

        addContent(Button.builder(Component.literal("Save preset"), b -> savePreset())
                .bounds(x + 156, this.height - 78, w - 156, 18).build());

        List<String> names = MantleConfigPresets.list();
        if (names.isEmpty()) {
            return;
        }
        for (String name : names) {
            String n = name;
            addContent(Button.builder(Component.literal("Load: " + n), b -> {
                MantleGenConfig loaded = MantleConfigPresets.load(n);
                if (loaded != null) {
                    copyInto(loaded, cfg);
                    status = "Loaded " + n;
                    rebuildTab();
                } else {
                    status = "Failed to load " + n;
                }
            }).bounds(x, y, w - 24, 18).build());
            addContent(Button.builder(Component.literal("X"), b -> {
                MantleConfigPresets.delete(n);
                status = "Deleted " + n;
                rebuildTab();
            }).bounds(x + w - 20, y, 18, 18).build());
            y += 22;
            if (y > this.height - 110) break;
        }
    }

    private void savePreset() {
        if (presetName == null) return;
        cfg.clampAll();
        if (MantleConfigPresets.save(presetName.getValue(), cfg)) {
            status = "Saved " + presetName.getValue();
            rebuildTab();
        } else {
            status = "Save failed (name?)";
        }
    }

    private void copyInto(MantleGenConfig from, MantleGenConfig to) {
        MantleGenConfig src = from.copy();
        to.debug = src.debug;
        to.seaLevel = src.seaLevel;
        to.mountainHeight = src.mountainHeight;
        to.landCurve = src.landCurve;
        to.seaFraction = src.seaFraction;
        to.continentScale = src.continentScale;
        to.erosionIntensity = src.erosionIntensity;
        to.riverDensity = src.riverDensity;
        to.temperatureScale = src.temperatureScale;
        to.rainfallScale = src.rainfallScale;
        to.polarColdness = src.polarColdness;
        to.continentalDryness = src.continentalDryness;
        to.ruggedness = src.ruggedness;
        to.sedimentStrength = src.sedimentStrength;
        to.glacialStrength = src.glacialStrength;
        to.coastalStrength = src.coastalStrength;
        to.enableOceans = src.enableOceans;
        to.enableMountains = src.enableMountains;
        to.enableCaves = src.enableCaves;
        to.enableRivers = src.enableRivers;
        to.enableSediment = src.enableSediment;
        to.enableGlaciers = src.enableGlaciers;
        to.enableCoasts = src.enableCoasts;
        to.oreDensity = src.oreDensity;
        to.oreDeepslateLevel = src.oreDeepslateLevel;
    }

    private AbstractWidget addContent(AbstractWidget w) {
        tabContent.add(w);
        return addRenderableWidget(w);
    }

    private void addToggle(int x, int y, int w, String label, boolean initial, java.util.function.Consumer<Boolean> set) {
        CycleButton<Boolean> b = CycleButton.onOffBuilder(initial)
                .create(x, y, w, 18, Component.literal(label), (btn, val) -> set.accept(val));
        addContent(b);
    }

    private void addSlider(int x, int y, int w, String label, double min, double max, DoubleSupplier get, DoubleConsumer set) {
        addContent(new ConfigSlider(x, y, w, 18, label, min, max, false, get, set));
    }

    private void addIntSlider(int x, int y, int w, String label, double min, double max, java.util.function.IntSupplier get, java.util.function.IntConsumer set) {
        addContent(new ConfigSlider(x, y, w, 18, label, min, max, true, () -> get.getAsInt(), v -> set.accept((int) Math.round(v))));
    }

    private long currentSeed() {
        String raw = lastScreen.getUiState().getSeed();
        OptionalLong parsed = WorldOptions.parseSeed(raw == null ? "" : raw);
        return parsed.orElse(0L);
    }

    private void apply() {
        cfg.clampAll();
        MantleGenConfig applied = cfg.copy();
        lastScreen.getUiState().updateDimensions((registryAccess, dimensions) -> {
            ChunkGenerator current = dimensions.overworld();
            BiomeSource biomeSource = current.getBiomeSource();
            ChunkGenerator replacement = new MantleChunkGenerator(biomeSource, applied);
            return dimensions.replaceOverworldGenerator(registryAccess, replacement);
        });
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);

        int tabW = Math.min(96, (panelWidth - 8) / Tab.values().length);
        int idx = tab.ordinal();
        g.fill(panelLeft + idx * tabW, 63, panelLeft + idx * tabW + tabW - 2, 65, 0xFF6f9a5a);

        renderPreview(g);

        if (tab == Tab.PRESETS) {
            String hint = "Folder: " + MantleConfigPresets.dir();
            g.drawString(this.font, trimToWidth(hint, panelWidth), panelLeft, this.height - 110, 0x808080);
            if (MantleConfigPresets.list().isEmpty()) {
                g.drawString(this.font, "No presets yet — drag .json files into the folder, then Refresh.",
                        panelLeft, 74 + 52, 0x808080);
            }
        }

        if (!status.isEmpty()) {
            g.drawString(this.font, status, panelLeft, this.height - 96, 0x90C090);
        }
    }

    private String trimToWidth(String s, int maxWidth) {
        if (this.font.width(s) <= maxWidth) return s;
        String ellipsis = "...";
        while (s.length() > 1 && this.font.width(s + ellipsis) > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + ellipsis;
    }

    private void renderPreview(GuiGraphics g) {
        int top = 70;
        g.fill(previewLeft - 1, top - 1, previewLeft + previewSize + 1, top + previewSize + 1, 0xFF000000);

        if (preview.mode() == WorldPreview.Mode.HIDE) {
            g.fill(previewLeft, top, previewLeft + previewSize, top + previewSize, 0xFF14181c);
            g.drawCenteredString(this.font, Component.literal("Hidden"), previewLeft + previewSize / 2, top + previewSize / 2 - 4, 0x808080);
            return;
        }

        preview.setDisplay(cfg.seaLevel, cfg.mountainHeight);
        preview.setParams(cfg.toParams());
        long seed = currentSeed();
        if (preview.needsRebuild(seed) && !preview.isBuilding()) {
            preview.request(seed);
        }
        ResourceLocation tex = preview.poll();

        if (tex != null && preview.readySeed() == seed) {
            g.blit(tex, previewLeft, top, 0, 0f, 0f, previewSize, previewSize, previewSize, previewSize);
        } else {
            g.fill(previewLeft, top, previewLeft + previewSize, top + previewSize, 0xFF0c1014);
            g.drawCenteredString(this.font, Component.literal("Generating map..."), previewLeft + previewSize / 2, top + previewSize / 2 - 4, 0xC0C0C0);
        }
    }

    @Override
    public void onClose() {
        preview.close();
        this.minecraft.setScreen(lastScreen);
    }

    private final class ConfigSlider extends AbstractSliderButton {
        private final String label;
        private final double min, max;
        private final boolean integer;
        private final DoubleConsumer set;

        ConfigSlider(int x, int y, int w, int h, String label, double min, double max, boolean integer,
                     DoubleSupplier get, DoubleConsumer set) {
            super(x, y, w, h, Component.empty(), Math.max(0.0, Math.min(1.0, (get.getAsDouble() - min) / (max - min))));
            this.label = label;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.set = set;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double v = min + value * (max - min);
            String shown = integer ? String.valueOf((int) Math.round(v)) : String.format(java.util.Locale.ROOT, "%.2f", v);
            setMessage(Component.literal(label + ": " + shown));
        }

        @Override
        protected void applyValue() {
            set.accept(min + value * (max - min));
        }
    }
}
