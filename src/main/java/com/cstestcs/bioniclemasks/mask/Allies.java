package com.cstestcs.bioniclemasks.mask;

import com.cstestcs.bioniclemasks.BionicleMasks;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Creatures temporarily on a mask wearer's side: minds taken by the Komau or Mask of Rahi Control,
 * creatures swayed by the Mask of Charisma, Rahi called by the Zatth and dead raised by the Tryna.
 * They never target their master and hunt the monsters around them.
 */
@EventBusSubscriber(modid = BionicleMasks.MOD_ID)
public final class Allies {
    /** How a creature came to be an ally. Summoned and raised ones vanish when their time is up. */
    public enum Bond { CONTROLLED, CHARMED, SUMMONED }

    private record Ally(UUID master, Bond bond, long until) {
    }

    private static final Map<Mob, Ally> ALLIES = new WeakHashMap<>();

    private Allies() {
    }

    public static void bind(Mob mob, Player master, Bond bond, int ticks) {
        ALLIES.put(mob, new Ally(master.getUUID(), bond, mob.level().getGameTime() + ticks));
        if (mob.getTarget() == master) {
            mob.setTarget(null);
        }
    }

    public static boolean isAlly(LivingEntity entity) {
        return entity instanceof Mob mob && ALLIES.containsKey(mob);
    }

    public static boolean isAllyOf(LivingEntity entity, Player player) {
        return entity instanceof Mob mob && ALLIES.containsKey(mob) && ALLIES.get(mob).master.equals(player.getUUID());
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Ally ally = ALLIES.get(mob);
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (ally == null || target == null) {
            return;
        }
        // Never turn on the master or the master's other allies.
        if (target.getUUID().equals(ally.master) || (target instanceof Mob other && ALLIES.containsKey(other)
                && ALLIES.get(other).master.equals(ally.master))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ALLIES.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Mob, Ally>> it = ALLIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Mob, Ally> entry = it.next();
            Mob mob = entry.getKey();
            Ally ally = entry.getValue();
            if (mob.isRemoved() || !mob.isAlive()) {
                it.remove();
                continue;
            }
            ServerLevel level = (ServerLevel) mob.level();
            if (level.getGameTime() >= ally.until) {
                it.remove();
                if (ally.bond == Bond.SUMMONED) {
                    level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.5, mob.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
                    mob.discard();
                } else if (mob.getTarget() != null && mob.getTarget().getUUID().equals(ally.master)) {
                    mob.setTarget(null);
                }
                continue;
            }
            // Once a second, pick a fight with the nearest monster that is not on our side.
            if ((level.getGameTime() + mob.getId()) % 20 == 0 && (mob.getTarget() == null || !mob.getTarget().isAlive())) {
                LivingEntity prey = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(mob.blockPosition()).inflate(16),
                        e -> e instanceof Enemy && e != mob && e.isAlive() && !(e instanceof Mob m && ALLIES.containsKey(m)))) {
                    double d = candidate.distanceToSqr(mob);
                    if (d < best) {
                        best = d;
                        prey = candidate;
                    }
                }
                if (prey != null) {
                    mob.setTarget(prey);
                } else {
                    Player master = level.getPlayerByUUID(ally.master);
                    if (master != null && master.distanceToSqr(mob) > 64) {
                        mob.getNavigation().moveTo(master, 1.1);
                    }
                }
            }
        }
    }
}
