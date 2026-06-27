package com.iseeethan.mantle.world.gen.biome;

import com.iseeethan.mantle.world.gen.tectonics.Climate;
import com.iseeethan.mantle.world.gen.tectonics.PlateSim;
import com.iseeethan.mantle.world.gen.tectonics.Soil;

public final class MantleBiomeClassifier {

    private final PlateSim sim;
    private final double seaY;
    private final double shoreY;

    private final ThreadLocal<Climate.Sample> climateTL = ThreadLocal.withInitial(Climate.Sample::new);
    private final ThreadLocal<Soil.Sample> soilTL = ThreadLocal.withInitial(Soil.Sample::new);

    public MantleBiomeClassifier(PlateSim sim, int seaY, int shoreBand) {
        this.sim = sim;
        this.seaY = seaY;
        this.shoreY = seaY + shoreBand;
    }

    public MantleBiomes classify(int wx, int wz) {
        double surfaceY = sim.surfaceY(wx, wz);

        if (surfaceY < seaY) {
            return ocean(wx, wz, surfaceY);
        }

        Climate.Sample c = sim.climate().sample(wx, wz, climateTL.get());
        double t = c.temperature;
        double r = c.rainfall;
        double cont = c.continentality;
        double elevAbove = surfaceY - seaY;
        double slope = sim.macroSlope(wx, wz);
        int flow = sim.flowAccumAt(wx, wz);
        Soil.Sample soil = sim.soil().sample(wx, wz, surfaceY, soilTL.get());

        boolean coastal = surfaceY < shoreY && cont < 0.18;
        boolean wet = soil.wetness > 0.72 || flow > 220;
        boolean valley = flow > 140 && slope < 0.35;

        if (elevAbove > 680) {
            if (slope > 0.85 || soil.depth < 2) return MantleBiomes.BARE_PEAKS;
            if (t < 0.32) return MantleBiomes.SNOWY_PEAKS;
            if (t > 0.58 && soil.type == Soil.Type.STONY) return MantleBiomes.VOLCANIC_SLOPE;
            return MantleBiomes.MONTANE_FOREST;
        }
        if (elevAbove > 430) {
            if (t < 0.28) return MantleBiomes.SNOWY_PEAKS;
            if (t < 0.42) return MantleBiomes.ALPINE_MEADOW;
            if (r < 0.4) return MantleBiomes.HIGHLAND_HEATH;
            if (t > 0.6 && slope > 0.7 && soil.type == Soil.Type.STONY) return MantleBiomes.VOLCANIC_SLOPE;
            return MantleBiomes.MONTANE_FOREST;
        }

        if (t < 0.16) {
            if (coastal) return MantleBiomes.COLD_ROCKY_COAST;
            if (elevAbove > 380) return MantleBiomes.GLACIER;
            if (r < 0.3) return MantleBiomes.FROZEN_BARRENS;
            if (r < 0.55) return MantleBiomes.SNOWY_TUNDRA;
            return MantleBiomes.SNOWY_TAIGA;
        }

        if (t < 0.38) {
            if (coastal) return MantleBiomes.COLD_ROCKY_COAST;
            if (wet) return MantleBiomes.BOREAL_WETLAND;
            if (r < 0.3) return MantleBiomes.STEPPE;
            if (r < 0.52) return MantleBiomes.TAIGA;
            return MantleBiomes.BOREAL_FOREST;
        }

        if (t < 0.62) {
            if (coastal) return MantleBiomes.TEMPERATE_COAST;
            if (valley) return MantleBiomes.RIVER_VALLEY;
            if (wet) return MantleBiomes.MARSH;
            if (r < 0.22) return MantleBiomes.STEPPE;
            if (r < 0.38) return MantleBiomes.GRASSLAND;
            if (r < 0.5) return MantleBiomes.MEADOW;
            if (r < 0.64) return soil.type == Soil.Type.STONY ? MantleBiomes.OAK_WOODLAND : MantleBiomes.BIRCH_FOREST;
            if (r < 0.8) return MantleBiomes.TEMPERATE_FOREST;
            return MantleBiomes.TEMPERATE_RAINFOREST;
        }

        if (coastal) return MantleBiomes.MANGROVE_COAST;
        if (slope > 0.62 && soil.type == Soil.Type.STONY) return MantleBiomes.VOLCANIC_SLOPE;
        if (r < 0.3) {
            if (soil.type == Soil.Type.STONY || slope > 0.5 || soil.depth <= 1) return MantleBiomes.ROCKY_DESERT;
            if (soil.type == Soil.Type.SANDY) return MantleBiomes.SAND_DESERT;
            return MantleBiomes.BADLANDS;
        }
        if (r < 0.42) return MantleBiomes.BADLANDS;
        if (wet) return MantleBiomes.TROPICAL_WETLAND;
        if (r < 0.5) return MantleBiomes.SAVANNA;
        if (r < 0.66) return MantleBiomes.SHRUBLAND;
        return MantleBiomes.TROPICAL_RAINFOREST;
    }

    private MantleBiomes ocean(int wx, int wz, double surfaceY) {
        double t = sim.climate().temperature(wx, wz);
        if (t < 0.16) return MantleBiomes.FROZEN_OCEAN;
        if (t < 0.4) return MantleBiomes.COLD_OCEAN;
        if (t > 0.72) return MantleBiomes.WARM_OCEAN;
        return MantleBiomes.OCEAN;
    }
}
