package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 以施法者为中心的友方名单（复原、青铜共用）：在半径内、反应键不是 none 的活体，提交瞬间锁定。 */
final class AllyTargets {
    private AllyTargets() {
    }

    /** 名单与各自的反应键（按距离由近到远）。{@code pets} 键只收驯服动物。 */
    static List<Map.Entry<LivingEntity, String>> collect(CastContext ctx) {
        double r = ctx.spec().radius();
        Set<UUID> own = TargetFactsCollector.ownVillages(ctx.caster());
        List<Map.Entry<LivingEntity, String>> out = new ArrayList<>();
        for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, new AABB(ctx.origin(), ctx.origin()).inflate(r),
                e -> e.isAlive() && e.position().distanceTo(ctx.origin()) <= r)) {
            String key = TargetClassifier.reaction(ctx.spec(), TargetFactsCollector.collect(ctx.caster(), e, own));
            if (TargetClassifier.NONE.equals(key) || (TargetClassifier.PETS.equals(key) && !tamed(e))) {
                continue;
            }
            out.add(Map.entry(e, key));
        }
        // 施法者自己不一定在实体查询里（测试用的假玩家没有加入世界）
        if (out.stream().noneMatch(x -> x.getKey() == ctx.caster())) {
            String key = TargetClassifier.reaction(ctx.spec(), TargetFactsCollector.collect(ctx.caster(), ctx.caster(), own));
            if (!TargetClassifier.NONE.equals(key)) {
                out.add(0, Map.entry(ctx.caster(), key));
            }
        }
        out.sort((a, b) -> Double.compare(a.getKey().distanceToSqr(ctx.origin()), b.getKey().distanceToSqr(ctx.origin())));
        return out;
    }

    static boolean tamed(Entity e) {
        return (e instanceof TamableAnimal t && t.isTame()) || (e instanceof AbstractHorse h && h.isTamed())
                || (e instanceof OwnableEntity o && o.getOwnerUUID() != null);
    }
}
