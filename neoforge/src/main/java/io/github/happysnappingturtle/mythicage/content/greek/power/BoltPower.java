package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.core.power.BoltRules;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.GodPower;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerTasks;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 雷击 GP-GR-01（T13 设计文档 §2.1）：提交后雷云跟随锁定目标预警 15 刻，第 15 刻必定命中该目标并结算
 * （{@link BoltRules}）。预警期间目标死亡 / 移除 / 换维度 → 结束，不退道具。不生成原版闪电：不点火、不给苦力怕充能。
 */
public final class BoltPower implements GodPower {
    static final int WARNING_TICKS = 15;

    @Override
    public String id() {
        return GreekPowers.BOLT;
    }

    @Override
    public void execute(CastContext ctx) {
        Entity target = ctx.target();
        if (target == null) {
            return;
        }
        ServerLevel level = ctx.level();
        GreekPowers.play(level, target.position(), "gp.gr.bolt.lock", 1f, 1f);
        GreekPowers.play(level, target.position().add(0, 3, 0), "gp.gr.bolt.charge", 0.8f, 1f);
        PowerTasks.schedule((server, age) -> {
            if (!target.isAlive() || target.isRemoved() || target.level() != level) {
                PowerFx.emit(ctx, PowerFxPayload.Phase.END, List.of(), target.position(), 0, "");
                return false;
            }
            if (age < WARNING_TICKS) {
                return true;
            }
            strike(ctx, target);
            return false;
        });
    }

    /** 落雷结算（GameTest 也直接调用它测 PvP：假玩家不在世界里，射线选不到）。 */
    public static void strike(CastContext ctx, Entity target) {
        ServerLevel level = ctx.level();
        PowerFx.emit(ctx, PowerFxPayload.Phase.HIT, List.of(target), target.position(), 0, "");
        GreekPowers.play(level, target.position(), "gp.gr.bolt.strike", 2f, 1f);
        if (!(target instanceof LivingEntity l)) {
            return;
        }
        BoltRules.Outcome o = BoltRules.resolve(l.getMaxHealth(), TargetFactsCollector.isBoss(l),
                TargetFactsCollector.isElite(l), l instanceof Player);
        DamageSource src = level.damageSources().source(GreekPowers.DIVINE_BOLT, ctx.caster());
        // 受击无敌帧会吞掉伤害；雷击必定命中
        l.invulnerableTime = 0;
        l.hurt(src, o.kill() ? Float.MAX_VALUE : o.damage());
    }
}
