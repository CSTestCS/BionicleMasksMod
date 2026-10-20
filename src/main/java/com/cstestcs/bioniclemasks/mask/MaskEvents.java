package com.cstestcs.bioniclemasks.mask;

import static com.cstestcs.bioniclemasks.mask.MaskPowers.effect;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.kanoka.ReconstitutedEffect;
import com.cstestcs.bioniclemasks.lore.DiskMaterial;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.Infusion;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import com.cstestcs.bioniclemasks.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Mask powers that react to the world: shields, dodges, elemental strikes, targeting and death. */
@EventBusSubscriber(modid = BionicleMasks.MOD_ID)
public final class MaskEvents {
    private static final TagKey<DamageType> IS_FIRE = damageTag("is_fire");
    private static final TagKey<DamageType> IS_FALL = damageTag("is_fall");
    private static final TagKey<DamageType> IS_FREEZING = damageTag("is_freezing");
    private static final TagKey<DamageType> IS_DROWNING = damageTag("is_drowning");
    private static final TagKey<DamageType> BYPASSES_INVULNERABILITY = damageTag("bypasses_invulnerability");

    private MaskEvents() {
    }

    private static TagKey<DamageType> damageTag(String path) {
        return TagKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace(path));
    }

    private static boolean toggledOn(MaskContext ctx, KanohiPower power) {
        return ctx != null && ctx.power() == power && MaskState.get(ctx.player()).active;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // --- The attacker wears a mask ---
        if (attacker instanceof ServerPlayer striker && striker != victim) {
            MaskContext ctx = MaskContext.of(striker);
            if (toggledOn(ctx, KanohiPower.INTANGIBILITY) || toggledOn(ctx, KanohiPower.IDEN)) {
                event.setCanceled(true); // an intangible being or a spirit cannot strike
                return;
            }
            if (ctx != null && source.getDirectEntity() == striker) {
                elementalStrike(ctx, victim, event);
            }
            MobEffectInstance echo = striker.getEffect(ModEffects.TIME_ECHO);
            if (echo != null) {
                // Past selves strike alongside every blow.
                event.setAmount(event.getAmount() * (1.0F + 0.5F * (echo.getAmplifier() + 1)));
                MaskPowers.particles((net.minecraft.server.level.ServerLevel) victim.level(), ParticleTypes.CRIT, victim.position().add(0, 1, 0), 6 * (echo.getAmplifier() + 1), 0.4);
            }
        }

        // --- The victim wears a mask ---
        if (!(victim instanceof ServerPlayer wearer)) {
            return;
        }
        MaskContext ctx = MaskContext.of(wearer);
        if (ctx == null) {
            return;
        }
        var data = ctx.data();
        if (source.is(IS_FIRE) && (data.has(Infusion.FIRE) || data.has(Infusion.PLASMA))) {
            event.setCanceled(true);
            return;
        }
        if (source.is(IS_FALL) && (data.has(Infusion.GRAVITY) || toggledOn(ctx, KanohiPower.MIRU))) {
            event.setCanceled(true);
            return;
        }
        if (source.is(IS_FREEZING) && (data.has(Infusion.ICE) || ctx.power() == KanohiPower.ADAPTATION)) {
            event.setCanceled(true);
            return;
        }
        if (source.is(IS_DROWNING) && ctx.power() == KanohiPower.ADAPTATION) {
            event.setCanceled(true);
            return;
        }
        if (toggledOn(ctx, KanohiPower.IDEN) && !source.is(BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            return;
        }
        Entity direct = source.getDirectEntity();
        if (toggledOn(ctx, KanohiPower.INTANGIBILITY) && (attacker != null || direct != null)) {
            event.setCanceled(true);
            return;
        }
        // Hau: blocks attacks the wearer can see coming - from the front - but not surprise attacks.
        if (toggledOn(ctx, KanohiPower.HAU) && (attacker != null || direct != null)) {
            Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition()
                    : (direct != null ? direct.position() : attacker.position());
            Vec3 toSource = from.subtract(wearer.position()).normalize();
            if (toSource.dot(wearer.getLookAngle()) > 0.0) {
                event.setCanceled(true);
                wearer.causeFoodExhaustion(0.3F);
                MaskPowers.particles(ctx.level(), ParticleTypes.ENCHANTED_HIT, wearer.position().add(toSource).add(0, 1, 0), 8, 0.3);
                return;
            }
        }
        // Calix: a lucky leap out of harm's way.
        if (toggledOn(ctx, KanohiPower.CALIX) && attacker != null
                && wearer.getRandom().nextDouble() < Math.min(0.4, 0.12 * ctx.strength())) {
            event.setCanceled(true);
            MaskPowers.particles(ctx.level(), ParticleTypes.CLOUD, wearer.position(), 8, 0.4);
            return;
        }
        if (source.is(IS_FIRE) && (data.material() == DiskMaterial.NETHERITE || ctx.power() == KanohiPower.ADAPTATION)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
        if (source.is(IS_FALL) && data.has(DiskTrait.BOUNCING)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
        if (data.has(DiskTrait.GALE) && attacker != null && attacker != wearer) {
            Vec3 push = attacker.position().subtract(wearer.position()).normalize().scale(1.0);
            attacker.push(push.x, 0.3, push.z);
            attacker.hurtMarked = true;
        }
    }

    /** Melee hits from a mask infused with an element. */
    private static void elementalStrike(MaskContext ctx, LivingEntity victim, LivingIncomingDamageEvent event) {
        if (ctx.data().infusion().isEmpty()) {
            return;
        }
        int k = ctx.elementalEnergy() ? 2 : 1;
        ServerPlayer p = ctx.player();
        switch (ctx.data().infusion().get()) {
            case FIRE -> victim.igniteForSeconds(3 * k);
            case PLASMA -> {
                event.setAmount(event.getAmount() + 2 * k);
                victim.igniteForSeconds(2);
            }
            case AIR -> {
                Vec3 push = victim.position().subtract(p.position()).normalize().scale(0.8 * k);
                victim.push(push.x, 0.3, push.z);
                victim.hurtMarked = true;
            }
            case ICE -> {
                victim.addEffect(effect(MobEffects.SLOWNESS, 60, k - 1));
                victim.setTicksFrozen(victim.getTicksFrozen() + 60 * k);
            }
            case LIGHTNING -> event.setAmount(event.getAmount() + 2 * k);
            case LIGHT -> {
                if (MaskPowers.undead(victim)) {
                    event.setAmount(event.getAmount() + 4 * k);
                }
            }
            case SHADOW -> p.heal(k);
            case SONICS -> victim.addEffect(effect(MobEffects.SLOWNESS, 20 * k, 3));
            case GRAVITY -> victim.addEffect(effect(ModEffects.CRUSHING_GRAVITY, 40 * k, 0));
            case PLANT_LIFE -> victim.addEffect(effect(MobEffects.SLOWNESS, 40 * k, 2));
            case PSIONICS -> {
                if (victim instanceof Mob mob) {
                    mob.addEffect(effect(ModEffects.VISIONS, 40 * k, 0));
                    mob.setTarget(null);
                } else {
                    victim.addEffect(effect(MobEffects.NAUSEA, 60 * k, 0));
                }
            }
            default -> {
            }
        }
    }

    /** The Ignika, the Mask of Life, will not let its wearer die - once per recharge. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MaskContext ctx = MaskContext.of(player);
        if (ctx == null || ctx.power() != KanohiPower.IGNIKA) {
            return;
        }
        var stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth() / 2);
        player.addEffect(effect(MobEffects.REGENERATION, 200, 2));
        player.addEffect(effect(MobEffects.ABSORPTION, 200, 1));
        player.getCooldowns().addCooldown(stack, (int) (6000 * ctx.cooldown()));
        MaskPowers.particles(ctx.level(), ParticleTypes.TOTEM_OF_UNDYING, player.position().add(0, 1, 0), 60, 0.8);
        MaskPowers.tell(player, "The Mask of Life refuses to let you die.", 0x7FFF7F);
    }

    /** Spirits and intangible beings cannot strike. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MaskContext ctx = MaskContext.of(player);
            if (toggledOn(ctx, KanohiPower.INTANGIBILITY) || toggledOn(ctx, KanohiPower.IDEN)) {
                event.setCanceled(true);
            }
        }
    }

    /** Kanohi Sanok: everything the wearer shoots flies perfectly true. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Projectile projectile)
                || !(projectile.getOwner() instanceof ServerPlayer shooter)) {
            return;
        }
        MaskContext ctx = MaskContext.of(shooter);
        if (ctx == null || ctx.power() != KanohiPower.SANOK) {
            return;
        }
        double speed = projectile.getDeltaMovement().length();
        projectile.setDeltaMovement(shooter.getLookAngle().scale(speed));
        if (projectile instanceof AbstractArrow arrow) {
            arrow.setCritArrow(true);
        }
    }

    /** Hidden wearers go unnoticed; dazed, silenced and visions-struck creatures cannot pick targets. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        LivingEntity seeker = event.getEntity();
        if (target == null || seeker.level().isClientSide()) {
            return;
        }
        if (seeker.hasEffect(ModEffects.VISIONS) || seeker.hasEffect(ModEffects.MIND_CONTROLLED) && target instanceof Player) {
            event.setCanceled(true);
            return;
        }
        if (seeker.hasEffect(ModEffects.SILENCED) && seeker.distanceTo(target) > 4) {
            event.setCanceled(true);
            return;
        }
        if (target instanceof ServerPlayer player) {
            MaskContext ctx = MaskContext.of(player);
            if (toggledOn(ctx, KanohiPower.IDEN)) {
                event.setCanceled(true);
            } else if ((toggledOn(ctx, KanohiPower.VOLITAK) || toggledOn(ctx, KanohiPower.HUNA)) && seeker.distanceTo(player) > 3) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null) {
            effectEnded(event.getEntity(), event.getEffectInstance());
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null) {
            effectEnded(event.getEntity(), event.getEffectInstance());
        }
    }

    private static void effectEnded(LivingEntity entity, MobEffectInstance instance) {
        if (instance.getEffect().is(ModEffects.SILENCED.getKey())) {
            entity.setSilent(false);
        } else if (instance.getEffect().is(ModEffects.RECONSTITUTED.getKey())) {
            ReconstitutedEffect.restore(entity);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MaskTicker.clearToggle(event.getEntity());
        MaskState.forget(event.getEntity());
    }
}
