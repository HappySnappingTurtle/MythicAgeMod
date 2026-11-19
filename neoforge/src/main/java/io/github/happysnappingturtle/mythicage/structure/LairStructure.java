package io.github.happysnappingturtle.mythicage.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.happysnappingturtle.mythicage.core.lair.LairSiteRules;
import io.github.happysnappingturtle.mythicage.core.structure.SiteRules;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 精英巢穴（五文明通用，T25a §5.2）：嵌进坡面的洞。模板约定洞口朝 +Z（锚点 {@code entrance} 在洞口那一排），洞底在 z 小的一端。
 * <ul>
 *   <li>4 个朝向 × 区块中部 {@value #CANDIDATE_OFFSETS} 个位置，每个沿洞的纵轴采三排地表高度（洞口、洞身中段、洞底，各 3 点），
 *       交给 core {@link LairSiteRules}：洞口平、洞身与洞底埋在山里才合格；取「洞底比地面高得最多」的；</li>
 *   <li>片段：{@link LairPiece}（岩壳遇到实心原地形不放，洞底与平台下面补到实地）+ 元数据件（种类与锚点）；</li>
 *   <li>群系、间距、离村庄多远由结构 JSON 与结构集决定。</li>
 * </ul>
 */
public final class LairStructure extends Structure {
    public static final int CANDIDATE_OFFSETS = 25;
    /** 区块中部 5 × 5 个候选（步长 6 格）；群系先查过，只有合格群系的区块才付这份采样。 */
    private static final int[][] OFFSETS = offsets();

    private static int[][] offsets() {
        int[][] o = new int[25][];
        int n = 0;
        for (int dx = -12; dx <= 12; dx += 6) {
            for (int dz = -12; dz <= 12; dz += 6) {
                o[n++] = new int[]{dx, dz};
            }
        }
        return o;
    }
    /** 洞身中段、洞底两排的采样离中轴多远（格）。 */
    private static final int LATERAL = 3;

    public static final MapCodec<LairStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("spec").forGetter(s -> s.spec),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind),
            Codec.INT.optionalFieldOf("cave_height", 6).forGetter(s -> s.caveHeight)
    ).apply(i, LairStructure::new));

    private final String spec;
    private final String kind;
    private final int caveHeight;

    public LairStructure(StructureSettings settings, String spec, String kind, int caveHeight) {
        super(settings);
        this.spec = spec;
        this.kind = kind;
        this.caveHeight = caveHeight;
    }

    @Override
    public StructureType<?> type() {
        return MythStructures.LAIR.get();
    }

    record Site(Turn turn, int minX, int minZ, int floor, int score) {
    }

    /** 模板里洞的纵轴：中轴 x、洞口 z、洞底 z（模板相对坐标）。 */
    record Axis(int cx, int front, int back) {
        static Axis of(BuildingSpec b) {
            List<Pos> e = b.anchors().getOrDefault("entrance", List.of());
            if (e.isEmpty()) {
                return new Axis(b.sizeX() / 2, b.sizeZ() - 1, 2);
            }
            int sx = 0;
            int front = Integer.MIN_VALUE;
            for (Pos p : e) {
                sx += p.x();
                front = Math.max(front, p.z());
            }
            return new Axis(Math.round(sx / (float) e.size()), front, 2);
        }
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        Optional<BuildingSpec> s = StructureSpecs.get(spec);
        if (s.isEmpty()) {
            return Optional.empty();
        }
        BuildingSpec b = s.get();
        // 先在区块中心查一次群系（便宜）：巢穴的群系都少见，不合格的区块不必再采几十次地表高度
        int cx = ctx.chunkPos().getMiddleBlockX();
        int cz = ctx.chunkPos().getMiddleBlockZ();
        int cy = ctx.chunkGenerator().getSeaLevel() + 8;
        if (!ctx.validBiome().test(ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(cy),
                QuartPos.fromBlock(cz), ctx.randomState().sampler()))) {
            return Optional.empty();
        }
        Axis axis = Axis.of(b);
        int first = ctx.random().nextInt(4);
        Site best = null;
        for (int k = 0; k < 4; k++) {
            Turn turn = Turn.values()[(first + k) % 4];
            int[] size = TemplateTransform.rotatedSize(b.sizeX(), b.sizeZ(), turn);
            for (int[] off : OFFSETS) {
                int minX = ctx.chunkPos().getMiddleBlockX() + off[0] - size[0] / 2;
                int minZ = ctx.chunkPos().getMiddleBlockZ() + off[1] - size[1] / 2;
                Optional<Site> site = sample(ctx, b, axis, turn, minX, minZ);
                if (site.isPresent() && (best == null || site.get().score() > best.score())) {
                    best = site.get();
                }
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        Site site = best;
        // 结构起点放在洞口外那一格：原版按起点的群系做最后一道检查
        Pos o = TemplateTransform.originFor(new Pos(site.minX(), 0, site.minZ()), b.sizeX(), b.sizeZ(), site.turn());
        Pos mouth = TemplateTransform.toWorld(o, new Pos(axis.cx(), 0, axis.front() + 1), site.turn());
        BlockPos at = new BlockPos(mouth.x(), site.floor(), mouth.z());
        return Optional.of(new GenerationStub(at, builder -> addPieces(builder, ctx.structureTemplateManager(), b, site)));
    }

    /** 按洞口 → 洞底 → 中段的顺序采样，哪一步不合格就不再采后面的（每排 3 次高度查询，世界生成里最贵的一步）。 */
    private Optional<Site> sample(GenerationContext ctx, BuildingSpec b, Axis axis, Turn turn, int minX, int minZ) {
        Pos origin = TemplateTransform.originFor(new Pos(minX, 0, minZ), b.sizeX(), b.sizeZ(), turn);
        int[] front = new int[3];
        for (int i = 0; i < 3; i++) {
            front[i] = surface(ctx, origin, turn, axis.cx() + (i - 1), axis.front() + 1);
        }
        Pos fc = TemplateTransform.toWorld(origin, new Pos(axis.cx(), 0, axis.front() + 1), turn);
        boolean wet = SiteRules.wet(front[1], ctx.chunkGenerator().getFirstFreeHeight(fc.x(), fc.z(), Heightmap.Types.OCEAN_FLOOR_WG,
                ctx.heightAccessor(), ctx.randomState()));
        OptionalInt floor = LairSiteRules.frontFloor(front, wet, ctx.chunkGenerator().getSeaLevel());
        if (floor.isEmpty()) {
            return Optional.empty();
        }
        int f = floor.getAsInt();
        int[] back = new int[3];
        for (int i = 0; i < 3; i++) {
            back[i] = surface(ctx, origin, turn, axis.cx() + (i - 1) * LATERAL, axis.back());
            if (back[i] < f + caveHeight + LairSiteRules.ROOF_COVER) {
                return Optional.empty();
            }
        }
        int mid = (axis.front() + axis.back()) / 2;
        int[] middle = new int[3];
        for (int i = 0; i < 3; i++) {
            middle[i] = surface(ctx, origin, turn, axis.cx() + (i - 1) * LATERAL, mid);
        }
        if (!LairSiteRules.midOk(middle, f, caveHeight)) {
            return Optional.empty();
        }
        return Optional.of(new Site(turn, minX, minZ, f, LairSiteRules.score(back, f)));
    }

    private static int surface(GenerationContext ctx, Pos origin, Turn turn, int lx, int lz) {
        Pos w = TemplateTransform.toWorld(origin, new Pos(lx, 0, lz), turn);
        return ctx.chunkGenerator().getFirstFreeHeight(w.x(), w.z(), Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(),
                ctx.randomState());
    }

    /** 只供 GameTest：在指定位置直接摆一座（不选址），返回结构起点。 */
    public static net.minecraft.world.level.levelgen.structure.StructureStart startForTest(
            net.minecraft.server.level.ServerLevel level, Structure structure, Turn turn, int minX, int floor, int minZ) {
        LairStructure l = (LairStructure) structure;
        BuildingSpec b = StructureSpecs.get(l.spec).orElseThrow();
        StructurePiecesBuilder builder = new StructurePiecesBuilder();
        l.addPieces(builder, level.getStructureManager(), b, new Site(turn, minX, minZ, floor, 0));
        return new net.minecraft.world.level.levelgen.structure.StructureStart(structure,
                new net.minecraft.world.level.ChunkPos(new BlockPos(minX, floor, minZ)), 0, builder.build());
    }

    void addPieces(StructurePiecesBuilder builder,
                   net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager manager, BuildingSpec b,
                   Site site) {
        Turn turn = site.turn();
        Pos origin = TemplateTransform.originFor(new Pos(site.minX(), site.floor() - b.floorY(), site.minZ()), b.sizeX(),
                b.sizeZ(), turn);
        builder.addPiece(new LairPiece(manager, b.template(), new BlockPos(origin.x(), origin.y(), origin.z()),
                RuinStructure.rotation(turn), b.floorY()));
        Map<String, List<BlockPos>> anchors = new LinkedHashMap<>();
        b.anchors().forEach((name, pts) -> {
            List<BlockPos> out = new ArrayList<>();
            for (Pos p : pts) {
                Pos w = TemplateTransform.toWorld(origin, p, turn);
                out.add(new BlockPos(w.x(), w.y(), w.z()));
            }
            anchors.put(name, List.copyOf(out));
        });
        BlockPos info = anchors.getOrDefault("entrance", List.of(new BlockPos(origin.x(), origin.y(), origin.z()))).get(0);
        builder.addPiece(new StructureInfoPiece(new StructureInfoPiece.Info(kind, anchors), info));
    }
}
