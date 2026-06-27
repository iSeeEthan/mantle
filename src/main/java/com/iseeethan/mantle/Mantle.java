package com.iseeethan.mantle;

import com.iseeethan.mantle.command.MantleCommand;
import com.iseeethan.mantle.world.Locator;
import com.iseeethan.mantle.world.MantleWorld;
import com.iseeethan.mantle.world.gen.MantleChunkGenerator;
import com.iseeethan.mantle.world.gen.MantleGenerators;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(Mantle.MOD_ID)
public class Mantle {
    public static final String MOD_ID = "mantle";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Mantle(IEventBus modEventBus) {
        MantleGenerators.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Mantle is loading");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MantleCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        ChunkGenerator gen = overworld.getChunkSource().getGenerator();
        if (!(gen instanceof MantleChunkGenerator)) return;

        MantleWorld world = MantleWorld.forSeed(overworld.getSeed());
        Locator.Found spawn = new Locator(world).findCoastalSpawn();
        if (spawn != null) {
            overworld.setDefaultSpawnPos(new BlockPos(spawn.x, spawn.y, spawn.z), 0.0f);
            LOGGER.info("Mantle coastal spawn set to {} {} {}", spawn.x, spawn.y, spawn.z);
        }
    }
}
