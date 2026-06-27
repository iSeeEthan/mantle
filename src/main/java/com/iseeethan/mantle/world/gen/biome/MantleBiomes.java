package com.iseeethan.mantle.world.gen.biome;

import com.iseeethan.mantle.Mantle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public enum MantleBiomes {

    FROZEN_BARRENS("frozen_barrens"),
    SNOWY_TUNDRA("snowy_tundra"),
    SNOWY_TAIGA("snowy_taiga"),
    GLACIER("glacier"),
    SNOWY_PEAKS("snowy_peaks"),
    COLD_ROCKY_COAST("cold_rocky_coast"),

    TAIGA("taiga"),
    BOREAL_FOREST("boreal_forest"),
    BOREAL_WETLAND("boreal_wetland"),
    HIGHLAND_HEATH("highland_heath"),
    MONTANE_FOREST("montane_forest"),
    ALPINE_MEADOW("alpine_meadow"),

    GRASSLAND("grassland"),
    MEADOW("meadow"),
    TEMPERATE_FOREST("temperate_forest"),
    OAK_WOODLAND("oak_woodland"),
    BIRCH_FOREST("birch_forest"),
    TEMPERATE_RAINFOREST("temperate_rainforest"),
    RIVER_VALLEY("river_valley"),
    MARSH("marsh"),
    TEMPERATE_COAST("temperate_coast"),

    STEPPE("steppe"),
    SAVANNA("savanna"),
    SHRUBLAND("shrubland"),
    BADLANDS("badlands"),
    ROCKY_DESERT("rocky_desert"),
    SAND_DESERT("sand_desert"),

    TROPICAL_RAINFOREST("tropical_rainforest"),
    TROPICAL_WETLAND("tropical_wetland"),
    MANGROVE_COAST("mangrove_coast"),

    BARE_PEAKS("bare_peaks"),
    VOLCANIC_SLOPE("volcanic_slope"),

    OCEAN("ocean"),
    COLD_OCEAN("cold_ocean"),
    WARM_OCEAN("warm_ocean"),
    FROZEN_OCEAN("frozen_ocean");

    private final String id;
    private ResourceKey<Biome> key;

    MantleBiomes(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public ResourceKey<Biome> key() {
        ResourceKey<Biome> k = key;
        if (k == null) {
            k = ResourceKey.create(Registries.BIOME,
                    ResourceLocation.fromNamespaceAndPath(Mantle.MOD_ID, id));
            key = k;
        }
        return k;
    }
}
