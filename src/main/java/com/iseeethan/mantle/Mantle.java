package com.iseeethan.mantle;

import com.iseeethan.mantle.world.gen.MantleGenerators;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Mantle.MOD_ID)
public class Mantle {
    public static final String MOD_ID = "mantle";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Mantle(IEventBus modEventBus) {
        MantleGenerators.register(modEventBus);
        LOGGER.info("Mantle is loading");
    }
}
