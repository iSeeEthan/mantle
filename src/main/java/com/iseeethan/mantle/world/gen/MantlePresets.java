package com.iseeethan.mantle.world.gen;

import com.iseeethan.mantle.Mantle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public final class MantlePresets {

    public static final ResourceKey<WorldPreset> MANTLE =
            ResourceKey.create(Registries.WORLD_PRESET,
                    ResourceLocation.fromNamespaceAndPath(Mantle.MOD_ID, "mantle"));

    private MantlePresets() {}
}
