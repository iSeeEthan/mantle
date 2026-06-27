package com.iseeethan.mantle.client;

import com.iseeethan.mantle.Mantle;
import com.iseeethan.mantle.world.gen.MantlePresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;

@EventBusSubscriber(modid = Mantle.MOD_ID, value = Dist.CLIENT)
public final class MantleClientEvents {

    private MantleClientEvents() {}

    @SubscribeEvent
    public static void onRegisterPresetEditors(RegisterPresetEditorsEvent event) {
        event.register(MantlePresets.MANTLE, MantleCustomizeScreen::new);
    }
}
