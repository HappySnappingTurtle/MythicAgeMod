package io.github.happysnappingturtle.mythicage.village.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.village.gen.BiomeView;
import io.github.happysnappingturtle.mythicage.core.village.gen.CivRule;
import io.github.happysnappingturtle.mythicage.core.village.gen.CivSelector;
import io.github.happysnappingturtle.mythicage.core.village.gen.FixedPlanner;
import io.github.happysnappingturtle.mythicage.core.village.gen.HillsidePlanner;
import io.github.happysnappingturtle.mythicage.core.village.gen.PatronPicker;
import io.github.happysnappingturtle.mythicage.core.village.gen.SiteScorer;
import io.github.happysnappingturtle.mythicage.core.village.gen.TerrainSampler;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文明村庄结构（V0 §2、T13b §2.3）：文明选择 → 选址评分 → 抽主神 → 规划 → 校验 → 片段。
 * 阶段 ① 只有固定布局（{@code fixed}）；阶段 ② 加入 {@code hillside}。
 */
public final class CivVillageStructure extends Structure {
    private static final Codec<CivRule> RULE = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("civ").forGetter(CivRule::civ),
            Codec.STRING.optionalFieldOf("region", "").forGetter(CivRule::region),
            Codec.STRING.listOf().optionalFieldOf("not_region", List.of()).forGetter(CivRule::notRegions),
            Codec.INT.optionalFieldOf("near_river", 0).forGetter(CivRule::nearRiver),
            Codec.DOUBLE.optionalFieldOf("chance", 1.0).forGetter(CivRule::chance),
            Codec.STRING.optionalFieldOf("chance_key", "").forGetter(CivRule::chanceKey),
            Codec.STRING.optionalFieldOf("requires_template", "").forGetter(CivRule::requiresTemplate)
    ).apply(i, CivRule::new));

    private static final Codec<SiteScorer.Config> SITE = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("flatness", SiteScorer.Config.DEFAULT.flatness()).forGetter(SiteScorer.Config::flatness),
            Codec.DOUBLE.optionalFieldOf("high_ground", SiteScorer.Config.DEFAULT.highGround()).forGetter(SiteScorer.Config::highGround),
            Codec.DOUBLE.optionalFieldOf("min", SiteScorer.Config.DEFAULT.minScore()).forGetter(SiteScorer.Config::minScore),
            Codec.INT.optionalFieldOf("max_relief", SiteScorer.Config.DEFAULT.maxRelief()).forGetter(SiteScorer.Config::maxRelief)
    ).apply(i, SiteScorer.Config::new));

    public static final MapCodec<CivVillageStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            RULE.listOf().fieldOf("civ_rules").forGetter(s -> s.rules),
            SITE.optionalFieldOf("site", SiteScorer.Config.DEFAULT).forGetter(s -> s.site),
            Codec.STRING.optionalFieldOf("layout", FixedPlanner.ID).forGetter(s -> s.layout),
            Codec.STRING.optionalFieldOf("template_set", "v1").forGetter(s -> s.templateSet)
    ).apply(i, CivVillageStructure::new));

    private final List<CivRule> rules;
    private final SiteScorer.Config site;
    private final String layout;
    private final String templateSet;

    public CivVillageStructure(StructureSettings settings, List<CivRule> rules, SiteScorer.Config site, String layout,
                               String templateSet) {
        super(settings);
        this.rules = List.copyOf(rules);
        this.site = site;
        this.layout = layout;
        this.templateSet = templateSet;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        BiomeView biomes = WorldgenViews.biomes(context.chunkGenerator(), context.randomState());
        TerrainSampler terrain = WorldgenViews.terrain(context.chunkGenerator(), context.heightAccessor(), context.randomState(), x, z);
        Optional<VillagePlan> plan = plan(x, z, biomes, terrain, new ArrayList<>());
        return plan.map(p -> new GenerationStub(new BlockPos(p.center().x(), p.center().y(), p.center().z()),
                builder -> addPieces(builder, p, context.structureTemplateManager())));
    }

    /** 片段顺序：预留地块（整平）→ 建筑 → 道路 → 信息片段。 */
    void addPieces(net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder builder, VillagePlan p,
                   net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager manager) {
        for (VillagePlan.Plot plot : p.plots()) {
            builder.addPiece(new VillagePlotPiece(plot.box()));
        }
        Map<String, io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec> specs = VillageBuildings.all();
        for (VillagePlan.Placement b : p.buildings()) {
            var spec = specs.get(b.building());
            boolean shape = spec == null || !(spec.category().equals("terrain") || b.building().endsWith("_harbor"));
            builder.addPiece(new VillageBuildingPiece(manager, b.template(),
                    new BlockPos(b.origin().x(), b.origin().y(), b.origin().z()), rotation(b.turn()),
                    spec == null ? 0 : spec.floorY(), shape,
                    spec == null || !shape ? null : io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.door(spec, b.origin(), b.turn())));
        }
        for (VillagePlan.Path path : p.paths()) {
            builder.addPiece(new VillagePathPiece(path.width(), path.points()));
        }
        builder.addPiece(new VillageInfoPiece(p, templateSet));
    }

    /** 不碰世界的规划（GameTest、密度工具也用）。失败原因写进 problems。 */
    public Optional<VillagePlan> plan(int x, int z, BiomeView biomes, TerrainSampler terrain, List<String> problems) {
        VillagesConfig cfg = ModConfigs.villages().current();
        Optional<String> civ = CivSelector.select(rules, biomes, x, z, VillageBuildings.civsWithTemplates(),
                Map.of("chineseRiverChance", cfg.chineseRiverChance()));
        if (civ.isEmpty()) {
            problems.add("没有文明");
            return Optional.empty();
        }
        Optional<SiteScorer.Site> s = SiteScorer.best(x, z, terrain, site);
        if (s.isEmpty()) {
            problems.add("选址：没有合格的中心");
            return Optional.empty();
        }
        int cx = s.get().x();
        int cz = s.get().z();
        String patron = PatronPicker.pick(cfg, civ.get(), biomes, cx, s.get().y(), cz);
        if (HillsidePlanner.ID.equals(layout)) {
            return HillsidePlanner.plan(cx, cz, terrain, VillageBuildings.all(), civ.get(), patron, problems);
        }
        if (!FixedPlanner.ID.equals(layout)) {
            MythicAge.LOGGER.warn("未知的村庄布局 {}，按 fixed 处理", layout);
        }
        return FixedPlanner.planAny(List.of(new int[]{cx, cz}), terrain, VillageBuildings.all(), civ.get(), patron, problems);
    }

    public String templateSet() {
        return templateSet;
    }

    static Rotation rotation(Turn t) {
        return switch (t) {
            case R0 -> Rotation.NONE;
            case R90 -> Rotation.CLOCKWISE_90;
            case R180 -> Rotation.CLOCKWISE_180;
            case R270 -> Rotation.COUNTERCLOCKWISE_90;
        };
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CIV_VILLAGE.get();
    }
}
