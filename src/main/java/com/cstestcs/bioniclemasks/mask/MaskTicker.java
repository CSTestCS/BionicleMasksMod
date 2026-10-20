package com.cstestcs.bioniclemasks.mask;

import static com.cstestcs.bioniclemasks.mask.MaskPowers.effect;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.KanohiForm;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Applies passive mask powers, element/crystal boosts and switched-on toggle powers once a second.
 * Effects are given for a little over a second, so they fade quickly once the mask comes off.
 */
@EventBusSubscriber(modid = BionicleMasks.MOD_ID)
public final class MaskTicker {
    private static final int SHORT = 50;
    /** Night Vision flickers when under 10 seconds, so it is given for longer. */
    private static final int NIGHT_VISION = 260;

    private static final Identifier FLIGHT_ID = BionicleMasks.id("mask.flight");
    private static final Identifier SCALE_ID = BionicleMasks.id("mask.scale");
    private static final Identifier ATTACK_ID = BionicleMasks.id("mask.attack");
    private static final Identifier ENTITY_REACH_ID = BionicleMasks.id("mask.entity_reach");
    private static final Identifier BLOCK_REACH_ID = BionicleMasks.id("mask.block_reach");

    private MaskTicker() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MaskContext ctx = MaskContext.of(player);
        MaskState state = MaskState.get(player);
        KanohiPower current = ctx == null ? null : ctx.power();
        if (current != state.lastPower) {
            // The mask was taken off or swapped: everything it was doing stops.
            if (state.active || state.lastPower != null) {
                clearToggle(player);
            }
            state.active = false;
            state.lastPower = current;
        }
        if (ctx == null || player.tickCount % 20 != 0) {
            return;
        }

        passives(ctx);
        infusion(ctx);

        if (ctx.power().kind() == KanohiPower.Kind.TOGGLE && state.active) {
            List<MobEffectInstance> shared = toggle(ctx, state);
            float cost = ctx.data().form() == KanohiForm.NOBLE ? 0.15F : 0.1F;
            if (ctx.power() == KanohiPower.KADIN || ctx.power() == KanohiPower.IDEN) {
                cost = player.getAbilities().flying ? 0.4F : 0.1F;
            }
            player.causeFoodExhaustion(cost);
            // Kanohi Nuva share their power with nearby allies.
            if (ctx.nuva() && !shared.isEmpty()) {
                for (LivingEntity ally : ctx.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8),
                        e -> e != player && (e instanceof Player || (e instanceof OwnableEntity own && own.getOwner() == player)))) {
                    shared.forEach(e -> ally.addEffect(new MobEffectInstance(e)));
                }
            }
        }

        if (ctx.power() == KanohiPower.CLAIRVOYANCE) {
            long now = ctx.level().getGameTime();
            if (state.nextVision == 0) {
                state.nextVision = now + 2400 + ctx.level().getRandom().nextInt(3600);
            } else if (now >= state.nextVision) {
                MaskPowers.unbiddenVision(ctx);
                state.nextVision = now + 2400 + ctx.level().getRandom().nextInt(3600);
            }
        }
    }

    /** Always-on powers of the mask itself. */
    private static void passives(MaskContext ctx) {
        ServerPlayer p = ctx.player();
        switch (ctx.power()) {
            case KAUKAU -> {
                p.addEffect(effect(MobEffects.WATER_BREATHING, SHORT, 0));
                if (p.isInWater() && ctx.greatOrBetter()) {
                    p.addEffect(effect(MobEffects.DOLPHINS_GRACE, SHORT, 0));
                }
                if (p.isInWater() && (ctx.nuva() || ctx.data().form() == KanohiForm.GOLDEN)) {
                    p.addEffect(effect(MobEffects.CONDUIT_POWER, SHORT, 0));
                }
            }
            case RURU -> {
                p.addEffect(effect(MobEffects.NIGHT_VISION, NIGHT_VISION, 0));
                if (ctx.greatOrBetter()) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.DARKNESS);
                }
            }
            case RAU -> p.addEffect(effect(MobEffects.HERO_OF_THE_VILLAGE, SHORT, 0));
            case AVOHKII -> {
                p.removeEffect(MobEffects.BLINDNESS);
                p.removeEffect(MobEffects.DARKNESS);
            }
            case RODE -> {
                p.removeEffect(MobEffects.NAUSEA);
                p.removeEffect(MobEffects.BLINDNESS);
            }
            case ADAPTATION -> {
                if (p.isInWater() || p.isUnderWater()) {
                    p.addEffect(effect(MobEffects.WATER_BREATHING, SHORT, 0));
                }
                if (p.isOnFire() || p.isInLava()) {
                    p.addEffect(effect(MobEffects.FIRE_RESISTANCE, SHORT, 0));
                }
                if (p.getTicksFrozen() > 0) {
                    p.setTicksFrozen(0);
                }
            }
            case IGNIKA -> p.addEffect(effect(MobEffects.REGENERATION, SHORT, 0));
            default -> {
            }
        }
        if (ctx.data().has(DiskTrait.FORTUNATE)) {
            p.addEffect(effect(MobEffects.LUCK, SHORT, 0));
        }
    }

    /** The element or crystal boost of an infused mask (doubled by the Mask of Elemental Energy). */
    private static void infusion(MaskContext ctx) {
        if (ctx.data().infusion().isEmpty()) {
            return;
        }
        ServerPlayer p = ctx.player();
        int boost = ctx.elementalEnergy() ? 1 : 0;
        ServerLevel level = ctx.level();
        BlockPos pos = p.blockPosition();
        switch (ctx.data().infusion().get()) {
            case FIRE, PLASMA -> p.addEffect(effect(MobEffects.FIRE_RESISTANCE, SHORT, 0));
            case WATER -> {
                p.addEffect(effect(MobEffects.WATER_BREATHING, SHORT, 0));
                if (p.isInWater()) {
                    p.addEffect(effect(MobEffects.DOLPHINS_GRACE, SHORT, boost));
                }
            }
            case AIR -> {
                if (p.getDeltaMovement().y < -0.5 && !p.isShiftKeyDown()) {
                    p.addEffect(effect(MobEffects.SLOW_FALLING, SHORT, 0));
                }
            }
            case EARTH -> {
                p.addEffect(effect(MobEffects.HASTE, SHORT, boost));
                if (!level.canSeeSky(pos)) {
                    p.addEffect(effect(MobEffects.RESISTANCE, SHORT, boost));
                }
            }
            case STONE -> p.addEffect(effect(MobEffects.RESISTANCE, SHORT, boost));
            case ICE -> p.setTicksFrozen(0);
            case LIGHTNING -> p.addEffect(effect(MobEffects.SPEED, SHORT, boost));
            case LIGHT -> p.addEffect(effect(MobEffects.NIGHT_VISION, NIGHT_VISION, 0));
            case SHADOW -> {
                if (level.getMaxLocalRawBrightness(pos) < 6) {
                    p.addEffect(effect(MobEffects.INVISIBILITY, SHORT, 0));
                }
            }
            case SONICS, ECHO -> p.removeEffect(MobEffects.DARKNESS);
            case MAGNETISM -> {
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(6 + 3 * boost))) {
                    Vec3 pull = p.position().subtract(item.position()).normalize().scale(0.45);
                    item.setDeltaMovement(pull);
                    item.hurtMarked = true;
                }
            }
            case PLANT_LIFE -> {
                if (level.canSeeSky(pos) && level.getMaxLocalRawBrightness(pos) > 12) {
                    p.addEffect(effect(MobEffects.REGENERATION, SHORT, boost));
                }
            }
            case PRISMARINE -> p.addEffect(effect(MobEffects.WATER_BREATHING, SHORT, 0));
            case EMERALD -> p.addEffect(effect(MobEffects.HERO_OF_THE_VILLAGE, SHORT, boost));
            case LAPIS -> p.addEffect(effect(MobEffects.LUCK, SHORT, boost));
            case END_CRYSTAL -> {
                if (p.tickCount % 100 == 0) {
                    p.addEffect(effect(MobEffects.REGENERATION, 60, boost));
                }
            }
            default -> {
            }
        }
    }

    /** One second of a switched-on toggle power. Returns the effects a Kanohi Nuva shares with allies. */
    private static List<MobEffectInstance> toggle(MaskContext ctx, MaskState state) {
        ServerPlayer p = ctx.player();
        int a = ctx.amp();
        List<MobEffectInstance> shared = new ArrayList<>();
        switch (ctx.power()) {
            case HAU -> {
                MaskPowers.particles(ctx.level(), ParticleTypes.ENCHANT, p.position().add(0, 1, 0), 6, 0.6);
                shared.add(effect(MobEffects.RESISTANCE, SHORT, Math.min(2, a)));
            }
            case MIRU -> {
                give(p, shared, effect(MobEffects.SLOW_FALLING, SHORT, 0));
                give(p, shared, effect(MobEffects.JUMP_BOOST, SHORT, a));
            }
            case KAKAMA -> give(p, shared, effect(MobEffects.SPEED, SHORT, a + 1));
            case PAKARI -> {
                give(p, shared, effect(MobEffects.STRENGTH, SHORT, a));
                give(p, shared, effect(MobEffects.HASTE, SHORT, a));
            }
            case AKAKU -> {
                give(p, shared, effect(MobEffects.NIGHT_VISION, NIGHT_VISION, 0));
                for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(ctx.radius(16)))) {
                    mob.addEffect(effect(MobEffects.GLOWING, SHORT, 0));
                }
            }
            case HUNA -> p.addEffect(effect(MobEffects.INVISIBILITY, SHORT, 0));
            case CALIX -> {
                p.addEffect(effect(MobEffects.JUMP_BOOST, SHORT, a + 1));
                p.addEffect(effect(MobEffects.SPEED, SHORT, 0));
            }
            case KADIN -> modifier(p, NeoForgeMod.CREATIVE_FLIGHT, FLIGHT_ID, 1, AttributeModifier.Operation.ADD_VALUE);
            case IDEN -> {
                p.addEffect(effect(MobEffects.INVISIBILITY, SHORT, 0));
                modifier(p, NeoForgeMod.CREATIVE_FLIGHT, FLIGHT_ID, 1, AttributeModifier.Operation.ADD_VALUE);
                // The spirit can only stay out of the body for so long.
                if (ctx.level().getGameTime() - state.activeSince > ctx.ticks(30)) {
                    state.active = false;
                    clearToggle(p);
                    MaskPowers.tell(p, "Your spirit is pulled back into your body.", 0xC0C0FF);
                }
            }
            case VOLITAK -> {
                p.addEffect(effect(MobEffects.INVISIBILITY, SHORT, 0));
                p.setSilent(true);
                state.madeSilent = true;
            }
            case PEHKUI -> modifier(p, Attributes.SCALE, SCALE_ID, ctx.greatOrBetter() ? -0.75 : -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case GROWTH -> {
                double grow = Math.min(3.0, ctx.strength() * 1.2);
                modifier(p, Attributes.SCALE, SCALE_ID, grow, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                modifier(p, Attributes.ATTACK_DAMAGE, ATTACK_ID, 1.5 * ctx.strength(), AttributeModifier.Operation.ADD_VALUE);
                modifier(p, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH_ID, 1.5 * grow, AttributeModifier.Operation.ADD_VALUE);
                modifier(p, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID, 1.5 * grow, AttributeModifier.Operation.ADD_VALUE);
            }
            case ELEMENTAL_ENERGY -> {
                p.addEffect(effect(MobEffects.STRENGTH, SHORT, 0));
                p.addEffect(effect(MobEffects.RESISTANCE, SHORT, 0));
            }
            case INTANGIBILITY -> MaskPowers.particles(ctx.level(), ParticleTypes.REVERSE_PORTAL, p.position().add(0, 1, 0), 4, 0.4);
            case POSSIBILITIES -> p.addEffect(effect(MobEffects.LUCK, SHORT, a + 1));
            case SENSORY_APTITUDE -> {
                p.addEffect(effect(MobEffects.NIGHT_VISION, NIGHT_VISION, 0));
                for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(ctx.radius(10)), e -> e != p)) {
                    e.addEffect(effect(MobEffects.GLOWING, SHORT, 0));
                }
            }
            default -> {
            }
        }
        return shared;
    }

    private static void give(ServerPlayer p, List<MobEffectInstance> shared, MobEffectInstance effect) {
        p.addEffect(effect);
        shared.add(effect);
    }

    private static void modifier(Player p, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = p.getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    /** Undoes everything a toggle power changed on the wearer. */
    public static void clearToggle(Player p) {
        remove(p, NeoForgeMod.CREATIVE_FLIGHT, FLIGHT_ID);
        remove(p, Attributes.SCALE, SCALE_ID);
        remove(p, Attributes.ATTACK_DAMAGE, ATTACK_ID);
        remove(p, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH_ID);
        remove(p, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID);
        MaskState state = MaskState.get(p);
        if (state.madeSilent) {
            p.setSilent(false);
            state.madeSilent = false;
        }
    }

    private static void remove(Player p, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance instance = p.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
