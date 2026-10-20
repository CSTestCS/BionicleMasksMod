package com.cstestcs.bioniclemasks.client;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.component.KanohiData;
import com.cstestcs.bioniclemasks.component.KanokaData;
import com.cstestcs.bioniclemasks.item.KanohiItem;
import com.cstestcs.bioniclemasks.item.KanohiStacks;
import com.cstestcs.bioniclemasks.item.KanokaItem;
import com.cstestcs.bioniclemasks.item.KanokaStacks;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.Infusion;
import com.cstestcs.bioniclemasks.lore.KanohiForm;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import com.cstestcs.bioniclemasks.lore.KanokaPower;
import com.cstestcs.bioniclemasks.lore.MaskForging;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Lore and stats on Kanoka and Kanohi tooltips. */
@EventBusSubscriber(modid = BionicleMasks.MOD_ID, value = Dist.CLIENT)
public final class Tooltips {
    private static final int GRAY = 0xA0A0A0;
    private static final int LORE = 0x8C8CA8;
    private static final int GOOD = 0x7FD1FF;
    private static final int GOLD = 0xFFC93C;

    private Tooltips() {
    }

    private static MutableComponent text(String s, int color) {
        return Component.literal(s).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> lines = event.getToolTip();
        if (stack.getItem() instanceof KanokaItem disk) {
            kanoka(disk, KanokaStacks.data(stack), lines);
        } else if (stack.getItem() instanceof KanohiItem mask) {
            kanohi(mask.power(), KanohiStacks.data(stack), lines);
        }
    }

    private static void kanoka(KanokaItem disk, KanokaData data, List<Component> lines) {
        if (disk.kind() == KanokaItem.Kind.TIME) {
            lines.add(text("The Disk of Time, merged from the six Great Disks.", GOLD));
            lines.add(text("Thrown: slows time for everything nearby.", GOOD));
            lines.add(text("Carve it alone in the Mask Forge to make the Kanohi Vahi.", LORE));
            return;
        }
        KanokaPower power = disk.power();
        String code = MaskForging.code(data.metru(), power, data.level());
        lines.add(text((disk.kind() == KanokaItem.Kind.GREAT ? "Great Disk - code " : "Code ") + code, GOLD));
        lines.add(text(power.displayName() + ": " + power.description(), GOOD));
        lines.add(text("Power level " + data.level() + " - " + data.material().displayName() + ". " + data.material().trait(), GRAY));
        lines.add(text(data.metru().displayName() + ": " + data.metru().trait(), GRAY));
        for (DiskTrait trait : data.traits()) {
            lines.add(text(trait.displayName() + ": " + trait.diskEffect(), GRAY));
        }
        lines.add(text("Right-click to throw.", LORE));
    }

    private static void kanohi(KanohiPower power, KanohiData data, List<Component> lines) {
        lines.add(text(power.title(), GOLD));
        lines.add(text(data.form().displayName() + " " + data.material().displayName() + " mask. " + data.form().description(), GRAY));
        if (data.form().powered()) {
            KanohiPower active = power;
            if (data.form() == KanohiForm.GOLDEN) {
                active = KanohiPower.MATA_MASKS.get(Math.floorMod(data.selected(), KanohiPower.MATA_MASKS.size()));
                lines.add(text("Selected power: " + active.maskName() + " (" + active.powerName() + ")", GOLD));
            }
            lines.add(text(active.gameplay(), GOOD));
            String key = ClientSetup.USE_MASK.getTranslatedKeyMessage().getString();
            lines.add(text("Wear it and press [" + key + "]" + (active.kind() == KanohiPower.Kind.PASSIVE ? " to check it" : "")
                    + ". Sneak for the alternate use, where there is one.", LORE));
        }
        lines.add(text(power.lore(), LORE));
        lines.add(text(data.material().trait(), GRAY));
        data.infusion().ifPresent(infusion -> lines.add(text(
                (infusion.kind() == Infusion.Kind.ELEMENT ? "Element of " : "Crystal: ") + infusion.displayName() + " - " + infusion.effect(),
                infusion.color())));
        for (DiskTrait trait : data.traits()) {
            lines.add(text(trait.displayName() + ": " + trait.maskEffect(), GRAY));
        }
        lines.add(text("Forged from Kanoka in " + data.origin().displayName() + ".", LORE));
    }
}
