package io.github.happysnappingturtle.mythicage.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.happysnappingturtle.mythicage.core.structure.MazePlanner;
import io.github.happysnappingturtle.mythicage.core.structure.MazePlanner.Cell;
import io.github.happysnappingturtle.mythicage.core.structure.MazePlanner.Dir;
import io.github.happysnappingturtle.mythicage.core.structure.MazePlanner.Room;
import io.github.happysnappingturtle.mythicage.core.structure.SiteRules;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.MythicAge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 地下迷宫（T22 §5，D2、D3、D9）：克诺索斯；以后的金字塔迷宫也用它（模块前缀不同）。
 * <ul>
 *   <li>网格由 {@link MazePlanner} 按结构种子规划；每格放一个模块模板（整块实心再挖空，外墙就是外壳）；</li>
 *   <li>中心大厅 4 片、宝库房、牛首门前室按固定格位；入口格南面接下行台阶（{@link DromosPiece}），一路升到地面；</li>
 *   <li>迷宫地面 = 台阶顶的地表 − {@code depth}（默认 24），夹在 −40 与海平面 − 8 之间；夹取后深度变了，台阶顶的位置也变，
 *       重新采样，差 {@value #DEPTH_TOLERANCE} 级以内就接受，最多 {@value #SITE_ITERATIONS} 次（有界）；</li>
 *   <li>地表残迹（模板，轻整地）的中庭接台阶出口；台阶顶 3 × 3 采样起伏 ≤ {@code max_relief}、不在水里才生成；</li>
 *   <li>选址候选：起点区块与周围 {@code search} 圈区块（默认 3 × 3，有界），按离起点的远近依次试，第一个合格的用；</li>
 *   <li>{@code flatten_fallback}（近出生点那座，§5.5）：都不合格时取不在水里、起伏最小的候选，地表取中位数，
 *       残迹照常轻整地（削掉压在上面的山、补坑、外圈削成缓坡）；全在水里就放弃并记日志；</li>
 *   <li>{@code preferred_biomes}：落点不在其中时记一条「降级」日志（结构本身的群系条件放宽到任意陆地）；</li>
 *   <li>不旋转（大厅入口固定朝南）。</li>
 * </ul>
 */
public final class LabyrinthStructure extends Structure {
    public static final int SITE_ITERATIONS = 3;
    /** 重新采样后级数差这么多以内就接受。 */
    public static final int DEPTH_TOLERANCE = 2;
    public static final int GRID = MazePlanner.SIZE * 8;

    public static final MapCodec<LabyrinthStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("prefix").forGetter(s -> s.prefix),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind),
            Codec.INT.optionalFieldOf("max_relief", 6).forGetter(s -> s.maxRelief),
            Codec.INT.optionalFieldOf("depth", 24).forGetter(s -> s.depth),
            Codec.intRange(0, 2).optionalFieldOf("search", 1).forGetter(s -> s.search),
            Codec.BOOL.optionalFieldOf("flatten_fallback", false).forGetter(s -> s.flattenFallback),
            RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("preferred_biomes")
                    .forGetter(s -> s.preferredBiomes)
    ).apply(i, LabyrinthStructure::new));

    /** 模块前缀，如 {@code greek:knossos}（模块 id = 前缀 + _corridor_5 等）。 */
    private final String prefix;
    private final String kind;
    private final int maxRelief;
    private final int depth;
    /** 选址时向外找几圈区块（1 = 3 × 3）。 */
    private final int search;
    private final boolean flattenFallback;
    private final Optional<HolderSet<Biome>> preferredBiomes;

    public LabyrinthStructure(StructureSettings settings, String prefix, String kind, int maxRelief, int depth, int search,
                              boolean flattenFallback, Optional<HolderSet<Biome>> preferredBiomes) {
        super(settings);
        this.prefix = prefix;
        this.kind = kind;
        this.maxRelief = maxRelief;
        this.depth = depth;
        this.search = search;
        this.flattenFallback = flattenFallback;
        this.preferredBiomes = preferredBiomes;
    }

    @Override
    public StructureType<?> type() {
        return MythStructures.LABYRINTH.get();
    }

    /** 选好的落点：网格最小角（x、z）、迷宫地面高度、台阶级数。 */
    public record Site(int gx, int gz, int floor, int depth) {
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (StructureSpecs.get(prefix + "_surface").isEmpty()) {
            return Optional.empty();
        }
        Optional<Site> site = Optional.empty();
        Sample flattest = null;
        for (int[] off : candidates(search)) {
            int gx = ctx.chunkPos().getMiddleBlockX() + off[0] * 16 - GRID / 2;
            int gz = ctx.chunkPos().getMiddleBlockZ() + off[1] * 16 - GRID / 2;
            site = site(ctx, gx, gz);
            if (site.isPresent()) {
                break;
            }
            if (flattenFallback) {
                Sample sm = sample(ctx, gx, gz, depth);
                if (!sm.wet() && (flattest == null || SiteRules.relief(sm.heights()) < SiteRules.relief(flattest.heights()))) {
                    flattest = sm;
                }
            }
        }
        if (site.isEmpty() && flattest != null) {
            // 整地版：地表取中位数，残迹的轻整地把坡削平
            int surface = SiteRules.floor(flattest.heights(), false, Integer.MAX_VALUE).orElseThrow();
            int floor = SiteRules.mazeFloor(surface, depth, ctx.chunkGenerator().getSeaLevel());
            site = Optional.of(new Site(flattest.gx(), flattest.gz(), floor, surface - floor));
            MythicAge.LOGGER.info("迷宫 {}（区块 {}）周围没有起伏 ≤ {} 的地，取最平的一处（起伏 {}）整地", kind, ctx.chunkPos(),
                    maxRelief, SiteRules.relief(flattest.heights()));
        }
        if (site.isEmpty()) {
            if (flattenFallback) {
                MythicAge.LOGGER.warn("迷宫 {}（区块 {}）周围都在水里，放弃这一处", kind, ctx.chunkPos());
            }
            return Optional.empty();
        }
        long seed = ctx.random().nextLong();
        Site s = site.get();
        BlockPos top = new BlockPos(s.gx() + 3, s.floor() + s.depth(), s.gz() + GRID + s.depth());
        preferredBiomes.ifPresent(pref -> {
            Holder<Biome> biome = ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(top.getX()), QuartPos.fromBlock(top.getY()),
                    QuartPos.fromBlock(top.getZ()), ctx.randomState().sampler());
            if (!pref.contains(biome)) {
                MythicAge.LOGGER.info("迷宫 {} 降级：落在 {}（{}），不在偏好群系", kind, top,
                        biome.unwrapKey().map(k -> k.location().toString()).orElse("?"));
            }
        });
        return Optional.of(new GenerationStub(top,
                builder -> addPieces(builder, ctx.structureTemplateManager(), s, MazePlanner.plan(seed))));
    }

    /** 候选区块偏移：起点区块在前，再按离起点的远近（同距离按 z、x）排；共 (2r + 1)² 个。 */
    static List<int[]> candidates(int r) {
        List<int[]> out = new ArrayList<>();
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                out.add(new int[]{dx, dz});
            }
        }
        out.sort(java.util.Comparator.comparingInt(o -> Math.abs(o[0]) + Math.abs(o[1])));
        return out;
    }

    /** 台阶顶（地表）3 × 3 采样：地表高度与中心是否在水里。 */
    record Sample(int gx, int gz, int[] heights, boolean wet) {
    }

    static Sample sample(GenerationContext ctx, int gx, int gz, int d) {
        int topX = gx + 3;
        int topZ = gz + GRID + d;
        int[] hs = new int[9];
        boolean wet = false;
        int n = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int x = topX + i * 8;
                int z = topZ + j * 8;
                int top = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(),
                        ctx.randomState());
                if (i == 0 && j == 0) {
                    wet = SiteRules.wet(top, ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                            ctx.heightAccessor(), ctx.randomState()));
                }
                hs[n++] = top;
            }
        }
        return new Sample(gx, gz, hs, wet);
    }

    /** 台阶顶（地表）采样：夹取后深度变了就挪到新的台阶顶再采，最多 {@value #SITE_ITERATIONS} 次。 */
    Optional<Site> site(GenerationContext ctx, int gx, int gz) {
        int d = depth;
        for (int it = 0; it < SITE_ITERATIONS; it++) {
            Sample sm = sample(ctx, gx, gz, d);
            OptionalInt surface = SiteRules.floor(sm.heights(), sm.wet(), maxRelief);
            if (surface.isEmpty()) {
                return Optional.empty();
            }
            int floor = SiteRules.mazeFloor(surface.getAsInt(), depth, ctx.chunkGenerator().getSeaLevel());
            int nd = surface.getAsInt() - floor;
            // 台阶顶跟着级数挪了，新位置的地表差一两格无妨（地表残迹的轻整地会抹平）
            if (Math.abs(nd - d) <= DEPTH_TOLERANCE) {
                return Optional.of(new Site(gx, gz, floor, nd));
            }
            d = nd;
        }
        return Optional.empty();
    }

    /** 片段：模块、大厅、宝库房、前室、下行台阶、地表残迹、元数据件。 */
    public void addPieces(StructurePiecesBuilder builder, StructureTemplateManager manager, Site s, MazePlanner.Plan plan) {
        Map<String, List<BlockPos>> anchors = new LinkedHashMap<>();
        int y = s.floor() - 2; // 模块模板最下面是地板下的第二层
        for (Map.Entry<Cell, Room> e : plan.rooms().entrySet()) {
            Cell c = e.getKey();
            int bx = s.gx() + c.x() * 8;
            int bz = s.gz() + c.z() * 8;
            String id = switch (e.getValue()) {
                case CORRIDOR, ENTRANCE -> prefix + "_corridor_" + mask(plan.openings(c));
                case REST -> prefix + "_rest_" + mask(plan.openings(c));
                case COLONNADE -> prefix + "_colonnade_" + mask(plan.openings(c));
                case STORAGE -> prefix + "_storage_" + mask(plan.openings(c));
                case ANTECHAMBER -> c.equals(MazePlanner.ANTECHAMBER.get(0)) ? prefix + "_antechamber_" + anteMask(plan) : null;
            };
            if (id != null) {
                place(builder, manager, id, new BlockPos(bx, y, bz), anchors);
            }
        }
        place(builder, manager, prefix + "_hall_nw", new BlockPos(s.gx() + 16, y, s.gz() + 16), anchors);
        place(builder, manager, prefix + "_hall_ne", new BlockPos(s.gx() + 32, y, s.gz() + 16), anchors);
        place(builder, manager, prefix + "_hall_sw", new BlockPos(s.gx() + 16, y, s.gz() + 32), anchors);
        place(builder, manager, prefix + "_hall_se", new BlockPos(s.gx() + 32, y, s.gz() + 32), anchors);
        place(builder, manager, prefix + "_vault", new BlockPos(s.gx() + 24, y, s.gz() + 8), anchors);
        // 入口格中心：线团祭坛（T23）
        Cell entrance = MazePlanner.ENTRANCE;
        anchors.computeIfAbsent("thread", k -> new ArrayList<>()).add(new BlockPos(s.gx() + entrance.x() * 8 + 3, s.floor(),
                s.gz() + entrance.z() * 8 + 3));
        int dx = s.gx() + entrance.x() * 8 + 2;
        // 地表残迹：出口槽（模板 x 10..12、z 9..13）对准台阶最后 5 级
        BuildingSpec ruin = StructureSpecs.get(prefix + "_surface").orElseThrow();
        Pos cut = ruin.anchors().getOrDefault("cut", List.of(new Pos(10, 1, 9))).get(0);
        int surface = s.floor() + s.depth();
        BlockPos ruinOrigin = new BlockPos(dx - cut.x(), surface - ruin.floorY(), s.gz() + GRID + s.depth() - 5 - cut.z());
        builder.addPiece(new TemplatePiece(manager, ruin.template(), ruinOrigin, Rotation.NONE, ruin.floorY(),
                TerrainTemplatePiece.Terrain.LIGHT));
        ruin.anchors().forEach((name, pts) -> {
            if (!name.equals("cut")) {
                pts.forEach(p -> anchors.computeIfAbsent(name, k -> new ArrayList<>()).add(ruinOrigin.offset(p.x(), p.y(), p.z())));
            }
        });
        // 台阶放在残迹之后：残迹的轻整地会往出口槽里补土，台阶最后挖，把它清掉
        builder.addPiece(new DromosPiece(dx, s.gz() + GRID, s.floor(), s.depth(), "mythic_age:marble_ashlar_stairs",
                "mythic_age:plaster", "mythic_age:olive_planks", "minecraft:tuff"));
        Map<String, List<BlockPos>> frozen = new LinkedHashMap<>();
        anchors.forEach((k, v) -> frozen.put(k, List.copyOf(v)));
        BlockPos at = frozen.getOrDefault("entrance", List.of(ruinOrigin)).get(0);
        builder.addPiece(new StructureInfoPiece(new StructureInfoPiece.Info(kind, frozen), at));
    }

    private static void place(StructurePiecesBuilder builder, StructureTemplateManager manager, String id, BlockPos origin,
                              Map<String, List<BlockPos>> anchors) {
        Optional<BuildingSpec> spec = StructureSpecs.get(id);
        if (spec.isEmpty()) {
            return;
        }
        builder.addPiece(new TemplatePiece(manager, spec.get().template(), origin, Rotation.NONE, spec.get().floorY(),
                TerrainTemplatePiece.Terrain.NONE));
        spec.get().anchors().forEach((name, pts) -> pts.forEach(p ->
                anchors.computeIfAbsent(name, k -> new ArrayList<>()).add(origin.offset(p.x(), p.y(), p.z()))));
    }

    /** 开口掩码：N 1、E 2、S 4、W 8（同 tools/buildings/greek_knossos.py）。 */
    static int mask(java.util.Set<Dir> open) {
        int m = 0;
        for (Dir d : open) {
            m |= switch (d) {
                case N -> 1;
                case E -> 2;
                case S -> 4;
                case W -> 8;
            };
        }
        return m;
    }

    /** 前室的开口：左格西 1、右格东 2、左格南 4、右格南 8。 */
    static int anteMask(MazePlanner.Plan plan) {
        Cell left = MazePlanner.ANTECHAMBER.get(0);
        Cell right = MazePlanner.ANTECHAMBER.get(1);
        int m = 0;
        m |= plan.openings(left).contains(Dir.W) ? 1 : 0;
        m |= plan.openings(right).contains(Dir.E) ? 2 : 0;
        m |= plan.openings(left).contains(Dir.S) ? 4 : 0;
        m |= plan.openings(right).contains(Dir.S) ? 8 : 0;
        return m;
    }

    /** 只供 GameTest：在指定位置直接摆一座（不选址）。 */
    public net.minecraft.world.level.levelgen.structure.StructureStart startForTest(net.minecraft.server.level.ServerLevel level,
                                                                                   Site s, long seed) {
        StructurePiecesBuilder builder = new StructurePiecesBuilder();
        addPieces(builder, level.getStructureManager(), s, MazePlanner.plan(seed));
        return new net.minecraft.world.level.levelgen.structure.StructureStart(this,
                new net.minecraft.world.level.ChunkPos(new BlockPos(s.gx(), s.floor(), s.gz())), 0, builder.build());
    }
}
