package com.cstestcs.bioniclemasks.registry;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.component.KanohiData;
import com.cstestcs.bioniclemasks.component.KanokaData;
import com.cstestcs.bioniclemasks.item.KanohiItem;
import com.cstestcs.bioniclemasks.item.KanokaItem;
import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import com.cstestcs.bioniclemasks.lore.KanokaPower;
import com.cstestcs.bioniclemasks.lore.Metru;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BionicleMasks.MOD_ID);

    /** One Kanoka item per disk power. Material, level, Metru and traits are stored on the stack. */
    public static final Map<KanokaPower, DeferredItem<KanokaItem>> KANOKA = new EnumMap<>(KanokaPower.class);
    /** The six level-9 Great Disks, by the Metru they were hidden in. */
    public static final Map<Metru, DeferredItem<KanokaItem>> GREAT_DISKS = new EnumMap<>(Metru.class);
    /** Every Kanohi mask. Form, material and infusion are stored on the stack. */
    public static final Map<KanohiPower, DeferredItem<KanohiItem>> KANOHI = new EnumMap<>(KanohiPower.class);

    public static final DeferredItem<KanokaItem> DISK_OF_TIME;

    public static final DeferredItem<BlockItem> MASK_FORGE = ITEMS.registerSimpleBlockItem(ModBlocks.MASK_FORGE);
    public static final DeferredItem<BlockItem> KANOKA_FORGE = ITEMS.registerSimpleBlockItem(ModBlocks.KANOKA_FORGE);

    static {
        for (KanokaPower power : KanokaPower.values()) {
            KANOKA.put(power, ITEMS.registerItem("kanoka_" + power.id(),
                    props -> new KanokaItem(props, power, KanokaItem.Kind.NORMAL),
                    props -> props.stacksTo(16).component(ModComponents.KANOKA.get(), KanokaData.DEFAULT)));
        }
        for (KanokaPower power : KanokaPower.values()) {
            Metru metru = power.greatDiskMetru();
            if (metru != null) {
                KanokaData data = new KanokaData(DiskMaterial.NETHERITE, 9, metru, List.of());
                GREAT_DISKS.put(metru, ITEMS.registerItem("great_disk_" + metru.id(),
                        props -> new KanokaItem(props, power, KanokaItem.Kind.GREAT),
                        props -> props.stacksTo(1).fireResistant().component(ModComponents.KANOKA.get(), data)));
            }
        }
        DISK_OF_TIME = ITEMS.registerItem("disk_of_time",
                props -> new KanokaItem(props, null, KanokaItem.Kind.TIME),
                props -> props.stacksTo(1).fireResistant()
                        .component(ModComponents.KANOKA.get(), new KanokaData(DiskMaterial.NETHERITE, 9, Metru.TA, List.of())));

        for (KanohiPower power : KanohiPower.values()) {
            String name = power.itemName();
            ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset =
                    ResourceKey.create(EquipmentAssets.ROOT_ID, BionicleMasks.id(name));
            KANOHI.put(power, ITEMS.registerItem(name,
                    props -> new KanohiItem(props, power),
                    props -> props.stacksTo(1).fireResistant()
                            .component(ModComponents.KANOHI.get(), KanohiData.DEFAULT)
                            .component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                                    Equippable.builder(EquipmentSlot.HEAD).setAsset(asset).build())));
        }
    }

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
