package com.iseeethan.mantle.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class MantleGenConfig {

    public boolean debug = false;

    public int seaLevel = 64;
    public int mountainHeight = 760;
    public double landCurve = 1.9;
    public double seaFraction = 0.52;
    public double continentScale = 1.0;
    public double erosionIntensity = 1.0;
    public double riverDensity = 1.0;

    public double temperatureScale = 1.0;
    public double rainfallScale = 1.0;
    public double polarColdness = 1.0;
    public double continentalDryness = 1.0;

    public double ruggedness = 1.0;
    public double sedimentStrength = 1.0;
    public double glacialStrength = 1.0;
    public double coastalStrength = 1.0;

    public boolean enableOceans = true;
    public boolean enableMountains = true;
    public boolean enableCaves = true;
    public boolean enableRivers = true;
    public boolean enableSediment = true;
    public boolean enableGlaciers = true;
    public boolean enableCoasts = true;

    public double oreDensity = 1.0;
    public int oreDeepslateLevel = 0;

    public MantleGenConfig() {}

    public MantleGenConfig copy() {
        MantleGenConfig c = new MantleGenConfig();
        c.debug = debug;
        c.seaLevel = seaLevel;
        c.mountainHeight = mountainHeight;
        c.landCurve = landCurve;
        c.seaFraction = seaFraction;
        c.continentScale = continentScale;
        c.erosionIntensity = erosionIntensity;
        c.riverDensity = riverDensity;
        c.temperatureScale = temperatureScale;
        c.rainfallScale = rainfallScale;
        c.polarColdness = polarColdness;
        c.continentalDryness = continentalDryness;
        c.ruggedness = ruggedness;
        c.sedimentStrength = sedimentStrength;
        c.glacialStrength = glacialStrength;
        c.coastalStrength = coastalStrength;
        c.enableOceans = enableOceans;
        c.enableMountains = enableMountains;
        c.enableCaves = enableCaves;
        c.enableRivers = enableRivers;
        c.enableSediment = enableSediment;
        c.enableGlaciers = enableGlaciers;
        c.enableCoasts = enableCoasts;
        c.oreDensity = oreDensity;
        c.oreDeepslateLevel = oreDeepslateLevel;
        return c;
    }

    public com.iseeethan.mantle.world.gen.tectonics.PlateSim.Params toParams() {
        com.iseeethan.mantle.world.gen.tectonics.PlateSim.Params p =
                new com.iseeethan.mantle.world.gen.tectonics.PlateSim.Params();
        p.seaFraction = seaFraction;
        p.landCurve = landCurve;
        p.mountainTopY = mountainHeight;
        p.continentScale = continentScale;
        p.erosionIntensity = enableMountains ? erosionIntensity : 0.0;
        p.riverDensity = enableRivers ? riverDensity : 0.0;
        p.temperatureScale = temperatureScale;
        p.rainfallScale = rainfallScale;
        p.polarColdness = polarColdness;
        p.continentalDryness = continentalDryness;
        p.ruggedness = ruggedness;
        p.sedimentStrength = enableSediment ? sedimentStrength : 0.0;
        p.glacialStrength = enableGlaciers ? glacialStrength : 0.0;
        p.coastalStrength = enableCoasts ? coastalStrength : 0.0;
        return p;
    }

    public void clampAll() {
        seaLevel = clampI(seaLevel, 0, 200);
        mountainHeight = clampI(mountainHeight, 120, 1000);
        landCurve = clampD(landCurve, 0.5, 4.0);
        seaFraction = clampD(seaFraction, 0.1, 0.9);
        continentScale = clampD(continentScale, 0.3, 3.0);
        erosionIntensity = clampD(erosionIntensity, 0.0, 3.0);
        riverDensity = clampD(riverDensity, 0.0, 3.0);
        temperatureScale = clampD(temperatureScale, 0.2, 3.0);
        rainfallScale = clampD(rainfallScale, 0.2, 3.0);
        polarColdness = clampD(polarColdness, 0.0, 2.0);
        continentalDryness = clampD(continentalDryness, 0.0, 2.0);
        ruggedness = clampD(ruggedness, 0.0, 3.0);
        sedimentStrength = clampD(sedimentStrength, 0.0, 3.0);
        glacialStrength = clampD(glacialStrength, 0.0, 3.0);
        coastalStrength = clampD(coastalStrength, 0.0, 3.0);
        oreDensity = clampD(oreDensity, 0.0, 4.0);
        oreDeepslateLevel = clampI(oreDeepslateLevel, -64, 128);
    }

    private static final Codec<MantleGenConfig> TERRAIN = RecordCodecBuilder.create(in -> in.group(
            Codec.BOOL.optionalFieldOf("debug", false).forGetter(c -> c.debug),
            Codec.INT.optionalFieldOf("sea_level", 64).forGetter(c -> c.seaLevel),
            Codec.INT.optionalFieldOf("mountain_height", 760).forGetter(c -> c.mountainHeight),
            Codec.DOUBLE.optionalFieldOf("land_curve", 1.9).forGetter(c -> c.landCurve),
            Codec.DOUBLE.optionalFieldOf("sea_fraction", 0.52).forGetter(c -> c.seaFraction),
            Codec.DOUBLE.optionalFieldOf("continent_scale", 1.0).forGetter(c -> c.continentScale),
            Codec.DOUBLE.optionalFieldOf("erosion_intensity", 1.0).forGetter(c -> c.erosionIntensity),
            Codec.DOUBLE.optionalFieldOf("river_density", 1.0).forGetter(c -> c.riverDensity),
            Codec.DOUBLE.optionalFieldOf("ruggedness", 1.0).forGetter(c -> c.ruggedness)
    ).apply(in, (debug, sea, mh, lc, sf, cs, ei, rd, rug) -> {
        MantleGenConfig c = new MantleGenConfig();
        c.debug = debug; c.seaLevel = sea; c.mountainHeight = mh; c.landCurve = lc;
        c.seaFraction = sf; c.continentScale = cs; c.erosionIntensity = ei; c.riverDensity = rd;
        c.ruggedness = rug;
        return c;
    }));

    private static final Codec<MantleGenConfig> CLIMATE = RecordCodecBuilder.create(in -> in.group(
            Codec.DOUBLE.optionalFieldOf("temperature_scale", 1.0).forGetter(c -> c.temperatureScale),
            Codec.DOUBLE.optionalFieldOf("rainfall_scale", 1.0).forGetter(c -> c.rainfallScale),
            Codec.DOUBLE.optionalFieldOf("polar_coldness", 1.0).forGetter(c -> c.polarColdness),
            Codec.DOUBLE.optionalFieldOf("continental_dryness", 1.0).forGetter(c -> c.continentalDryness)
    ).apply(in, (ts, rs, pc, cd) -> {
        MantleGenConfig c = new MantleGenConfig();
        c.temperatureScale = ts; c.rainfallScale = rs; c.polarColdness = pc; c.continentalDryness = cd;
        return c;
    }));

    private static final Codec<MantleGenConfig> GEOMORPH = RecordCodecBuilder.create(in -> in.group(
            Codec.DOUBLE.optionalFieldOf("sediment_strength", 1.0).forGetter(c -> c.sedimentStrength),
            Codec.DOUBLE.optionalFieldOf("glacial_strength", 1.0).forGetter(c -> c.glacialStrength),
            Codec.DOUBLE.optionalFieldOf("coastal_strength", 1.0).forGetter(c -> c.coastalStrength),
            Codec.BOOL.optionalFieldOf("enable_sediment", true).forGetter(c -> c.enableSediment),
            Codec.BOOL.optionalFieldOf("enable_glaciers", true).forGetter(c -> c.enableGlaciers),
            Codec.BOOL.optionalFieldOf("enable_coasts", true).forGetter(c -> c.enableCoasts)
    ).apply(in, (ss, gs, cs, es, eg, ec) -> {
        MantleGenConfig c = new MantleGenConfig();
        c.sedimentStrength = ss; c.glacialStrength = gs; c.coastalStrength = cs;
        c.enableSediment = es; c.enableGlaciers = eg; c.enableCoasts = ec;
        return c;
    }));

    private static final Codec<MantleGenConfig> FEATURES = RecordCodecBuilder.create(in -> in.group(
            Codec.BOOL.optionalFieldOf("enable_oceans", true).forGetter(c -> c.enableOceans),
            Codec.BOOL.optionalFieldOf("enable_mountains", true).forGetter(c -> c.enableMountains),
            Codec.BOOL.optionalFieldOf("enable_caves", true).forGetter(c -> c.enableCaves),
            Codec.BOOL.optionalFieldOf("enable_rivers", true).forGetter(c -> c.enableRivers),
            Codec.DOUBLE.optionalFieldOf("ore_density", 1.0).forGetter(c -> c.oreDensity),
            Codec.INT.optionalFieldOf("ore_deepslate_level", 0).forGetter(c -> c.oreDeepslateLevel)
    ).apply(in, (eo, em, ec, er, od, odl) -> {
        MantleGenConfig c = new MantleGenConfig();
        c.enableOceans = eo; c.enableMountains = em; c.enableCaves = ec; c.enableRivers = er;
        c.oreDensity = od; c.oreDeepslateLevel = odl;
        return c;
    }));

    public static final Codec<MantleGenConfig> CODEC = RecordCodecBuilder.create(in -> in.group(
            TERRAIN.optionalFieldOf("terrain", new MantleGenConfig()).forGetter(c -> c),
            CLIMATE.optionalFieldOf("climate", new MantleGenConfig()).forGetter(c -> c),
            GEOMORPH.optionalFieldOf("geomorphology", new MantleGenConfig()).forGetter(c -> c),
            FEATURES.optionalFieldOf("features", new MantleGenConfig()).forGetter(c -> c)
    ).apply(in, (terrain, climate, geomorph, features) -> {
        MantleGenConfig c = terrain.copy();
        c.temperatureScale = climate.temperatureScale;
        c.rainfallScale = climate.rainfallScale;
        c.polarColdness = climate.polarColdness;
        c.continentalDryness = climate.continentalDryness;
        c.sedimentStrength = geomorph.sedimentStrength;
        c.glacialStrength = geomorph.glacialStrength;
        c.coastalStrength = geomorph.coastalStrength;
        c.enableSediment = geomorph.enableSediment;
        c.enableGlaciers = geomorph.enableGlaciers;
        c.enableCoasts = geomorph.enableCoasts;
        c.enableOceans = features.enableOceans;
        c.enableMountains = features.enableMountains;
        c.enableCaves = features.enableCaves;
        c.enableRivers = features.enableRivers;
        c.oreDensity = features.oreDensity;
        c.oreDeepslateLevel = features.oreDeepslateLevel;
        c.clampAll();
        return c;
    }));

    private static int clampI(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
    private static double clampD(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
