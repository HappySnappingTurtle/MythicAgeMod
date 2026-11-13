package io.github.happysnappingturtle.mythicage.village.temple;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 村庄神庙的查找（GA-13 §2.1）：各文明登记一个 {@link Provider}，按村里的建筑算出 {@link VillageTemple}；
 * 结果缓存 {@link #TTL} 刻，建筑落成、拆改时调用 {@link #invalidate} 立即作废。
 */
public final class VillageTemples {
    /** 缓存的有效期（刻）。 */
    public static final int TTL = 100;

    /** 一个文明怎样从村子算出神庙。 */
    public interface Provider {
        /** @param plan 村子的规划；旧式村子没有，为 null */
        Optional<VillageTemple> resolve(ServerLevel level, VillageRegistry.Village village,
                                        @org.jetbrains.annotations.Nullable VillagePlans.Plan plan, VillageEconomy.State state);
    }

    /** 神庙祭坛上的手持物品交互（献供、供奉圣物）；还没有神庙的新村，祭坛是奠基石，奠基石通过它接入。 */
    public interface AltarUse {
        net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
                                                            BlockPos pos, net.minecraft.world.entity.player.Player player);
    }

    private record Cached(long until, Optional<VillageTemple> temple) {
    }

    private static final Map<String, AltarUse> ALTAR_USE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FORCED_LEVEL = new ConcurrentHashMap<>();

    /** 只供 GameTest：把某村神庙的等级定为 level（不用真的盖卫城）；传负数清除。 */
    public static void forceLevelForTest(UUID village, int level) {
        if (level < 0) {
            FORCED_LEVEL.remove(village);
        } else {
            FORCED_LEVEL.put(village, level);
        }
        invalidate(village);
    }

    private static final Map<UUID, GlobalPos> FORCED_ALTAR = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> FORCED_INTEGRITY = new ConcurrentHashMap<>();

    /** 只供 GameTest：把某村神庙的完好度定为 v（不用真的拆方块）；传负数清除。 */
    public static void forceIntegrityForTest(UUID village, double v) {
        if (v < 0) {
            FORCED_INTEGRITY.remove(village);
        } else {
            FORCED_INTEGRITY.put(village, v);
        }
        invalidate(village);
    }

    /** 测试强制的完好度；没有为空。各文明的 {@link Provider} 读它。 */
    public static Optional<Double> forcedIntegrity(UUID village) {
        return Optional.ofNullable(FORCED_INTEGRITY.get(village));
    }

    /** 只供 GameTest：把某村神庙的祭坛（与圆心）放到 altar；传 null 清除。 */
    public static void forceAltarForTest(UUID village, @org.jetbrains.annotations.Nullable GlobalPos altar) {
        if (altar == null) {
            FORCED_ALTAR.remove(village);
        } else {
            FORCED_ALTAR.put(village, altar);
        }
        invalidate(village);
    }

    /** 测试强制的等级；没有为空。各文明的 {@link Provider} 读它。 */
    public static Optional<Integer> forcedLevel(UUID village) {
        return Optional.ofNullable(FORCED_LEVEL.get(village));
    }

    public static void registerAltarUse(String civ, AltarUse use) {
        ALTAR_USE.put(civ, use);
    }

    public static Optional<AltarUse> altarUse(String civ) {
        return Optional.ofNullable(ALTAR_USE.get(civ));
    }

    private static final Map<String, Provider> PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();

    private VillageTemples() {
    }

    public static void register(String civ, Provider p) {
        PROVIDERS.put(civ, p);
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        CACHE.clear();
        FORCED_LEVEL.clear();
        FORCED_ALTAR.clear();
        FORCED_INTEGRITY.clear();
    }

    public static void invalidate(UUID village) {
        CACHE.remove(village);
    }

    public static void invalidateAll() {
        CACHE.clear();
    }

    /** 这个村子的神庙；村子没有规划、文明没有神庙、或维度没加载时为空。 */
    public static Optional<VillageTemple> of(MinecraftServer server, VillageRegistry.Village village) {
        ServerLevel level = server.getLevel(village.bell().dimension());
        if (level == null) {
            return Optional.empty();
        }
        long now = level.getGameTime();
        Cached c = CACHE.get(village.id());
        if (c != null && now < c.until()) {
            return c.temple();
        }
        Optional<VillageTemple> t = resolve(level, village);
        CACHE.put(village.id(), new Cached(now + TTL, t));
        return t;
    }

    public static Optional<VillageTemple> of(MinecraftServer server, UUID village) {
        return VillageRegistry.get(server).byId(village).flatMap(v -> of(server, v));
    }

    private static Optional<VillageTemple> resolve(ServerLevel level, VillageRegistry.Village village) {
        Provider p = PROVIDERS.get(village.civ());
        if (p == null || village.neutral()) {
            return Optional.empty();
        }
        // 没有规划（旧式村子）也有神庙：0 级，祭坛在钟的位置
        VillagePlans.Plan plan = VillagePlans.get(level.getServer()).byVillage(village.id()).orElse(null);
        VillageEconomy.State state = VillageEconomy.get(level.getServer()).state(village.id());
        Optional<VillageTemple> t = p.resolve(level, village, plan, state);
        GlobalPos forced = FORCED_ALTAR.get(village.id());
        if (forced != null && t.isPresent()) {
            VillageTemple x = t.get();
            t = Optional.of(new VillageTemple(x.village(), x.civ(), forced, forced.pos(), x.level(), x.tierName(),
                    x.tierCoefficient(), x.integrity(), x.slotLimit(), prayerSlots(level, forced.pos(), x.slotLimit())));
        }
        return t;
    }

    /** 神庙簿（村庄运行数据里的那一份）。 */
    public static TempleBook book(MinecraftServer server, UUID village) {
        return VillageEconomy.get(server).state(village).temple;
    }

    /** 这个村子有没有供奉者（只读，不新建村庄运行数据）。 */
    public static boolean hasDevotees(MinecraftServer server, UUID village) {
        return VillageEconomy.get(server).peek(village).map(s -> !s.temple.devotees().isEmpty()).orElse(false);
    }

    public static void changed(MinecraftServer server) {
        VillageEconomy.get(server).setDirty();
    }

    /** 这块祭坛是不是某个村子的神庙祭坛。 */
    public static Optional<VillageTemple> byAltar(MinecraftServer server, GlobalPos altar) {
        for (VillageRegistry.Village v : VillageRegistry.get(server).in(altar.dimension())) {
            Optional<VillageTemple> t = of(server, v);
            if (t.isPresent() && t.get().altar().equals(altar)) {
                return t;
            }
        }
        return Optional.empty();
    }

    /** 某玩家供奉的全部村子。 */
    public static List<UUID> devotedVillages(MinecraftServer server, UUID player) {
        List<UUID> out = new ArrayList<>();
        for (VillageEconomy.State s : VillageEconomy.get(server).all()) {
            if (s.temple.isDevotee(player)) {
                out.add(s.village);
            }
        }
        return out;
    }

    /** 某玩家供奉的、某文明村子的神庙祭坛（取代原来「玩家自己的神坛」的位置用途：召唤物复活点、神力距离）。 */
    public static List<GlobalPos> devotedAltars(MinecraftServer server, UUID player, Civilization civ) {
        List<GlobalPos> out = new ArrayList<>();
        for (UUID v : devotedVillages(server, player)) {
            VillageRegistry.get(server).byId(v).filter(x -> x.civ().equals(civ.id()))
                    .flatMap(x -> of(server, x)).ifPresent(t -> out.add(t.altar()));
        }
        return out;
    }

    /** 某玩家今天在所有供奉的村子合计的信徒数。 */
    public static int believersToday(MinecraftServer server, UUID player, long today) {
        int n = 0;
        for (VillageEconomy.State s : VillageEconomy.get(server).all()) {
            if (s.temple.isDevotee(player)) {
                n += s.temple.believersToday(today);
            }
        }
        return n;
    }

    /** 把玩家加为这个村子的供奉者；返回是否新加入。新加入时按这里的神庙等级补发进度事件。 */
    public static boolean addDevotee(MinecraftServer server, UUID village, UUID player, long day, TempleBook.Via via) {
        boolean added = book(server, village).addDevotee(player, day, via);
        if (added) {
            changed(server);
            of(server, village).ifPresent(t -> fireLevel(server, t, player));
        }
        return added;
    }

    /** 神庙等级的进度事件（「柱廊之间」：供奉的村子建成中神庙，GA-13 D2）：发给这位在线的玩家。 */
    static void fireLevel(MinecraftServer server, VillageTemple t, UUID player) {
        net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(player);
        if (p != null && t.level() > 0) {
            Civilization.byId(t.civ()).ifPresent(c -> io.github.happysnappingturtle.mythicage.advancement.MythEvents.fire(p,
                    io.github.happysnappingturtle.mythicage.advancement.MythEvents.TEMPLE_LEVEL, c, t.level()));
        }
    }

    /** 建筑落成后：作废缓存；神庙等级变了就通知在线的供奉者。 */
    public static void afterConstruction(MinecraftServer server, UUID village) {
        int before = Optional.ofNullable(CACHE.get(village)).flatMap(Cached::temple).map(VillageTemple::level).orElse(-1);
        invalidate(village);
        of(server, village).filter(t -> t.level() != before).ifPresent(t -> {
            for (UUID d : book(server, village).devotees()) {
                fireLevel(server, t, d);
            }
        });
    }

    /**
     * 祭坛周围的祈祷位：水平距离 1–4、高差 ≤1、脚下实心、身体两格空，并且从祭坛旁边一格起能一格一格走到（相邻格高差 ≤1）；
     * 再按 {@code TempleGeometry} 的选位规则选。墙后、隔着空当的格子不算。
     */
    public static List<BlockPos> prayerSlots(ServerLevel level, BlockPos altar, int limit) {
        java.util.Map<Long, int[]> cand = new java.util.HashMap<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = altar.getY() - 1; y <= altar.getY() + 1; y++) {
            for (int z = altar.getZ() - 4; z <= altar.getZ() + 4; z++) {
                for (int x = altar.getX() - 4; x <= altar.getX() + 4; x++) {
                    int d = Math.max(Math.abs(x - altar.getX()), Math.abs(z - altar.getZ()));
                    if (d < 1 || !level.isLoaded(m.set(x, y, z))) {
                        continue;
                    }
                    if (standable(level, m)) {
                        cand.put(BlockPos.asLong(x, y, z), new int[]{x, y, z});
                    }
                }
            }
        }
        // 从紧挨祭坛的一圈出发，按水平四邻（高差 ≤1）连通
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int[] c : cand.values()) {
            if (Math.max(Math.abs(c[0] - altar.getX()), Math.abs(c[2] - altar.getZ())) == 1) {
                queue.add(c);
                seen.add(BlockPos.asLong(c[0], c[1], c[2]));
            }
        }
        List<int[]> reachable = new ArrayList<>();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            reachable.add(c);
            for (int[] dxz : dirs) {
                for (int dy = -1; dy <= 1; dy++) {
                    long k = BlockPos.asLong(c[0] + dxz[0], c[1] + dy, c[2] + dxz[1]);
                    int[] n = cand.get(k);
                    if (n != null && seen.add(k)) {
                        queue.add(n);
                    }
                }
            }
        }
        List<BlockPos> out = new ArrayList<>();
        for (int[] c : io.github.happysnappingturtle.mythicage.core.greek.temple.TempleGeometry.pickSlots(reachable,
                altar.getX(), altar.getY(), altar.getZ(), limit)) {
            out.add(new BlockPos(c[0], c[1], c[2]));
        }
        return out;
    }

    static boolean standable(ServerLevel level, BlockPos p) {
        BlockPos below = p.below();
        return level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)
                && level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && level.getFluidState(p).isEmpty() && level.getFluidState(p.above()).isEmpty();
    }
}
