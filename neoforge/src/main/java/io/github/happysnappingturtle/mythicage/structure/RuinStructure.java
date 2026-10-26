package io.github.happysnappingturtle.mythicage.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.happysnappingturtle.mythicage.core.structure.SiteRules;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
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
 * 单模板遗迹（T22 §4.5，D8）：废弃神殿；以后其他文明的地表遗迹也用它。
 * <ul>
 *   <li>4 向随机旋转；在区块中部取最多 {@value #CANDIDATES} 个候选位置（有界），每个在占地上采 3 × 3 个高度，起伏 ≤ {@code max_relief}
 *       且中心不在水里的才要，取起伏最小的；都不行这个区块就不生成；</li>
 *   <li>片段：模板（轻整地：补土、削山、外圈 1:1 缓坡，不砌墙）+ 元数据件（结构种类与旋转后的锚点）；</li>
 *   <li>群系、间距、离村庄多远由结构 JSON 与结构集决定。</li>
 * </ul>
 */
public final class RuinStructure extends Structure {
    public static final int CANDIDATES = 5;
    private static final int[][] OFFSETS = {{0, 0}, {-4, -4}, {4, -4}, {-4, 4}, {4, 4}};

    public static final MapCodec<RuinStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("spec").forGetter(s -> s.spec),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind),
            Codec.INT.optionalFieldOf("max_relief", 4).forGetter(s -> s.maxRelief)
    ).apply(i, RuinStructure::new));

    private final String spec;
    private final String kind;
    private final int maxRelief;

    public RuinStructure(StructureSettings settings, String spec, String kind, int maxRelief) {
        super(settings);
        this.spec = spec;
        this.kind = kind;
        this.maxRelief = maxRelief;
    }

    @Override
    public StructureType<?> type() {
        return MythStructures.RUIN.get();
    }

    /** 一个合格的落点：旋转后包围盒的最小角（x、z）、地面上第一层的高度、起伏。 */
    record Site(int minX, int minZ, int floor, int relief) {
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        Optional<BuildingSpec> s = StructureSpecs.get(spec);
        if (s.isEmpty()) {
            return Optional.empty();
        }
        BuildingSpec b = s.get();
        Turn turn = Turn.values()[ctx.random().nextInt(4)];
        int[] size = TemplateTransform.rotatedSize(b.sizeX(), b.sizeZ(), turn);
        Site best = null;
        for (int[] off : OFFSETS) {
            int minX = ctx.chunkPos().getMiddleBlockX() + off[0] - size[0] / 2;
            int minZ = ctx.chunkPos().getMiddleBlockZ() + off[1] - size[1] / 2;
            Optional<Site> site = sample(ctx, minX, minZ, size[0], size[1], maxRelief);
            if (site.isPresent() && (best == null || site.get().relief() < best.relief())) {
                best = site.get();
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        Site site = best;
        BlockPos at = new BlockPos(site.minX() + size[0] / 2, site.floor(), site.minZ() + size[1] / 2);
        return Optional.of(new GenerationStub(at, builder -> addPieces(builder, ctx.structureTemplateManager(), b, turn, site)));
    }

    /** 占地上 3 × 3 采样（地表高度、中心是否在水里），交给 {@link SiteRules}。 */
    static Optional<Site> sample(GenerationContext ctx, int minX, int minZ, int w, int d, int maxRelief) {
        int[] hs = new int[9];
        boolean wet = false;
        int n = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int x = minX + i * (w - 1) / 2;
                int z = minZ + j * (d - 1) / 2;
                int top = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(),
                        ctx.randomState());
                if (i == 1 && j == 1) {
                    wet = SiteRules.wet(top, ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                            ctx.heightAccessor(), ctx.randomState()));
                }
                hs[n++] = top;
            }
        }
        OptionalInt floor = SiteRules.floor(hs, wet, maxRelief);
        return floor.isPresent() ? Optional.of(new Site(minX, minZ, floor.getAsInt(), SiteRules.relief(hs))) : Optional.empty();
    }

    /** 只供 GameTest：在指定位置直接摆一座（不选址），返回结构起点。 */
    public static net.minecraft.world.level.levelgen.structure.StructureStart startForTest(
            net.minecraft.server.level.ServerLevel level, Structure structure, Turn turn, int minX, int floor, int minZ) {
        RuinStructure r = (RuinStructure) structure;
        BuildingSpec b = StructureSpecs.get(r.spec).orElseThrow();
        StructurePiecesBuilder builder = new StructurePiecesBuilder();
        r.addPieces(builder, level.getStructureManager(), b, turn, new Site(minX, minZ, floor, 0));
        return new net.minecraft.world.level.levelgen.structure.StructureStart(structure,
                new net.minecraft.world.level.ChunkPos(new BlockPos(minX, floor, minZ)), 0, builder.build());
    }

    void addPieces(StructurePiecesBuilder builder,
                   net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager manager, BuildingSpec b,
                   Turn turn, Site site) {
        Pos origin = TemplateTransform.originFor(new Pos(site.minX(), site.floor() - b.floorY(), site.minZ()), b.sizeX(),
                b.sizeZ(), turn);
        builder.addPiece(new TemplatePiece(manager, b.template(),
                new BlockPos(origin.x(), origin.y(), origin.z()), rotation(turn), b.floorY(), TerrainTemplatePiece.Terrain.LIGHT));
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

    static Rotation rotation(Turn t) {
        return switch (t) {
            case R0 -> Rotation.NONE;
            case R90 -> Rotation.CLOCKWISE_90;
            case R180 -> Rotation.CLOCKWISE_180;
            case R270 -> Rotation.COUNTERCLOCKWISE_90;
        };
    }
}
