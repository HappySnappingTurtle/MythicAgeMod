package io.github.happysnappingturtle.mythicage.lair;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.lair.LairRules;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.power.TempBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 精英巢穴（五文明通用，T25a §6，06 号 E1）：每处巢穴记下种类、锚点、登记的精英与清空的日子。
 * <ul>
 *   <li>精英全死 → 记清空日；空置 {@value LairRules#VACANT_DAYS} 日后，区块载入且 {@value LairRules#PLAYER_CLEAR} 格内没有玩家时，
 *       生成满血精英并补足随从（玩家带走的不追回、多出来的不删）；圣物宝库按玩家首开，不随入住重置；</li>
 *   <li>会封洞的种类（独眼巨人洞穴）：傍晚起，精英在洞里就用临时方块封住洞口，到天亮；临时方块在区块卸载、关服时会复原，
 *       所以每 {@value #CHECK_TICKS} 刻重新检查一次（TC-73）；</li>
 *   <li>洞内的玩家贴着巨石潜行右键 {@value LairRules#PUSH_TICKS} 刻，巨石让出 1 × 2 的缝，当晚不再封（D15）。</li>
 * </ul>
 * 每种巢穴的行为由 {@link Kind} 提供，各文明在 init 里 {@link #kind} 注册；结构登记 handler 调 {@link #create}。
 */
public final class EliteLairs extends SavedData {
    public static final String NAME = "mythic_age_elite_lairs";
    public static final int CHECK_TICKS = 100;
    /** 随从、精英身上记巢穴键的持久数据键。 */
    public static final String LAIR_KEY = "mythic_age.lair";

    /** 一种巢穴的行为。 */
    public interface Kind {
        /** 生成并放进世界一只满血精英（绑定好家与睡处），返回它；生成失败返回 null。 */
        @Nullable
        Mob spawnElite(ServerLevel level, Lair lair);

        /** 生成一只随从（不必放进世界之外的处理），失败返回 null。随从数为 0 的种类不会调用。 */
        @Nullable
        Mob spawnFollower(ServerLevel level, Lair lair);

        /** 登记的随从数。 */
        int followers();

        /** 夜里封洞用的方块；不封洞的种类返回 null。 */
        @Nullable
        BlockState sealBlock();

        /** 精英现在算不算「在洞里」（封洞条件）。 */
        boolean inside(Mob elite, Lair lair);

        /** 找精英与随从时以哪个锚点为中心。 */
        String homeAnchor();
    }

    /** 一处巢穴。 */
    public static final class Lair {
        public final String key;
        public final ResourceKey<Level> dim;
        public final String kind;
        public final Map<String, List<BlockPos>> anchors;
        public final List<UUID> elites = new ArrayList<>();
        public long clearedDay = LairRules.NOT_CLEARED;
        /** 被推开巨石的那一晚（{@link LairRules#nightIndex}），没推开过为 {@link Long#MIN_VALUE}。 */
        public long pushedNight = Long.MIN_VALUE;

        Lair(String key, ResourceKey<Level> dim, String kind, Map<String, List<BlockPos>> anchors) {
            this.key = key;
            this.dim = dim;
            this.kind = kind;
            this.anchors = anchors;
        }

        public List<BlockPos> anchors(String name) {
            return anchors.getOrDefault(name, List.of());
        }

        public BlockPos home(Kind k) {
            List<BlockPos> h = anchors(k.homeAnchor());
            return h.isEmpty() ? anchors.values().iterator().next().get(0) : h.get(0);
        }
    }

    private static final Map<String, Kind> KINDS = new ConcurrentHashMap<>();
    /** 只供 GameTest：把「一天内的时刻」固定成这个值（{@code null} = 用世界时间），整批测试共用，批前设、批后清。 */
    private static volatile Long testDayTime;
    /** 推巨石的进度：玩家 → (累计刻数, 上一下的刻)。 */
    private static final Map<UUID, long[]> PUSH = new HashMap<>();

    private final Map<String, Lair> lairs = new LinkedHashMap<>();

    public static void kind(String kind, Kind k) {
        KINDS.put(kind, k);
    }

    public static EliteLairs get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EliteLairs::new, EliteLairs::load, null), NAME);
    }

    @Nullable
    public Lair lair(String key) {
        return lairs.get(key);
    }

    public List<Lair> all() {
        return List.copyOf(lairs.values());
    }

    /** 结构登记时建账：生成精英与随从。 */
    public static Lair create(ServerLevel level, String key, String kind, Map<String, List<BlockPos>> anchors) {
        EliteLairs data = get(level.getServer());
        Lair lair = new Lair(key, level.dimension(), kind, Map.copyOf(anchors));
        data.lairs.put(key, lair);
        Kind k = KINDS.get(kind);
        if (k != null) {
            populate(level, lair, k);
        } else {
            MythicAge.LOGGER.warn("巢穴种类 {} 没有登记行为", kind);
        }
        data.setDirty();
        return lair;
    }

    /** 生成满血精英并补足随从。 */
    static void populate(ServerLevel level, Lair lair, Kind k) {
        lair.elites.clear();
        Mob elite = k.spawnElite(level, lair);
        if (elite != null) {
            elite.getPersistentData().putString(LAIR_KEY, lair.key);
            lair.elites.add(elite.getUUID());
        }
        int add = LairRules.followersToAdd(k.followers(), followersAlive(level, lair, k));
        for (int i = 0; i < add; i++) {
            Mob f = k.spawnFollower(level, lair);
            if (f != null) {
                f.getPersistentData().putString(LAIR_KEY, lair.key);
            }
        }
        lair.clearedDay = LairRules.NOT_CLEARED;
    }

    public static int followersAlive(ServerLevel level, Lair lair, Kind k) {
        BlockPos h = lair.home(k);
        return level.getEntitiesOfClass(Mob.class, new AABB(h).inflate(LairRules.LEASH),
                e -> e.isAlive() && lair.key.equals(e.getPersistentData().getString(LAIR_KEY)) && !lair.elites.contains(e.getUUID()))
                .size();
    }

    @Nullable
    public static Mob elite(ServerLevel level, Lair lair) {
        for (UUID id : lair.elites) {
            if (level.getEntity(id) instanceof Mob m && m.isAlive()) {
                return m;
            }
        }
        return null;
    }

    // ───────────── 事件 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        tick(server);
    }

    /** 检查所有巢穴：到期的再入住、该封洞的封洞（GameTest 直接调）。 */
    public static void tick(MinecraftServer server) {
        EliteLairs data = get(server);
        long today = MythDays.today(server);
        for (Lair lair : data.lairs.values()) {
            ServerLevel level = server.getLevel(lair.dim);
            Kind k = KINDS.get(lair.kind);
            if (level == null || k == null) {
                continue;
            }
            BlockPos home = lair.home(k);
            boolean loaded = level.isLoaded(home);
            long normalized = LairRules.normalize(lair.clearedDay, today);
            if (normalized != lair.clearedDay) {
                lair.clearedDay = normalized;
                data.setDirty();
            }
            boolean playerNear = level.getNearestPlayer(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, LairRules.PLAYER_CLEAR,
                    p -> !p.isSpectator()) != null;
            if (LairRules.shouldRepopulate(lair.clearedDay, today, loaded, playerNear)) {
                populate(level, lair, k);
                data.setDirty();
            }
            seal(level, lair, k);
        }
    }

    private static void seal(ServerLevel level, Lair lair, Kind k) {
        BlockState block = k.sealBlock();
        if (block == null) {
            return;
        }
        long dayTime = dayTime(level);
        Mob elite = elite(level, lair);
        boolean loaded = lair.anchors("entrance").stream().allMatch(level::isLoaded);
        boolean pushed = lair.pushedNight == LairRules.nightIndex(dayTime);
        if (!LairRules.shouldSeal(dayTime, loaded, elite != null && k.inside(elite, lair), pushed)) {
            return;
        }
        long until = LairRules.ticksUntilDawn(dayTime);
        UUID owner = elite.getUUID();
        for (BlockPos p : lair.anchors("entrance")) {
            BlockState here = level.getBlockState(p);
            if (here.is(block.getBlock()) || !here.canBeReplaced()) {
                continue;
            }
            // 洞口站着人、羊时不封那一格（不把实体关进石头里）
            if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(p)).isEmpty()) {
                continue;
            }
            TempBlocks.place(level, p, block, until, owner, false);
        }
    }

    /** 精英死亡：全死了就记清空日。 */
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) {
            return;
        }
        String key = e.getPersistentData().getString(LAIR_KEY);
        if (key.isEmpty()) {
            return;
        }
        EliteLairs data = get(level.getServer());
        Lair lair = data.lairs.get(key);
        if (lair == null || !lair.elites.contains(e.getUUID())) {
            return;
        }
        int alive = 0;
        for (UUID id : lair.elites) {
            if (!id.equals(e.getUUID()) && level.getEntity(id) instanceof LivingEntity l && l.isAlive()) {
                alive++;
            }
        }
        lair.clearedDay = LairRules.afterDeath(alive, lair.clearedDay, MythDays.today(level.getServer()));
        data.setDirty();
    }

    /** D15：洞内贴着封洞巨石潜行右键 3 秒，巨石让出 1 × 2 的缝，当晚不再封。 */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown()) {
            return;
        }
        BlockPos pos = event.getPos();
        Lair lair = lairAtEntrance(level, pos);
        if (lair == null) {
            return;
        }
        Kind k = KINDS.get(lair.kind);
        if (k == null || k.sealBlock() == null || !level.getBlockState(pos).is(k.sealBlock().getBlock())) {
            return;
        }
        event.setCanceled(true);
        if (!insideOf(lair, k, player.position(), pos)) {
            return;
        }
        long now = level.getGameTime();
        long[] st = PUSH.computeIfAbsent(player.getUUID(), u -> new long[]{0, now});
        int progress = LairRules.pushProgress((int) st[0], st[1], now);
        st[0] = progress;
        st[1] = now;
        if (progress < LairRules.PUSH_TICKS) {
            player.displayClientMessage(Component.translatable("mythic_age.lair.pushing",
                    Math.min(100, progress * 100 / LairRules.PUSH_TICKS)), true);
            if (progress % 20 < 5) {
                level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 0.5F);
            }
            return;
        }
        PUSH.remove(player.getUUID());
        openGap(level, lair, pos);
        lair.pushedNight = LairRules.nightIndex(dayTime(level));
        get(level.getServer()).setDirty();
        player.displayClientMessage(Component.translatable("mythic_age.lair.pushed"), true);
        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.2F, 0.5F);
    }

    /** 让出缝：被推的那一列最低的两格巨石复原（底下那格 + 上面一格）。 */
    static void openGap(ServerLevel level, Lair lair, BlockPos pushed) {
        int minY = Integer.MAX_VALUE;
        for (BlockPos p : lair.anchors("entrance")) {
            if (p.getX() == pushed.getX() && p.getZ() == pushed.getZ()) {
                minY = Math.min(minY, p.getY());
            }
        }
        if (minY == Integer.MAX_VALUE) {
            return;
        }
        for (int dy = 0; dy < 2; dy++) {
            BlockPos p = new BlockPos(pushed.getX(), minY + dy, pushed.getZ());
            if (!TempBlocks.restoreNow(level, p)) {
                level.removeBlock(p, false);
            }
        }
    }

    /** 玩家在巨石的洞内一侧：离睡处比巨石离睡处近。 */
    static boolean insideOf(Lair lair, Kind k, Vec3 player, BlockPos boulder) {
        List<BlockPos> sleep = lair.anchors("sleep");
        BlockPos in = sleep.isEmpty() ? lair.home(k) : sleep.get(0);
        Vec3 c = Vec3.atBottomCenterOf(in);
        double dp = horizontalSq(player, c);
        double db = horizontalSq(Vec3.atBottomCenterOf(boulder), c);
        return dp < db;
    }

    private static double horizontalSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    @Nullable
    static Lair lairAtEntrance(ServerLevel level, BlockPos pos) {
        for (Lair l : get(level.getServer()).lairs.values()) {
            if (l.dim.equals(level.dimension()) && l.anchors("entrance").contains(pos)) {
                return l;
            }
        }
        return null;
    }

    static long dayTime(ServerLevel level) {
        Long t = testDayTime;
        return t != null ? t : level.getDayTime();
    }

    /** 只供 GameTest：固定一天内的时刻（传 null 恢复）。 */
    public static void dayTimeForTest(@Nullable Long t) {
        testDayTime = t;
    }

    /** 只供 GameTest：只对这一处巢穴做一次封洞检查。 */
    public static void sealForTest(ServerLevel level, String key) {
        Lair l = get(level.getServer()).lairs.get(key);
        Kind k = l == null ? null : KINDS.get(l.kind);
        if (k != null) {
            seal(level, l, k);
        }
    }

    /** 只供 GameTest：删掉一处巢穴的登记。 */
    public static void forgetForTest(MinecraftServer server, String key) {
        get(server).lairs.remove(key);
    }

    /** 只供 GameTest：改清空日。 */
    public static void setClearedForTest(MinecraftServer server, String key, long day) {
        Lair l = get(server).lairs.get(key);
        if (l != null) {
            l.clearedDay = day;
        }
    }

    // ───────────── 存取 ─────────────

    public static EliteLairs load(CompoundTag tag, HolderLookup.Provider registries) {
        EliteLairs d = new EliteLairs();
        for (Tag x : tag.getList("lairs", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            try {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim")));
                Map<String, List<BlockPos>> anchors = new LinkedHashMap<>();
                CompoundTag a = c.getCompound("anchors");
                for (String name : a.getAllKeys()) {
                    List<BlockPos> pts = new ArrayList<>();
                    for (long l : a.getLongArray(name)) {
                        pts.add(BlockPos.of(l));
                    }
                    anchors.put(name, List.copyOf(pts));
                }
                Lair lair = new Lair(c.getString("key"), dim, c.getString("kind"), Map.copyOf(anchors));
                for (Tag u : c.getList("elites", Tag.TAG_INT_ARRAY)) {
                    lair.elites.add(NbtUtils.loadUUID(u));
                }
                lair.clearedDay = c.getLong("cleared");
                lair.pushedNight = c.contains("pushed") ? c.getLong("pushed") : Long.MIN_VALUE;
                d.lairs.put(lair.key, lair);
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("跳过损坏的巢穴登记：{}（{}）", c, e.getMessage());
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Lair l : lairs.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("key", l.key);
            c.putString("dim", l.dim.location().toString());
            c.putString("kind", l.kind);
            CompoundTag a = new CompoundTag();
            l.anchors.forEach((name, pts) -> a.put(name, new LongArrayTag(pts.stream().mapToLong(BlockPos::asLong).toArray())));
            c.put("anchors", a);
            ListTag elites = new ListTag();
            for (UUID u : l.elites) {
                elites.add(NbtUtils.createUUID(u));
            }
            c.put("elites", elites);
            c.putLong("cleared", l.clearedDay);
            c.putLong("pushed", l.pushedNight);
            list.add(c);
        }
        tag.put("lairs", list);
        return tag;
    }

    /** 只用于实体可能不在世界里的检查。 */
    static boolean isLairEntity(Entity e, String key) {
        return key.equals(e.getPersistentData().getString(LAIR_KEY));
    }
}
