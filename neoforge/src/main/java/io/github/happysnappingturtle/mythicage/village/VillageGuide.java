package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.village.GuideCandidates;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 村庄指引（T13b 设计文档 §7）：在后台按距离逐个确认结构集的候选起点，找到最近一座玩家没进过的同文明村庄，
 * 回到主线程定位村庄中心、做一张标着该村的地图。每个玩家同时只有一次搜索。
 */
public final class VillageGuide {
    /** 搜索半径（区块），同原版探险家地图。 */
    public static final int RADIUS_CHUNKS = 100;
    /** 地图图标：白色神庙轮廓（8 × 8，{@code textures/map/decorations/greek_village.png}）。 */
    public static final DeferredHolder<MapDecorationType, MapDecorationType> GREEK_VILLAGE_ICON =
            ModRegistries.MAP_DECORATIONS.register("greek_village", () -> new MapDecorationType(
                    ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "greek_village"), true, 0xE8E2D2, true, false));

    private static final Set<UUID> RUNNING = ConcurrentHashMap.newKeySet();

    /** 找到的村庄：中心、主神（拿不到时为空）。 */
    public record Found(BlockPos center, String patron) {
    }

    private VillageGuide() {
    }

    public static void init() {
    }

    public static TagKey<Structure> tag(Civilization civ) {
        return TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID,
                "civ_village/" + civ.id()));
    }

    public static boolean running(ServerPlayer player) {
        return RUNNING.contains(player.getUUID());
    }

    /**
     * 开始搜索；已有搜索在跑时返回 false 且不启动。结果在主线程交给 {@code done}（空 = 找不到）。
     */
    public static boolean begin(ServerPlayer player, Civilization civ, Consumer<Optional<Found>> done) {
        if (!RUNNING.add(player.getUUID())) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        ChunkPos from = player.chunkPosition();
        Set<Long> visited = new HashSet<>(VillageVisits.get(player.server).visited(player.getUUID(), level.dimension()));
        List<Target> targets = targets(level, civ);
        CompletableFuture.supplyAsync(() -> search(level, from, targets, visited), Util.backgroundExecutor())
                .exceptionally(e -> {
                    MythicAge.LOGGER.warn("村庄指引搜索失败", e);
                    return Optional.empty();
                })
                .thenAcceptAsync(hit -> {
                    try {
                        done.accept(hit.flatMap(h -> locate(level, h)));
                    } finally {
                        RUNNING.remove(player.getUUID());
                    }
                }, player.server);
        return true;
    }

    /**
     * 管理用搜索（GA-3 快速验证）：不跳过去过的村庄、不占玩家的搜索名额。结果在主线程交给 {@code done}。
     */
    public static void searchAny(ServerLevel level, BlockPos from, Civilization civ, Consumer<Optional<Found>> done) {
        List<Target> targets = targets(level, civ);
        ChunkPos chunk = new ChunkPos(from);
        CompletableFuture.supplyAsync(() -> search(level, chunk, targets, Set.of()), Util.backgroundExecutor())
                .exceptionally(e -> {
                    MythicAge.LOGGER.warn("村庄搜索失败", e);
                    return Optional.empty();
                })
                .thenAcceptAsync(hit -> done.accept(hit.flatMap(h -> locate(level, h))), level.getServer());
    }

    /** 结构标签里每座结构与它的随机散布放置。 */
    record Target(Holder<Structure> structure, RandomSpreadStructurePlacement placement) {
    }

    record Hit(Holder<Structure> structure, ChunkPos start) {
    }

    private static List<Target> targets(ServerLevel level, Civilization civ) {
        List<Target> out = new ArrayList<>();
        Optional<HolderSet.Named<Structure>> set = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getTag(tag(civ));
        if (set.isEmpty()) {
            return out;
        }
        var state = level.getChunkSource().getGeneratorState();
        for (Holder<Structure> h : set.get()) {
            for (StructurePlacement p : state.getPlacementsForStructure(h)) {
                if (p instanceof RandomSpreadStructurePlacement r) {
                    out.add(new Target(h, r));
                }
            }
        }
        return out;
    }

    /** 后台线程：候选按距离排序后逐个确认（未生成的区块会跑一次村庄规划）。 */
    static Optional<Hit> search(ServerLevel level, ChunkPos from, List<Target> targets, Set<Long> visited) {
        var state = level.getChunkSource().getGeneratorState();
        long seed = state.getLevelSeed();
        record Cand(GuideCandidates.Cand c, Target t) {
        }
        List<Cand> all = new ArrayList<>();
        for (Target t : targets) {
            int spacing = t.placement().spacing();
            int[] r = GuideCandidates.regions(from.x, from.z, RADIUS_CHUNKS, spacing);
            List<int[]> starts = new ArrayList<>();
            for (int rx = r[0]; rx <= r[1]; rx++) {
                for (int rz = r[2]; rz <= r[3]; rz++) {
                    ChunkPos c = t.placement().getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                    if (t.placement().isStructureChunk(state, c.x, c.z)) {
                        starts.add(new int[]{c.x, c.z});
                    }
                }
            }
            for (GuideCandidates.Cand c : GuideCandidates.order(from.x, from.z, starts, visited, RADIUS_CHUNKS)) {
                all.add(new Cand(c, t));
            }
        }
        all.sort(java.util.Comparator.comparingLong(x -> x.c().distSq()));
        for (Cand c : all) {
            ChunkPos pos = new ChunkPos(c.c().chunkX(), c.c().chunkZ());
            StructureCheckResult r = level.structureManager().checkStructurePresence(pos, c.t().structure().value(),
                    c.t().placement(), false);
            if (r != StructureCheckResult.START_NOT_PRESENT) {
                return Optional.of(new Hit(c.t().structure(), pos));
            }
        }
        return Optional.empty();
    }

    /** 主线程：把起点区块载入到「结构起点」状态，从信息片段取村庄中心与主神。 */
    private static Optional<Found> locate(ServerLevel level, Hit hit) {
        ChunkAccess chunk = level.getChunk(hit.start().x, hit.start().z, ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk),
                hit.structure().value(), chunk);
        if (start == null || !start.isValid()) {
            return Optional.empty();
        }
        for (var piece : start.getPieces()) {
            if (piece instanceof VillageInfoPiece info) {
                Pos c = info.info().center();
                return Optional.of(new Found(new BlockPos(c.x(), c.y(), c.z()), info.info().patron()));
            }
        }
        return Optional.of(new Found(start.getBoundingBox().getCenter(), ""));
    }

    /** 兑换页上显示的样品（真正的地图在找到村庄后再做）。 */
    public static ItemStack displayStack(Civilization civ) {
        ItemStack s = new ItemStack(Items.FILLED_MAP);
        s.set(DataComponents.ITEM_NAME, Component.translatable("mythic_age.village_guide.offer"));
        return s;
    }

    /** 做一张以村庄为中心、标着村庄图标的地图（原版藏宝图写法：比例 1:4、预渲染群系）。 */
    public static ItemStack map(ServerLevel level, Civilization civ, BlockPos from, Found f) {
        ItemStack map = MapItem.create(level, f.center().getX(), f.center().getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, f.center(), "+", GREEK_VILLAGE_ICON);
        Component civName = Component.translatable("mythic_age.civ." + civ.id());
        map.set(DataComponents.ITEM_NAME, Component.translatable("mythic_age.village_guide.name", civName));
        int dx = f.center().getX() - from.getX();
        int dz = f.center().getZ() - from.getZ();
        int dist = (int) Math.round(Math.hypot(dx, dz) / 10.0) * 10;
        Component patron = f.patron().isEmpty() ? Component.literal("?")
                : Component.translatable("mythic_age.god." + f.patron());
        map.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("mythic_age.village_guide.lore", patron,
                dist, Component.translatable("mythic_age.direction." + direction(dx, dz))))));
        return map;
    }

    /** 8 方位（x 东、z 南）。 */
    static String direction(int dx, int dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        String[] d = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return d[(int) Math.floorMod(Math.round(a / 45.0), 8)];
    }
}
