package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.core.power.RestorationRules;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.GodPower;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerTasks;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 复原 GP-GR-04（T13 设计文档 §2.2）：提交瞬间锁定半径内的友方名单；当刻清除 {@code #restoration_cleanses} 里的效果、
 * 友方玩家装备各修最大耐久的 10%（只一次），并补第一次血；之后每 20 刻补最大生命的 20%，共 5 次；第 100 刻散叶结束。
 * 名单里的目标离开半径照样回血，死亡或换维度的跳过。
 */
final class RestorationPower implements GodPower {
    static final int END_TICK = RestorationRules.PULSES * RestorationRules.PULSE_INTERVAL;

    @Override
    public String id() {
        return GreekPowers.RESTORATION;
    }

    @Override
    public void execute(CastContext ctx) {
        List<LivingEntity> list = AllyTargets.collect(ctx).stream().map(Map.Entry::getKey).toList();
        GreekPowers.play(ctx.level(), ctx.origin(), "gp.gr.restoration.cast", 1f, 1f);
        PowerFx.emitAll(ctx, PowerFxPayload.Phase.HIT, list, "sweep");
        List<LivingEntity> cleansed = new ArrayList<>();
        List<LivingEntity> repaired = new ArrayList<>();
        for (LivingEntity e : list) {
            if (cleanse(e)) {
                cleansed.add(e);
            }
            if (e instanceof Player p && repair(p)) {
                repaired.add(e);
            }
        }
        PowerFx.emitAll(ctx, PowerFxPayload.Phase.HIT, cleansed, "cleanse");
        PowerFx.emitAll(ctx, PowerFxPayload.Phase.HIT, repaired, "repair");
        if (!repaired.isEmpty()) {
            GreekPowers.play(ctx.level(), ctx.origin(), "gp.gr.restoration.repair", 0.8f, 1f);
        }
        pulse(ctx, list);
        PowerTasks.schedule((server, age) -> {
            if (age % RestorationRules.PULSE_INTERVAL == 0 && age < END_TICK) {
                pulse(ctx, list);
            }
            if (age >= END_TICK) {
                PowerFx.emit(ctx, PowerFxPayload.Phase.END, List.of());
                return false;
            }
            return true;
        });
    }

    private static void pulse(CastContext ctx, List<LivingEntity> list) {
        List<LivingEntity> alive = list.stream().filter(e -> e.isAlive() && e.level() == ctx.level()).toList();
        for (LivingEntity e : alive) {
            e.heal(RestorationRules.pulse(e.getMaxHealth()));
        }
        PowerFx.emitAll(ctx, PowerFxPayload.Phase.HIT, alive, "pulse");
        GreekPowers.play(ctx.level(), ctx.caster().position(), "gp.gr.restoration.pulse", 0.5f, 1f);
    }

    /** 清除标签里的效果；返回是否清除了任何一个。 */
    static boolean cleanse(LivingEntity e) {
        List<Holder<MobEffect>> remove = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) {
            if (i.getEffect().is(GreekPowers.RESTORATION_CLEANSES)) {
                remove.add(i.getEffect());
            }
        }
        remove.forEach(e::removeEffect);
        return !remove.isEmpty();
    }

    /** 身上与手上的可损坏物品各修最大耐久的 10%；返回是否修了任何一件。 */
    static boolean repair(Player p) {
        boolean any = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = p.getItemBySlot(slot);
            if (s.isDamageableItem() && s.isDamaged()) {
                s.setDamageValue(RestorationRules.repairedDamage(s.getMaxDamage(), s.getDamageValue()));
                any = true;
            }
        }
        return any;
    }
}
