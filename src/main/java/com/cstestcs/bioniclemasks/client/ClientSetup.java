package com.cstestcs.bioniclemasks.client;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.network.ModNetwork;
import com.cstestcs.bioniclemasks.registry.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Client-only setup: forge screens and the mask keys. */
@EventBusSubscriber(modid = BionicleMasks.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(BionicleMasks.id("masks"));
    public static final KeyMapping USE_MASK = new KeyMapping("key.bioniclemasks.use_mask", InputConstants.KEY_V, CATEGORY);
    public static final KeyMapping CYCLE_MASK = new KeyMapping("key.bioniclemasks.cycle_mask", InputConstants.KEY_G, CATEGORY);

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MASK_FORGE.get(), MaskForgeScreen::new);
        event.register(ModMenus.KANOKA_FORGE.get(), KanokaForgeScreen::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(USE_MASK);
        event.register(CYCLE_MASK);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        while (USE_MASK.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ModNetwork.MaskKey(0));
        }
        while (CYCLE_MASK.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ModNetwork.MaskKey(2));
        }
    }
}
