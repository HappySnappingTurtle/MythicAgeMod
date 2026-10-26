package io.github.happysnappingturtle.mythicage.boss;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.boss.ArenaClock;
import io.github.happysnappingturtle.mythicage.core.boss.ArenaPhase;
import io.github.happysnappingturtle.mythicage.core.boss.BossScaling;
import io.github.happysnappingturtle.mythicage.core.boss.MechanicWindows;
import io.github.happysnappingturtle.mythicage.core.boss.Participants;
import io.github.happysnappingturtle.mythicage.core.boss.WallCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 一个试炼竞技场实例（T23 §4）。格子坐标来自结构锚点（T22），参数来自 {@link ArenaSpec}。五文明通用：
 * <ul>
 *   <li>门：IDLE 时持钥匙右键打开 → OPEN；有合格玩家（生存 / 冒险、活着）踏进大厅 → 生成 Boss、按人数设血量 → FIGHT_P1，
 *       40 刻后关门（门洞里有东西就等）；战中大厅一侧右键门开 60 刻撤离；OPEN 1200 刻没人进就关门回 IDLE；</li>
 *   <li>战斗：Boss 拴在大厅里；人数变多按比例提高血量；大厅空 600 刻 → 复原（Boss 消失、柱与墙写回、关门）；
 *       Boss 实体 600 刻找不到也复原，不算胜利；</li>
 *   <li>胜利：门打开、宝库门永久打开；大厅空 600 刻后复原柱与墙并关门；</li>
 *   <li>竞技场改过的每一格都记下原方块（柱、碎块、墙、门、宝库门），复原时只写回仍是竞技场放的那个方块的格子。</li>
 * </ul>
 */
public final class TrialArena {
    public static final int GATE_CLOSE_DELAY = 40;
    public static final int GATE_EXIT_TICKS = 60;
    public static final int BOSS_MISSING_RESET = 600;
    public static final int BAR_RANGE = 8;

    /** 只供 GameTest：额外算作在线的假玩家（假玩家不在服务器玩家表里）。各用例的竞技场相距很远，共用一张表不会串。 */
    public static final List<ServerPlayer> TEST_PLAYERS = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final String id;
    private final String kind;
    private final ResourceKey<Level> dimension;
    private final BlockPos bossOrigin;
    private final BlockPos center;
    private final List<BlockPos> gate;
    private final List<BlockPos> vaultDoor;
    private final List<BlockPos> pillars;
    private final Map<WallCycle.Group, List<BlockPos>> walls;
    private final BoundingBox region;
    private final BlockPos gateAnchor;
    private final BlockPos vaultAnchor;

    private ArenaPhase phase = ArenaPhase.IDLE;
    private final ArenaClock clock = new ArenaClock();
    private int fightId;
    private UUID boss;
    private int players;
    private final MechanicWindows windows = new MechanicWindows();
    private final Participants participants = new Participants();
    private final WallCycle wallCycle = new WallCycle();
    private int brokenPillars;
    private boolean vaultOpen;
    private long gateCloseAt = -1;
    /** 竞技场改过的格子：原方块、放上去的方块。 */
    private final Map<BlockPos, BlockState[]> changed = new LinkedHashMap<>();
    private int bossMissing;
    private ServerBossEvent bar;

    private TrialArena(String id, String kind, ResourceKey<Level> dimension, BlockPos bossOrigin, BlockPos gateAnchor,
                      BlockPos vaultAnchor, List<BlockPos> pillars, List<BlockPos> wallA, List<BlockPos> wallB,
                      BoundingBox region, ArenaSpec spec) {
        this.id = id;
        this.kind = kind;
        this.dimension = dimension;
        this.bossOrigin = bossOrigin.immutable();
        this.center = bossOrigin.offset(0, 0, -spec.bossOffsetZ());
        this.gate = gateCells(gateAnchor, spec);
        this.vaultDoor = vaultCells(vaultAnchor, spec);
        this.pillars = List.copyOf(pillars);
        this.walls = new EnumMap<>(WallCycle.Group.class);
        walls.put(WallCycle.Group.A, List.copyOf(wallA));
        walls.put(WallCycle.Group.B, List.copyOf(wallB));
        this.region = region;
        this.gateAnchor = gateAnchor;
        this.vaultAnchor = vaultAnchor;
    }

    static List<BlockPos> gateCells(BlockPos a, ArenaSpec spec) {
        List<BlockPos> out = new ArrayList<>();
        if (a == null) {
            return out;
        }
        for (int y = 0; y < spec.gateHeight(); y++) {
            for (int dx = -spec.gateWidth() / 2; dx <= spec.gateWidth() / 2; dx++) {
                out.add(a.offset(dx, y, 0));
            }
        }
        return out;
    }

    /** 宝库门：锚点在大厅北墙内侧，向北挖 depth 格。 */
    static List<BlockPos> vaultCells(BlockPos a, ArenaSpec spec) {
        List<BlockPos> out = new ArrayList<>();
        if (a == null) {
            return out;
        }
        for (int d = 0; d < spec.vaultDepth(); d++) {
            for (int y = 0; y < spec.vaultHeight(); y++) {
                for (int dx = -spec.vaultWidth() / 2; dx <= spec.vaultWidth() / 2; dx++) {
                    out.add(a.offset(dx, y, -d));
                }
            }
        }
        return out;
    }

    // ───────────── 查询 ─────────────

    public String id() {
        return id;
    }

    public String kind() {
        return kind;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public Optional<ArenaSpec> spec() {
        return ArenaSpec.Loader.get(kind);
    }

    public ArenaPhase phase() {
        return phase;
    }

    public int fightId() {
        return fightId;
    }

    public Optional<UUID> bossId() {
        return Optional.ofNullable(boss);
    }

    public BlockPos bossOrigin() {
        return bossOrigin;
    }

    public BlockPos center() {
        return center;
    }

    public List<BlockPos> gate() {
        return gate;
    }

    public List<BlockPos> vaultDoor() {
        return vaultDoor;
    }

    public List<BlockPos> pillars() {
        return pillars;
    }

    public List<BlockPos> wall(WallCycle.Group g) {
        return walls.get(g);
    }

    public BoundingBox region() {
        return region;
    }

    public MechanicWindows windows() {
        return windows;
    }

    public Participants participants() {
        return participants;
    }

    public WallCycle wallCycle() {
        return wallCycle;
    }

    public int players() {
        return players;
    }

    public boolean vaultOpen() {
        return vaultOpen;
    }

    public boolean pillarBroken(int i) {
        return (brokenPillars & (1 << i)) != 0;
    }

    public boolean allPillarsBroken() {
        return Integer.bitCount(brokenPillars) >= pillars.size();
    }

    private int half() {
        return spec().map(ArenaSpec::halfSize).orElse(15);
    }

    private int roof() {
        return spec().map(ArenaSpec::roof).orElse(7);
    }

    /** 大厅的方块范围（地面下一层到穹顶）。 */
    public BoundingBox hall() {
        int h = half();
        return new BoundingBox(center.getX() - h, center.getY() - 1, center.getZ() - h, center.getX() + h,
                center.getY() + roof(), center.getZ() + h);
    }

    public boolean inHall(double x, double y, double z) {
        int h = half();
        return x >= center.getX() - h && x < center.getX() + h + 1 && z >= center.getZ() - h && z < center.getZ() + h + 1
                && y >= center.getY() - 1 && y < center.getY() + roof();
    }

    public boolean inHall(Entity e) {
        return e.level().dimension().equals(dimension) && inHall(e.getX(), e.getY(), e.getZ());
    }

    /** 受保护的格子：大厅、门洞、宝库门槽（总纲 §3.12）。 */
    public boolean protects(BlockPos p) {
        return hall().isInside(p) || gate.contains(p) || vaultDoor.contains(p);
    }

    public boolean isGate(BlockPos p) {
        return gate.contains(p);
    }

    /** 玩家在门的大厅一侧（门洞北面）。 */
    public boolean hallSide(Entity e) {
        return !gate.isEmpty() && e.getZ() < gate.get(0).getZ();
    }

    /** 大厅里合格的玩家：活着、生存或冒险模式。 */
    public List<ServerPlayer> playersInHall(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : allPlayers(level)) {
            if (eligible(p) && inHall(p)) {
                out.add(p);
            }
        }
        return out;
    }

    static List<ServerPlayer> allPlayers(ServerLevel level) {
        if (TEST_PLAYERS.isEmpty()) {
            return level.players();
        }
        List<ServerPlayer> all = new ArrayList<>(level.players());
        for (ServerPlayer p : TEST_PLAYERS) {
            if (p.level() == level) {
                all.add(p);
            }
        }
        return all;
    }

    public static boolean eligible(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative();
    }

    public Optional<Mob> boss(ServerLevel level) {
        if (boss == null) {
            return Optional.empty();
        }
        Entity e = level.getEntity(boss);
        return e instanceof Mob m && m.isAlive() ? Optional.of(m) : Optional.empty();
    }

    // ───────────── 门 ─────────────

    /** 右键门。返回给玩家看的提示（空 = 不提示）。 */
    public Optional<Component> useGate(ServerLevel level, ServerPlayer player, ItemStack held) {
        ArenaSpec spec = spec().orElse(null);
        if (spec == null) {
            return Optional.empty();
        }
        boolean key = BuiltInRegistries.ITEM.getKey(held.getItem()).equals(spec.keyItem());
        switch (phase) {
            case IDLE -> {
                if (!key) {
                    return Optional.of(Component.translatable("mythic_age.arena.gate.locked"));
                }
                openGate(level);
                setPhase(ArenaPhase.OPEN);
                clock.reset();
                return Optional.of(Component.translatable("mythic_age.arena.gate.opened"));
            }
            case FIGHT_P1, FIGHT_P2 -> {
                if (!key && !hallSide(player)) {
                    return Optional.of(Component.translatable("mythic_age.arena.gate.locked"));
                }
                openGate(level);
                gateCloseAt = level.getGameTime() + GATE_EXIT_TICKS;
                return Optional.of(Component.translatable("mythic_age.arena.gate.exit"));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    void openGate(ServerLevel level) {
        for (BlockPos p : gate) {
            BlockState s = level.getBlockState(p);
            if (!s.isAir()) {
                change(level, p, Blocks.AIR.defaultBlockState());
            }
        }
        gateCloseAt = -1;
    }

    /** 关门：门洞里有生物或掉落物就不关（返回 false，稍后再试）。 */
    boolean closeGate(ServerLevel level) {
        for (BlockPos p : gate) {
            if (!level.getEntitiesOfClass(Entity.class, new AABB(p), e -> e instanceof LivingEntity || e instanceof
                    net.minecraft.world.entity.item.ItemEntity).isEmpty()) {
                return false;
            }
        }
        for (BlockPos p : gate) {
            restore(level, p);
        }
        gateCloseAt = -1;
        return true;
    }

    public boolean gateOpen(ServerLevel level) {
        return !gate.isEmpty() && level.getBlockState(gate.get(0)).isAir();
    }

    // ───────────── 方块改动与复原 ─────────────

    /** 改一格并记下原方块（同一格多次改动只记最早的原方块）。 */
    public void change(ServerLevel level, BlockPos p, BlockState to) {
        BlockState now = level.getBlockState(p);
        BlockState[] rec = changed.get(p);
        if (rec == null) {
            changed.put(p.immutable(), new BlockState[]{now, to});
        } else {
            rec[1] = to;
        }
        level.setBlock(p, to, Block.UPDATE_ALL);
    }

    /** 写回一格：只在它仍是竞技场放的方块时写回（玩家改过就不覆盖）。 */
    public void restore(ServerLevel level, BlockPos p) {
        BlockState[] rec = changed.get(p);
        if (rec == null) {
            return;
        }
        if (level.getBlockState(p).equals(rec[1])) {
            level.setBlock(p, rec[0], Block.UPDATE_ALL);
        }
        changed.remove(p);
    }

    /** 写回全部（宝库门除外，它打开后永久开着）。 */
    void restoreAll(ServerLevel level) {
        for (BlockPos p : new ArrayList<>(changed.keySet())) {
            if (!vaultDoor.contains(p)) {
                restore(level, p);
            }
        }
        brokenPillars = 0;
    }

    public void markPillarBroken(int i) {
        brokenPillars |= 1 << i;
    }

    /** 升 / 落一组机关墙。 */
    public void setWall(ServerLevel level, WallCycle.Group g, boolean up) {
        ArenaSpec spec = spec().orElse(null);
        if (spec == null) {
            return;
        }
        BlockState wall = BuiltInRegistries.BLOCK.get(spec.wallBlock()).defaultBlockState();
        for (BlockPos base : walls.get(g)) {
            for (int y = 0; y < spec.wallHeight(); y++) {
                BlockPos p = base.above(y);
                if (up && level.getBlockState(p).isAir()) {
                    change(level, p, wall);
                } else if (!up) {
                    restore(level, p);
                }
            }
        }
    }

    public boolean wallUp(ServerLevel level, WallCycle.Group g) {
        List<BlockPos> cells = walls.get(g);
        return !cells.isEmpty() && changed.containsKey(cells.get(0));
    }

    // ───────────── 推进 ─────────────

    private void setPhase(ArenaPhase to) {
        phase = phase.go(to);
        TrialArenas.markDirty();
    }

    public void tick(ServerLevel level) {
        if (phase == ArenaPhase.IDLE) {
            return;
        }
        long now = level.getGameTime();
        List<ServerPlayer> inHall = playersInHall(level);
        if (gateCloseAt >= 0 && now >= gateCloseAt && !closeGate(level)) {
            gateCloseAt = now + 10;
        }
        switch (phase) {
            case OPEN -> {
                if (!inHall.isEmpty()) {
                    startFight(level, inHall);
                } else if (clock.tick(false, ArenaClock.OPEN_TIMEOUT)) {
                    if (closeGate(level)) {
                        setPhase(ArenaPhase.IDLE);
                        clock.reset();
                    }
                }
            }
            case FIGHT_P1, FIGHT_P2 -> tickFight(level, inHall);
            case VICTORY -> {
                if (clock.tick(!inHall.isEmpty(), ArenaClock.EMPTY_RESET)) {
                    reset(level);
                }
            }
            default -> {
            }
        }
        updateBar(level);
    }

    private void startFight(ServerLevel level, List<ServerPlayer> inHall) {
        ArenaSpec spec = spec().orElseThrow();
        setPhase(ArenaPhase.FIGHT_P1);
        fightId++;
        participants.clear();
        windows.reset();
        clock.reset();
        bossMissing = 0;
        players = Math.max(1, inHall.size());
        var type = BuiltInRegistries.ENTITY_TYPE.get(spec.boss());
        Entity e = type.create(level);
        if (!(e instanceof Mob mob)) {
            MythicAge.LOGGER.error("竞技场 {} 的 Boss 类型 {} 不是生物", id, spec.boss());
            reset(level);
            return;
        }
        mob.moveTo(bossOrigin.getX() + 0.5, bossOrigin.getY(), bossOrigin.getZ() + 0.5, 0, 0);
        mob.setPersistenceRequired();
        if (mob instanceof TrialBoss tb) {
            tb.bindArena(id);
        }
        float max = BossScaling.maxHealth(spec.baseHealth(), players);
        var attr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(max);
        }
        mob.setHealth(max);
        level.addFreshEntity(mob);
        boss = mob.getUUID();
        gateCloseAt = level.getGameTime() + GATE_CLOSE_DELAY;
        for (ServerPlayer p : inHall) {
            p.displayClientMessage(Component.translatable("mythic_age.arena.fight.start", mob.getDisplayName()), true);
        }
    }

    private void tickFight(ServerLevel level, List<ServerPlayer> inHall) {
        for (ServerPlayer p : inHall) {
            participants.present(p.getUUID());
        }
        Optional<Mob> b = boss(level);
        if (b.isEmpty()) {
            if (++bossMissing >= BOSS_MISSING_RESET) {
                MythicAge.LOGGER.info("竞技场 {}：Boss 实体 {} 刻找不到，复原（不算胜利）", id, bossMissing);
                reset(level);
            }
            return;
        }
        bossMissing = 0;
        Mob mob = b.get();
        ArenaSpec spec = spec().orElseThrow();
        if (!inHall(mob) || mob.position().distanceTo(bossOrigin.getBottomCenter()) > spec.leash()) {
            mob.teleportTo(bossOrigin.getX() + 0.5, bossOrigin.getY(), bossOrigin.getZ() + 0.5);
            mob.getNavigation().stop();
            if (mob instanceof TrialBoss tb) {
                tb.onLeashed();
            }
        }
        if (inHall.size() > players) {
            var attr = mob.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) {
                float oldMax = (float) attr.getBaseValue();
                float newMax = BossScaling.raise(oldMax, BossScaling.maxHealth(spec.baseHealth(), inHall.size()));
                float cur = BossScaling.rescale(mob.getHealth(), oldMax, newMax);
                attr.setBaseValue(newMax);
                mob.setHealth(cur);
            }
            players = inHall.size();
            TrialArenas.markDirty();
        }
        if (phase == ArenaPhase.FIGHT_P1 && mob.getHealth() <= mob.getMaxHealth() * 0.5F && mob instanceof TrialBoss tb) {
            setPhase(ArenaPhase.FIGHT_P2);
            tb.onPhaseTwo();
        }
        if (clock.tick(!inHall.isEmpty(), ArenaClock.EMPTY_RESET)) {
            reset(level);
        }
    }

    /** Boss 死了：胜利（结算在阶段 ③）、开门、开宝库门、落墙。 */
    public void onBossDeath(ServerLevel level, Mob mob) {
        if (!phase.fighting() || !mob.getUUID().equals(boss)) {
            return;
        }
        setPhase(ArenaPhase.VICTORY);
        clock.reset();
        for (WallCycle.Action a : wallCycle.stop()) {
            setWall(level, a.group(), false);
        }
        openGate(level);
        openVault(level);
        boss = null;
    }

    void openVault(ServerLevel level) {
        if (vaultOpen) {
            return;
        }
        for (BlockPos p : vaultDoor) {
            if (!level.getBlockState(p).isAir()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        vaultOpen = true;
        TrialArenas.markDirty();
    }

    /** 复原：Boss 消失、墙落下、柱写回、关门 → IDLE。 */
    public void reset(ServerLevel level) {
        boss(level).ifPresent(Entity::discard);
        boss = null;
        wallCycle.stop();
        restoreAll(level);
        if (phase == ArenaPhase.OPEN) {
            phase = phase.go(ArenaPhase.IDLE);
        } else if (phase != ArenaPhase.IDLE) {
            if (phase != ArenaPhase.RESETTING) {
                phase = phase.go(ArenaPhase.RESETTING);
            }
            phase = phase.go(ArenaPhase.IDLE);
        }
        gateCloseAt = -1;
        clock.reset();
        windows.reset();
        players = 0;
        clearBar();
        TrialArenas.markDirty();
    }

    // ───────────── 血条 ─────────────

    private void updateBar(ServerLevel level) {
        Optional<Mob> b = phase.fighting() ? boss(level) : Optional.empty();
        if (b.isEmpty()) {
            clearBar();
            return;
        }
        Mob mob = b.get();
        if (bar == null) {
            bar = new ServerBossEvent(mob.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        }
        bar.setName(phase == ArenaPhase.FIGHT_P2
                ? Component.translatable("mythic_age.arena.bar.phase2", mob.getDisplayName()) : mob.getDisplayName());
        bar.setProgress(Math.max(0, Math.min(1, mob.getHealth() / mob.getMaxHealth())));
        List<ServerPlayer> all = allPlayers(level);
        AABB near = AABB.of(hall()).inflate(BAR_RANGE);
        for (ServerPlayer p : all) {
            boolean show = near.contains(p.position());
            if (show && !bar.getPlayers().contains(p)) {
                bar.addPlayer(p);
            } else if (!show && bar.getPlayers().contains(p)) {
                bar.removePlayer(p);
            }
        }
    }

    private void clearBar() {
        if (bar != null) {
            bar.removeAllPlayers();
            bar = null;
        }
    }

    public Optional<ServerBossEvent> barForTest() {
        return Optional.ofNullable(bar);
    }

    // ───────────── 存档 ─────────────

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Id", id);
        t.putString("Kind", kind);
        t.putString("Dim", dimension.location().toString());
        t.putLong("Origin", bossOrigin.asLong());
        t.putLong("GateAnchor", gateAnchor == null ? Long.MIN_VALUE : gateAnchor.asLong());
        t.putLong("VaultAnchor", vaultAnchor == null ? Long.MIN_VALUE : vaultAnchor.asLong());
        t.put("Pillars", longs(pillars));
        t.put("WallA", longs(walls.get(WallCycle.Group.A)));
        t.put("WallB", longs(walls.get(WallCycle.Group.B)));
        t.putIntArray("Region", new int[]{region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ()});
        t.putString("Phase", phase.name());
        t.putInt("Empty", clock.empty());
        t.putInt("FightId", fightId);
        if (boss != null) {
            t.putUUID("Boss", boss);
        }
        t.putInt("Players", players);
        t.putInt("Windows", windows.count());
        t.putLong("WindowAt", windows.lastStart());
        ListTag ps = new ListTag();
        participants.entries().forEach((uuid, e) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Id", uuid);
            c.putBoolean("Damaged", e.damaged());
            c.putInt("Present", e.presentTicks());
            ps.add(c);
        });
        t.put("Participants", ps);
        t.putString("Walls", wallCycle.save());
        t.putInt("Broken", brokenPillars);
        t.putBoolean("VaultOpen", vaultOpen);
        t.putLong("GateCloseAt", gateCloseAt);
        ListTag ch = new ListTag();
        changed.forEach((p, rec) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", p.asLong());
            c.put("Was", NbtUtils.writeBlockState(rec[0]));
            c.put("Now", NbtUtils.writeBlockState(rec[1]));
            ch.add(c);
        });
        t.put("Changed", ch);
        return t;
    }

    /** 新建实例（登记时）。 */
    public static TrialArena create(String id, String kind, ResourceKey<Level> dim, BlockPos bossOrigin, BlockPos gateAnchor,
                                    BlockPos vaultAnchor, List<BlockPos> pillars, List<BlockPos> wallA, List<BlockPos> wallB,
                                    BoundingBox region, ArenaSpec spec) {
        return new TrialArena(id, kind, dim, bossOrigin, gateAnchor, vaultAnchor, pillars, wallA, wallB, region, spec);
    }

    public static Optional<TrialArena> load(CompoundTag t, HolderGetter<Block> blocks) {
        String kind = t.getString("Kind");
        Optional<ArenaSpec> spec = ArenaSpec.Loader.get(kind);
        if (spec.isEmpty()) {
            MythicAge.LOGGER.warn("竞技场 {} 的种类 {} 没有数据，跳过", t.getString("Id"), kind);
            return Optional.empty();
        }
        int[] r = t.getIntArray("Region");
        BoundingBox region = r.length == 6 ? new BoundingBox(r[0], r[1], r[2], r[3], r[4], r[5]) : new BoundingBox(0, 0, 0, 0, 0, 0);
        TrialArena a = create(t.getString("Id"), kind,
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("Dim"))),
                BlockPos.of(t.getLong("Origin")), pos(t.getLong("GateAnchor")), pos(t.getLong("VaultAnchor")),
                positions(t.getList("Pillars", Tag.TAG_LONG)), positions(t.getList("WallA", Tag.TAG_LONG)),
                positions(t.getList("WallB", Tag.TAG_LONG)), region, spec.get());
        a.phase = ArenaPhase.valueOf(t.getString("Phase"));
        a.clock.restore(t.getInt("Empty"));
        a.fightId = t.getInt("FightId");
        a.boss = t.hasUUID("Boss") ? t.getUUID("Boss") : null;
        a.players = t.getInt("Players");
        a.windows.restore(t.getInt("Windows"), t.getLong("WindowAt"));
        for (Tag x : t.getList("Participants", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            a.participants.restore(c.getUUID("Id"), new Participants.Entry(c.getBoolean("Damaged"), c.getInt("Present")));
        }
        a.wallCycle.load(t.getString("Walls"));
        a.brokenPillars = t.getInt("Broken");
        a.vaultOpen = t.getBoolean("VaultOpen");
        a.gateCloseAt = t.getLong("GateCloseAt");
        for (Tag x : t.getList("Changed", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            a.changed.put(BlockPos.of(c.getLong("Pos")), new BlockState[]{NbtUtils.readBlockState(blocks, c.getCompound("Was")),
                    NbtUtils.readBlockState(blocks, c.getCompound("Now"))});
        }
        return Optional.of(a);
    }

    private static BlockPos pos(long l) {
        return l == Long.MIN_VALUE ? null : BlockPos.of(l);
    }

    private static ListTag longs(List<BlockPos> ps) {
        ListTag l = new ListTag();
        ps.forEach(p -> l.add(net.minecraft.nbt.LongTag.valueOf(p.asLong())));
        return l;
    }

    private static List<BlockPos> positions(ListTag l) {
        List<BlockPos> out = new ArrayList<>();
        for (Tag x : l) {
            out.add(BlockPos.of(((net.minecraft.nbt.LongTag) x).getAsLong()));
        }
        return out;
    }
}
