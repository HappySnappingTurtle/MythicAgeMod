package io.github.happysnappingturtle.mythicage.core.greek.temple;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.happysnappingturtle.mythicage.core.greek.temple.TempleBlueprint.Part;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 快速验证的神庙蓝图（TC-GA3-40～43）。数量对照正式的 temple/greek.json。 */
class TempleBlueprintTest {
    static TempleRules rules() {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        try {
            return TempleRules.parse(Files.readString(root.resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/temple/greek.json")));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void meets(int level) {
        List<TempleBlueprint.Block> b = TempleBlueprint.of(level);
        TempleRules.Requirement r = rules().tier(level).require();
        assertTrue(TempleBlueprint.count(b, Part.PILLAR) >= r.columns() * 3L, "柱 " + r.columns() + " 根 × 3 高");
        assertTrue(TempleBlueprint.count(b, Part.BRAZIER) >= r.litBraziers());
        assertTrue(TempleBlueprint.count(b, Part.STATUE_LOWER) >= r.statues());
        assertTrue(TempleBlueprint.count(b, Part.PEDIMENT) >= r.pediments());
        long floor = TempleBlueprint.count(b, Part.FLOOR) + TempleBlueprint.count(b, Part.PLATFORM);
        assertTrue(floor >= r.marbleFloor(), "大理石地面 " + floor + " ≥ " + r.marbleFloor());
        if (r.roof()) {
            // 柱围 9×9 全盖
            assertEquals(81, TempleBlueprint.count(b, Part.ROOF));
        }
        assertEquals(1, TempleBlueprint.count(b, Part.ALTAR));
    }

    @Test
    void tc40Level1() {
        meets(1);
        List<TempleBlueprint.Block> b = TempleBlueprint.of(1);
        assertEquals(12, TempleBlueprint.count(b, Part.PILLAR));
        assertEquals(0, TempleBlueprint.count(b, Part.PLATFORM));
    }

    @Test
    void tc41Level2() {
        meets(2);
        List<TempleBlueprint.Block> b = TempleBlueprint.of(2);
        assertEquals(24, TempleBlueprint.count(b, Part.PILLAR));
        assertTrue(TempleBlueprint.count(b, Part.PLATFORM) > 0, "台基");
        assertEquals(1, TempleBlueprint.count(b, Part.STATUE_UPPER));
    }

    @Test
    void tc42InsideScanBox() {
        TempleRules.Scan s = rules().scan();
        for (int level : new int[]{1, 2}) {
            for (TempleBlueprint.Block b : TempleBlueprint.of(level)) {
                assertTrue(Math.abs(b.dx()) <= s.halfWidth() && b.dz() >= -s.back() && b.dz() <= s.front()
                        && b.dy() >= -s.below() && b.dy() <= s.above(), "越界：" + b);
                assertTrue(b.dx() >= TempleBlueprint.MIN_DX && b.dx() <= TempleBlueprint.MAX_DX
                        && b.dz() >= TempleBlueprint.MIN_DZ && b.dz() <= TempleBlueprint.MAX_DZ
                        && b.dy() <= TempleBlueprint.MAX_DY, "超出清理范围：" + b);
            }
        }
    }

    @Test
    void tc43NoOverlap() {
        for (int level : new int[]{1, 2}) {
            Set<String> seen = new HashSet<>();
            for (TempleBlueprint.Block b : TempleBlueprint.of(level)) {
                assertTrue(seen.add(b.dx() + "," + b.dy() + "," + b.dz()), "重叠：" + b);
            }
        }
        assertThrows(IllegalArgumentException.class, () -> TempleBlueprint.of(3));
        assertThrows(IllegalArgumentException.class, () -> TempleBlueprint.of(0));
    }
}
