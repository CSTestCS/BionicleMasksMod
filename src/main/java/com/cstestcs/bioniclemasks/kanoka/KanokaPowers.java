package com.cstestcs.bioniclemasks.kanoka;

import com.cstestcs.bioniclemasks.component.KanokaData;
import com.cstestcs.bioniclemasks.lore.DiskTrait;
import com.cstestcs.bioniclemasks.lore.KanokaPower;
import com.cstestcs.bioniclemasks.lore.Metru;
import com.cstestcs.bioniclemasks.registry.ModEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What each Kanoka power does when a disk strikes. The power level (1-9) scales strength and duration;
 * Resonant disks spread their power to everything near the impact.
 */
public final class KanokaPowers {
    private KanokaPowers() {
    }

    /** Blocks that Regenerate restores, and Weaken cracks. */
    private static final Map<Block, Block> CRACKED_TO_WHOLE = Map.of(
            Blocks.CRACKED_STONE_BRICKS, Blocks.STONE_BRICKS,
            Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS,
            Blocks.CRACKED_DEEPSLATE_TILES, Blocks.DEEPSLATE_TILES,
            Blocks.CRACKED_NETHER_BRICKS, Blocks.NETHER_BRICKS,
            Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.CHIPPED_ANVIL, Blocks.ANVIL,
            Blocks.DAMAGED_ANVIL, Blocks.CHIPPED_ANVIL,
            Blocks.COARSE_DIRT, Blocks.DIRT,
            Blocks.DIRT, Blocks.GRASS_BLOCK,
            Blocks.COBBLESTONE, Blocks.STONE);

    /** The undamaged form of a cracked or worn block, or null. */
    public static Block wholeBlockFor(Block block) {
        return CRACKED_TO_WHOLE.get(block);
    }

    public static int durationTicks(int level, KanokaData data) {
        int ticks = 20 * (4 + 2 * level);
        return data.has(DiskTrait.LASTING) ? ticks * 3 / 2 : ticks;
    }

    /**
     * Releases a disk's power.
     *
     * @param power      the disk power, or null for the Disk of Time
     * @param hitEntity  the entity struck directly, if any
     * @param hitBlock   the block struck, if any
     */
    public static void release(ServerLevel level, Entity owner, Projectile disk, KanokaPower power, int powerLevel, KanokaData data,
                               Vec3 at, Entity hitEntity, BlockPos hitBlock) {
        int duration = durationTicks(powerLevel, data);
        boolean resonant = data.has(DiskTrait.RESONANT);

        // Who is affected.
        List<LivingEntity> targets = new ArrayList<>();
        if (power == null || resonant) {
            double radius = power == null ? 6.0 : 1.5 + 0.4 * powerLevel;
            targets.addAll(level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                    e -> e != owner && e.isAlive()));
        } else if (hitEntity instanceof LivingEntity living) {
            targets.add(living);
        }

        // Which blocks are affected.
        List<BlockPos> blocks = new ArrayList<>();
        if (hitBlock != null) {
            blocks.add(hitBlock);
            if (resonant) {
                BlockPos.betweenClosed(hitBlock.offset(-1, -1, -1), hitBlock.offset(1, 1, 1)).forEach(p -> blocks.add(p.immutable()));
            }
        }

        if (power == null) {
            timeBurst(level, at, targets, duration);
        } else {
            for (LivingEntity target : targets) {
                affectEntity(level, owner, power, powerLevel, duration, target);
            }
            for (BlockPos pos : blocks) {
                affectBlock(level, power, powerLevel, duration, pos);
            }
            particles(level, power, at);
        }

        if (data.has(DiskTrait.GALE)) {
            gust(level, owner, at, 1.0 + powerLevel * 0.15);
        }
        if (!data.has(DiskTrait.SILENT)) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F,
                    power == null ? 0.5F : 0.6F + 0.1F * power.digit());
        }
    }

    private static void affectEntity(ServerLevel level, Entity owner, KanokaPower power, int powerLevel, int duration, LivingEntity target) {
        int amp = Math.max(0, powerLevel - 1);
        switch (power) {
            case RECONSTITUTE -> {
                // The target's molecules are rearranged at random for a while.
                target.addEffect(new MobEffectInstance(ModEffects.RECONSTITUTED, duration, amp));
                if (target instanceof Player) {
                    target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, Math.min(duration, 200), 0));
                }
            }
            case FREEZE -> {
                // Frozen solid: powder-snow freezing plus heavy slowness; fire is put out.
                target.clearFire();
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + duration * 2));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, Math.min(5, powerLevel / 2 + 1)));
                if (powerLevel >= 6) {
                    target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 2));
                }
            }
            case WEAKEN -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, Math.min(3, powerLevel / 3)));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, Math.min(2, powerLevel / 4)));
                if (powerLevel >= 5) {
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration / 2, 0));
                }
            }
            case REMOVE_POISON -> {
                for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
                    if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        target.removeEffect(effect.getEffect());
                    }
                }
                target.setTicksFrozen(0);
            }
            case ENLARGE -> {
                target.removeEffect(ModEffects.SHRUNKEN);
                target.addEffect(new MobEffectInstance(ModEffects.ENLARGED, duration, amp));
            }
            case SHRINK -> {
                target.removeEffect(ModEffects.ENLARGED);
                target.addEffect(new MobEffectInstance(ModEffects.SHRUNKEN, duration, amp));
            }
            case REGENERATE -> {
                target.heal(2.0F + powerLevel);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration / 2, Math.min(2, powerLevel / 3)));
            }
            case TELEPORT -> Teleports.randomNearby(level, target, 4 + 3 * powerLevel);
        }
    }

    private static void affectBlock(ServerLevel level, KanokaPower power, int powerLevel, int duration, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || level.getBlockEntity(pos) != null) {
            return;
        }
        float hardness = state.getDestroySpeed(level, pos);
        boolean breakable = hardness >= 0 && hardness < 50;
        RandomSource random = level.getRandom();
        switch (power) {
            case RECONSTITUTE -> {
                Block other = Tags.randomFamilyMember(state, random);
                if (other != null && breakable) {
                    BlockReverter.swap(level, pos, other.defaultBlockState(), duration);
                }
            }
            case FREEZE -> {
                int r = 1 + powerLevel / 3;
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
                    BlockState s = level.getBlockState(p);
                    if (s.is(Blocks.WATER) && s.getFluidState().isSource()) {
                        level.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
                    } else if (s.is(Blocks.LAVA) && s.getFluidState().isSource() && powerLevel >= 5) {
                        level.setBlockAndUpdate(p, Blocks.OBSIDIAN.defaultBlockState());
                    } else if (s.is(Blocks.FIRE)) {
                        level.removeBlock(p, false);
                    }
                }
            }
            case WEAKEN -> {
                Block cracked = null;
                for (Map.Entry<Block, Block> e : CRACKED_TO_WHOLE.entrySet()) {
                    if (e.getValue() == state.getBlock() && e.getKey() != Blocks.DIRT && e.getKey() != Blocks.COBBLESTONE) {
                        cracked = e.getKey();
                    }
                }
                if (cracked != null) {
                    level.setBlockAndUpdate(pos, cracked.defaultBlockState());
                } else if (breakable && hardness <= powerLevel * 0.4F) {
                    level.destroyBlock(pos, true);
                }
            }
            case REMOVE_POISON -> {
                if (state.is(Blocks.WITHER_ROSE)) {
                    level.setBlockAndUpdate(pos, Blocks.POPPY.defaultBlockState());
                }
            }
            case ENLARGE -> {
                for (int i = 0; i < 1 + powerLevel / 3; i++) {
                    BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos);
                }
            }
            case SHRINK -> {
                if (state.is(Blocks.TALL_GRASS)) {
                    level.setBlockAndUpdate(pos, Blocks.SHORT_GRASS.defaultBlockState());
                } else if (state.is(Blocks.LARGE_FERN)) {
                    level.setBlockAndUpdate(pos, Blocks.FERN.defaultBlockState());
                }
            }
            case REGENERATE -> {
                Block whole = CRACKED_TO_WHOLE.get(state.getBlock());
                if (whole != null) {
                    level.setBlockAndUpdate(pos, whole.defaultBlockState());
                } else {
                    BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos);
                }
            }
            case TELEPORT -> {
                if (!breakable) {
                    return;
                }
                int r = 2 + powerLevel;
                for (int attempt = 0; attempt < 16; attempt++) {
                    BlockPos target = pos.offset(random.nextInt(2 * r + 1) - r, random.nextInt(2 * r + 1) - r, random.nextInt(2 * r + 1) - r);
                    if (level.isInWorldBounds(target) && level.getBlockState(target).isAir()) {
                        level.removeBlock(pos, false);
                        level.setBlockAndUpdate(target, state);
                        level.sendParticles(ParticleTypes.PORTAL, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.2);
                        return;
                    }
                }
            }
        }
    }

    /** The Disk of Time: everything nearby is slowed in time, and projectiles hang in the air. */
    private static void timeBurst(ServerLevel level, Vec3 at, List<LivingEntity> targets, int duration) {
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(ModEffects.TIME_SLOWED, duration, 2));
        }
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(at, at).inflate(8))) {
            p.setDeltaMovement(p.getDeltaMovement().scale(0.05));
            p.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 80, 3, 2, 3, 0.02);
    }

    /** The Gale trait: a gust of wind blasts everything away from the impact. */
    public static void gust(ServerLevel level, Entity owner, Vec3 at, double strength) {
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(at, at).inflate(3.5), e -> e != owner)) {
            Vec3 push = e.position().subtract(at).normalize().scale(strength).add(0, 0.35, 0);
            e.push(push.x, push.y, push.z);
            e.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.GUST, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void particles(ServerLevel level, KanokaPower power, Vec3 at) {
        ParticleOptions particle = switch (power) {
            case RECONSTITUTE -> ParticleTypes.WITCH;
            case FREEZE -> ParticleTypes.SNOWFLAKE;
            case WEAKEN -> ParticleTypes.SMOKE;
            case REMOVE_POISON -> ParticleTypes.HAPPY_VILLAGER;
            case ENLARGE, SHRINK -> ParticleTypes.ENCHANT;
            case REGENERATE -> ParticleTypes.HEART;
            case TELEPORT -> ParticleTypes.PORTAL;
        };
        level.sendParticles(particle, at.x, at.y, at.z, 24, 0.5, 0.5, 0.5, 0.05);
    }

    /** Direct-hit damage: light, since Kanoka are meant to deliver their power rather than wound. */
    public static float impactDamage(int powerLevel, KanokaData data) {
        float damage = 1.0F + powerLevel * 0.5F;
        if (data.metru() == Metru.TA) {
            damage += 2.0F;
        }
        if (data.has(DiskTrait.HEAVY)) {
            damage += 2.0F;
        }
        return damage;
    }
}
