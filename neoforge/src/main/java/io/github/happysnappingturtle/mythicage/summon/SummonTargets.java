package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.summon.SummonTargetRules;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 召唤单位能不能打某个实体（T14 设计文档 §4.1）：主人在线时按 T12 的目标矩阵（以主人为施法者收集事实）；
 * 主人不在线时只打敌对生物（不会误伤任何人的村民、宠物、玩家）。
 */
public final class SummonTargets {
    /** 主人的「本村」缓存：主人 → (刻, 村庄集合)，5 秒刷新。 */
    private static final Map<UUID, Map.Entry<Long, Set<UUID>>> OWN_VILLAGES = new HashMap<>();

    private SummonTargets() {
    }

    /** 只供 GameTest：FakePlayer 不在玩家列表里，测试把它登记成可查到的主人。 */
    private static final Map<UUID, ServerPlayer> TEST_OWNERS = new java.util.concurrent.ConcurrentHashMap<>();

    public static void ownerForTest(ServerPlayer p, boolean add) {
        if (add) {
            TEST_OWNERS.put(p.getUUID(), p);
        } else {
            TEST_OWNERS.remove(p.getUUID());
        }
    }

    @Nullable
    public static ServerPlayer owner(Entity summon) {
        if (!(summon instanceof MythSummon s) || s.link().owner == null || !(summon.level() instanceof ServerLevel sl)) {
            return null;
        }
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(s.link().owner);
        return p != null ? p : TEST_OWNERS.get(s.link().owner);
    }

    public static boolean canAttack(Entity summon, LivingEntity target) {
        if (!(summon instanceof MythSummon s) || target == summon || !target.isAlive()) {
            return false;
        }
        if (target instanceof MythSummon other && other.link().owner != null && other.link().owner.equals(s.link().owner)) {
            return false;
        }
        ServerPlayer owner = owner(summon);
        if (owner == null) {
            return s.combatUnit() && target instanceof Enemy && !(target instanceof Mob m && m.isLeashed());
        }
        return SummonTargetRules.canAttack(TargetFactsCollector.collect(owner, target, ownVillages(owner)), s.combatUnit());
    }

    /** 从主人的角度看 target 是不是敌方（不论单位能不能打）：天马的天眼按它标记。 */
    public static boolean isEnemyOf(ServerPlayer owner, LivingEntity target) {
        return target.isAlive() && target != owner
                && SummonTargetRules.canAttack(TargetFactsCollector.collect(owner, target, ownVillages(owner)), true);
    }

    private static Set<UUID> ownVillages(ServerPlayer owner) {
        long now = owner.level().getGameTime();
        Map.Entry<Long, Set<UUID>> e = OWN_VILLAGES.get(owner.getUUID());
        if (e == null || now - e.getKey() > 100) {
            e = Map.entry(now, TargetFactsCollector.ownVillages(owner));
            OWN_VILLAGES.put(owner.getUUID(), e);
        }
        return e.getValue();
    }

    public static void clear() {
        OWN_VILLAGES.clear();
    }
}
