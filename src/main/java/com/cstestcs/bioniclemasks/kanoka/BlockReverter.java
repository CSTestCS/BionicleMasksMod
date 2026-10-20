package com.cstestcs.bioniclemasks.kanoka;

import com.cstestcs.bioniclemasks.BionicleMasks;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Temporary block changes: Kanoka of Reconstitutes at Random only scrambles matter for a while, and the
 * Mask of Conjuring's creations fade. Everything is put back when the time runs out or the server stops.
 */
@EventBusSubscriber(modid = BionicleMasks.MOD_ID)
public final class BlockReverter {
    private record Change(ServerLevel level, BlockPos pos, BlockState original, BlockState replacement, long revertAt) {
    }

    private static final List<Change> CHANGES = new ArrayList<>();

    private BlockReverter() {
    }

    /** Replaces the block and schedules the original to come back after {@code ticks}. */
    public static void swap(ServerLevel level, BlockPos pos, BlockState replacement, int ticks) {
        BlockState original = level.getBlockState(pos);
        for (Change change : CHANGES) {
            if (change.level == level && change.pos.equals(pos)) {
                original = change.original; // keep the very first state
                CHANGES.remove(change);
                break;
            }
        }
        level.setBlockAndUpdate(pos, replacement);
        CHANGES.add(new Change(level, pos.immutable(), original, replacement, level.getGameTime() + ticks));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (CHANGES.isEmpty()) {
            return;
        }
        Iterator<Change> it = CHANGES.iterator();
        while (it.hasNext()) {
            Change change = it.next();
            if (change.level.getGameTime() >= change.revertAt) {
                restore(change);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CHANGES.forEach(BlockReverter::restore);
        CHANGES.clear();
    }

    private static void restore(Change change) {
        if (change.level.isLoaded(change.pos) && change.level.getBlockState(change.pos) == change.replacement) {
            change.level.setBlockAndUpdate(change.pos, change.original);
        }
    }
}
