package com.cstestcs.bioniclemasks.mask;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** "What is the wearer looking at" helpers for targeted mask powers. */
public final class Raycast {
    private Raycast() {
    }

    public static BlockHitResult block(Player player, double range) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(range));
        return player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    /** The closest living entity under the crosshair within range, not hidden behind blocks. */
    public static LivingEntity living(ServerLevel level, Player player, double range) {
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult wall = block(player, range);
        double maxDist = wall.getType() == HitResult.Type.MISS ? range : wall.getLocation().distanceTo(from);
        Vec3 to = from.add(look.scale(maxDist));

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB search = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.0);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search, e -> e != player && e.isAlive() && !e.isSpectator())) {
            AABB box = candidate.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = box.clip(from, to);
            if (box.contains(from)) {
                return candidate;
            }
            if (hit.isPresent()) {
                double d = from.distanceTo(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = candidate;
                }
            }
        }
        return best;
    }

    /** Horizontal compass direction from one entity to another, e.g. "north-east". */
    public static String direction(Entity from, Entity to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0) {
            angle += 360;
        }
        String[] names = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
        return names[(int) Math.round(angle / 45.0) % 8];
    }
}
