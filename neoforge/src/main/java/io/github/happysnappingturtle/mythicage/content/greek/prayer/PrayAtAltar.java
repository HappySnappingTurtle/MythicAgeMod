package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import com.google.common.collect.ImmutableMap;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.BelieverRules;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 信徒到神坛的祈祷位跪拜（T10 设计文档 §4）。由 Mixin 追加到村民的工作包与闲逛包末尾，
 * 只对希腊村庄的成年成员生效；白天 2000–9000 走到祈祷位，面向神坛跪拜，每 20 刻向 {@link PrayerService} 报到。
 * 跪满 {@link PrayerFormula#PRAY_TICKS} 刻入账后结束，当天不再来（GA-4）。
 * <p>
 * 每个村民的两个包各有一个实例，用 {@link PrayerRecord#running} 保证同一时刻只有一个在跑。
 */
public final class PrayAtAltar extends Behavior<Villager> {
    /** 在包里的优先级：先于原版的「走回工作站点」（2），使祈祷时的走路目标不被覆盖。 */
    public static final int PRIORITY = 1;
    private static final float SPEED = 0.5F;
    private static final int CHECK_INTERVAL = 20;
    /** 走不到祈祷位（寻路报告无法到达）超过这么多刻就放弃。 */
    private static final int GIVE_UP_TICKS = 100;
    /** 走了这么久还没到也放弃。 */
    private static final int WALK_TIMEOUT = 1200;
    /** 放弃后隔多久再试。 */
    private static final int RETRY_COOLDOWN = 600;
    private static final double ARRIVE = 1.0;
    private static final double DRIFT = 1.6;
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.35F), 0.9F);

    private GlobalPos altar;
    private BlockPos slot;
    /** GA-13：信徒所属的村子（原来是神坛主人）。 */
    private UUID owner;
    private long startedAt;
    private boolean arrived;
    private boolean counted;
    /** 到达祈祷位的刻；满 {@link PrayerFormula#PRAY_TICKS} 刻入账后结束（GA-4）。 */
    private long arrivedAt;
    private boolean done;
    private boolean lost;
    private boolean gaveUp;

    public PrayAtAltar() {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT), PrayerFormula.WINDOW_END - PrayerFormula.WINDOW_START);
    }

    public static PrayerRecord record(Villager v) {
        return v.getData(GreekPrayer.RECORD);
    }

    /** 只供 GameTest：去掉检查节流，使下一次 tryStart 立即检查。 */
    public static void resetCheckForTest(Villager v) {
        record(v).nextCheck = 0;
    }

    /** 是否处在可以祈祷的状态（不含时段与神坛）。 */
    private static boolean free(Villager v) {
        return !v.isBaby() && !v.isLeashed() && !v.isPassenger() && !v.isSleeping() && !v.isTrading()
                && (v.getBrain().isActive(Activity.WORK) || v.getBrain().isActive(Activity.IDLE));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager v) {
        PrayerRecord rec = record(v);
        long now = level.getGameTime();
        if (rec.running || now < rec.nextCheck) {
            return false;
        }
        rec.nextCheck = now + CHECK_INTERVAL;
        Optional<VillageRegistry.Village> village = BelieverEvents.greekVillage(v);
        if (rec.lastAltar() != null && (village.isEmpty()
                || !TemplePrayer.isTempleAltar(level.getServer(), village.get().id(), rec.lastAltar()))) {
            rec.clearAltar();
        }
        BelieverEvents.sync(v, rec, village.isPresent(), false);
        if (village.isEmpty() || !free(v) || !PrayerFormula.inWindow(level.getDayTime())) {
            return false;
        }
        GlobalPos home = v.getBrain().getMemory(MemoryModuleType.HOME).orElseThrow();
        if (!home.dimension().equals(level.dimension())) {
            return false;
        }
        return choose(level, v, rec, village.get());
    }

    /** GA-13：去本村神庙的祭坛，占一个离自己最近的空祈祷位。 */
    private boolean choose(ServerLevel level, Villager v, PrayerRecord rec, VillageRegistry.Village village) {
        long today = MythDays.today(level.getServer());
        if (rec.creditedOn(today)) {
            return false;
        }
        Optional<io.github.happysnappingturtle.mythicage.village.temple.VillageTemple> temple =
                TemplePrayer.templeOf(level.getServer(), village);
        // 没有供奉者时不去（同居民）
        if (temple.isEmpty() || !temple.get().altar().dimension().equals(level.dimension())
                || !io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.hasDevotees(level.getServer(), village.id())) {
            return false;
        }
        // 空位里取离自己最近的：先到的信徒不会去占神坛另一侧的位置
        List<BlockPos> slots = new ArrayList<>(temple.get().prayerSlots());
        slots.sort(java.util.Comparator.comparingDouble(p -> p.distToCenterSqr(v.position())));
        Optional<BlockPos> s = PrayerService.SLOTS.claim(temple.get().altar(), slots, v.getUUID(), level.getGameTime());
        if (s.isEmpty()) {
            return false;
        }
        altar = temple.get().altar();
        slot = s.get();
        owner = village.id();
        return true;
    }

    private static double horizontal(double x, double z, BlockPos p) {
        double dx = x - (p.getX() + 0.5);
        double dz = z - (p.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    protected void start(ServerLevel level, Villager v, long gameTime) {
        PrayerRecord rec = record(v);
        rec.running = true;
        startedAt = gameTime;
        arrived = false;
        counted = false;
        done = false;
        lost = false;
        gaveUp = false;
        v.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        walk(v);
    }

    private void walk(Villager v) {
        v.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(slot, SPEED, 0));
        v.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(altar.pos()));
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager v, long gameTime) {
        if (done || lost || !free(v) || !PrayerFormula.inWindow(level.getDayTime())
                || !TemplePrayer.isTempleAltar(level.getServer(), owner, altar)) {
            return false;
        }
        if (!arrived) {
            Optional<Long> stuck = v.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            // 只认本次出发之后记下的「走不到」：之前别的行为留下的旧记忆不算
            boolean unreachable = stuck.isPresent() && stuck.get() >= startedAt && gameTime - stuck.get() > GIVE_UP_TICKS;
            if (unreachable || gameTime - startedAt > WALK_TIMEOUT) {
                gaveUp = true;
                return false;
            }
        }
        return true;
    }

    @Override
    protected void tick(ServerLevel level, Villager v, long gameTime) {
        Vec3 center = Vec3.atBottomCenterOf(slot);
        double dx = v.getX() - center.x;
        double dz = v.getZ() - center.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        boolean near = dist <= ARRIVE && Math.abs(v.getY() - center.y) < 1.2;
        if (!arrived && near) {
            arrive(level, v);
        } else if (arrived && dist > DRIFT) {
            // 被推开了：重新走回去，这段时间不算在祈祷
            arrived = false;
            counted = false;
            startedAt = gameTime;
            BelieverEvents.sync(v, record(v), true, false);
        }
        if (arrived) {
            kneel(v, center, dist);
        } else if (v.getBrain().getMemory(MemoryModuleType.WALK_TARGET).map(t -> !t.getTarget().currentBlockPosition().equals(slot))
                .orElse(true)) {
            walk(v);
        }
        if ((gameTime - startedAt) % PrayerSlots.REPORT_INTERVAL == 0) {
            lost = !PrayerService.SLOTS.report(altar, v.getUUID(), gameTime, arrived, counted, arrived && !counted);
        }
        if (counted && gameTime % 40 == v.getId() % 40) {
            level.sendParticles(GOLD, v.getX(), v.getY() + 1.1, v.getZ(), 3, 0.3, 0.3, 0.3, 0);
        }
        if (arrived && counted && !done && gameTime - arrivedAt >= PrayerFormula.PRAY_TICKS) {
            TemplePrayer.credit(level, owner, v.getUUID());
            record(v).markCredited(MythDays.today(level.getServer()));
            done = true;
        }
    }

    private void arrive(ServerLevel level, Villager v) {
        arrived = true;
        arrivedAt = level.getGameTime();
        GlobalPos home = v.getBrain().getMemory(MemoryModuleType.HOME).orElseThrow();
        counted = BelieverRules.pilgrimageOk(home.pos().getX() + 0.5, home.pos().getZ() + 0.5, slot.getX() + 0.5, slot.getZ() + 0.5);
        PrayerService.SLOTS.report(altar, v.getUUID(), level.getGameTime(), true, counted, !counted);
        if (counted) {
            record(v).record(MythDays.today(level.getServer()), owner, altar);
        }
        BelieverEvents.sync(v, record(v), true, true);
    }

    /** 停在祈祷位上、面向神坛；轻推到格子中心，跪姿更整齐。 */
    private void kneel(Villager v, Vec3 center, double dist) {
        v.getNavigation().stop();
        // 保持走路目标 = 已到达的祈祷位：原版其他行为只在没有走路目标时才会另找去处
        v.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(slot, SPEED, 0));
        if (dist > 0.15) {
            v.getMoveControl().setWantedPosition(center.x, v.getY(), center.z, 0.3);
        }
        double ax = altar.pos().getX() + 0.5 - v.getX();
        double az = altar.pos().getZ() + 0.5 - v.getZ();
        float yaw = (float) (Mth.atan2(az, ax) * Mth.RAD_TO_DEG) - 90.0F;
        v.setYRot(yaw);
        v.setYHeadRot(yaw);
        v.setYBodyRot(yaw);
        v.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(altar.pos()));
    }

    @Override
    protected void stop(ServerLevel level, Villager v, long gameTime) {
        PrayerRecord rec = record(v);
        rec.running = false;
        if (gaveUp) {
            TemplePrayer.markUnreachable(owner, gameTime);
            rec.nextCheck = gameTime + RETRY_COOLDOWN;
        }
        if (!TemplePrayer.isTempleAltar(level.getServer(), owner, altar) && altar.equals(rec.lastAltar())) {
            rec.clearAltar();
        }
        PrayerService.SLOTS.release(altar, v.getUUID());
        v.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .filter(t -> t.getTarget().currentBlockPosition().equals(slot))
                .ifPresent(t -> v.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET));
        BelieverEvents.sync(v, rec, BelieverEvents.greekVillage(v).isPresent(), false);
        arrived = false;
        counted = false;
    }
}
