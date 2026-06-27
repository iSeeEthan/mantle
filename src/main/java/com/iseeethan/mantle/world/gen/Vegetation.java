package com.iseeethan.mantle.world.gen;

import com.iseeethan.mantle.world.gen.tectonics.Flora;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class Vegetation {

    private Vegetation() {}

    private static final int SET_FLAGS = 2;

    private static final long TREE_SALT = 0x9E37L;

    public static void tree(WorldGenLevel level, int wx, int wz, int surfaceY, Flora.Cover cover) {
        if (cover.tree == Flora.TreeKind.NONE || cover.treeDensity <= 0) return;
        if (!treeRoll(level, wx, wz, cover.treeDensity)) return;
        placeTree(level, wx, surfaceY + 1, wz, cover, hash(wx, wz, TREE_SALT));
    }

    public static void undergrowth(WorldGenLevel level, int wx, int wz, int surfaceY, Flora.Cover cover) {
        if (cover.tree != Flora.TreeKind.NONE && cover.treeDensity > 0 && treeRoll(level, wx, wz, cover.treeDensity)) {
            return;
        }
        placeUndergrowth(level, wx, wz, surfaceY, cover, hash(wx, wz, 0x1L));
    }

    private static int spacingFor(double density) {
        if (density <= 0) return 64;
        double area = 1.0 / density;
        int s = (int) Math.round(Math.sqrt(area) * 0.9);
        return Math.max(3, Math.min(12, s));
    }

    private static boolean treeRoll(WorldGenLevel level, int wx, int wz, double density) {
        int spacing = spacingFor(density);
        int gx = Math.floorDiv(wx, spacing);
        int gz = Math.floorDiv(wz, spacing);
        long h = hash(gx, gz, 0xA11CE);
        int jx = (int) (h % spacing);
        int jz = (int) ((h >>> 16) % spacing);
        if (Math.floorMod(wx, spacing) != jx || Math.floorMod(wz, spacing) != jz) return false;
        double r = ((h >>> 32) & 0xFFFF) / 65535.0;
        double cellArea = spacing * spacing;
        return r < Math.min(0.96, density * cellArea);
    }

    private static void placeUndergrowth(WorldGenLevel level, int wx, int wz, int surfaceY,
                                         Flora.Cover cover, long h) {
        int plantY = surfaceY + 1;
        BlockPos pos = new BlockPos(wx, plantY, wz);
        if (!level.getBlockState(pos).isAir()) return;

        double roll = (h & 0xFFFF) / 65535.0;
        double r2 = ((h >>> 16) & 0xFFFF) / 65535.0;

        double bushCut = cover.bushDensity;
        double grassCut = bushCut + cover.grassDensity;
        double fernCut = grassCut + cover.fernDensity;
        double flowerCut = fernCut + cover.flowerDensity;

        if (roll < bushCut) {
            placeBush(level, pos, cover, h);
        } else if (roll < grassCut) {
            boolean tall = r2 < 0.18;
            if (tall && placeDouble(level, pos, Blocks.TALL_GRASS)) return;
            set(level, pos, Blocks.SHORT_GRASS.defaultBlockState());
        } else if (roll < fernCut) {
            boolean large = r2 < 0.2;
            if (large && placeDouble(level, pos, Blocks.LARGE_FERN)) return;
            set(level, pos, Blocks.FERN.defaultBlockState());
        } else if (roll < flowerCut) {
            set(level, pos, flower(cover.biome, r2).defaultBlockState());
        }
    }

    private static void placeBush(WorldGenLevel level, BlockPos pos, Flora.Cover cover, long h) {
        switch (cover.biome) {
            case DESERT:
            case BARREN:
            case STEPPE:
                set(level, pos, Blocks.DEAD_BUSH.defaultBlockState());
                break;
            case SHRUBLAND:
            case TEMPERATE_FOREST:
            case TAIGA:
            case BOREAL_FOREST:
                if ((h & 0x3) == 0) {
                    set(level, pos, Blocks.SWEET_BERRY_BUSH.defaultBlockState()
                            .setValue(SweetBerryBushBlock.AGE, 2 + (int) ((h >>> 4) & 1)));
                } else {
                    leafBush(level, pos, cover);
                }
                break;
            default:
                set(level, pos, Blocks.SHORT_GRASS.defaultBlockState());
        }
    }

    private static void leafBush(WorldGenLevel level, BlockPos pos, Flora.Cover cover) {
        BlockState leaf = leavesFor(cover.tree, true);
        set(level, pos, leaf);
        if (level.getBlockState(pos.above()).isAir() && (pos.getX() ^ pos.getZ()) % 3 == 0) {
            set(level, pos.above(), leaf);
        }
    }

    private static boolean placeDouble(WorldGenLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        BlockPos up = pos.above();
        if (!level.getBlockState(up).isAir()) return false;
        net.minecraft.world.level.block.DoublePlantBlock dp =
                (net.minecraft.world.level.block.DoublePlantBlock) block;
        net.minecraft.world.level.block.DoublePlantBlock.placeAt(level, dp.defaultBlockState(), pos, SET_FLAGS);
        return true;
    }

    private static void placeTree(WorldGenLevel level, int x, int baseY, int z, Flora.Cover cover, long h) {
        Flora.TreeKind kind = cover.tree;
        double scale = cover.canopyScale;

        if (kind == Flora.TreeKind.SCRUB) {
            placeScrub(level, x, baseY, z, cover, h);
            return;
        }

        BlockState log = logFor(kind);
        BlockState leaves = leavesFor(kind, false);

        int height = treeHeight(kind, cover, h);
        if (!groundOk(level, x, baseY, z)) return;

        boolean acacia = kind == Flora.TreeKind.ACACIA;
        boolean conifer = kind == Flora.TreeKind.CONIFER;
        int trunkH = (acacia || conifer) ? height : Math.max(2, height - 1);

        for (int i = 0; i < trunkH; i++) {
            BlockPos lp = new BlockPos(x, baseY + i, z);
            if (!canGrow(level, lp)) {
                if (i < 2) return;
                trunkH = i;
                height = i;
                break;
            }
            set(level, lp, log);
        }

        int topY = baseY + height - 1;
        switch (kind) {
            case CONIFER:
                conifer(level, x, topY, z, leaves, scale);
                break;
            case JUNGLE:
                crown(level, x, topY, z, leaves, scale * 1.15, height, h);
                jungleVines(level, x, topY, z, h);
                break;
            case ACACIA:
                acacia(level, x, baseY, topY, z, log, leaves, h);
                break;
            case BIRCH:
                crown(level, x, topY, z, leaves, scale * 0.8, height, h);
                break;
            default:
                crown(level, x, topY, z, leaves, scale, height, h);
        }
    }

    private static int treeHeight(Flora.TreeKind kind, Flora.Cover cover, long h) {
        int base;
        switch (kind) {
            case CONIFER: base = 9; break;
            case JUNGLE: base = 12; break;
            case ACACIA: base = 6; break;
            case BIRCH: base = 7; break;
            default: base = 7;
        }
        double depthDrive = (cover.soilDepth - 1) * 1.6;
        double rainDrive = cover.rainfall * 5.0;
        double vigorDrive = cover.vigor * 2.0;
        double jitter = (h & 0xFFFF) / 65535.0 * 4.0 - 1.5;
        int height = (int) Math.round(base + depthDrive + rainDrive + vigorDrive + jitter);
        return Math.max(5, height);
    }

    private static void conifer(WorldGenLevel level, int x, int topY, int z, BlockState leaves, double scale) {
        int layers = Math.max(3, (int) Math.round(4 + scale * 3));
        int radius = Math.max(2, (int) Math.round(2 + scale));
        for (int l = 0; l < layers; l++) {
            int ly = topY - 1 - l;
            int r = (int) Math.round((double) l / layers * radius);
            disc(level, x, ly, z, leaves, r);
        }
        set(level, new BlockPos(x, topY, z), leaves);
        set(level, new BlockPos(x, topY + 1, z), leaves);
    }

    private static void crown(WorldGenLevel level, int x, int topY, int z, BlockState leaves,
                              double scale, int height, long h) {
        int maxR = Math.max(2, (int) Math.round(2 + scale * 1.5));
        int crownH = Math.max(4, Math.min(height - 1, (int) Math.round(4 + scale * 3)));
        int bottomY = topY - crownH + 2;

        for (int dy = 0; dy < crownH; dy++) {
            int y = bottomY + dy;
            double f = (double) dy / (crownH - 1);
            double profile;
            if (f < 0.3) {
                profile = 0.5 + (f / 0.3) * 0.5;
            } else {
                profile = 1.0 - Math.pow((f - 0.3) / 0.7, 1.7);
            }
            int r = (int) Math.round(maxR * profile);
            if (dy == crownH - 1) r = 0;
            long rowSalt = h ^ (y * 0x9E3779B97F4A7C15L);
            irregularDisc(level, x, y, z, leaves, r, rowSalt);
        }
    }

    private static void acacia(WorldGenLevel level, int x, int baseY, int topY, int z,
                               BlockState log, BlockState leaves, long h) {
        int dir = (int) (h & 0x3);
        int dx = dir == 0 ? 1 : dir == 1 ? -1 : 0;
        int dz = dir == 2 ? 1 : dir == 3 ? -1 : 0;
        int lean = 2;
        int cx = x + dx * lean;
        int cz = z + dz * lean;
        for (int i = 1; i <= lean; i++) {
            set(level, new BlockPos(x + dx * i, topY - lean + i, z + dz * i), log);
        }
        disc(level, cx, topY + 1, z + dz * lean, leaves, 3);
        disc(level, cx, topY + 2, cz, leaves, 2);
    }

    private static void placeScrub(WorldGenLevel level, int x, int baseY, int z, Flora.Cover cover, long h) {
        BlockState leaves = leavesFor(Flora.TreeKind.BROADLEAF, true);
        int hgt = 1 + (int) (h & 1);
        for (int i = 0; i < hgt; i++) {
            set(level, new BlockPos(x, baseY + i, z), Blocks.OAK_LOG.defaultBlockState());
        }
        disc(level, x, baseY + hgt, z, leaves, 1);
        set(level, new BlockPos(x, baseY + hgt, z), leaves);
    }

    private static void jungleVines(WorldGenLevel level, int x, int topY, int z, long h) {
        for (int i = 0; i < 3; i++) {
            int dx = ((h >>> (i * 4)) & 1) == 0 ? -1 : 1;
            BlockPos p = new BlockPos(x + dx * 2, topY - 1 - i, z);
            if (level.getBlockState(p).isAir()) {
                set(level, p, Blocks.JUNGLE_LEAVES.defaultBlockState()
                        .setValue(BlockStateProperties.PERSISTENT, true));
            }
        }
    }

    private static void disc(WorldGenLevel level, int cx, int cy, int cz, BlockState leaves, int r) {
        if (r <= 0) {
            leafAt(level, cx, cy, cz, leaves);
            return;
        }
        int r2 = r * r + r;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r2) continue;
                leafAt(level, cx + dx, cy, cz + dz, leaves);
            }
        }
    }

    private static void irregularDisc(WorldGenLevel level, int cx, int cy, int cz, BlockState leaves, int r, long salt) {
        if (r <= 0) {
            leafAt(level, cx, cy, cz, leaves);
            return;
        }
        int rInner = r * r;
        int rOuter = r * r + r;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > rOuter) continue;
                if (d2 > rInner) {
                    long e = hash(cx + dx, cz + dz, salt);
                    if ((e & 0x3) == 0) continue;
                }
                leafAt(level, cx + dx, cy, cz + dz, leaves);
            }
        }
    }

    private static void leafAt(WorldGenLevel level, int x, int y, int z, BlockState leaves) {
        BlockPos p = new BlockPos(x, y, z);
        BlockState cur = level.getBlockState(p);
        if (cur.isAir() || cur.is(Blocks.SHORT_GRASS) || cur.is(Blocks.FERN)) {
            set(level, p, leaves);
        }
    }

    private static boolean groundOk(WorldGenLevel level, int x, int y, int z) {
        BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
        return !below.isAir() && !below.is(Blocks.WATER);
    }

    private static boolean canGrow(WorldGenLevel level, BlockPos p) {
        if (p.getY() >= level.getMaxBuildHeight() - 1) return false;
        BlockState s = level.getBlockState(p);
        return s.isAir() || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.FERN) || s.canBeReplaced();
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return;
        level.setBlock(pos, state, SET_FLAGS);
    }

    private static BlockState logFor(Flora.TreeKind kind) {
        net.minecraft.world.level.block.Block b;
        switch (kind) {
            case CONIFER: b = Blocks.SPRUCE_LOG; break;
            case JUNGLE: b = Blocks.JUNGLE_LOG; break;
            case ACACIA: b = Blocks.ACACIA_LOG; break;
            case BIRCH: b = Blocks.BIRCH_LOG; break;
            default: b = Blocks.OAK_LOG;
        }
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }

    private static BlockState leavesFor(Flora.TreeKind kind, boolean persistentOnly) {
        net.minecraft.world.level.block.Block b;
        switch (kind) {
            case CONIFER: b = Blocks.SPRUCE_LEAVES; break;
            case JUNGLE: b = Blocks.JUNGLE_LEAVES; break;
            case ACACIA: b = Blocks.ACACIA_LEAVES; break;
            case BIRCH: b = Blocks.BIRCH_LEAVES; break;
            case SCRUB: b = Blocks.OAK_LEAVES; break;
            default: b = Blocks.OAK_LEAVES;
        }
        return b.defaultBlockState()
                .setValue(BlockStateProperties.PERSISTENT, true)
                .setValue(LeavesBlock.DISTANCE, 1);
    }

    private static net.minecraft.world.level.block.Block flower(Flora.Biome biome, double r) {
        switch (biome) {
            case GRASSLAND:
            case STEPPE:
                return r < 0.5 ? Blocks.DANDELION : Blocks.OXEYE_DAISY;
            case SAVANNA:
                return r < 0.5 ? Blocks.DANDELION : Blocks.AZURE_BLUET;
            case TEMPERATE_FOREST:
                return r < 0.4 ? Blocks.POPPY : (r < 0.7 ? Blocks.ALLIUM : Blocks.CORNFLOWER);
            case TAIGA:
            case BOREAL_FOREST:
                return Blocks.CORNFLOWER;
            case ALPINE_TUNDRA:
                return r < 0.5 ? Blocks.OXEYE_DAISY : Blocks.AZURE_BLUET;
            default:
                return r < 0.5 ? Blocks.DANDELION : Blocks.POPPY;
        }
    }

    private static long hash(int x, int z, long salt) {
        long h = (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0xD1B54A32D192ED03L);
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        return h & Long.MAX_VALUE;
    }
}
