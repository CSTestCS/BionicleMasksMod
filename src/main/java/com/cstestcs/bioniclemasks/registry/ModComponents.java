package com.cstestcs.bioniclemasks.registry;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.component.KanohiData;
import com.cstestcs.bioniclemasks.component.KanokaData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BionicleMasks.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KanokaData>> KANOKA =
            COMPONENTS.registerComponentType("kanoka", b -> b.persistent(KanokaData.CODEC).networkSynchronized(KanokaData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KanohiData>> KANOHI =
            COMPONENTS.registerComponentType("kanohi", b -> b.persistent(KanohiData.CODEC).networkSynchronized(KanohiData.STREAM_CODEC));

    private ModComponents() {
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
