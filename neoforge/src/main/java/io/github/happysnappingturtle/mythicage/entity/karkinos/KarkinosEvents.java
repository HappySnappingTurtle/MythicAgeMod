package io.github.happysnappingturtle.mythicage.entity.karkinos;

import io.github.happysnappingturtle.mythicage.core.wild.WildCombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * 掀翻判定（T15 §5）：在玩家近战出手的那一刻（AttackEntityEvent，蓄力值还没清零）判断满蓄力 + 暴击 / 疾跑 / 斧头，
 * 满足就给卡尔基诺斯记一笔，紧接着的这次伤害生效时翻倒。暴击条件与原版 Player.attack 相同。
 */
public final class KarkinosEvents {
    private KarkinosEvents() {
    }

    public static void onPlayerAttack(AttackEntityEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !(event.getTarget() instanceof KarkinosEntity k)) {
            return;
        }
        boolean full = p.getAttackStrengthScale(0.5F) > 0.9F;
        boolean crit = p.fallDistance > 0.0F && !p.onGround() && !p.onClimbable() && !p.isInWater()
                && !p.hasEffect(MobEffects.BLINDNESS) && !p.isPassenger() && !p.isSprinting();
        boolean axe = p.getMainHandItem().getItem() instanceof AxeItem;
        if (WildCombatRules.flips(full, crit, p.isSprinting(), axe)) {
            k.armFlip(p);
        }
    }
}
