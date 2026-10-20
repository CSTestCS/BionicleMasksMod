package com.cstestcs.bioniclemasks.kanoka;

import com.cstestcs.bioniclemasks.BionicleMasks;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Reconstitutes at Random: every second the target's molecules are rearranged - its size, speed and
 * strength change at random. Everything returns to normal when the effect ends (see KanokaEvents).
 */
public class ReconstitutedEffect extends MobEffect {
    public static final Identifier SCALE_ID = BionicleMasks.id("reconstituted.scale");
    public static final Identifier SPEED_ID = BionicleMasks.id("reconstituted.speed");
    public static final Identifier DAMAGE_ID = BionicleMasks.id("reconstituted.damage");

    public ReconstitutedEffect() {
        super(MobEffectCategory.HARMFUL, 0xB43CFF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        RandomSource random = entity.getRandom();
        double strength = 0.15 + 0.06 * amplifier;
        scramble(entity.getAttribute(Attributes.SCALE), SCALE_ID, (random.nextDouble() * 2 - 1) * strength * 2);
        scramble(entity.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, (random.nextDouble() * 2 - 1) * strength);
        scramble(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, (random.nextDouble() * 2 - 1) * strength);
        return true;
    }

    private static void scramble(AttributeInstance attribute, Identifier id, double amount) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** Puts the target back together. */
    public static void restore(LivingEntity entity) {
        remove(entity.getAttribute(Attributes.SCALE), SCALE_ID);
        remove(entity.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID);
        remove(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID);
    }

    private static void remove(AttributeInstance attribute, Identifier id) {
        if (attribute != null) {
            attribute.removeModifier(id);
        }
    }
}
