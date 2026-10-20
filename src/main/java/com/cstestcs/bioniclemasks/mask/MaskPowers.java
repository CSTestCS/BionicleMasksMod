package com.cstestcs.bioniclemasks.mask;

import com.cstestcs.bioniclemasks.item.KanohiItem;
import com.cstestcs.bioniclemasks.item.KanohiStacks;
import com.cstestcs.bioniclemasks.kanoka.BlockReverter;
import com.cstestcs.bioniclemasks.kanoka.KanokaPowers;
import com.cstestcs.bioniclemasks.kanoka.Teleports;
import com.cstestcs.bioniclemasks.lore.KanohiForm;
import com.cstestcs.bioniclemasks.lore.KanohiPower;
import com.cstestcs.bioniclemasks.registry.ModEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The active powers of every Kanohi: what happens when the wearer presses the "Use mask power" key.
 * Passive and toggled effects are applied every second by {@link MaskTicker}; defensive powers hook
 * into damage and targeting in {@link MaskEvents}.
 */
public final class MaskPowers {
    private MaskPowers() {
    }

    // ------------------------------------------------------------------------------------------
    // Key handling
    // ------------------------------------------------------------------------------------------

    /** The "Use mask power" key. Sneaking selects the alternate use where a mask has one. */
    public static void use(ServerPlayer player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(stack.getItem() instanceof KanohiItem)) {
            tell(player, "You are not wearing a Kanohi.", 0xAAAAAA);
            return;
        }
        MaskContext ctx = MaskContext.of(player);
        if (ctx == null) {
            tell(player, "This mask is powerless. Matoran masks hold no power.", 0xAAAAAA);
            return;
        }
        KanohiPower power = ctx.power();
        MaskState state = MaskState.get(player);
        switch (power.kind()) {
            case PASSIVE -> tell(player, label(ctx) + " is always active while worn.", 0x7FD1FF);
            case TOGGLE -> {
                state.active = !state.active;
                state.activeSince = ctx.level().getGameTime();
                if (!state.active) {
                    MaskTicker.clearToggle(player);
                }
                tell(player, label(ctx) + (state.active ? ": on" : ": off"), state.active ? 0x7FFF7F : 0xFF7F7F);
                sound(ctx, state.active ? 1.4F : 0.8F);
            }
            case BURST -> {
                if (player.getCooldowns().isOnCooldown(stack)) {
                    tell(player, label(ctx) + " is still recharging.", 0xFFAA55);
                    return;
                }
                boolean used = burst(ctx, player.isShiftKeyDown());
                if (used) {
                    int ticks = (int) Math.max(20, power.cooldownSeconds() * 20 * ctx.cooldown());
                    player.getCooldowns().addCooldown(stack, ticks);
                    sound(ctx, 1.0F);
                }
            }
        }
    }

    /** The "Cycle" key: a Golden Kanohi switches between the six Mata powers. */
    public static void cycle(ServerPlayer player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(stack.getItem() instanceof KanohiItem) || KanohiStacks.data(stack).form() != KanohiForm.GOLDEN) {
            tell(player, "Only a Golden Kanohi holds more than one power.", 0xAAAAAA);
            return;
        }
        var data = KanohiStacks.data(stack);
        int next = Math.floorMod(data.selected() + 1, KanohiPower.MATA_MASKS.size());
        KanohiStacks.setData(stack, data.withSelected(next));
        MaskState state = MaskState.get(player);
        state.active = false;
        MaskTicker.clearToggle(player);
        KanohiPower now = KanohiPower.MATA_MASKS.get(next);
        tell(player, "Golden Kanohi: " + now.maskName() + " (" + now.powerName() + ")", KanohiStacks.GOLDEN_COLOR);
    }

    public static String label(MaskContext ctx) {
        return ctx.power().maskName() + " (" + ctx.power().powerName() + ")";
    }

    // ------------------------------------------------------------------------------------------
    // Burst powers
    // ------------------------------------------------------------------------------------------

    /** Runs a burst power. Returns false if nothing happened (no target, etc.), so no cooldown is used. */
    private static boolean burst(MaskContext ctx, boolean sneaking) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        return switch (ctx.power()) {
            case RAU -> rau(ctx);
            case MAHIKI -> {
                p.addEffect(effect(MobEffects.INVISIBILITY, ctx.ticks(6), 0));
                for (Mob mob : mobsAround(ctx, 14)) {
                    if (mob.getTarget() == p) {
                        mob.setTarget(null);
                        mob.addEffect(effect(ModEffects.VISIONS, ctx.ticks(6), 0));
                    }
                }
                particles(level, ParticleTypes.WITCH, p.position().add(0, 1, 0), 40, 0.6);
                yield true;
            }
            case KOMAU -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(16));
                if (target instanceof Player other) {
                    other.addEffect(effect(MobEffects.NAUSEA, ctx.ticks(6), 0));
                    tell(p, other.getName().getString() + "'s will resists, but their thoughts reel.", 0xC080FF);
                    yield true;
                }
                if (!(target instanceof Mob mob) || !sentient(mob) || boss(mob)) {
                    tell(p, "Look at a thinking being (villager, illager, piglin, witch...) to control its mind.", 0xAAAAAA);
                    yield false;
                }
                mob.addEffect(effect(ModEffects.MIND_CONTROLLED, ctx.ticks(20), 0));
                Allies.bind(mob, p, Allies.Bond.CONTROLLED, ctx.ticks(20));
                particles(level, ParticleTypes.ENCHANT, mob.position().add(0, 1, 0), 30, 0.5);
                yield true;
            }
            case MATATU -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(20));
                if (target != null && !boss(target)) {
                    Vec3 dir = p.getEyePosition().subtract(target.position()).normalize();
                    if (sneaking) {
                        launch(target, dir.scale(-1.6 * Math.min(2, ctx.strength())).add(0, 0.5, 0));
                    } else {
                        target.addEffect(effect(MobEffects.LEVITATION, ctx.ticks(2), 1));
                        launch(target, dir.scale(0.9 * Math.min(2, ctx.strength())).add(0, 0.4, 0));
                    }
                    particles(level, ParticleTypes.END_ROD, target.position().add(0, 1, 0), 15, 0.4);
                    yield true;
                }
                // No creature in sight: pull loose items to the wearer instead.
                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(ctx.radius(12)));
                for (ItemEntity item : items) {
                    launch(item, p.position().subtract(item.position()).normalize().scale(0.8).add(0, 0.2, 0));
                }
                yield !items.isEmpty();
            }
            case ELDA -> {
                int count = 0;
                for (LivingEntity e : livingAround(ctx, 24)) {
                    e.removeEffect(MobEffects.INVISIBILITY);
                    e.addEffect(effect(MobEffects.GLOWING, ctx.ticks(10), 0));
                    count++;
                }
                Player nearest = null;
                for (Player other : level.players()) {
                    if (other != p && other.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof KanohiItem
                            && (nearest == null || other.distanceToSqr(p) < nearest.distanceToSqr(p))) {
                        nearest = other;
                    }
                }
                String mask = nearest == null ? "You sense no other Kanohi nearby."
                        : "You sense a Kanohi " + (int) nearest.distanceTo(p) + " blocks to the " + Raycast.direction(p, nearest) + "."
                        + (KanohiStacks.data(nearest.getItemBySlot(EquipmentSlot.HEAD)).form() == KanohiForm.LEGENDARY
                        ? " Its power is overwhelming - a Legendary mask!" : "");
                tell(p, "Detected " + count + " beings. " + mask, 0x7FFFD4);
                yield true;
            }
            case SULETU -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(32));
                if (target == null) {
                    yield false;
                }
                if (target instanceof Player other) {
                    tell(other, p.getName().getString() + "'s thoughts reach into your mind.", 0xC0A0FF);
                    tell(p, other.getName().getString() + " feels " + feeling(other) + ", holding "
                            + other.getMainHandItem().getHoverName().getString() + ", and is thinking of " + Raycast.direction(other, p) + "...", 0xC0A0FF);
                } else {
                    tell(p, thoughts(target), 0xC0A0FF);
                }
                yield true;
            }
            case ARTHRON -> {
                double radius = ctx.radius(p.isInWater() ? 48 : 24);
                List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), e -> e != p);
                LivingEntity closestFoe = null;
                for (LivingEntity e : found) {
                    e.addEffect(effect(MobEffects.GLOWING, ctx.ticks(8), 0));
                    if (e instanceof Enemy && (closestFoe == null || e.distanceToSqr(p) < closestFoe.distanceToSqr(p))) {
                        closestFoe = e;
                    }
                }
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.5F, 0.5F);
                tell(p, "Sonar: " + found.size() + " echoes." + (closestFoe == null ? ""
                        : " Nearest threat " + (int) closestFoe.distanceTo(p) + " blocks " + Raycast.direction(p, closestFoe) + "."), 0x5FD3FF);
                yield true;
            }
            case FAXON -> faxon(ctx);
            case ZATTH -> {
                int count = 1 + ctx.amp();
                for (int i = 0; i < count; i++) {
                    summon(ctx, EntityTypes.WOLF, ctx.ticks(60), 0);
                }
                tell(p, "Rahi answer the call of the Zatth.", 0x9FE07F);
                yield true;
            }
            case GARAI -> {
                if (sneaking) {
                    p.addEffect(effect(ModEffects.LIGHT_GRAVITY, ctx.ticks(30), ctx.amp()));
                } else {
                    for (LivingEntity e : livingAround(ctx, 7)) {
                        if (Allies.isAllyOf(e, p)) {
                            continue;
                        }
                        e.addEffect(effect(ModEffects.CRUSHING_GRAVITY, ctx.ticks(5), ctx.amp()));
                        launch(e, new Vec3(0, -1.2, 0));
                    }
                    particles(level, ParticleTypes.REVERSE_PORTAL, p.position(), 60, 3);
                }
                yield true;
            }
            case KUALSI -> {
                BlockHitResult hit = Raycast.block(p, ctx.radius(40));
                if (hit.getType() == HitResult.Type.MISS) {
                    yield false;
                }
                Vec3 spot = Teleports.findStandingSpot(level, hit.getBlockPos().relative(hit.getDirection()), 3);
                if (spot == null) {
                    yield false;
                }
                particles(level, ParticleTypes.PORTAL, p.position().add(0, 1, 0), 30, 0.5);
                Teleports.moveTo(level, p, spot);
                yield true;
            }
            case CLAIRVOYANCE -> {
                vision(ctx);
                yield true;
            }
            case EMULATION -> {
                BlockHitResult hit = Raycast.block(p, ctx.radius(32));
                Vec3 at = hit.getLocation();
                level.playSound(null, at.x, at.y, at.z, SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 2.0F, 0.8F);
                for (Mob mob : mobsAround(ctx, 20)) {
                    if (mob.getTarget() == p) {
                        mob.setTarget(null);
                    }
                    mob.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
                }
                yield true;
            }
            case RAHI_CONTROL -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(16));
                if (!(target instanceof Mob mob) || sentient(mob) || boss(mob)) {
                    tell(p, "Look at a Rahi - an animal or beast - to control it.", 0xAAAAAA);
                    yield false;
                }
                if (mob instanceof TamableAnimal tamable && !tamable.isTame()) {
                    tamable.tame(p);
                } else {
                    mob.addEffect(effect(ModEffects.MIND_CONTROLLED, ctx.ticks(30), 0));
                    Allies.bind(mob, p, Allies.Bond.CONTROLLED, ctx.ticks(30));
                }
                particles(level, ParticleTypes.HAPPY_VILLAGER, mob.position().add(0, 1, 0), 20, 0.5);
                yield true;
            }
            case AVOHKII -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(32));
                Vec3 end = target != null ? target.position().add(0, target.getBbHeight() / 2, 0)
                        : Raycast.block(p, ctx.radius(32)).getLocation();
                beam(level, p.getEyePosition(), end, ParticleTypes.END_ROD);
                if (target != null) {
                    float damage = (float) (4 * ctx.strength());
                    if (undead(target)) {
                        damage *= 2;
                        target.igniteForSeconds(4);
                    }
                    target.hurtServer(level, level.damageSources().indirectMagic(p, p), damage);
                    target.addEffect(effect(MobEffects.GLOWING, ctx.ticks(8), 0));
                }
                yield true;
            }
            case KRAAHKAN -> {
                for (LivingEntity e : livingAround(ctx, 9)) {
                    if (Allies.isAllyOf(e, p)) {
                        continue;
                    }
                    e.addEffect(effect(MobEffects.WEAKNESS, ctx.ticks(10), ctx.amp()));
                    e.addEffect(effect(MobEffects.BLINDNESS, ctx.ticks(4), 0));
                    e.addEffect(effect(MobEffects.DARKNESS, ctx.ticks(10), 0));
                    e.hurtServer(level, level.damageSources().indirectMagic(p, p), (float) (2 * ctx.strength()));
                }
                // The Mask of Shadows drains any wearer who is not a Makuta.
                p.addEffect(effect(MobEffects.WITHER, 60, 0));
                p.addEffect(effect(MobEffects.HUNGER, 200, 1));
                particles(level, ParticleTypes.SQUID_INK, p.position().add(0, 1, 0), 80, 3);
                yield true;
            }
            case AVSA -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(16));
                if (target == null) {
                    yield false;
                }
                float drained = (float) (3 * ctx.strength());
                target.hurtServer(level, level.damageSources().indirectMagic(p, p), drained);
                p.heal(drained);
                p.getFoodData().eat(2, 0.3F);
                beam(level, target.position().add(0, 1, 0), p.position().add(0, 1, 0), ParticleTypes.SCULK_SOUL);
                yield true;
            }
            case CRAST -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(20));
                if (target == null || boss(target)) {
                    yield false;
                }
                Vec3 away = target.position().subtract(p.position()).normalize().scale(2.0 + 1.2 * ctx.strength());
                launch(target, away.add(0, 0.5, 0));
                yield true;
            }
            case FELNAS -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(20));
                if (target == null) {
                    yield false;
                }
                for (MobEffectInstance e : List.copyOf(target.getActiveEffects())) {
                    if (e.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                        target.removeEffect(e.getEffect());
                    }
                }
                if (target instanceof ServerPlayer other) {
                    ItemStack theirMask = other.getItemBySlot(EquipmentSlot.HEAD);
                    if (theirMask.getItem() instanceof KanohiItem) {
                        other.getCooldowns().addCooldown(theirMask, ctx.ticks(10));
                        MaskState.get(other).active = false;
                        MaskTicker.clearToggle(other);
                        tell(other, "Your mask's power has been disrupted!", 0xFF5555);
                    }
                }
                target.clearFire();
                particles(level, ParticleTypes.SMOKE, target.position().add(0, 1, 0), 25, 0.5);
                yield true;
            }
            case JUTLIN -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(12));
                if (target != null) {
                    target.addEffect(effect(MobEffects.WITHER, ctx.ticks(5), Math.min(2, ctx.amp())));
                    for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        ItemStack armor = target.getItemBySlot(slot);
                        if (armor.isDamageableItem()) {
                            armor.setDamageValue(Math.min(armor.getMaxDamage() - 1, armor.getDamageValue() + (int) (armor.getMaxDamage() * 0.1 * ctx.strength())));
                        }
                    }
                    yield true;
                }
                BlockHitResult hit = Raycast.block(p, ctx.radius(12));
                if (hit.getType() == HitResult.Type.MISS) {
                    yield false;
                }
                rot(level, hit.getBlockPos());
                yield true;
            }
            case KIRIL -> {
                double fraction = 0.25 * ctx.strength();
                var inventory = p.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    mend(inventory.getItem(i), fraction);
                }
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    mend(p.getItemBySlot(slot), fraction);
                }
                int r = (int) Math.round(ctx.radius(4));
                for (BlockPos pos : BlockPos.betweenClosed(p.blockPosition().offset(-r, -r, -r), p.blockPosition().offset(r, r, r))) {
                    Block whole = KanokaPowers.wholeBlockFor(level.getBlockState(pos).getBlock());
                    if (whole != null && whole != Blocks.GRASS_BLOCK && whole != Blocks.STONE && whole != Blocks.DIRT) {
                        level.setBlockAndUpdate(pos, whole.defaultBlockState());
                    }
                }
                particles(level, ParticleTypes.WAX_OFF, p.position().add(0, 1, 0), 40, 2);
                yield true;
            }
            case MOHTREK -> {
                p.addEffect(effect(ModEffects.TIME_ECHO, ctx.ticks(20), ctx.amp()));
                tell(p, (ctx.amp() + 1) + " echoes of your past self stand with you.", 0xE0C060);
                yield true;
            }
            case OLISI -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(20));
                if (target == null || boss(target)) {
                    yield false;
                }
                target.addEffect(effect(ModEffects.VISIONS, ctx.ticks(8), 0));
                target.addEffect(effect(MobEffects.SLOWNESS, ctx.ticks(8), 1));
                if (target instanceof Player) {
                    target.addEffect(effect(MobEffects.NAUSEA, ctx.ticks(8), 0));
                } else if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
                particles(level, ParticleTypes.WITCH, target.position().add(0, 1, 0), 25, 0.5);
                yield true;
            }
            case OLMAK -> sneaking ? dimensionGate(ctx) : distanceGate(ctx);
            case RODE -> {
                StringBuilder truths = new StringBuilder();
                int shown = 0;
                for (LivingEntity e : livingAround(ctx, 24)) {
                    e.removeEffect(MobEffects.INVISIBILITY);
                    e.addEffect(effect(MobEffects.GLOWING, ctx.ticks(10), 0));
                    if (shown < 4 && (e.isInvisible() || e.hasCustomName() || e instanceof Player)) {
                        truths.append(" ").append(e.getName().getString()).append(" is truly a ")
                                .append(e.getType().getDescription().getString()).append(".");
                        shown++;
                    }
                }
                tell(p, "The truth is revealed." + truths, 0xFFFFFF);
                yield true;
            }
            case SHELEK -> {
                for (LivingEntity e : livingAround(ctx, 10)) {
                    e.addEffect(effect(ModEffects.SILENCED, ctx.ticks(10), 0));
                    e.setSilent(true);
                    if (e instanceof Mob mob && mob.getTarget() == p && mob.distanceTo(p) > 4) {
                        mob.setTarget(null);
                    }
                }
                yield true;
            }
            case TRYNA -> {
                int count = 1 + ctx.amp() / 2;
                for (int i = 0; i < count; i++) {
                    Mob raised = summon(ctx, level.getRandom().nextBoolean() ? EntityTypes.ZOMBIE : EntityTypes.SKELETON, ctx.ticks(60), 1);
                    if (raised != null) {
                        raised.addEffect(effect(MobEffects.FIRE_RESISTANCE, ctx.ticks(60) + 20, 0));
                    }
                }
                tell(p, "The dead rise to serve the wearer of the Tryna.", 0x88AA66);
                yield true;
            }
            case CHARISMA -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(16));
                if (target instanceof AbstractVillager) {
                    p.addEffect(effect(MobEffects.HERO_OF_THE_VILLAGE, ctx.ticks(60), ctx.amp()));
                    tell(p, "The villager hangs on your every word.", 0xFFD27F);
                    yield true;
                }
                if (!(target instanceof Mob mob) || boss(mob)) {
                    yield false;
                }
                Allies.bind(mob, p, Allies.Bond.CHARMED, ctx.ticks(60));
                particles(level, ParticleTypes.HEART, mob.position().add(0, 1.5, 0), 6, 0.4);
                yield true;
            }
            case CONJURING -> conjure(ctx, sneaking);
            case FUSION -> {
                LivingEntity target = Raycast.living(level, p, ctx.radius(8));
                boolean ally = target instanceof Player || (target instanceof OwnableEntity own && own.getOwner() == p)
                        || (target != null && Allies.isAllyOf(target, p));
                if (!ally) {
                    tell(p, "Look at an ally - a player, or a creature you own - to fuse with it.", 0xAAAAAA);
                    yield false;
                }
                int ticks = ctx.ticks(30);
                for (LivingEntity e : new LivingEntity[]{p, target}) {
                    e.addEffect(effect(ModEffects.FUSED, ticks, ctx.amp()));
                    e.addEffect(effect(MobEffects.ABSORPTION, ticks, Math.min(4, (int) (target.getHealth() / 4))));
                }
                beam(level, p.position().add(0, 1, 0), target.position().add(0, 1, 0), ParticleTypes.TOTEM_OF_UNDYING);
                yield true;
            }
            case INCOMPREHENSION -> {
                for (Mob mob : mobsAround(ctx, 12)) {
                    if (boss(mob)) {
                        continue;
                    }
                    mob.setTarget(null);
                    mob.addEffect(effect(ModEffects.VISIONS, ctx.ticks(10), 0));
                }
                yield true;
            }
            case PSYCHOMETRY -> psychometry(ctx);
            case SONICS -> {
                Vec3 look = p.getLookAngle();
                for (LivingEntity e : livingAround(ctx, 9)) {
                    Vec3 to = e.position().subtract(p.position()).normalize();
                    if (to.dot(look) > 0.6 && !Allies.isAllyOf(e, p)) {
                        e.hurtServer(level, level.damageSources().sonicBoom(p), (float) (4 * ctx.strength()));
                        launch(e, to.scale(1.2).add(0, 0.3, 0));
                    }
                }
                int r = (int) Math.round(ctx.radius(5));
                for (BlockPos pos : BlockPos.betweenClosed(p.blockPosition().offset(-r, -r, -r), p.blockPosition().offset(r, r, r))) {
                    Vec3 to = Vec3.atCenterOf(pos).subtract(p.getEyePosition()).normalize();
                    if (to.dot(look) > 0.6 && level.getBlockState(pos).is(com.cstestcs.bioniclemasks.kanoka.Tags.block("impermeable"))) {
                        level.destroyBlock(pos, false);
                    }
                }
                Vec3 at = p.getEyePosition().add(look.scale(2));
                level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.2F);
                yield true;
            }
            case VAHI -> {
                if (sneaking) {
                    p.addEffect(effect(MobEffects.SPEED, ctx.ticks(15), 3));
                    p.addEffect(effect(MobEffects.HASTE, ctx.ticks(15), 2));
                    tell(p, "Time quickens around you.", 0xFFD45A);
                } else {
                    for (LivingEntity e : livingAround(ctx, 14)) {
                        if (!Allies.isAllyOf(e, p)) {
                            e.addEffect(effect(ModEffects.TIME_SLOWED, ctx.ticks(10), 2));
                        }
                    }
                    for (Projectile proj : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(ctx.radius(14)), e -> e.getOwner() != p)) {
                        proj.setDeltaMovement(proj.getDeltaMovement().scale(0.05));
                        proj.hurtMarked = true;
                    }
                    particles(level, ParticleTypes.END_ROD, p.position().add(0, 1, 0), 80, 5);
                    tell(p, "Time slows to a crawl.", 0xFFD45A);
                }
                yield true;
            }
            case IGNIKA -> {
                if (sneaking) {
                    LivingEntity target = Raycast.living(level, p, ctx.radius(16));
                    if (target == null) {
                        yield false;
                    }
                    float drained = (float) (6 * ctx.strength());
                    target.hurtServer(level, level.damageSources().indirectMagic(p, p), drained);
                    p.heal(drained);
                    beam(level, target.position().add(0, 1, 0), p.position().add(0, 1, 0), ParticleTypes.SCULK_SOUL);
                } else {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(ctx.radius(12)),
                            e -> e == p || e instanceof Player || (e instanceof OwnableEntity own && own.getOwner() == p) || Allies.isAllyOf(e, p))) {
                        e.heal(e.getMaxHealth());
                        e.addEffect(effect(MobEffects.REGENERATION, ctx.ticks(10), 1));
                        particles(level, ParticleTypes.TOTEM_OF_UNDYING, e.position().add(0, 1, 0), 20, 0.5);
                    }
                }
                yield true;
            }
            case CREATION -> {
                int r = (int) Math.round(ctx.radius(8));
                var random = level.getRandom();
                for (int i = 0; i < 40; i++) {
                    BlockPos pos = p.blockPosition().offset(random.nextInt(2 * r + 1) - r, random.nextInt(5) - 2, random.nextInt(2 * r + 1) - r);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.DIRT) && level.getBlockState(pos.above()).isAir()) {
                        level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
                    } else {
                        BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos);
                    }
                }
                List<EntityType<? extends Mob>> life = List.of(EntityTypes.COW, EntityTypes.SHEEP, EntityTypes.PIG, EntityTypes.CHICKEN, EntityTypes.RABBIT);
                for (int i = 0; i < 1 + ctx.amp() / 2; i++) {
                    Mob animal = life.get(random.nextInt(life.size())).create(level, EntitySpawnReason.MOB_SUMMONED);
                    if (animal != null) {
                        Vec3 spot = Teleports.findStandingSpot(level, p.blockPosition().offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3), 3);
                        if (spot != null) {
                            animal.setPos(spot.x, spot.y, spot.z);
                            level.addFreshEntity(animal);
                        }
                    }
                }
                particles(level, ParticleTypes.HAPPY_VILLAGER, p.position(), 80, r / 2.0);
                yield true;
            }
            default -> false;
        };
    }

    // ------------------------------------------------------------------------------------------
    // Larger powers
    // ------------------------------------------------------------------------------------------

    private static boolean rau(MaskContext ctx) {
        LivingEntity target = Raycast.living(ctx.level(), ctx.player(), ctx.radius(24));
        if (target == null) {
            tell(ctx.player(), "Look at a creature to understand it.", 0xAAAAAA);
            return false;
        }
        tell(ctx.player(), thoughts(target), 0xFFE08A);
        return true;
    }

    private static String thoughts(LivingEntity target) {
        String name = target.getName().getString();
        String mood;
        if (target instanceof Mob mob && mob.getTarget() != null) {
            mood = "is hunting " + mob.getTarget().getName().getString();
        } else if (Allies.isAlly(target)) {
            mood = "is bound to serve a mask wearer";
        } else if (target instanceof Enemy) {
            mood = "is looking for prey";
        } else if (target instanceof Animal) {
            mood = "is calm, thinking of food";
        } else {
            mood = "is going about its day";
        }
        return name + " " + mood + ". It feels " + feeling(target) + ".";
    }

    private static String feeling(LivingEntity e) {
        float f = e.getHealth() / e.getMaxHealth();
        return f > 0.9 ? "unhurt" : f > 0.5 ? "bruised" : f > 0.25 ? "badly hurt" : "close to death";
    }

    private static boolean faxon(MaskContext ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity nearest = null;
        for (LivingEntity e : livingAround(ctx, 16)) {
            if (!(e instanceof Player) && (nearest == null || e.distanceToSqr(p) < nearest.distanceToSqr(p))) {
                nearest = e;
            }
        }
        if (nearest == null) {
            tell(p, "No creature is near enough to borrow from.", 0xAAAAAA);
            return false;
        }
        int t = ctx.ticks(30);
        int a = Math.min(2, ctx.amp());
        EntityType<?> type = nearest.getType();
        String gift;
        if (nearest.getType().is(com.cstestcs.bioniclemasks.kanoka.Tags.entity("aquatic"))) {
            p.addEffect(effect(MobEffects.WATER_BREATHING, t, 0));
            p.addEffect(effect(MobEffects.DOLPHINS_GRACE, t, 0));
            gift = "gills and fins";
        } else if (nearest.fireImmune()) {
            p.addEffect(effect(MobEffects.FIRE_RESISTANCE, t, 0));
            gift = "immunity to fire";
        } else if (type == EntityTypes.BAT || type == EntityTypes.PARROT || type == EntityTypes.BEE || type == EntityTypes.PHANTOM
                || type == EntityTypes.ALLAY || type == EntityTypes.VEX || type == EntityTypes.CHICKEN || nearest.isNoGravity()) {
            p.addEffect(effect(MobEffects.SLOW_FALLING, t, 0));
            p.addEffect(effect(MobEffects.JUMP_BOOST, t, a));
            gift = "the lightness of flight";
        } else if (type == EntityTypes.RABBIT || type == EntityTypes.HORSE || type == EntityTypes.GOAT || type == EntityTypes.FROG) {
            p.addEffect(effect(MobEffects.JUMP_BOOST, t, a + 1));
            p.addEffect(effect(MobEffects.SPEED, t, a));
            gift = "a mighty leap";
        } else if (type == EntityTypes.IRON_GOLEM || type == EntityTypes.RAVAGER || type == EntityTypes.HOGLIN || type == EntityTypes.WARDEN
                || type == EntityTypes.POLAR_BEAR) {
            p.addEffect(effect(MobEffects.STRENGTH, t, a));
            p.addEffect(effect(MobEffects.RESISTANCE, t, 0));
            gift = "great strength";
        } else if (type == EntityTypes.SPIDER || type == EntityTypes.CAVE_SPIDER || type == EntityTypes.CAT || type == EntityTypes.OCELOT
                || type == EntityTypes.FOX) {
            p.addEffect(effect(MobEffects.NIGHT_VISION, t, 0));
            p.addEffect(effect(MobEffects.SPEED, t, a));
            gift = "keen night eyes";
        } else if (type == EntityTypes.ENDERMAN || type == EntityTypes.SHULKER || type == EntityTypes.ENDERMITE) {
            Teleports.randomNearby(ctx.level(), p, 12);
            gift = "the power to blink through space";
        } else if (undead(nearest)) {
            p.addEffect(effect(MobEffects.RESISTANCE, t, a));
            gift = "the toughness of the undead";
        } else {
            p.addEffect(effect(MobEffects.SPEED, t, 0));
            p.addEffect(effect(MobEffects.REGENERATION, t / 3, 0));
            gift = "its vitality";
        }
        tell(p, "Faxon: you take on " + nearest.getName().getString() + "'s " + gift + ".", 0x9FE07F);
        return true;
    }

    private static void vision(MaskContext ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        List<String> lines = new ArrayList<>();
        lines.add(level.isThundering() ? "You see lightning tearing the sky." : level.isRaining() ? "You see the rain passing." : "You see clouds gathering, or perhaps not.");
        int foes = 0;
        for (Mob mob : mobsAround(ctx, 32)) {
            if (mob instanceof Enemy && mob.hasLineOfSight(p)) {
                mob.addEffect(effect(MobEffects.GLOWING, ctx.ticks(10), 0));
                foes++;
            }
        }
        lines.add(foes == 0 ? "No foe will find you soon." : foes + " foes will soon come for you - they glow in your vision.");
        tell(p, "A vision: " + String.join(" ", lines), 0xB0A0FF);
    }

    /** A Clairvoyance vision may arrive unbidden - the wearer cannot control when. */
    public static void unbiddenVision(MaskContext ctx) {
        vision(ctx);
    }

    private static boolean psychometry(MaskContext ctx) {
        ServerPlayer p = ctx.player();
        LivingEntity target = Raycast.living(ctx.level(), p, ctx.radius(8));
        if (target != null) {
            StringBuilder s = new StringBuilder(target.getName().getString()).append(": ");
            s.append("has lived ").append(target.tickCount / 20).append(" seconds here");
            LivingEntity hurtBy = target.getLastHurtByMob();
            if (hurtBy != null) {
                s.append(", was last hurt by ").append(hurtBy.getName().getString());
            }
            if (target instanceof OwnableEntity own && own.getOwner() != null) {
                s.append(", belongs to ").append(own.getOwner().getName().getString());
            }
            tell(p, s.append(".").toString(), 0xE0C0A0);
            return true;
        }
        BlockHitResult hit = Raycast.block(p, ctx.radius(8));
        if (hit.getType() == HitResult.Type.MISS) {
            return false;
        }
        BlockEntity be = ctx.level().getBlockEntity(hit.getBlockPos());
        BlockState state = ctx.level().getBlockState(hit.getBlockPos());
        if (be instanceof Container container) {
            int stacks = 0;
            int items = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (!container.getItem(i).isEmpty()) {
                    stacks++;
                    items += container.getItem(i).getCount();
                }
            }
            tell(p, state.getBlock().getName().getString() + " remembers being filled: it holds " + items + " items in " + stacks + " stacks.", 0xE0C0A0);
        } else {
            tell(p, state.getBlock().getName().getString() + " has stood here, quietly, for a long time.", 0xE0C0A0);
        }
        return true;
    }

    private static boolean conjure(MaskContext ctx, boolean platform) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        BlockState conjured = Blocks.AMETHYST_BLOCK.defaultBlockState();
        int ticks = ctx.ticks(20);
        List<BlockPos> spots = new ArrayList<>();
        if (platform) {
            BlockPos below = p.blockPosition().below();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    spots.add(below.offset(dx, 0, dz));
                }
            }
        } else {
            BlockHitResult hit = Raycast.block(p, ctx.radius(16));
            BlockPos base = hit.getType() == HitResult.Type.MISS ? BlockPos.containing(hit.getLocation()) : hit.getBlockPos().relative(hit.getDirection());
            Direction facing = p.getDirection();
            Direction side = facing.getClockWise();
            for (int w = -1; w <= 1; w++) {
                for (int h = 0; h <= 2; h++) {
                    spots.add(base.relative(side, w).above(h));
                }
            }
        }
        int placed = 0;
        for (BlockPos pos : spots) {
            if (level.getBlockState(pos).isAir()) {
                BlockReverter.swap(level, pos, conjured, ticks);
                placed++;
            }
        }
        return placed > 0;
    }

    private static boolean distanceGate(MaskContext ctx) {
        ServerPlayer p = ctx.player();
        ServerLevel level = ctx.level();
        BlockHitResult hit = Raycast.block(p, ctx.radius(96));
        Vec3 spot = Teleports.findStandingSpot(level, BlockPos.containing(hit.getLocation()).relative(hit.getDirection()), 8);
        if (spot == null) {
            tell(p, "No gate can open there.", 0xAAAAAA);
            return false;
        }
        Vec3 offset = spot.subtract(p.position());
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4),
                e -> e == p || e instanceof Player || (e instanceof OwnableEntity own && own.getOwner() == p))) {
            particles(level, ParticleTypes.REVERSE_PORTAL, e.position().add(0, 1, 0), 30, 0.5);
            Teleports.moveTo(level, e, e.position().add(offset));
        }
        return true;
    }

    private static boolean dimensionGate(MaskContext ctx) {
        ServerPlayer p = ctx.player();
        if (!ctx.greatOrBetter()) {
            tell(p, "A Noble Olmak is too weak to open a gate between worlds.", 0xAAAAAA);
            return false;
        }
        ServerLevel here = ctx.level();
        ResourceKey<Level> destKey = here.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
        ServerLevel dest = here.getServer().getLevel(destKey);
        if (dest == null) {
            return false;
        }
        double scale = destKey == Level.NETHER ? 1.0 / 8.0 : 8.0;
        int x = (int) Math.floor(p.getX() * scale);
        int z = (int) Math.floor(p.getZ() * scale);
        Vec3 spot = null;
        int top = destKey == Level.NETHER ? 120 : 200;
        for (int y = top; y > 8 && spot == null; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (Teleports.canStand(dest, pos)) {
                spot = Vec3.atBottomCenterOf(pos);
            }
        }
        if (spot == null) {
            // Build a small landing so the gate never drops the wearer into lava or the void.
            BlockPos pad = new BlockPos(x, 70, z);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    dest.setBlockAndUpdate(pad.offset(dx, -1, dz), Blocks.OBSIDIAN.defaultBlockState());
                    for (int dy = 0; dy <= 2; dy++) {
                        dest.setBlockAndUpdate(pad.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                    }
                }
            }
            spot = Vec3.atBottomCenterOf(pad);
        }
        particles(here, ParticleTypes.REVERSE_PORTAL, p.position().add(0, 1, 0), 60, 0.8);
        p.teleport(new TeleportTransition(dest, spot, Vec3.ZERO, p.getYRot(), p.getXRot(), Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private static Mob summon(MaskContext ctx, EntityType<? extends Mob> type, int ticks, int spread) {
        ServerLevel level = ctx.level();
        Mob mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) {
            return null;
        }
        var random = level.getRandom();
        Vec3 spot = Teleports.findStandingSpot(level, ctx.player().blockPosition().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2), 3);
        if (spot == null) {
            spot = ctx.player().position();
        }
        mob.setPos(spot.x, spot.y, spot.z);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        Allies.bind(mob, ctx.player(), Allies.Bond.SUMMONED, ticks);
        particles(level, ParticleTypes.POOF, spot.add(0, 0.5, 0), 12, 0.3 + spread * 0.1);
        return mob;
    }

    private static void mend(ItemStack stack, double fraction) {
        if (stack.isDamageableItem() && stack.isDamaged()) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - (int) Math.ceil(stack.getMaxDamage() * fraction)));
        }
    }

    private static void rot(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState rotted;
        if (state.is(Blocks.STONE)) {
            rotted = Blocks.COBBLESTONE.defaultBlockState();
        } else if (state.is(Blocks.COBBLESTONE)) {
            rotted = Blocks.GRAVEL.defaultBlockState();
        } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL)) {
            rotted = Blocks.COARSE_DIRT.defaultBlockState();
        } else if (state.is(Blocks.STONE_BRICKS)) {
            rotted = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        } else {
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness >= 0 && hardness <= 2.5F && level.getBlockEntity(pos) == null) {
                level.destroyBlock(pos, false);
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0.01);
            }
            return;
        }
        level.setBlockAndUpdate(pos, rotted);
    }

    static boolean sentient(Mob mob) {
        return mob instanceof AbstractVillager || mob instanceof AbstractIllager || mob instanceof AbstractPiglin
                || mob.getType() == EntityTypes.WITCH || mob.getType() == EntityTypes.ENDERMAN;
    }

    static boolean boss(LivingEntity e) {
        return e.getType() == EntityTypes.WITHER || e.getType() == EntityTypes.ENDER_DRAGON || e.getType() == EntityTypes.WARDEN
                || e.getType() == EntityTypes.ELDER_GUARDIAN;
    }

    static boolean undead(LivingEntity e) {
        return e.getType().is(com.cstestcs.bioniclemasks.kanoka.Tags.entity("undead"));
    }

    private static List<LivingEntity> livingAround(MaskContext ctx, double baseRadius) {
        ServerPlayer p = ctx.player();
        return ctx.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(ctx.radius(baseRadius)), e -> e != p && e.isAlive());
    }

    private static List<Mob> mobsAround(MaskContext ctx, double baseRadius) {
        ServerPlayer p = ctx.player();
        return ctx.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(ctx.radius(baseRadius)), Entity::isAlive);
    }

    static MobEffectInstance effect(Holder<MobEffect> effect, int ticks, int amp) {
        return new MobEffectInstance(effect, ticks, amp, true, false, true);
    }

    private static void launch(Entity e, Vec3 velocity) {
        e.setDeltaMovement(velocity);
        e.hurtMarked = true;
    }

    private static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        Vec3 step = to.subtract(from);
        int n = (int) Math.max(4, step.length() * 2);
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(step.scale(i / (double) n));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    static void particles(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
    }

    private static void sound(MaskContext ctx, float pitch) {
        if (!ctx.silent()) {
            ServerPlayer p = ctx.player();
            ctx.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5F, pitch);
        }
    }

    public static void tell(Player player, String text, int color) {
        player.sendOverlayMessage(Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
    }
}
