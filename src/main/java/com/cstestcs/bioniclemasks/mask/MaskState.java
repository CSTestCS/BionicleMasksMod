package com.cstestcs.bioniclemasks.mask;

import com.cstestcs.bioniclemasks.lore.KanohiPower;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;

/** Per-player runtime state for worn masks (toggles and timers). Not saved: toggles reset on relog. */
public final class MaskState {
    private static final Map<UUID, MaskState> STATES = new HashMap<>();

    /** Whether a toggle power is switched on. */
    public boolean active;
    /** The power that was worn last tick, to notice when the mask is swapped. */
    public KanohiPower lastPower;
    /** Game time when the current toggle was switched on. */
    public long activeSince;
    /** Next game time a Mask of Clairvoyance vision may arrive on its own. */
    public long nextVision;
    /** Whether the mask (Volitak) has made the wearer silent. */
    public boolean madeSilent;

    private MaskState() {
    }

    public static MaskState get(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new MaskState());
    }

    public static void forget(Player player) {
        STATES.remove(player.getUUID());
    }
}
