package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.core.power.CurseBudget;
import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.GodPower;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerTasks;
import io.github.happysnappingturtle.mythicage.power.PowerThrow;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 诅咒 GP-GR-09（T13 设计文档 §3.4）：投掷金苹果，落点半径 8 内：
 * <ul>
 *   <li>反应为 {@code curse} 的敌方按最大生命升序挑选，累计不超过 60（{@link CurseBudget}），每隔 2 刻依次变成被诅咒的猪；</li>
 *   <li>精英、Boss（反应 {@code immune}）不变形、不占预算，弹出免疫提示；</li>
 *   <li>敌对玩家（PvP 开，反应 {@code pvp}）不变形，改为缓慢 II + 虚弱 6 秒。</li>
 * </ul>
 * 变形：在原位置生成诅咒猪、继承朝向与自定义名，原实体直接移除（不掉落、不给经验）。
 */
public final class CursePower implements GodPower {
    static final int STEP = 2;
    static final int PVP_TICKS = 120;

    @Override
    public String id() {
        return GreekPowers.CURSE;
    }

    @Override
    public void execute(CastContext ctx) {
        PowerThrow.launch(ctx, () -> land(ctx));
    }

    static void land(CastContext ctx) {
        ServerLevel level = ctx.level();
        double r = ctx.spec().radius();
        Set<UUID> own = TargetFactsCollector.ownVillages(ctx.caster());
        List<CurseBudget.Candidate<LivingEntity>> candidates = new ArrayList<>();
        List<LivingEntity> immune = new ArrayList<>();
        List<LivingEntity> players = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(ctx.origin(), ctx.origin()).inflate(r),
                x -> x.isAlive() && x.position().distanceTo(ctx.origin()) <= r && !(x instanceof CursedPig))) {
            String key = TargetClassifier.reaction(ctx.spec(), TargetFactsCollector.collect(ctx.caster(), e, own));
            switch (key) {
                case "curse" -> {
                    if (e instanceof Mob) {
                        candidates.add(new CurseBudget.Candidate<>(e, e.getMaxHealth()));
                    }
                }
                case "immune" -> immune.add(e);
                case "pvp" -> players.add(e);
                default -> {
                }
            }
        }
        GreekPowers.play(level, ctx.origin(), "gp.gr.curse.impact", 1f, 1f);
        PowerFx.emit(ctx, PowerFxPayload.Phase.HIT, List.of(), ctx.origin(), 0, "ring:" + r);
        if (!immune.isEmpty()) {
            PowerFx.emitAll(ctx, PowerFxPayload.Phase.HIT, immune, "immune");
            GreekPowers.play(level, immune.getFirst().position(), "gp.gr.curse.immune", 1f, 1f);
        }
        for (LivingEntity p : players) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PVP_TICKS, 1), ctx.caster());
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PVP_TICKS, 0), ctx.caster());
        }
        List<LivingEntity> chosen = CurseBudget.select(candidates, CurseBudget.BUDGET);
        int last = chosen.size() * STEP;
        PowerTasks.schedule((server, age) -> {
            if (age % STEP == 0 && age / STEP - 1 < chosen.size()) {
                LivingEntity e = chosen.get(age / STEP - 1);
                if (e.isAlive() && !e.isRemoved() && e.level() == level) {
                    CursedPig pig = transform(level, e);
                    PowerFx.emit(ctx, PowerFxPayload.Phase.HIT, List.of(pig), pig.position(), 0, "transform");
                    GreekPowers.play(level, pig.position(), "gp.gr.curse.transform", 1f, 1f);
                }
            }
            if (age >= last + 6) {
                PowerFx.emit(ctx, PowerFxPayload.Phase.END, List.of());
                return false;
            }
            return true;
        });
    }

    /** 原地换成诅咒猪；原实体直接移除，不掉落、不给经验。 */
    public static CursedPig transform(ServerLevel level, LivingEntity e) {
        CursedPig pig = GreekPowers.CURSED_PIG.get().create(level);
        pig.moveTo(e.getX(), e.getY(), e.getZ(), e.getYRot(), e.getXRot());
        pig.setYHeadRot(e.getYHeadRot());
        if (e.hasCustomName()) {
            pig.setCustomName(e.getCustomName());
        }
        level.addFreshEntity(pig);
        e.discard();
        return pig;
    }
}
