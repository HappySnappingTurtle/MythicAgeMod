package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageZones;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 村庄地标索引（T20 设计文档 §7）：各文明的独有机制要找「村里某种方块」（圣火盆、纪念碑、战号角、箭楼、神谕塔）。
 * <ul>
 *   <li>类别用方块标签声明：{@code #mythic_age:village_landmark/<类别>}（希腊把基础圣火盆放进 {@code beacon}）；</li>
 *   <li>第一次查询某村时扫描它的已建建筑盒（生成的与施工建成的），之后建成新建筑时重扫；方块放置 / 破坏在已建范围里时增删；</li>
 *   <li>只存位置，用时再读方块状态（圣火盆此刻点没点着）；不存档，重启后按需重建。</li>
 * </ul>
 */
public final class VillageLandmarks {
    private record Index(int constructed, Map<String, Set<BlockPos>> byKind) {
    }

    private static final Set<String> KINDS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Index> INDEX = new ConcurrentHashMap<>();

    private VillageLandmarks() {
    }

    /** 声明一种地标类别（文明接入时调用）。 */
    public static void register(String kind) {
        KINDS.add(kind);
    }

    public static TagKey<Block> tag(String kind) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "village_landmark/" + kind));
    }

    /** 本村某类地标的位置（只看已加载的格子）。 */
    public static List<BlockPos> find(ServerLevel level, UUID village, String kind) {
        Index idx = index(level, village);
        return new ArrayList<>(idx.byKind().getOrDefault(kind, Set.of()));
    }

    private static Index index(ServerLevel level, UUID village) {
        int built = VillageEconomy.get(level.getServer()).peek(village).map(s -> s.constructed.size()).orElse(0);
        Index idx = INDEX.get(village);
        if (idx == null || idx.constructed() != built) {
            idx = build(level, village, built);
            INDEX.put(village, idx);
        }
        return idx;
    }

    private static Index build(ServerLevel level, UUID village, int built) {
        Map<String, Set<BlockPos>> out = new LinkedHashMap<>();
        List<Box> boxes = new ArrayList<>();
        VillagePlans.get(level.getServer()).byVillage(village).ifPresent(p -> boxes.addAll(p.info().built()));
        VillageEconomy.get(level.getServer()).peek(village).ifPresent(s -> s.constructed.forEach(b -> boxes.add(b.box())));
        for (Box b : boxes) {
            for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
                if (!level.isLoaded(p)) {
                    continue;
                }
                BlockState st = level.getBlockState(p);
                for (String k : KINDS) {
                    if (st.is(tag(k))) {
                        out.computeIfAbsent(k, x -> new LinkedHashSet<>()).add(p.immutable());
                    }
                }
            }
        }
        return new Index(built, out);
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        update(e.getLevel(), e.getPos(), e.getPlacedBlock(), true);
    }

    public static void onBreak(BlockEvent.BreakEvent e) {
        update(e.getLevel(), e.getPos(), e.getState(), false);
    }

    private static void update(LevelAccessor la, BlockPos pos, BlockState st, boolean add) {
        if (!(la instanceof ServerLevel level)) {
            return;
        }
        for (String k : KINDS) {
            if (!st.is(tag(k))) {
                continue;
            }
            VillageZones.Hit h = VillageZones.zoneAt(level, pos);
            if (!h.builtKnown() || h.village().isEmpty()) {
                return;
            }
            Index idx = INDEX.get(h.village().get().id());
            if (idx != null) {
                Set<BlockPos> set = idx.byKind().computeIfAbsent(k, x -> new LinkedHashSet<>());
                if (add) {
                    set.add(pos.immutable());
                } else {
                    set.remove(pos);
                }
            }
        }
    }

    /** 方块被代码改掉（施工、测试）时手动登记。 */
    public static void noteForTest(ServerLevel level, BlockPos pos, boolean add) {
        update(level, pos, level.getBlockState(pos), add);
    }

    public static void invalidate(UUID village) {
        INDEX.remove(village);
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        INDEX.clear();
    }
}
