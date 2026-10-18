package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.GodPower;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.List;
import java.util.Map;

/**
 * 青铜 GP-GR-08（T13 设计文档 §2.3）：半径内友方获得「青铜之躯」90 秒（药水效果，见 {@link BronzeBodyEffect}）。
 * 守护者列只给铁傀儡（反应键 {@code golem}），中立动物只给驯服的（{@code pets}）。外观由 Aura 同步。
 */
final class BronzePower implements GodPower {
    static final int DURATION = 1800;

    @Override
    public String id() {
        return GreekPowers.BRONZE;
    }

    @Override
    public void execute(CastContext ctx) {
        List<LivingEntity> list = AllyTargets.collect(ctx).stream()
                .filter(x -> !"golem".equals(x.getValue()) || x.getKey() instanceof IronGolem)
                .map(Map.Entry::getKey).toList();
        for (LivingEntity e : list) {
            e.addEffect(new MobEffectInstance(GreekPowers.BRONZE_BODY, DURATION, 0, false, true, true), ctx.caster());
        }
        GreekPowers.play(ctx.level(), ctx.origin(), "gp.gr.bronze.cast", 1f, 1f);
        GreekPowers.play(ctx.level(), ctx.origin(), "gp.gr.bronze.harden", 0.8f, 1f);
        PowerFx.emitAll(ctx, PowerFxPayload.Phase.HIT, list, "harden");
    }

    /** 青铜之躯自然到期时的褪色音效。 */
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance i = event.getEffectInstance();
        if (i != null && i.getEffect().is(GreekPowers.BRONZE_BODY.getKey()) && !event.getEntity().level().isClientSide()) {
            GreekPowers.play(event.getEntity(), "gp.gr.bronze.fade");
        }
    }
}
