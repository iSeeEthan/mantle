package com.iseeethan.mantle.world.gen;

import com.iseeethan.mantle.world.MantleWorld;
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
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, MantleChunkGenerator::new));

    private static final int SEA_LEVEL = MantleWorld.SEA_Y;

    private final BlockState stone = Blocks.STONE.defaultBlockState();
    private final BlockState water = Blocks.WATER.defaultBlockState();

    private final AtomicLong worldSeed = new AtomicLong(Long.MIN_VALUE);
    private volatile MantleWorld world;

    public MantleChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
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
            world = MantleWorld.forSeed(seed);
        }
    }

    private MantleWorld world() {
        MantleWorld w = world;
        return w != null ? w : (world = MantleWorld.get());
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

                for (int y = minY; y <= Math.max(stoneTop, waterTop); y++) {
                    BlockState state;
                    if (y <= stoneTop) {
                        state = stone;
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

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {

    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {

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
