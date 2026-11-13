package io.github.happysnappingturtle.mythicage.content.greek.village.soldier;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.ResidentPrayer;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.village.soldier.DutyRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.InterceptRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierAbility;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierBrain;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierOrders;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 护祷拦截（01 号 SO-GR-SP-01 已确认的独有机制，T20 设计文档 §9.1）：
 * <ul>
 *   <li>本村某座祭坛的扫描范围内，敌对生物以一名<b>正在祈祷</b>或<b>刚从祈祷撤离（200 刻内）</b>的居民为目标时，
 *       最近的一名在岗、没在护别人、生命不低于撤退线的盾矛卫兵去拦：站到两者连线上（离居民 ≤ 1.5 格），面向威胁举盾，
 *       威胁进入触及时照常矛刺；一名卫兵同时只护 1 人；</li>
 *   <li>拦截期间，被护者挨投射物时，射手在卫兵正前方扇形内、卫兵正在举盾、离被护者 ≤ 3 格，这次伤害 ×(1 − 减伤)；</li>
 *   <li>威胁死了或不再盯着他、他离开扫描范围、卫兵生命低于撤退线时结束。不让祈祷继续计产（祈祷中断按祈祷规则）。</li>
 * </ul>
 */
public final class PrayerGuard implements SoldierAbility {
    public static final String ID = "greek:prayer_guard";
    /** 刚从祈祷撤离多久内还算「祈祷的人」（刻）。 */
    public static final long EVACUATING = 200;
    static final int INTERVAL = 10;

    /** 居民 → 最近一次看到他在祈祷（动作段）的世界刻。 */
    private static final Map<UUID, Long> PRAYING = new ConcurrentHashMap<>();

    /** 正在祈祷：手上是祈祷工作单、已经走到祈祷位开始动作。 */
    static boolean prayingNow(ResidentEntity r) {
        return r.job().filter(j -> j.type().equals(ResidentPrayer.ID)).map(j -> r.step() < j.steps().size()
                && j.steps().get(r.step()).kind() == JobStep.Kind.ACT).orElse(false);
    }

    /** 正在祈祷或刚从祈祷撤离。 */
    static boolean praying(ResidentEntity r, long now) {
        if (prayingNow(r)) {
            PRAYING.put(r.getUUID(), now);
            return true;
        }
        Long t = PRAYING.get(r.getUUID());
        return t != null && now - t <= EVACUATING;
    }

    private static final Map<UUID, String> REASON = new ConcurrentHashMap<>();

    /** 调试：这名卫兵上一次结束拦截的原因。 */
    public static String reasonForTest(UUID soldier) {
        return REASON.getOrDefault(soldier, "-");
    }

    /** 只供 GameTest：当作他刚在祈祷。 */
    public static void prayingForTest(UUID resident, long at) {
        PRAYING.put(resident, at);
    }

    @Override
    public boolean tick(ResidentEntity soldier, SoldierBrain brain) {
        if (!(soldier.level() instanceof ServerLevel level) || soldier.tickCount % INTERVAL != 0) {
            return false;
        }
        long now = level.getGameTime();
        SoldierOrders o = brain.orders();
        LivingEntity ward = brain.ward();
        boolean able = o != null && o.state() == DutyRules.State.ON_DUTY && !brain.retreating()
                && soldier.getHealth() >= soldier.getMaxHealth() * o.profile().retreatBelow();
        List<AABB> areas = o == null ? List.of() : areas(level, o.village());
        if (ward != null) {
            LivingEntity threat = brain.target();
            boolean keep = able && ward.isAlive() && ward instanceof ResidentEntity r && praying(r, now)
                    && areas.stream().anyMatch(a -> a.contains(ward.position()))
                    && threat instanceof Mob m && m.isAlive() && m.getTarget() == ward;
            if (!keep) {
                // 结束拦截：威胁还在的话交还给普通的接敌判断
                REASON.put(soldier.getUUID(), "able=" + able + " alive=" + ward.isAlive() + " praying="
                        + (ward instanceof ResidentEntity r && praying(r, now)) + " inArea=" + areas.stream().anyMatch(a -> a.contains(ward.position()))
                        + " areas=" + areas + " targetsWard=" + (threat instanceof Mob m && m.getTarget() == ward)
                        + " threat=" + threat);
                brain.clearWard();
            }
            return false;
        }
        if (!able || areas.isEmpty()) {
            return false;
        }
        for (AABB area : areas) {
            for (ResidentEntity r : level.getEntitiesOfClass(ResidentEntity.class, area,
                    x -> !x.isSoldier() && x.village().map(o.village()::equals).orElse(false))) {
                if (!praying(r, now) || SoldierBrain.guardOf(r.getUUID()).isPresent()) {
                    continue;
                }
                Optional<Mob> threat = level.getEntitiesOfClass(Mob.class, area.inflate(16),
                        m -> m instanceof Enemy && m.isAlive() && m.getTarget() == r).stream()
                        .min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(r)));
                if (threat.isEmpty() || !nearestFree(soldier, o.village(), r)) {
                    continue;
                }
                brain.guard(r, threat.get());
                return false;
            }
        }
        return false;
    }

    /** 这名卫兵是不是离被护者最近的一名能去拦的（在岗、没在护人、同一种独有机制）。 */
    static boolean nearestFree(ResidentEntity me, UUID village, ResidentEntity ward) {
        double mine = me.distanceToSqr(ward);
        for (ResidentEntity s : VillageScheduler.loaded(village)) {
            if (s == me || !s.isSoldier()) {
                continue;
            }
            SoldierOrders so = s.soldierBrain().orders();
            if (so == null || so.state() != DutyRules.State.ON_DUTY || !ID.equals(so.profile().ability())
                    || s.soldierBrain().ward() != null || s.soldierBrain().retreating()) {
                continue;
            }
            if (s.distanceToSqr(ward) < mine) {
                return false;
            }
        }
        return true;
    }

    /** 本村神庙祭坛周围（GA-13：信徒在这里祈祷）：祭坛水平 8 格、上下 4 格。 */
    static List<AABB> areas(ServerLevel level, UUID village) {
        List<AABB> out = new ArrayList<>();
        io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.of(level.getServer(), village)
                .filter(t -> t.altar().dimension().equals(level.dimension()))
                .ifPresent(t -> out.add(new AABB(t.altar().pos()).inflate(8, 4, 8)));
        return out;
    }

    @Override
    public float protectedHurt(ResidentEntity soldier, SoldierBrain brain, ResidentEntity victim, DamageSource source) {
        SoldierOrders o = brain.orders();
        Entity direct = source.getDirectEntity();
        Entity shooter = source.getEntity();
        if (o == null || !o.profile().hasShield() || !(direct instanceof Projectile) || shooter == null) {
            return 1.0F;
        }
        SoldierProfile.Shield s = o.profile().shield();
        boolean covered = InterceptRules.covers(s, brain.blockingEffective(), soldier.getYRot(), soldier.getX(), soldier.getZ(),
                shooter.getX(), shooter.getZ(), victim.getX(), victim.getZ());
        return covered ? (float) (1 - s.reduction()) : 1.0F;
    }

    public static void clearForTest() {
        PRAYING.clear();
    }
}
