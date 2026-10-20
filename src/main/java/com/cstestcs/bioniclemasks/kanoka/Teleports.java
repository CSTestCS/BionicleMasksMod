package com.cstestcs.bioniclemasks.kanoka;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Safe-landing teleport helpers shared by Kanoka of Teleport and several Kanohi. */
public final class Teleports {
    private Teleports() {
    }

    /** True if an entity can stand at {@code feet}: solid ground below and two clear blocks. */
    public static boolean canStand(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getFluidState(feet).isEmpty();
    }

    /** Finds a standing spot near {@code target}, searching up and down a little. */
    public static Vec3 findStandingSpot(ServerLevel level, BlockPos target, int verticalSearch) {
        for (int dy = 0; dy <= verticalSearch; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = target.above(dy * sign);
                if (level.isInWorldBounds(p) && canStand(level, p)) {
                    return Vec3.atBottomCenterOf(p);
                }
            }
        }
        return null;
    }

    /** Teleports the entity to a random safe spot within {@code radius} blocks. Returns true on success. */
    public static boolean randomNearby(ServerLevel level, Entity entity, double radius) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 24; attempt++) {
            double x = entity.getX() + (random.nextDouble() * 2 - 1) * radius;
            double z = entity.getZ() + (random.nextDouble() * 2 - 1) * radius;
            BlockPos target = BlockPos.containing(x, entity.getY() + random.nextInt(5) - 2, z);
            Vec3 spot = findStandingSpot(level, target, (int) Math.max(4, radius / 2));
            if (spot != null) {
                moveTo(level, entity, spot);
                return true;
            }
        }
        return false;
    }

    public static void moveTo(ServerLevel level, Entity entity, Vec3 spot) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.0F);
        entity.teleportTo(spot.x, spot.y, spot.z);
        entity.resetFallDistance();
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.2F);
    }
}
