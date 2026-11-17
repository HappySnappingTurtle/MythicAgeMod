package io.github.happysnappingturtle.mythicage.core.combat;

import io.github.happysnappingturtle.mythicage.core.combat.DamageZones.Mod;
import io.github.happysnappingturtle.mythicage.core.combat.DamageZones.Source;
import io.github.happysnappingturtle.mythicage.core.combat.DamageZones.Target;
import io.github.happysnappingturtle.mythicage.core.combat.DamageZones.Zone;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T25.1 伤害乘区（TC-T25.1-01～14、33）。 */
class DamageZonesTest {
    private static final double EPS = 1e-9;

    private static double mult(Source s, Target t, Mod... mods) {
        return DamageZones.compute(1, s, t, List.of(mods)).multiplier();
    }

    private static double player(Mod... mods) {
        return mult(Source.PLAYER, Target.NORMAL, mods);
    }

    private static final Mod HERO_GEAR = Mod.of(Zone.GEAR, "vs_mythic", 0.25);
    private static final Mod HYDRA_ARROW = Mod.of(Zone.AMMO, "vs_mythic", 0.5);
    private static final Mod HERACLES_WORN = Mod.of(Zone.BLESSING, "heracles_arrow", 0.1);

    @Test
    void tc01Nothing() {
        assertEquals(1, player(), EPS);
        DamageZones.Result r = DamageZones.compute(6, Source.PLAYER, Target.NORMAL, List.of());
        assertEquals(6, r.total(), EPS);
        assertTrue(r.lines().isEmpty());
    }

    @Test
    void tc02GearTimesAmmo() {
        assertEquals(1.875, player(HERO_GEAR, HYDRA_ARROW), EPS);
    }

    @Test
    void tc03SameGroupTakesMax() {
        assertEquals(1.25, player(HERO_GEAR, Mod.of(Zone.GEAR, "vs_mythic", 0.25)), EPS);
        assertEquals(1.25, player(Mod.of(Zone.GEAR, "vs_mythic", 0.1), HERO_GEAR), EPS);
    }

    @Test
    void tc04DifferentGroupsAdd() {
        assertEquals(1.3, player(HERACLES_WORN, Mod.of(Zone.BLESSING, "heracles_offered", 0.2)), EPS);
    }

    @Test
    void tc05BoostCap() {
        DamageZones.Result r = DamageZones.compute(1, Source.PLAYER, Target.NORMAL, List.of(Mod.of(Zone.GEAR, "a", 0.5),
                Mod.of(Zone.AMMO, "b", 0.5), Mod.of(Zone.BLESSING, "c", 0.5)));
        assertEquals(DamageZones.BOOST_CAP, r.multiplier(), EPS);
        assertTrue(r.capped());
        // 封顶只管三个增伤区，窗口照乘
        assertEquals(5.0, player(Mod.of(Zone.GEAR, "a", 0.5), Mod.of(Zone.AMMO, "b", 0.5), Mod.of(Zone.BLESSING, "c", 0.5),
                Mod.of(Zone.WINDOW, "stagger", 2)), EPS);
    }

    @Test
    void tc06WindowTakesMaxForElite() {
        assertEquals(3, mult(Source.PLAYER, Target.ELITE, Mod.of(Zone.WINDOW, "stagger", 2), Mod.of(Zone.WINDOW, "drunk", 3)), EPS);
    }

    @Test
    void tc07BossWindowCapped() {
        assertEquals(2, mult(Source.PLAYER, Target.BOSS, Mod.of(Zone.WINDOW, "drunk", 3)), EPS);
    }

    @Test
    void tc08ResistMultiplies() {
        assertEquals(0.1, player(Mod.of(Zone.RESIST, "lion_hide", 0.8), Mod.of(Zone.RESIST, "relic", 0.5)), EPS);
        assertEquals(0.4, player(Mod.of(Zone.RESIST, "a", 0.5), Mod.of(Zone.RESIST, "b", 0.2)), EPS);
    }

    @Test
    void tc09ResistFloor() {
        assertEquals(DamageZones.RESIST_FLOOR, player(Mod.of(Zone.RESIST, "a", 0.8), Mod.of(Zone.RESIST, "b", 0.8)), EPS);
        // 同组取大：两条 −80% 同组只算一次
        assertEquals(0.2, player(Mod.of(Zone.RESIST, "a", 0.8), Mod.of(Zone.RESIST, "a", 0.5)), EPS);
    }

    @Test
    void tc10MoveMultiplies() {
        assertEquals(0.45, mult(Source.SUMMON, Target.NORMAL, Mod.of(Zone.MOVE, "volley", 1.5), Mod.of(Zone.MOVE, "vs_boss", 0.3)),
                EPS);
    }

    @Test
    void tc11PowerIgnoresGearAndAmmo() {
        assertEquals(1, mult(Source.POWER, Target.NORMAL, HERO_GEAR, HYDRA_ARROW), EPS);
        assertEquals(0.3, mult(Source.POWER, Target.BOSS, HERO_GEAR, Mod.of(Zone.MOVE, "quake_vs_boss", 0.3)), EPS);
        // 祝福区：写明「神力伤害」的才算
        assertEquals(1, mult(Source.POWER, Target.NORMAL, HERACLES_WORN), EPS);
        assertEquals(1.2, mult(Source.POWER, Target.NORMAL, Mod.blessing("power_dmg", 0.2, Source.POWER)), EPS);
    }

    @Test
    void tc12SummonOnlyGetsSummonBlessings() {
        assertEquals(1.2, mult(Source.SUMMON, Target.NORMAL, Mod.blessing("to_summons", 0.2, Source.SUMMON), HERACLES_WORN), EPS);
        // 野怪不吃祝福；玩家不吃招式区
        assertEquals(1, mult(Source.MOB, Target.NORMAL, Mod.blessing("x", 0.2, Source.MOB)), EPS);
        assertEquals(1, player(Mod.of(Zone.MOVE, "m", 1.5)), EPS);
    }

    /** §1.3 例子表：四舍五入到一位小数后与表一致。 */
    private static void row(double expected, double amount, Target t, Mod... mods) {
        double total = DamageZones.compute(amount, Source.PLAYER, t, List.of(mods)).total();
        assertEquals(expected, Math.round(total * 10) / 10.0, EPS, "实际 " + total);
    }

    @Test
    void tc13Examples() {
        row(7.5, 6, Target.NORMAL, HERO_GEAR);
        row(16.9, 9, Target.NORMAL, HERO_GEAR, HYDRA_ARROW);
        row(18.6, 9, Target.NORMAL, HERO_GEAR, HYDRA_ARROW, HERACLES_WORN);
        row(9.3, 9, Target.BOSS, HERO_GEAR, HYDRA_ARROW, HERACLES_WORN, Mod.of(Zone.RESIST, "hydra_heads", 0.5));
        row(37.1, 9, Target.BOSS, HERO_GEAR, HYDRA_ARROW, HERACLES_WORN, Mod.of(Zone.WINDOW, "stagger", 2));
        row(11.3, 9, Target.NORMAL, HERO_GEAR, Mod.of(Zone.GEAR, "vs_mythic", 0.25));
    }

    @Test
    void tc14Breakdown() {
        DamageZones.Result r = DamageZones.compute(9, Source.PLAYER, Target.NORMAL, List.of(HERO_GEAR, HYDRA_ARROW,
                Mod.of(Zone.RESIST, "a", 0.5)));
        assertEquals(List.of(Zone.GEAR, Zone.AMMO, Zone.RESIST), r.lines().stream().map(DamageZones.Line::zone).toList(), "空区不列");
        String s = DamageZones.describe(9, r, z -> z.name().toLowerCase());
        assertEquals("9.0 ×1.25 gear ×1.5 ammo ×0.5 resist = 8.44", s);
        // 基础区加值
        DamageZones.Result b = DamageZones.compute(6, Source.PLAYER, Target.NORMAL, List.of(Mod.of(Zone.BASE, "counter", 3)));
        assertEquals(9, b.total(), EPS);
        assertEquals("6.0+3.0 = 9.0", DamageZones.describe(6, b, z -> z.name()));
    }

    @Test
    void nonFiniteIgnoredAndNeverNegative() {
        assertEquals(1, player(Mod.of(Zone.GEAR, "x", Double.NaN)), EPS);
        assertEquals(0, DamageZones.compute(2, Source.PLAYER, Target.NORMAL, List.of(Mod.of(Zone.BASE, "x", -5))).total(), EPS);
    }

    /** TC-T25.1-33：除 DamagePipeline 外，本作代码不在伤害事件里改伤害量（setAmount / setNewDamage）。 */
    @Test
    void tc33OnlyPipelineSetsAmount() throws IOException {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        Path src = root.resolve("neoforge/src/main/java");
        Pattern call = Pattern.compile("\\.(setAmount|setNewDamage)\\(");
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(src)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = f.getFileName().toString();
                if (name.equals("DamagePipeline.java") || f.toString().contains("/gametest/")) {
                    continue;
                }
                List<String> lines = Files.readAllLines(f);
                for (int i = 0; i < lines.size(); i++) {
                    if (call.matcher(lines.get(i)).find()) {
                        bad.add(src.relativize(f) + ":" + (i + 1));
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), "只有 DamagePipeline 能改伤害量：" + bad);
        assertFalse(Files.notExists(src), "找得到源码目录");
    }
}
