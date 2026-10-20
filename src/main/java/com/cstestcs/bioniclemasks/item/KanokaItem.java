package com.cstestcs.bioniclemasks.item;

import com.cstestcs.bioniclemasks.component.KanokaData;
import com.cstestcs.bioniclemasks.kanoka.KanokaThrowing;
import com.cstestcs.bioniclemasks.lore.KanokaPower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A Kanoka disk. Right-click to throw it, as Matoran did in the sport of Kanoka and as the Vahki and
 * Toa Metru did with Kanoka launchers. On impact it releases its power.
 */
public class KanokaItem extends Item {
    public enum Kind { NORMAL, GREAT, TIME }

    private final KanokaPower power;
    private final Kind kind;

    public KanokaItem(Properties properties, KanokaPower power, Kind kind) {
        super(properties);
        this.power = power;
        this.kind = kind;
    }

    /** The disk's power, or null for the Disk of Time. */
    public KanokaPower power() {
        return power;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            KanokaData data = KanokaStacks.data(stack);
            player.getCooldowns().addCooldown(stack, kind == Kind.NORMAL ? 10 : 40);
            KanokaThrowing.launch(serverLevel, player, stack.copyWithCount(1), data);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
