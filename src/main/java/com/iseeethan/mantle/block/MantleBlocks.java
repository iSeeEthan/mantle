package com.iseeethan.mantle.block;

import com.iseeethan.mantle.Mantle;
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

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    static {
        register(Strata.Rock.SEDIMENTARY, "sedimentary_stone");
        register(Strata.Rock.IGNEOUS, "igneous_stone");
        register(Strata.Rock.METAMORPHIC, "metamorphic_stone");
        register(Strata.Rock.BASEMENT, "basement_stone");
    }

    public static final Supplier<CreativeModeTab> STONE_TAB = TABS.register("stone", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mantle.stone"))
                    .icon(() -> MantleBlocks.forRock(Strata.Rock.SEDIMENTARY).asItem().getDefaultInstance())
                    .displayItems((params, output) -> {
                        for (Supplier<? extends Item> item : TAB_ITEMS) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    private MantleBlocks() {}

    private static void register(Strata.Rock rock, String name) {
        Supplier<Block> block = BLOCKS.register(name,
                () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
        Supplier<Item> item = ITEMS.register(name,
                () -> new BlockItem(block.get(), new Item.Properties()));
        BY_ROCK.put(rock, block);
        TAB_ITEMS.add(item);
    }

    public static Block forRock(Strata.Rock rock) {
        Supplier<Block> s = BY_ROCK.get(rock);
        return s != null ? s.get() : Blocks.STONE;
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
    }
}
