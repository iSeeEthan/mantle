package com.iseeethan.mantle.world.gen;

import com.iseeethan.mantle.block.MantleBlocks;
import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.tectonics.Strata;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class MantleChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MantleChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
                    MantleGenConfig.CODEC.optionalFieldOf("config", new MantleGenConfig()).forGetter(g -> g.config)
            ).apply(instance, MantleChunkGenerator::new));

    private static final int SEA_LEVEL = MantleWorld.SEA_Y;

    private final MantleGenConfig config;
    private final boolean debug;

    private final BlockState stone = Blocks.STONE.defaultBlockState();
    private final BlockState water = Blocks.WATER.defaultBlockState();
    private final BlockState grassBlock = Blocks.GRASS_BLOCK.defaultBlockState();

    private volatile java.util.EnumMap<Strata.Rock, BlockState> rockStates;
    private volatile java.util.EnumMap<com.iseeethan.mantle.world.gen.tectonics.Soil.Type, BlockState> soilStates;
    private volatile java.util.Map<String, BlockState> oreStates;
    private volatile BlockState deepslateState;
    private volatile OreVeins oreVeins;
    private volatile long oreVeinsSeed = Long.MIN_VALUE;


    private BlockState rockState(Strata.Rock rock) {
        java.util.EnumMap<Strata.Rock, BlockState> map = rockStates;
        if (map == null) {
            map = new java.util.EnumMap<>(Strata.Rock.class);
            for (Strata.Rock r : Strata.Rock.values()) {
                map.put(r, MantleBlocks.forRock(r).defaultBlockState());
            }
            rockStates = map;
        }
        return map.get(rock);
    }

    private BlockState oreState(MantleBlocks.Host host, MantleBlocks.Ore ore) {
        java.util.Map<String, BlockState> map = oreStates;
        if (map == null) {
            map = new java.util.HashMap<>();
            for (MantleBlocks.Host h : MantleBlocks.Host.values()) {
                for (MantleBlocks.Ore o : MantleBlocks.Ore.values()) {
                    map.put(h.name() + "_" + o.name(), MantleBlocks.forOre(h, o).defaultBlockState());
                }
            }
            oreStates = map;
        }
        return map.get(host.name() + "_" + ore.name());
    }

    private BlockState deepslateState() {
        BlockState s = deepslateState;
        if (s == null) {
            s = MantleBlocks.deepslate().defaultBlockState();
            deepslateState = s;
        }
        return s;
    }

    private OreVeins oreVeins() {
        long seed = worldSeed.get();
        OreVeins v = oreVeins;
        if (v == null || oreVeinsSeed != seed) {
            v = new OreVeins(seed, config.oreDensity);
            oreVeins = v;
            oreVeinsSeed = seed;
        }
        return v;
    }

    private BlockState soilState(com.iseeethan.mantle.world.gen.tectonics.Soil.Type type) {
        java.util.EnumMap<com.iseeethan.mantle.world.gen.tectonics.Soil.Type, BlockState> map = soilStates;
        if (map == null) {
            map = new java.util.EnumMap<>(com.iseeethan.mantle.world.gen.tectonics.Soil.Type.class);
            for (com.iseeethan.mantle.world.gen.tectonics.Soil.Type t : com.iseeethan.mantle.world.gen.tectonics.Soil.Type.values()) {
                map.put(t, MantleBlocks.forSoil(t).defaultBlockState());
            }
            soilStates = map;
        }
        return map.get(type);
    }

    private final AtomicLong worldSeed = new AtomicLong(Long.MIN_VALUE);
    private volatile MantleWorld world;
    private volatile com.iseeethan.mantle.world.noise.GradientNoise coverNoise;
    private volatile long coverNoiseSeed = Long.MIN_VALUE;

    public MantleChunkGenerator(BiomeSource biomeSource) {
        this(biomeSource, new MantleGenConfig());
    }

    public MantleChunkGenerator(BiomeSource biomeSource, boolean debug) {
        this(biomeSource, debugConfig(debug));
    }

    public MantleChunkGenerator(BiomeSource biomeSource, MantleGenConfig config) {
        super(biomeSource);
        this.config = config;
        this.debug = config.debug;
    }

    private static MantleGenConfig debugConfig(boolean debug) {
        MantleGenConfig c = new MantleGenConfig();
        c.debug = debug;
        return c;
    }

    public MantleGenConfig config() {
        return config;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSetLookup,
                                                    RandomState randomState, long seed) {
        bindSeed(seed);
        return ChunkGeneratorStructureState.createForFlat(randomState, seed, this.biomeSource, java.util.stream.Stream.empty());
    }

    public void bindSeed(long seed) {
        if (worldSeed.getAndSet(seed) != seed || world == null) {
            world = MantleWorld.forSeed(seed, config.toParams());
        }
    }

    private MantleWorld world() {
        MantleWorld w = world;
        return w != null ? w : (world = MantleWorld.get());
    }

    private static final double LOWLAND_MAX_SLOPE = 1.08;
    private static final double ALPINE_MAX_SLOPE = 0.40;
    private static final double CLIFF_SLOPE = 1.25;
    private static final int VEGETATION_LINE = 480;
    private static final int VEGETATION_FADE = 190;
    private static final double TRANSITION_BAND = 0.34;

    private final ThreadLocal<com.iseeethan.mantle.world.gen.tectonics.Soil.Sample> soilTL =
            ThreadLocal.withInitial(com.iseeethan.mantle.world.gen.tectonics.Soil.Sample::new);

    private int soilTop(MantleWorld w, int wx, int wz, int stoneTop, boolean hasWater) {
        if (debug) return Integer.MIN_VALUE;
        if (hasWater) return Integer.MIN_VALUE;
        if (stoneTop < SEA_LEVEL + 1) return Integer.MIN_VALUE;

        double slope = w.sim().macroSlope(wx, wz);
        if (slope > CLIFF_SLOPE) return Integer.MIN_VALUE;

        double alpine = (stoneTop - VEGETATION_LINE) / (double) VEGETATION_FADE;
        alpine = alpine < 0 ? 0 : (alpine > 1 ? 1 : alpine);
        double maxSlope = LOWLAND_MAX_SLOPE + (ALPINE_MAX_SLOPE - LOWLAND_MAX_SLOPE) * alpine;

        double grassiness = (maxSlope - slope) / TRANSITION_BAND + 0.5;
        if (grassiness >= 1.0) return stoneTop;
        if (grassiness <= 0.0) return Integer.MIN_VALUE;

        return grassiness > coverThreshold(w, wx, wz) ? stoneTop : Integer.MIN_VALUE;
    }

    private double coverThreshold(MantleWorld w, int wx, int wz) {
        long seed = w.sim().seed();
        com.iseeethan.mantle.world.noise.GradientNoise n = coverNoise;
        if (n == null || coverNoiseSeed != seed) {
            n = new com.iseeethan.mantle.world.noise.GradientNoise(seed ^ 0x3C6EF372FE94F82AL);
            coverNoise = n;
            coverNoiseSeed = seed;
        }
        double v = n.fbm(wx / 22.0, wz / 22.0, 3, 2.0, 0.5);
        double t = 0.5 + 0.5 * v;
        return t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t);
    }

    private final ThreadLocal<Strata.Column> strataColTL =
            ThreadLocal.withInitial(Strata.Column::new);
    private final ThreadLocal<com.iseeethan.mantle.world.gen.tectonics.Caves.Column> cavesColTL =
            ThreadLocal.withInitial(com.iseeethan.mantle.world.gen.tectonics.Caves.Column::new);

    private BlockState surfaceState(MantleWorld w, int wx, int wz, int y, int stoneTop, int soilTop,
                                    Strata.Column strataCol, int soilDepth,
                                    com.iseeethan.mantle.world.gen.tectonics.Soil.Type soilType) {
        if (debug) return stone;
        if (soilTop != Integer.MIN_VALUE && y <= soilTop) {
            if (y == soilTop) {
                return grassBlock;
            }
            if (y > soilTop - soilDepth) {
                return soilState(soilType);
            }
        }
        Strata.Rock rock = w.strata().typeAt(strataCol, y);
        boolean deep = y < config.oreDeepslateLevel;
        MantleBlocks.Host host = deep ? MantleBlocks.Host.DEEPSLATE : MantleBlocks.Host.of(rock);
        MantleBlocks.Ore ore = oreVeins().oreAt(wx, y, wz);
        if (ore != null) {
            return oreState(host, ore);
        }
        return deep ? deepslateState() : rockState(rock);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();

        MantleWorld w = world();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        MantleWorld.Column col = new MantleWorld.Column();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                if (!w.inWorld(wx, wz)) {
                    continue;
                }
                w.column(wx, wz, col);
                int stoneTop = col.stoneTop;
                int waterTop = col.hasWater ? col.waterTop : stoneTop;

                int soilTop = soilTop(w, wx, wz, stoneTop, col.hasWater);

                double surfaceY = w.sim().surfaceY(wx, wz);
                Strata.Column strataCol = w.strata().column(wx, wz, surfaceY, strataColTL.get());
                var cavesCol = w.caves().column(wx, wz, stoneTop, surfaceY, cavesColTL.get());

                int soilDepth = 1;
                com.iseeethan.mantle.world.gen.tectonics.Soil.Type soilType =
                        com.iseeethan.mantle.world.gen.tectonics.Soil.Type.LOAM;
                if (soilTop != Integer.MIN_VALUE) {
                    var s = w.sim().soil().sample(wx, wz, stoneTop, soilTL.get());
                    soilDepth = Math.max(1, s.depth);
                    soilType = s.type;
                }

                for (int y = minY; y <= Math.max(stoneTop, waterTop); y++) {
                    BlockState state;
                    if (y <= stoneTop) {
                        if (config.enableCaves && w.caves().carved(cavesCol, y)) {
                            continue;
                        }
                        state = surfaceState(w, wx, wz, y, stoneTop, soilTop, strataCol, soilDepth, soilType);
                    } else if (y <= waterTop) {
                        state = water;
                    } else {
                        continue;
                    }
                    chunk.setBlockState(pos.set(lx, y, lz), state, false);
                    oceanFloor.update(lx, y, lz, state);
                    worldSurface.update(lx, y, lz, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        MantleWorld w = world();
        if (!w.inWorld(x, z)) {
            return level.getMinBuildHeight();
        }
        int top = w.solidTopY(x, z);
        return Math.max(top + 1, SEA_LEVEL + 1);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        MantleWorld w = world();
        boolean in = w.inWorld(x, z);
        int top = in ? w.solidTopY(x, z) : Integer.MIN_VALUE;
        BlockState air = Blocks.AIR.defaultBlockState();
        int height = level.getHeight();
        int minY = level.getMinBuildHeight();
        BlockState[] column = new BlockState[height];
        for (int i = 0; i < height; i++) {
            int y = minY + i;
            if (y <= top) {
                column[i] = stone;
            } else if (in && y <= SEA_LEVEL) {
                column[i] = water;
            } else {
                column[i] = air;
            }
        }
        return new NoiseColumn(minY, column);
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return MantleWorld.MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return MantleWorld.HEIGHT;
    }

    private final ThreadLocal<com.iseeethan.mantle.world.gen.tectonics.Flora.Cover> coverTL =
            ThreadLocal.withInitial(com.iseeethan.mantle.world.gen.tectonics.Flora.Cover::new);
    private final ThreadLocal<com.iseeethan.mantle.world.gen.tectonics.Soil.Sample> vegSoilTL =
            ThreadLocal.withInitial(com.iseeethan.mantle.world.gen.tectonics.Soil.Sample::new);
    private final ThreadLocal<com.iseeethan.mantle.world.gen.tectonics.Climate.Sample> climateTL =
            ThreadLocal.withInitial(com.iseeethan.mantle.world.gen.tectonics.Climate.Sample::new);

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
        if (debug) return;

        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        BlockState grass = grassBlock;

        MantleWorld w = world();
        var sim = w.sim();
        var flora = sim.flora();
        var climate = sim.climate();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                if (!w.inWorld(wx, wz)) continue;

                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
                pos.set(lx, top, lz);
                if (!chunk.getBlockState(pos).is(grass.getBlock())) continue;
                if (top + 1 >= chunk.getMaxBuildHeight()) continue;

                var soil = sim.soil().sample(wx, wz, top, vegSoilTL.get());
                var cs = climate.sample(wx, wz, climateTL.get());
                double rain = cs.rainfall;
                double temp = cs.temperature;
                double slope = sim.macroSlope(wx, wz);
                int flow = sim.flowAccumAt(wx, wz);

                var cover = flora.cover(wx, wz, top, soil, rain, temp, slope,
                        Math.min(1.0, flow / 80.0), coverTL.get());

                Vegetation.undergrowth(region, wx, wz, top, cover);
            }
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        if (debug) return;

        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();

        MantleWorld w = world();
        var sim = w.sim();
        var flora = sim.flora();
        var climate = sim.climate();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = chunkX + lx;
                int wz = chunkZ + lz;
                if (!w.inWorld(wx, wz)) continue;

                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
                if (top + 1 >= chunk.getMaxBuildHeight()) continue;
                pos.set(lx, top, lz);
                if (!chunk.getBlockState(pos).is(grassBlock.getBlock())) continue;

                var soil = sim.soil().sample(wx, wz, top, vegSoilTL.get());
                var cs = climate.sample(wx, wz, climateTL.get());
                double slope = sim.macroSlope(wx, wz);
                int flow = sim.flowAccumAt(wx, wz);

                var cover = flora.cover(wx, wz, top, soil, cs.rainfall, cs.temperature, slope,
                        Math.min(1.0, flow / 80.0), coverTL.get());

                Vegetation.tree(level, wx, wz, top, cover);
            }
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {

    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {

    }

    @Override
    public void addDebugScreenInfo(java.util.List<String> info, RandomState random, BlockPos pos) {

    }
}
