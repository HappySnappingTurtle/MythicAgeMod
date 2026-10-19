package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.core.power.LureRules;
import io.github.happysnappingturtle.mythicage.core.power.PowerReason;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.GodPower;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerSpecLoader;
import io.github.happysnappingturtle.mythicage.power.PowerZones;
import io.github.happysnappingturtle.mythicage.power.TempBlocks;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 诱兽石 GP-GR-02（T13 设计文档 §4.1；05 号）：在选中的面上立一块两格高的临时诱兽石（上下两个 {@link TempBlocks}，
 * 到期复原），
 * 登记一个区域（{@link PowerZones}，kind {@code lure}）：
 * <ul>
 *   <li>每 40 刻把半径 48 内未驯服、未拴绳、无主人的被动动物（马和驴优先）引向石头，同时最多 24 只；
 *       候选每 200 刻刷新一次缓存，不每刻扫；吸引是寻路目标（最高优先级的 AI 目标），不是传送；</li>
 *   <li>石头六面有水时改为只吸引鱼与鱿鱼；</li>
 *   <li>每 30 秒在石旁 2 格内生成 1 只普通鸡，最多 6 只，找不到空位就跳过这次；</li>
 *   <li>同一玩家同时只保留 1 块：新放置时旧石立即复原；石头被移除或到期时撤掉所有吸引目标。</li>
 * </ul>
 * 吸引状态只在内存里：服务器重启后区域照常运行，下一轮重新挑选动物。
 */
public final class LurePower implements GodPower {
    static final String KIND = "lure";
    static final double SPEED = 1.0;
    private static final int PULSE_INTERVAL = 100;

    /** 「从没做过」的时刻：离任何真实时刻都足够远，又不会让 now − NEVER 溢出。 */
    private static final long NEVER = Long.MIN_VALUE / 4;

    /** 一块诱兽石的运行状态。 */
    static final class State {
        final long start;
        long lastPull = NEVER;
        long lastCache = NEVER;
        long lastPulse;
        List<UUID> cache = List.of();
        final Map<UUID, LureGoal> pulled = new LinkedHashMap<>();
        int chickensTried;

        State(long start) {
            this.start = start;
            this.lastPulse = start;
        }
    }

    private static final Map<String, State> STATES = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return GreekPowers.LURE;
    }

    static BlockPos stonePos(CastContext ctx) {
        return ctx.blockPos().relative(ctx.face());
    }

    static BlockState half(DoubleBlockHalf half) {
        return GreekPowers.LURE_STONE.get().defaultBlockState().setValue(LureStoneBlock.HALF, half);
    }

    /** 石体两格空间（都可替换、没有方块实体、不超出建筑高度）且下方是结实的支撑面。 */
    @Override
    public Optional<PowerReason> validate(CastContext ctx) {
        if (ctx.blockPos() == null || ctx.face() == null) {
            return Optional.of(PowerReason.PLACEMENT_INVALID);
        }
        BlockPos pos = stonePos(ctx);
        ServerLevel level = ctx.level();
        if (pos.getY() + 1 >= level.getMaxBuildHeight()
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return Optional.of(PowerReason.PLACEMENT_INVALID);
        }
        for (BlockPos p : List.of(pos, pos.above())) {
            if (!level.getBlockState(p).canBeReplaced() || level.getBlockEntity(p) != null) {
                return Optional.of(PowerReason.PLACEMENT_INVALID);
            }
        }
        return Optional.empty();
    }

    @Override
    public void execute(CastContext ctx) {
        ServerLevel level = ctx.level();
        MinecraftServer server = level.getServer();
        ServerPlayer caster = ctx.caster();
        // 同一玩家只保留 1 块：旧石立即复原
        for (PowerZones.Zone old : PowerZones.all(server)) {
            if (old.kind().equals(KIND) && old.owner().equals(caster.getUUID())) {
                PowerZones.end(server, old);
            }
        }
        BlockPos pos = stonePos(ctx);
        int duration = ctx.spec().durationTicks();
        if (TempBlocks.place(level, pos, half(DoubleBlockHalf.LOWER), duration, caster.getUUID(), false)
                != TempBlocks.Result.PLACED) {
            return;
        }
        if (TempBlocks.place(level, pos.above(), half(DoubleBlockHalf.UPPER), duration, caster.getUUID(), false)
                != TempBlocks.Result.PLACED) {
            // 上半格放不下（受保护等）：下半格也撤回，不留半块石头
            TempBlocks.restoreNow(level, pos);
            return;
        }
        long now = WorldClock.now(server);
        PowerZones.Zone zone = new PowerZones.Zone(ctx.castId(), KIND, ctx.spec().id(), level.dimension(),
                Vec3.atBottomCenterOf(pos), ctx.spec().radius(), now + duration, caster.getUUID(), List.of(pos, pos.above()));
        PowerZones.add(server, zone);
        STATES.put(zone.castId(), new State(now));
        GreekPowers.play(level, zone.center(), "gp.gr.lure.place", 1f, 1f);
        PowerFx.emit(ctx, PowerFxPayload.Phase.HIT, List.of(), zone.center(), duration, "zone:" + zone.radius());
        PowerFx.emit(ctx, PowerFxPayload.Phase.HIT, List.of(), zone.center(), 10, "rise");
        pull(level, zone, state(zone, now), now);
    }

    static State state(PowerZones.Zone zone, long now) {
        return STATES.computeIfAbsent(zone.castId(), k -> {
            int duration = PowerSpecLoader.get(zone.powerId()).map(s -> s.durationTicks()).orElse(0);
            return new State(Math.min(now, zone.until() - duration));
        });
    }

    /** 石头下半格（区域标记的第一格；第二格是上半格）。 */
    static BlockPos stoneOf(PowerZones.Zone zone) {
        return zone.marks().getFirst();
    }

    /** 区域标记的每一格都还是诱兽石。 */
    static boolean intact(ServerLevel level, PowerZones.Zone zone) {
        return zone.marks().stream().allMatch(p -> level.getBlockState(p).is(GreekPowers.LURE_STONE.get()));
    }

    /** 区域行为：石头还在就吸引、产鸡、发波纹；石头缺了任何一半（被替换、被复原）就提前结束，整块复原。 */
    static final PowerZones.Kind ZONE = new PowerZones.Kind() {
        @Override
        public void tick(ServerLevel level, PowerZones.Zone zone, long now) {
            if (!intact(level, zone)) {
                PowerZones.remove(level.getServer(), zone.castId());
                end(level, zone);
                return;
            }
            State st = state(zone, now);
            if (now - st.lastPull >= LureRules.PULL_INTERVAL) {
                pull(level, zone, st, now);
            }
            spawnChickens(level, zone, st, now);
            if (now - st.lastPulse >= PULSE_INTERVAL) {
                st.lastPulse = now;
                GreekPowers.play(level, zone.center(), "gp.gr.lure.pulse", 0.6f, 1f);
                zoneFx(level, zone, PowerFxPayload.Phase.HIT, zone.center(), List.of(), 40, "pulse");
            }
        }

        @Override
        public void end(ServerLevel level, PowerZones.Zone zone) {
            release(level, zone);
            GreekPowers.play(level, zone.center(), "gp.gr.lure.sink", 1f, 1f);
            zoneFx(level, zone, PowerFxPayload.Phase.END, zone.center(), List.of(), 0, "sink");
            for (BlockPos p : zone.marks()) {
                TempBlocks.restoreNow(level, p);
            }
        }
    };

    // ───────────── 吸引 ─────────────

    /** 未驯服、未拴绳、无主人的生物。 */
    static boolean free(Mob m) {
        if (!m.isAlive() || m.isLeashed() || m.isVehicle()) {
            return false;
        }
        if (m instanceof TamableAnimal t && t.isTame()) {
            return false;
        }
        if (m instanceof AbstractHorse h && h.isTamed()) {
            return false;
        }
        return !(m instanceof OwnableEntity o && o.getOwnerUUID() != null);
    }

    /** 候选：岸上为被动动物，靠水时为鱼与鱿鱼。 */
    static boolean candidate(Entity e, boolean water) {
        if (!(e instanceof Mob m) || e instanceof Enemy || !free(m)) {
            return false;
        }
        return water ? (e instanceof AbstractFish || e instanceof Squid) : e instanceof Animal;
    }

    static boolean nearWater(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    /** 一轮吸引：刷新缓存（到时间才刷）、清掉失效的、按规则补到 24 只。 */
    static void pull(ServerLevel level, PowerZones.Zone zone, State st, long now) {
        st.lastPull = now;
        boolean water = nearWater(level, stoneOf(zone));
        double r = zone.radius();
        if (now - st.lastCache >= LureRules.CACHE_INTERVAL) {
            st.lastCache = now;
            st.cache = level.getEntitiesOfClass(Mob.class, new AABB(zone.center(), zone.center()).inflate(r, 16, r),
                    e -> zone.covers(e.position()) && candidate(e, water)).stream().map(Entity::getUUID).toList();
        }
        st.pulled.entrySet().removeIf(en -> {
            Entity e = level.getEntity(en.getKey());
            if (e instanceof Mob m && candidate(m, water) && zone.covers(m.position())) {
                return false;
            }
            if (e instanceof Mob m) {
                m.goalSelector.removeGoal(en.getValue());
            }
            return true;
        });
        List<LureRules.Candidate<Mob>> cands = new ArrayList<>();
        for (UUID id : st.cache) {
            if (st.pulled.containsKey(id)) {
                continue;
            }
            Entity e = level.getEntity(id);
            if (e instanceof Mob m && candidate(m, water) && zone.covers(m.position())) {
                cands.add(new LureRules.Candidate<>(m, m instanceof Horse || m instanceof Donkey,
                        m.position().distanceTo(zone.center())));
            }
        }
        List<Mob> picked = LureRules.pick(cands, st.pulled.size(), LureRules.CAP);
        for (Mob m : picked) {
            LureGoal g = new LureGoal(m, zone.center(), zone.castId());
            m.goalSelector.addGoal(0, g);
            st.pulled.put(m.getUUID(), g);
        }
        if (!picked.isEmpty()) {
            zoneFx(level, zone, PowerFxPayload.Phase.HIT, zone.center(), picked, 40, "attract");
        }
    }

    /** 撤掉这块石头的全部吸引目标。 */
    static void release(ServerLevel level, PowerZones.Zone zone) {
        State st = STATES.remove(zone.castId());
        if (st == null) {
            return;
        }
        for (Map.Entry<UUID, LureGoal> en : st.pulled.entrySet()) {
            if (level.getEntity(en.getKey()) instanceof Mob m) {
                m.goalSelector.removeGoal(en.getValue());
                m.getNavigation().stop();
            }
        }
        st.pulled.clear();
    }

    /** 按时钟补产鸡：每 30 秒 1 只、最多 6 只；没有空位的那次跳过（不补）。 */
    static void spawnChickens(ServerLevel level, PowerZones.Zone zone, State st, long now) {
        int due = LureRules.chickensDue(now - st.start);
        while (st.chickensTried < due) {
            st.chickensTried++;
            Optional<BlockPos> spot = chickenSpot(level, stoneOf(zone));
            if (spot.isEmpty()) {
                continue;
            }
            Chicken c = EntityType.CHICKEN.create(level);
            if (c == null) {
                continue;
            }
            BlockPos p = spot.get();
            c.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(c, level, level.getCurrentDifficultyAt(p), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(c);
            GreekPowers.play(level, Vec3.atCenterOf(p), "gp.gr.lure.chicken", 0.8f, 1f);
        }
    }

    /** 石旁 2 格内（由近到远）的陆地空位：脚下结实、身体所在格没有碰撞、不是液体。 */
    static Optional<BlockPos> chickenSpot(ServerLevel level, BlockPos stone) {
        for (int r = 1; r <= 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy = 0; dy >= -1; dy--) {
                        BlockPos p = stone.offset(dx, dy, dz);
                        if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                                && level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                                && level.getFluidState(p).isEmpty()) {
                            return Optional.of(p);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** 没有施放上下文时（区域逐刻、到期）发表现。 */
    static void zoneFx(ServerLevel level, PowerZones.Zone zone, PowerFxPayload.Phase phase, Vec3 at,
                       List<? extends Entity> targets, int duration, String tag) {
        var p = new PowerFxPayload(zone.castId(), zone.powerId(), phase, -1, level.dimension().location(), at,
                targets.stream().limit(PowerFxPayload.MAX_TARGETS).map(Entity::getId).toList(),
                WorldClock.now(level.getServer()), zone.castId().hashCode(), duration, "", tag);
        for (ServerPlayer to : PowerFx.recipients(level, null, at)) {
            PowerFx.sendRaw(to, p);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
    }

    /** 只供 GameTest：某块石头当前吸引的动物数；没有时为 0。 */
    public static int pulledForTest(String castId) {
        State st = STATES.get(castId);
        return st == null ? 0 : st.pulled.size();
    }

    /** 只供 GameTest：某块石头当前吸引的动物中，落在给定范围内的数量（相邻测试结构的动物不算）。 */
    public static int pulledWithinForTest(ServerLevel level, String castId, AABB box) {
        State st = STATES.get(castId);
        if (st == null) {
            return 0;
        }
        return (int) st.pulled.keySet().stream().map(level::getEntity)
                .filter(e -> e != null && box.contains(e.position())).count();
    }

    /** 只供 GameTest：这只动物身上挂着哪些诱兽石（castId）的吸引目标。 */
    public static List<String> luresOnForTest(Mob m) {
        return m.goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof LureGoal)
                .map(g -> ((LureGoal) g.getGoal()).castId).toList();
    }

    /** 只供 GameTest：立即强制跑一轮吸引（刷新缓存）。 */
    public static void pullForTest(ServerLevel level, String castId) {
        PowerZones.all(level.getServer()).stream().filter(z -> z.castId().equals(castId)).findFirst().ifPresent(z -> {
            State st = state(z, WorldClock.now(level.getServer()));
            st.lastCache = NEVER;
            pull(level, z, st, WorldClock.now(level.getServer()));
        });
    }

    /** 只供 GameTest：把这块石头的产鸡时钟往前拨（相当于已放了 elapsed 刻），再补产。 */
    public static void advanceChickensForTest(ServerLevel level, String castId, long elapsed) {
        PowerZones.all(level.getServer()).stream().filter(z -> z.castId().equals(castId)).findFirst().ifPresent(z -> {
            long now = WorldClock.now(level.getServer());
            State st = state(z, now);
            State moved = new State(now - elapsed);
            moved.chickensTried = st.chickensTried;
            moved.pulled.putAll(st.pulled);
            moved.cache = st.cache;
            moved.lastCache = st.lastCache;
            moved.lastPull = st.lastPull;
            STATES.put(castId, moved);
            spawnChickens(level, z, moved, now);
        });
    }

    /**
     * 走向诱兽石：最高优先级的移动目标；到石头 3 格内停下。每 20 刻重新寻路一次（动物被推开、路被挡时）。
     */
    static final class LureGoal extends Goal {
        private final Mob mob;
        private final Vec3 target;
        final String castId;
        private int repath;

        LureGoal(Mob mob, Vec3 target, String castId) {
            this.mob = mob;
            this.target = target;
            this.castId = castId;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean far() {
            return mob.position().distanceToSqr(target) > LureRules.STOP_DISTANCE * LureRules.STOP_DISTANCE;
        }

        @Override
        public boolean canUse() {
            return far();
        }

        @Override
        public boolean canContinueToUse() {
            return far();
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            if (--repath <= 0) {
                repath = 20;
                mob.getNavigation().moveTo(target.x, target.y, target.z, SPEED);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return false;
        }
    }
}
