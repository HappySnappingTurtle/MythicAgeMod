package io.github.happysnappingturtle.mythicage.core.greek.temple;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.Pos;
import static io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.State;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GA-6：快速神庙蓝图使用正式依附表；负例保证检查不是空跑。 */
class ArchitectureAttachmentsTest {
    private static ArchitectureAttachments rules() throws IOException {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        return ArchitectureAttachments.parse(Files.readString(root.resolve(
                "neoforge/src/main/resources/data/mythic_age/mythic_age/architecture_attach.json")));
    }

    private static Map<Pos, State> blueprint(int level) {
        Map<Pos, State> blocks = new HashMap<>();
        for (var b : TempleBlueprint.of(level)) {
            // 与快速搭建器保持一致；祭坛朝南，其余方块默认朝北，坐标层定义见 TempleBlueprint。
            String id = switch (b.part()) {
                case PLATFORM, FLOOR -> "smooth_marble";
                case PILLAR -> "marble_pillar";
                case ROOF -> "smooth_marble";
                case BRAZIER -> "greek_brazier";
                case STATUE_LOWER, STATUE_UPPER -> "greek_statue";
                case PEDIMENT -> "greek_pediment";
                case ALTAR -> "greek_altar";
            };
            blocks.put(new Pos(b.dx(), b.dy(), b.dz()), new State("mythic_age:" + id, b.part() == TempleBlueprint.Part.ALTAR ? "south" : "north"));
        }
        return blocks;
    }

    @Test
    void quickBlueprintsUseAttachmentTable() throws IOException {
        var rules = rules();
        for (int level : new int[]{1, 2}) {
            var blocks = blueprint(level);
            assertEquals(java.util.List.of(), rules.check("TempleBlueprint-" + level, blocks));
            // 移走火盆下面一格，必须被同一张表发现。
            Pos brazier = blocks.entrySet().stream().filter(e -> e.getValue().id().equals("mythic_age:greek_brazier"))
                    .findFirst().orElseThrow().getKey();
            blocks.remove(new Pos(brazier.x(), brazier.y() - 1, brazier.z()));
            assertFalse(rules.check("TempleBlueprint-" + level, blocks).isEmpty());
        }
    }

    @Test
    void antefixRequiresSupportBehindEveryFacing() throws IOException {
        var rules = rules();
        Map<String, Pos> behind = Map.of("north", new Pos(0, 0, 1), "south", new Pos(0, 0, -1),
                "east", new Pos(-1, 0, 0), "west", new Pos(1, 0, 0));
        for (var direction : behind.entrySet()) {
            Map<Pos, State> blocks = new HashMap<>();
            blocks.put(new Pos(0, 0, 0), new State("mythic_age:terracotta_antefix", direction.getKey()));
            assertEquals(1, rules.check("悬空瓦当", blocks).size());
            for (String support : new String[]{"marble_cornice", "terracotta_roof_slab", "terracotta_roof_stairs"}) {
                blocks.put(direction.getValue(), new State("mythic_age:" + support, "north"));
                assertTrue(rules.check("有效屋檐", blocks).isEmpty());
            }
            blocks.put(direction.getValue(), new State("minecraft:water", "north"));
            assertEquals(1, rules.check("水不是支撑", blocks).size());
        }
    }
    @Test
    void oilLampNeedsTopSurfaceAtBlockBoundary() throws IOException {
        var rules = rules();
        Map<Pos, State> blocks = new HashMap<>();
        blocks.put(new Pos(0, 1, 0), new State("mythic_age:greek_oil_lamp", "north"));
        for (String type : new String[]{"top", "double", "bottom"}) {
            blocks.put(new Pos(0, 0, 0), new State("mythic_age:smooth_marble_slab", "north", Map.of("type", type)));
            assertEquals(type.equals("bottom") ? 1 : 0, rules.check("灯座", blocks).size());
        }
        blocks.put(new Pos(0, 0, 0), new State("mythic_age:smooth_marble_slab", "north"));
        assertEquals(1, rules.check("灯座状态缺失", blocks).size());
    }

}
