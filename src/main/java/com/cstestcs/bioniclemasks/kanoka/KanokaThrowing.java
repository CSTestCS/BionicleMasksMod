package com.cstestcs.bioniclemasks.kanoka;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.component.KanokaData;
import com.cstestcs.bioniclemasks.item.KanokaItem;
import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.Metru;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Thrown Kanoka. A thrown disk is a vanilla throwable that carries the disk item (so it renders as the
 * disk). Its flight follows the trait of the Metru it was made in, and on impact it releases its power.
 */
@EventBusSubscriber(modid = BionicleMasks.MOD_ID)
public final class KanokaThrowing {
    /** Per-flight bookkeeping (bounces, pass-throughs, age). Lost on reload, which is harmless. */
    private static final class Flight {
        int age;
        int passes;
        boolean bounced;
    }

    private static final Map<Entity, Flight> FLIGHTS = new WeakHashMap<>();

    private KanokaThrowing() {
    }

    public static void launch(ServerLevel level, Player player, ItemStack disk, KanokaData data) {
        Snowball projectile = new Snowball(level, player, disk);
        float velocity = 1.6F;
        if (data.metru() == Metru.LE) {
            velocity *= 1.5F;
            projectile.setNoGravity(true);
        }
        if (data.has(DiskTrait.SWIFT)) {
            velocity *= 1.5F;
        }
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, data.metru() == Metru.GA ? 0.0F : 0.6F);
        level.addFreshEntity(projectile);
        FLIGHTS.put(projectile, new Flight());
        if (!data.has(DiskTrait.SILENT)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7F, 0.6F);
        }
    }

    /** The disk item carried by a projectile, or empty if it is not a Kanoka. */
    private static ItemStack diskOf(Entity entity) {
        if (entity instanceof ThrowableItemProjectile thrown && thrown.getItem().getItem() instanceof KanokaItem) {
            return thrown.getItem();
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof Projectile projectile)) {
            return;
        }
        ItemStack disk = diskOf(entity);
        if (disk.isEmpty()) {
            return;
        }
        KanokaData data = com.cstestcs.bioniclemasks.item.KanokaStacks.data(disk);
        Flight flight = FLIGHTS.computeIfAbsent(entity, e -> new Flight());
        flight.age++;
        Entity owner = projectile.getOwner();

        switch (data.metru()) {
            case GA -> {
                // Steers toward where the thrower is looking.
                if (owner instanceof LivingEntity thrower && flight.age < 60) {
                    Vec3 aim = thrower.getEyePosition().add(thrower.getLookAngle().scale(thrower.distanceTo(entity) + 4));
                    Vec3 wanted = aim.subtract(entity.position()).normalize().scale(entity.getDeltaMovement().length());
                    entity.setDeltaMovement(entity.getDeltaMovement().lerp(wanted, 0.15));
                    entity.syncVelocity = true;
                }
            }
            case PO -> {
                // Deflects other projectiles that come close.
                for (Projectile other : level.getEntitiesOfClass(Projectile.class, entity.getBoundingBox().inflate(1.5),
                        p -> p != entity && p.getOwner() != owner)) {
                    other.setDeltaMovement(other.getDeltaMovement().scale(-0.6));
                    other.syncVelocity = true;
                }
            }
            case LE -> {
                // Flies straight for longer before gravity takes it.
                if (flight.age == 40) {
                    entity.setNoGravity(false);
                }
            }
            case ONU -> {
                // A disk knocked off course comes home.
                if (flight.age > 100) {
                    finish(level, projectile, disk, data, entity.position());
                }
            }
            default -> {
            }
        }
        if (data.level() >= 7 && flight.age % 2 == 0) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, entity.getX(), entity.getY(), entity.getZ(), 1, 0, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        ItemStack disk = diskOf(projectile);
        if (disk.isEmpty()) {
            return;
        }
        // Always stop the vanilla snowball behaviour; the server decides what happens.
        event.setCanceled(true);
        if (!(projectile.level() instanceof ServerLevel level) || projectile.isRemoved()) {
            return;
        }
        KanokaItem item = (KanokaItem) disk.getItem();
        KanokaData data = com.cstestcs.bioniclemasks.item.KanokaStacks.data(disk);
        Flight flight = FLIGHTS.computeIfAbsent(projectile, e -> new Flight());
        HitResult hit = event.getRayTraceResult();
        Entity owner = projectile.getOwner();

        BlockPos hitBlock = null;
        Entity hitEntity = null;
        if (hit instanceof BlockHitResult blockHit) {
            hitBlock = blockHit.getBlockPos();
            BlockState state = level.getBlockState(hitBlock);
            float hardness = state.getDestroySpeed(level, hitBlock);
            // Ta-Metru disks smash through weak obstacles.
            if (data.metru() == Metru.TA && flight.passes < 3 && hardness >= 0 && hardness <= 0.8F) {
                level.destroyBlock(hitBlock, true, owner);
                flight.passes++;
                return;
            }
            // Ko-Metru disks dodge the first obstacle in their way.
            if (data.metru() == Metru.KO && flight.passes < 1) {
                flight.passes++;
                Vec3 v = projectile.getDeltaMovement();
                projectile.setDeltaMovement(v.x, Math.abs(v.y) + 0.35, v.z);
                projectile.syncVelocity = true;
                return;
            }
            // Bouncing disks ricochet once.
            if (data.has(DiskTrait.BOUNCING) && !flight.bounced) {
                flight.bounced = true;
                Direction face = blockHit.getDirection();
                Vec3 v = projectile.getDeltaMovement();
                Vec3 bounced = switch (face.getAxis()) {
                    case X -> new Vec3(-v.x, v.y, v.z);
                    case Y -> new Vec3(v.x, -v.y, v.z);
                    case Z -> new Vec3(v.x, v.y, -v.z);
                };
                projectile.setDeltaMovement(bounced.scale(0.75));
                projectile.syncVelocity = true;
                return;
            }
        } else if (hit instanceof EntityHitResult entityHit) {
            hitEntity = entityHit.getEntity();
            if (hitEntity == owner && flight.age < 5) {
                return;
            }
            if (hitEntity instanceof LivingEntity living) {
                float damage = KanokaPowers.impactDamage(data.level(), data);
                if (data.material() == DiskMaterial.STONE || data.has(DiskTrait.HEAVY)) {
                    Vec3 push = projectile.getDeltaMovement().normalize().scale(0.6);
                    living.push(push.x, 0.2, push.z);
                    living.syncVelocity = true;
                }
                living.hurtServer(level, level.damageSources().thrown(projectile, owner), damage);
            }
        }

        Vec3 at = hit.getLocation();
        int powerLevel = data.level();
        if (data.material() == DiskMaterial.COPPER && level.isThundering()) {
            powerLevel = Math.min(9, powerLevel + 1);
        }
        KanokaPowers.release(level, owner, projectile, item.power(), powerLevel, data, at, hitEntity, hitBlock);
        // Fortunate disks sometimes strike twice.
        if (data.has(DiskTrait.FORTUNATE) && level.getRandom().nextBoolean()) {
            KanokaPowers.release(level, owner, projectile, item.power(), powerLevel, data, at, hitEntity, hitBlock);
        }
        finish(level, projectile, disk, data, at);
    }

    /** Ends the flight: the disk drops, returns to its thrower (Onu-Metru), or shatters. */
    private static void finish(ServerLevel level, Projectile projectile, ItemStack disk, KanokaData data, Vec3 at) {
        FLIGHTS.remove(projectile);
        projectile.discard();
        KanokaItem item = (KanokaItem) disk.getItem();
        boolean unbreakable = data.has(DiskTrait.PRESERVED) || item.kind() != KanokaItem.Kind.NORMAL;
        if (!unbreakable && level.getRandom().nextDouble() < data.material().breakChance()) {
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.1);
            if (!data.has(DiskTrait.SILENT)) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            return;
        }
        ItemStack back = disk.copyWithCount(1);
        if (data.metru() == Metru.ONU && projectile.getOwner() instanceof Player thrower && thrower.isAlive()
                && thrower.level() == level && thrower.distanceTo(projectile) < 96) {
            if (!thrower.getInventory().add(back)) {
                level.addFreshEntity(new ItemEntity(level, thrower.getX(), thrower.getY(), thrower.getZ(), back));
            }
            return;
        }
        ItemEntity drop = new ItemEntity(level, at.x, at.y, at.z, back);
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
    }
}
