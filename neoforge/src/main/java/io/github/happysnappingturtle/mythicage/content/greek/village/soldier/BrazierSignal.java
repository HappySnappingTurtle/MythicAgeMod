package io.github.happysnappingturtle.mythicage.content.greek.village.soldier;

import io.github.happysnappingturtle.mythicage.content.greek.temple.BrazierBlock;
import io.github.happysnappingturtle.mythicage.core.village.soldier.DutyRules;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierAbility;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierBrain;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierOrders;
import io.github.happysnappingturtle.mythicage.village.soldier.VillageLandmarks;
import io.github.happysnappingturtle.mythicage.village.soldier.VillageThreats;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 圣火烽号（01 号 SO-GR-JAV-01 已确认的独有机制，T20 设计文档 §9.2）：
 * <ul>
 *   <li>每场警戒只触发一次：离 24 格内<b>此刻点着</b>的基础圣火盆最近的一名在岗投矛兵跑过去（最多 200 刻），扬火 8 刻；</li>
 *   <li>扬火：全村在岗士兵得知威胁（岗位离威胁 ≤ 24 格的去接敌）；全村正在祈祷的居民中断祈祷回屋；</li>
 *   <li>没有点着的圣火盆、跑去途中被浇灭或超时：退化为呼喊，只通知威胁 16 格内的士兵。之后转为投后撤步。</li>
 * </ul>
 * 史料：希腊用山顶火堆接力传信（phryktoria）；《阿伽门农》开场守望人看烽火得知特洛伊陷落。
 */
public final class BrazierSignal implements SoldierAbility {
    public static final String ID = "greek:brazier_signal";
    public static final String LANDMARK = "beacon";
    public static final double RANGE = 24;
    public static final int RUN_LIMIT = 200;
    public static final int SIGNAL_TICKS = 8;
    public static final double SHOUT = 16;

    /** 一次扬火任务。 */
    private record Task(UUID village, BlockPos brazier, long since, long animUntil) {
    }

    private static final Map<UUID, Task> TASKS = new ConcurrentHashMap<>();
    /** 村庄 → 已经安排过烽号的警戒场次。 */
    private static final Map<UUID, Long> HANDLED = new ConcurrentHashMap<>();
    /** 村庄 → 最近一次扬火（或呼喊）完成的世界刻与是否真的扬了火（GameTest 读）。 */
    private static final Map<UUID, long[]> DONE = new ConcurrentHashMap<>();

    static boolean lit(ServerLevel level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        return st.is(VillageLandmarks.tag(LANDMARK)) && st.hasProperty(BrazierBlock.LIT) && st.getValue(BrazierBlock.LIT);
    }

    @Override
    public void onAlert(ResidentEntity soldier, SoldierBrain brain, long serial) {
        SoldierOrders o = brain.orders();
        if (o == null || !(soldier.level() instanceof ServerLevel level)) {
            return;
        }
        Long h = HANDLED.get(o.village());
        if (h != null && h == serial) {
            return;
        }
        HANDLED.put(o.village(), serial);
        // 在岗投矛兵 × 24 格内点着的圣火盆：取距离最近的一对
        ResidentEntity best = null;
        BlockPos bestFire = null;
        double bd = RANGE * RANGE;
        for (ResidentEntity s : VillageScheduler.loaded(o.village())) {
            SoldierOrders so = s.isSoldier() ? s.soldierBrain().orders() : null;
            if (so == null || so.state() != DutyRules.State.ON_DUTY || !ID.equals(so.profile().ability())) {
                continue;
            }
            for (BlockPos f : VillageLandmarks.find(level, o.village(), LANDMARK)) {
                double d = f.distToCenterSqr(s.position());
                if (d <= bd && lit(level, f)) {
                    bd = d;
                    best = s;
                    bestFire = f;
                }
            }
        }
        if (best == null) {
            shout(level, o.village(), soldier);
            return;
        }
        TASKS.put(best.getUUID(), new Task(o.village(), bestFire, level.getGameTime(), 0));
    }

    @Override
    public boolean tick(ResidentEntity soldier, SoldierBrain brain) {
        Task t = TASKS.get(soldier.getUUID());
        if (t == null || !(soldier.level() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        if (now - t.since() > RUN_LIMIT || !lit(level, t.brazier())) {
            TASKS.remove(soldier.getUUID());
            shout(level, t.village(), soldier);
            return false;
        }
        if (t.animUntil() > 0) {
            soldier.getNavigation().stop();
            if (now >= t.animUntil()) {
                TASKS.remove(soldier.getUUID());
                soldier.playAnim("");
                signal(level, t.village(), t.brazier());
                return false;
            }
            return true;
        }
        if (soldier.distanceToSqr(Vec3.atBottomCenterOf(t.brazier())) <= 6.25) {
            soldier.getNavigation().stop();
            soldier.getLookControl().setLookAt(Vec3.atCenterOf(t.brazier()));
            soldier.playAnim("brazier_signal");
            TASKS.put(soldier.getUUID(), new Task(t.village(), t.brazier(), t.since(), now + SIGNAL_TICKS));
            return true;
        }
        if (soldier.getNavigation().isDone() || soldier.tickCount % 10 == 0) {
            soldier.getNavigation().moveTo(t.brazier().getX() + 0.5, t.brazier().getY(), t.brazier().getZ() + 0.5, 0.9 / 0.5);
        }
        return true;
    }

    /** 扬火：全村在岗士兵得知威胁，全村正在祈祷的居民中断祈祷回屋。 */
    static void signal(ServerLevel level, UUID village, BlockPos fire) {
        level.sendParticles(ParticleTypes.FLAME, fire.getX() + 0.5, fire.getY() + 1.2, fire.getZ() + 0.5, 30, 0.3, 0.8, 0.3, 0.05);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, fire.getX() + 0.5, fire.getY() + 2, fire.getZ() + 0.5, 12, 0.3, 1.0, 0.3, 0.02);
        level.playSound(null, fire, SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0F, 0.7F);
        LivingEntity threat = VillageThreats.alertThreat(village).orElse(null);
        for (ResidentEntity r : VillageScheduler.loaded(village)) {
            if (r.isSoldier()) {
                r.soldierBrain().notifyThreat(threat);
            } else if (PrayerGuard.prayingNow(r)) {
                r.abortJob(false);
            }
        }
        DONE.put(village, new long[]{level.getGameTime(), 1});
    }

    /** 呼喊：只通知威胁 16 格内的士兵（没有点着的圣火盆时，§9.2）。 */
    static void shout(ServerLevel level, UUID village, ResidentEntity from) {
        LivingEntity threat = VillageThreats.alertThreat(village).orElse(null);
        Vec3 c = threat != null ? threat.position() : from.position();
        for (ResidentEntity r : VillageScheduler.loaded(village)) {
            if (r.isSoldier() && r.position().distanceTo(c) <= SHOUT) {
                r.soldierBrain().notifyThreat(threat);
            }
        }
        DONE.put(village, new long[]{level.getGameTime(), 0});
    }

    /** 只供 GameTest：最近一次 {世界刻, 1 = 扬火 / 0 = 呼喊}；没有为 null。 */
    public static long[] lastForTest(UUID village) {
        return DONE.get(village);
    }

    /** 只供 GameTest：这名士兵手上有没有扬火任务。 */
    public static boolean taskedForTest(UUID soldier) {
        return TASKS.containsKey(soldier);
    }

    public static void clearForTest(UUID village) {
        HANDLED.remove(village);
        DONE.remove(village);
        TASKS.values().removeIf(t -> t.village().equals(village));
    }
}
