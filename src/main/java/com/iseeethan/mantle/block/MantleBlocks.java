package com.iseeethan.mantle.block;

import com.iseeethan.mantle.Mantle;
import com.iseeethan.mantle.world.gen.tectonics.Soil;
import com.iseeethan.mantle.world.gen.tectonics.Strata;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class MantleBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Mantle.MOD_ID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Mantle.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mantle.MOD_ID);

    private static final Map<Strata.Rock, Supplier<Block>> BY_ROCK =
            new EnumMap<>(Strata.Rock.class);

    private static final Map<Soil.Type, Supplier<Block>> BY_SOIL =
            new EnumMap<>(Soil.Type.class);

    public enum Ore { COPPER, IRON, COAL, GOLD }

    public enum Host {
        SEDIMENTARY, IGNEOUS, METAMORPHIC, BASEMENT, DEEPSLATE;

        public String id() { return name().toLowerCase(java.util.Locale.ROOT); }

        public static Host of(Strata.Rock rock) {
            switch (rock) {
                case SEDIMENTARY: return SEDIMENTARY;
                case IGNEOUS: return IGNEOUS;
                case METAMORPHIC: return METAMORPHIC;
                default: return BASEMENT;
            }
        }
    }

    private static final Map<String, Supplier<Block>> BY_NAME =
            new java.util.HashMap<>();

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    static {
        registerRock(Strata.Rock.SEDIMENTARY, "sedimentary_stone");
        registerRock(Strata.Rock.IGNEOUS, "igneous_stone");
        registerRock(Strata.Rock.METAMORPHIC, "metamorphic_stone");
        registerRock(Strata.Rock.BASEMENT, "basement_stone");
        registerVariant("deepslate_stone");

        for (Host host : Host.values()) {
            String base = host.id();
            registerVariant(base + "_cobblestone");
            registerVariant(base + "_granite");
            registerVariant(base + "_andesite");
        }

        for (Host host : Host.values()) {
            for (Ore ore : Ore.values()) {
                registerOre(host, ore);
            }
        }

        registerSoil(Soil.Type.LOAM, "loam_dirt");
        registerSoil(Soil.Type.SILT, "silt_dirt");
        registerSoil(Soil.Type.CLAY, "clay_dirt");
        registerSoil(Soil.Type.SANDY, "sandy_dirt");
        registerSoil(Soil.Type.STONY, "stony_dirt");
    }

    private static String baseName(Strata.Rock rock) {
        return rock.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static String oreName(Host host, Ore ore) {
        return host.id() + "_" + ore.name().toLowerCase(java.util.Locale.ROOT) + "_ore";
    }

    public static final Supplier<CreativeModeTab> TERRAIN_TAB = TABS.register("terrain", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mantle.terrain"))
                    .icon(() -> MantleBlocks.forRock(Strata.Rock.SEDIMENTARY).asItem().getDefaultInstance())
                    .displayItems((params, output) -> {
                        for (Supplier<? extends Item> item : TAB_ITEMS) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    private MantleBlocks() {}

    private static void registerRock(Strata.Rock rock, String name) {
        Supplier<Block> block = BLOCKS.register(name,
                () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
        Supplier<Item> item = ITEMS.register(name,
                () -> new BlockItem(block.get(), new Item.Properties()));
        BY_ROCK.put(rock, block);
        TAB_ITEMS.add(item);
    }

    private static void registerSoil(Soil.Type type, String name) {
        Supplier<Block> block = BLOCKS.register(name,
                () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));
        Supplier<Item> item = ITEMS.register(name,
                () -> new BlockItem(block.get(), new Item.Properties()));
        BY_SOIL.put(type, block);
        TAB_ITEMS.add(item);
    }

    private static void registerVariant(String name) {
        Supplier<Block> block = BLOCKS.register(name,
                () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
        Supplier<Item> item = ITEMS.register(name,
                () -> new BlockItem(block.get(), new Item.Properties()));
        BY_NAME.put(name, block);
        TAB_ITEMS.add(item);
    }

    private static void registerOre(Host host, Ore ore) {
        String name = oreName(host, ore);
        Supplier<Block> block = BLOCKS.register(name,
                () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(3.0f, 3.0f)));
        Supplier<Item> item = ITEMS.register(name,
                () -> new BlockItem(block.get(), new Item.Properties()));
        BY_NAME.put(name, block);
        TAB_ITEMS.add(item);
    }

    public static Block forRock(Strata.Rock rock) {
        Supplier<Block> s = BY_ROCK.get(rock);
        return s != null ? s.get() : Blocks.STONE;
    }

    public static Block forSoil(Soil.Type type) {
        Supplier<Block> s = BY_SOIL.get(type);
        return s != null ? s.get() : Blocks.DIRT;
    }

    public static Block forOre(Host host, Ore ore) {
        Supplier<Block> s = BY_NAME.get(oreName(host, ore));
        return s != null ? s.get() : Blocks.STONE;
    }

    public static Block deepslate() {
        Supplier<Block> s = BY_NAME.get("deepslate_stone");
        return s != null ? s.get() : Blocks.DEEPSLATE;
    }

    public static Block variant(Strata.Rock rock, String suffix) {
        Supplier<Block> s = BY_NAME.get(baseName(rock) + "_" + suffix);
        return s != null ? s.get() : forRock(rock);
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
    }
}
