package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.content.greek.ruin.GreekRuins;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.structure.NearSpawnRings;
import io.github.happysnappingturtle.mythicage.structure.StructureSpecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * T22 阶段 ④：克诺索斯的放置与正式方块（单元测试用例文档 TC-T22-75～80）。
 * 近出生点那一套用原版同心环：按几个世界种子各建一份主世界的结构状态（只含两套迷宫结构集，不算要塞），直接取环位置，
 * 不真的生成远处区块。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LabyrinthPlacementGameTests {
    private static final String BATCH = "labyrinth_placement";
    private static final String T = GameTestTemplates.EMPTY_3X3;
    /** 20261013：实机新世界，原版环位置落在水里（修之前近出生点那座生成不出来）。 */
    static final long[] SEEDS = {1L, 2L, 42L, 12345L, -7L, 20261013L, 20261026L, 987654321L, 5_000_000_000L};
    /** 出生点在 findSpawnPosition 周围 11 区块内找（原版），起点到台阶顶再差约 5 区块：留 256 格余量。 */
    static final int MARGIN = 256;

    private LabyrinthPlacementGameTests() {
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }

    static Holder<StructureSet> set(ServerLevel level, String name) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, id(name)));
    }

    static MultiNoiseBiomeSource biomes(ServerLevel level) {
        return MultiNoiseBiomeSource.createFromPreset(level.registryAccess().registryOrThrow(
                Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
    }

    /** 某个世界种子的主世界结构状态（只含迷宫两套结构集）。 */
    static ChunkGeneratorStructureState state(ServerLevel level, long seed, RandomState random) {
        RegistryAccess ra = level.registryAccess();
        MultiNoiseBiomeSource biomes = biomes(level);
        Set<StructureSet> ours = Set.of(set(level, "labyrinth_near_spawn").value(), set(level, "labyrinths").value());
        return ChunkGeneratorStructureState.createForNormal(random, seed, biomes,
                ra.lookupOrThrow(Registries.STRUCTURE_SET).filterElements(ours::contains));
    }

    static JsonObject encode(ServerLevel level, StructurePlacement placement) {
        JsonElement e = StructurePlacement.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()), placement)
                .getOrThrow();
        return e.getAsJsonObject();
    }

    /** TC-T22-75：几个世界种子里，近出生点那座的环位置离出生点都在 1500 格内（合同「1500 格保证」），且落在陆地群系上。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void near_spawn_within_1500(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ConcentricRingsStructurePlacement rings = (ConcentricRingsStructurePlacement) set(level, "labyrinth_near_spawn").value()
                .placement();
        h.assertTrue(rings.count() == 1, "只 1 座");
        double max = 0;
        List<String> report = new ArrayList<>();
        for (long seed : SEEDS) {
            RandomState random = RandomState.create(level.registryAccess().asGetterLookup(), NoiseGeneratorSettings.OVERWORLD, seed);
            List<ChunkPos> pos = state(level, seed, random).getRingPositionsFor(rings);
            h.assertTrue(pos != null && pos.size() == 1, seed + "：环位置应有 1 个，实际 " + pos);
            BlockPos spawn = random.sampler().findSpawnPosition();
            ChunkPos c = pos.get(0);
            double d = Math.hypot(c.getMiddleBlockX() - spawn.getX(), c.getMiddleBlockZ() - spawn.getZ());
            max = Math.max(max, d);
            var biome = NearSpawnRings.biome(c, biomes(level), random.sampler());
            h.assertTrue(!biome.is(NearSpawnRings.WET),
                    "种子 " + seed + "：环位置 " + c + " 落在湿地群系 " + biome.getRegisteredName());
            report.add(seed + ":" + (int) d + ":" + biome.getRegisteredName());
            h.assertTrue(d + MARGIN < 1500, "种子 " + seed + "：迷宫 " + c + " 离出生点 " + spawn + " 有 " + (int) d + " 格");
        }
        MythicAge.LOGGER.info("近出生点迷宫离出生点（种子:格）{}，最远 {}", report, (int) max);
        h.succeed();
    }

    /** TC-T22-76：普通结构集对近出生点那套设了 10 区块的互斥区；近出生点那套偏好 arcadia。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void labyrinth_sets_exclusive(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        JsonObject normal = encode(level, set(level, "labyrinths").value().placement());
        h.assertTrue(normal.has("exclusion_zone"), "普通结构集要有互斥区：" + normal);
        JsonObject zone = normal.getAsJsonObject("exclusion_zone");
        h.assertTrue(zone.get("other_set").getAsString().equals("mythic_age:labyrinth_near_spawn"), "互斥对象：" + zone);
        h.assertTrue(zone.get("chunk_count").getAsInt() == 10, "互斥 10 区块：" + zone);
        h.assertTrue(normal.get("spacing").getAsInt() == 80 && normal.get("separation").getAsInt() == 24, "80 / 24：" + normal);
        JsonObject near = encode(level, set(level, "labyrinth_near_spawn").value().placement());
        h.assertTrue(near.get("preferred_biomes").getAsString().equals("#mythic_age:region/greek/arcadia"), "偏好 arcadia：" + near);
        h.succeed();
    }

    /** TC-T22-77：近出生点那座的群系放宽到任意陆地（降级），普通那座只在 arcadia。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void near_spawn_biomes_relaxed(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure near = structures.getOrThrow(ResourceKey.create(Registries.STRUCTURE, id("knossos_near_spawn")));
        Structure normal = structures.getOrThrow(ResourceKey.create(Registries.STRUCTURE, id("knossos")));
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> forest = biomes.getHolderOrThrow(Biomes.FOREST);
        Holder<Biome> meadow = biomes.getHolderOrThrow(Biomes.MEADOW);
        h.assertTrue(near.biomes().contains(forest) && near.biomes().contains(meadow), "近出生点那座：森林、草甸都可以");
        h.assertTrue(normal.biomes().contains(meadow) && !normal.biomes().contains(forest), "普通那座：只在 arcadia");
        h.succeed();
    }

    /** TC-T22-79：换装后的迷宫模板里没有白模替身；红柱的分段写对了（结构放置不会按邻格更新）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void knossos_templates_have_no_stand_ins(GameTestHelper h) {
        List<Block> standIns = List.of(Blocks.RED_TERRACOTTA, Blocks.GOLD_BLOCK, Blocks.SMOOTH_QUARTZ_STAIRS);
        var manager = h.getLevel().getStructureManager();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        List<String> found = new ArrayList<>();
        List<String> ids = new ArrayList<>(List.of("surface", "vault", "hall_nw", "hall_ne", "hall_sw", "hall_se"));
        for (int m = 0; m < 16; m++) {
            ids.add("antechamber_" + m);
            if (m > 0) {
                ids.add("corridor_" + m);
            }
        }
        for (String kind : new String[]{"rest", "colonnade", "storage"}) {
            for (int d : new int[]{1, 2, 4, 8}) {
                ids.add(kind + "_" + d);
            }
        }
        for (String s : ids) {
            var spec = StructureSpecs.get("greek:knossos_" + s);
            h.assertTrue(spec.isPresent(), "缺模板数据 " + s);
            StructureTemplate tpl = manager.get(ResourceLocation.parse(spec.get().template())).orElseThrow();
            for (Block b : standIns) {
                if (!tpl.filterBlocks(BlockPos.ZERO, settings, b).isEmpty()) {
                    found.add(s + " 含 " + b);
                }
            }
        }
        h.assertTrue(found.isEmpty(), "模板里还有替身方块：" + found);
        // 地表残迹：两根 4 段红柱、正中双斧门楣、圣角
        var surface = manager.get(ResourceLocation.parse(StructureSpecs.get("greek:knossos_surface").orElseThrow().template()))
                .orElseThrow();
        var columns = surface.filterBlocks(BlockPos.ZERO, settings, GreekRuins.MINOAN_COLUMN.get());
        h.assertTrue(columns.size() == 8, "地表 2 根红柱共 8 段：" + columns.size());
        for (var info : columns) {
            int y = info.pos().getY() - columns.stream().mapToInt(i -> i.pos().getY()).min().orElse(0);
            String want = switch (y) {
                case 0 -> "bottom";
                case 3 -> "top";
                default -> "middle";
            };
            h.assertTrue(info.state().getValue(io.github.happysnappingturtle.mythicage.content.greek.architecture.ColumnShaftBlock.PART)
                    .getSerializedName().equals(want), "第 " + y + " 段应为 " + want + "：" + info.state());
        }
        h.assertTrue(surface.filterBlocks(BlockPos.ZERO, settings, GreekRuins.LABRYS_LINTEL.get()).size() == 1, "双斧门楣 1 块");
        h.assertTrue(surface.filterBlocks(BlockPos.ZERO, settings, GreekRuins.HORNS_OF_CONSECRATION.get()).size() == 5, "圣角 5 个");
        h.succeed();
    }

    /** TC-T22-80：半人马营地的群系含风袭森林（D6）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void centaur_camp_has_windswept_forest(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        TagKey<Biome> camp = TagKey.create(Registries.BIOME, id("wild/centaur_camp"));
        h.assertTrue(biomes.getHolderOrThrow(Biomes.WINDSWEPT_FOREST).is(camp), "风袭森林");
        h.assertTrue(biomes.getHolderOrThrow(Biomes.MEADOW).is(camp), "arcadia 仍在");
        h.succeed();
    }
}
