package com.cstestcs.bioniclemasks.registry;

import com.cstestcs.bioniclemasks.BionicleMasks;
import com.cstestcs.bioniclemasks.kanoka.ReconstitutedEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status effects used by Kanoka and Kanohi powers. */
public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, BionicleMasks.MOD_ID);

    /** Kanoka of Enlarge: the target grows by 12% per power level. */
    public static final DeferredHolder<MobEffect, MobEffect> ENLARGED = EFFECTS.register("enlarged", () ->
            new Simple(MobEffectCategory.NEUTRAL, 0xFF9628)
                    .addAttributeModifier(Attributes.SCALE, BionicleMasks.id("effect.enlarged"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, amp -> 0.12 * (amp + 1)));

    /** Kanoka of Shrink: the target shrinks by 8% per power level, down to a quarter of its size. */
    public static final DeferredHolder<MobEffect, MobEffect> SHRUNKEN = EFFECTS.register("shrunken", () ->
            new Simple(MobEffectCategory.NEUTRAL, 0xFAE63C)
                    .addAttributeModifier(Attributes.SCALE, BionicleMasks.id("effect.shrunken"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, amp -> -Math.min(0.75, 0.08 * (amp + 1))));

    /** Kanoka of Reconstitutes at Random: the target's body is scrambled for a while. */
    public static final DeferredHolder<MobEffect, MobEffect> RECONSTITUTED = EFFECTS.register("reconstituted", ReconstitutedEffect::new);

    /** Kanohi Garai: heavy gravity pins foes down. */
    public static final DeferredHolder<MobEffect, MobEffect> CRUSHING_GRAVITY = EFFECTS.register("crushing_gravity", () ->
            new Simple(MobEffectCategory.HARMFUL, 0x3B3B78)
                    .addAttributeModifier(Attributes.GRAVITY, BionicleMasks.id("effect.crushing_gravity"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, amp -> 1.0 + amp)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, BionicleMasks.id("effect.crushing_gravity_jump"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, amp -> -0.6)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, BionicleMasks.id("effect.crushing_gravity_speed"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amp -> -Math.min(0.8, 0.2 * (amp + 1))));

    /** Kanohi Garai (sneak-use): the wearer's gravity is lightened. */
    public static final DeferredHolder<MobEffect, MobEffect> LIGHT_GRAVITY = EFFECTS.register("light_gravity", () ->
            new Simple(MobEffectCategory.BENEFICIAL, 0xB8B8F0)
                    .addAttributeModifier(Attributes.GRAVITY, BionicleMasks.id("effect.light_gravity"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, amp -> -Math.min(0.85, 0.5 + 0.1 * amp))
                    .addAttributeModifier(Attributes.SAFE_FALL_DISTANCE, BionicleMasks.id("effect.light_gravity_fall"),
                            AttributeModifier.Operation.ADD_VALUE, amp -> 12.0 + 4 * amp));

    /** Kanohi Vahi and the Disk of Time: time slows to a crawl for the target. */
    public static final DeferredHolder<MobEffect, MobEffect> TIME_SLOWED = EFFECTS.register("time_slowed", () ->
            new Simple(MobEffectCategory.HARMFUL, 0xFFD45A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, BionicleMasks.id("effect.time_slowed_speed"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amp -> -Math.min(0.95, 0.6 + 0.1 * amp))
                    .addAttributeModifier(Attributes.ATTACK_SPEED, BionicleMasks.id("effect.time_slowed_attack"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amp -> -Math.min(0.9, 0.6 + 0.1 * amp))
                    .addAttributeModifier(Attributes.GRAVITY, BionicleMasks.id("effect.time_slowed_gravity"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, amp -> -0.7));

    /** Kanohi Mohtrek: echoes of the wearer's past self strike alongside each blow. */
    public static final DeferredHolder<MobEffect, MobEffect> TIME_ECHO = EFFECTS.register("time_echo", () ->
            new Simple(MobEffectCategory.BENEFICIAL, 0xE0C060));

    /** Kanohi Komau: the target's mind belongs to someone else. */
    public static final DeferredHolder<MobEffect, MobEffect> MIND_CONTROLLED = EFFECTS.register("mind_controlled", () ->
            new Simple(MobEffectCategory.HARMFUL, 0x9C3FD0));

    /** Kanohi Shelek: the target cannot hear. */
    public static final DeferredHolder<MobEffect, MobEffect> SILENCED = EFFECTS.register("silenced", () ->
            new Simple(MobEffectCategory.HARMFUL, 0x404050));

    /** Kanohi Olisi / Incomprehension / Mahiki: the target is lost in visions and cannot pick a target. */
    public static final DeferredHolder<MobEffect, MobEffect> VISIONS = EFFECTS.register("visions", () ->
            new Simple(MobEffectCategory.HARMFUL, 0xC080FF));

    /** Mask of Fusion: two beings share their power. */
    public static final DeferredHolder<MobEffect, MobEffect> FUSED = EFFECTS.register("fused", () ->
            new Simple(MobEffectCategory.BENEFICIAL, 0x60D0A0)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, BionicleMasks.id("effect.fused_attack"),
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, amp -> 0.25 * (amp + 1))
                    .addAttributeModifier(Attributes.ARMOR, BionicleMasks.id("effect.fused_armor"),
                            AttributeModifier.Operation.ADD_VALUE, amp -> 4.0 * (amp + 1)));

    private ModEffects() {
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    /** A plain effect whose behaviour lives in attribute modifiers or in the mod's event handlers. */
    public static class Simple extends MobEffect {
        public Simple(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
