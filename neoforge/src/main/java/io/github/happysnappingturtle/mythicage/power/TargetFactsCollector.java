package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.power.TargetFacts;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageResolver;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 从真实实体收集 {@link TargetFacts}（T12 设计文档 §2.2）。敌我口径按总纲 §3.10：
 * 村民、守护者、铁傀儡是友方；驯服、拴绳、他人宠物不受伤害型神力影响；Boss / 精英按实体类型标签
 * {@code #mythic_age:boss}、{@code #mythic_age:elite}（T23 往里加）。
 */
public final class TargetFactsCollector {
    public static final TagKey<EntityType<?>> BOSS = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "boss"));
    public static final TagKey<EntityType<?>> ELITE = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "elite"));

    /** 只供 GameTest：临时当作精英的实体类型（精英标签目前为空）。 */
    private static final java.util.Set<EntityType<?>> TEST_ELITE = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private TargetFactsCollector() {
    }

    private static final java.util.Set<EntityType<?>> TEST_BOSS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static boolean isBoss(Entity e) {
        return e.getType().is(BOSS) || TEST_BOSS.contains(e.getType());
    }

    /** 只供 GameTest：把某种实体临时当作 Boss（传 null 清空）。 */
    public static void bossForTest(@javax.annotation.Nullable EntityType<?> type) {
        if (type == null) {
            TEST_BOSS.clear();
        } else {
            TEST_BOSS.add(type);
        }
    }

    public static boolean isElite(Entity e) {
        return e.getType().is(ELITE) || TEST_ELITE.contains(e.getType());
    }

    /** 只供 GameTest：传 null 清空。 */
    public static void eliteForTest(@javax.annotation.Nullable EntityType<?> type) {
        if (type == null) {
            TEST_ELITE.clear();
        } else {
            TEST_ELITE.add(type);
        }
    }

    /** 施法者的「本村」（D3）：举行过仪式的村庄 ∪ 自己建立的村庄。 */
    public static Set<UUID> ownVillages(ServerPlayer caster) {
        Set<UUID> out = new HashSet<>();
        PlayerMythData data = MythData.get(caster);
        data.civsView().values().forEach(s -> out.addAll(s.riteVillages().keySet()));
        for (VillageRegistry.Village v : VillageRegistry.get(caster.server).all()) {
            if (caster.getUUID().equals(v.founder())) {
                out.add(v.id());
            }
        }
        return out;
    }

    public static TargetFacts collect(ServerPlayer caster, Entity e, Set<UUID> ownVillages) {
        boolean pvp = caster.server.isPvpAllowed();
        TargetFacts.Builder b = TargetFacts.builder().pvp(pvp);
        if (e == caster) {
            return b.caster().build();
        }
        if (isBoss(e)) {
            b.boss();
        }
        if (isElite(e)) {
            b.elite();
        }
        if (e instanceof Player p) {
            return b.player(caster.isAlliedTo(p), pvp).build();
        }
        if (e instanceof GuardianEntity || e instanceof IronGolem) {
            b.guardian();
        }
        if (e instanceof OwnedSummon s) {
            UUID owner = s.summonOwner();
            Player op = owner == null ? null : caster.level().getPlayerByUUID(owner);
            boolean mate = op != null && caster.isAlliedTo(op);
            b.summon(caster.getUUID().equals(owner), mate, op != null && pvp && !mate);
        }
        if (e instanceof io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity r) {
            // 文明村庄的居民（T20 §6.3）：转岗的士兵按士兵，其余按村民；「本村」看居民所属村庄
            boolean own = r.village().map(ownVillages::contains).orElse(false);
            if (r.isSoldier()) {
                b.soldier(own);
            } else {
                b.villager(own);
            }
        }
        if (e instanceof AbstractVillager v) {
            boolean own = v instanceof net.minecraft.world.entity.npc.Villager villager
                    && VillageResolver.villageOf(villager).map(x -> ownVillages.contains(x.id())).orElse(false);
            b.villager(own);
        }
        if ((e instanceof TamableAnimal t && t.isTame()) || (e instanceof AbstractHorse h && h.isTamed())
                || (e instanceof Mob m && m.isLeashed()) || (e instanceof OwnableEntity o && o.getOwnerUUID() != null)) {
            b.pet();
        }
        if (e instanceof Enemy || (e instanceof Mob m && attacking(m, caster))) {
            b.hostile();
        }
        return b.build();
    }

    private static boolean attacking(Mob m, ServerPlayer caster) {
        LivingEntity t = m.getTarget();
        if (t == null) {
            return false;
        }
        return t == caster || (t instanceof OwnedSummon s && caster.getUUID().equals(s.summonOwner()));
    }
}
