package io.github.happysnappingturtle.mythicage.core.village.temple;

import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-GA13-01～06、08：村庄神庙等级、完好度、晋升门槛、按供奉者入账、迁移选村。 */
class VillageTempleTest {
    private static final double EPS = 1e-9;

    static TempleRules greek() {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        Path file = root.resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/temple/greek.json");
        try {
            return TempleRules.parse(Files.readString(file));
        } catch (IOException e) {
            throw new IllegalStateException("读不到 " + file, e);
        }
    }

    /** TC-GA13-01：卫城远古 / 古典 / 英雄 / 新村无神庙。 */
    @Test
    void levelFromAcropolis() {
        TempleRules r = greek();
        assertEquals(1, r.levelOf(Set.of("acropolis_archaic", "house_small")));
        assertEquals(2, r.levelOf(Set.of("acropolis_classical")));
        assertEquals(3, r.levelOf(Set.of("acropolis_heroic", "agora")));
        assertEquals(0, r.levelOf(Set.of("house_small", "granary")));
        assertEquals(0, r.levelOf(Set.of()));
        // 扩建时旧建筑记录还没移走也取最高一级
        assertEquals(2, r.levelOf(Set.of("acropolis_archaic", "acropolis_classical")));
        assertEquals(1.5, r.tier(1).coefficient(), EPS);
        assertEquals(2.0, r.tier(2).coefficient(), EPS);
        assertEquals(3.0, r.tier(3).coefficient(), EPS);
        assertEquals(1.0, r.tier(0).coefficient(), EPS);
        assertEquals("", r.tier(0).building());
    }

    /** TC-GA13-02：完好度 1 / 0.6 / 0.1 → 系数 ×1 / ×0.6 / ×0.3（下限）。 */
    @Test
    void integrityScalesCoefficient() {
        assertEquals(2.0, TempleStanding.coefficient(2.0, 1), EPS);
        assertEquals(1.2, TempleStanding.coefficient(2.0, 0.6), EPS);
        assertEquals(0.6, TempleStanding.coefficient(2.0, 0.1), EPS);
        assertEquals(0.6, TempleStanding.coefficient(2.0, 0), EPS);
        // 越界与非有限值
        assertEquals(2.0, TempleStanding.coefficient(2.0, 1.7), EPS);
        assertEquals(2.0, TempleStanding.coefficient(2.0, Double.NaN), EPS);
        assertEquals(0.6, TempleStanding.coefficient(2.0, -1), EPS);
    }

    /** TC-GA13-03：等级 2、完好度 0.9 / 0.7，要求 ≥ 2。 */
    @Test
    void promotionGate() {
        assertTrue(TempleStanding.meets(2, 0.9, 2));
        assertFalse(TempleStanding.meets(2, 0.7, 2));
        assertTrue(TempleStanding.meets(2, 0.8, 2));
        assertTrue(TempleStanding.meets(3, 1, 2));
        assertFalse(TempleStanding.meets(1, 1, 2));
    }

    private static final TempleCredit.Inputs IN = new TempleCredit.Inputs(2.0, 1, 1, 0.5);

    /** TC-GA13-04：一名村民祈祷，村子有 2 位供奉者 → 两人各入一笔，各按自己的 n。 */
    @Test
    void eachDevoteeCreditedFully() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Map<UUID, Integer> n = Map.of(a, 1, b, 5);
        Map<UUID, TempleCredit.Share> s = TempleCredit.split(List.of(a, b), n::get, d -> 0, d -> 0, IN);
        assertEquals(2, s.size());
        assertEquals(PrayerFormula.credit(1, 2.0, 1, 0, 0, 1), s.get(a).gain(), EPS);
        assertEquals(PrayerFormula.credit(5, 2.0, 1, 0, 0, 1), s.get(b).gain(), EPS);
        assertTrue(s.get(a).gain() > s.get(b).gain());
        // 不因多一位供奉者而摊薄
        Map<UUID, TempleCredit.Share> alone = TempleCredit.split(List.of(a), n::get, d -> 0, d -> 0, IN);
        assertEquals(alone.get(a).gain(), s.get(a).gain(), EPS);
    }

    /** TC-GA13-05：两村今天合计第 25 名信徒不再给 A 入账。 */
    @Test
    void capAcrossVillages() {
        UUID a = UUID.randomUUID();
        Map<UUID, TempleCredit.Share> s24 = TempleCredit.split(List.of(a), d -> 24, d -> 0, d -> 0, IN);
        Map<UUID, TempleCredit.Share> s25 = TempleCredit.split(List.of(a), d -> 25, d -> 0, d -> 0, IN);
        assertTrue(s24.get(a).gain() > 0);
        assertEquals(0, s25.get(a).gain(), EPS);
        assertEquals(0, s25.get(a).base(), EPS);
    }

    /** TC-GA13-06：A 在甲村献供、乙村没献 → 只有甲村的祈祷 +50%。 */
    @Test
    void offeringPerVillage() {
        UUID a = UUID.randomUUID();
        TempleCredit.Share jia = TempleCredit.split(List.of(a), d -> 3, d -> 0, d -> 1, IN).get(a);
        TempleCredit.Share yi = TempleCredit.split(List.of(a), d -> 3, d -> 0, d -> 0, IN).get(a);
        assertEquals(jia.base() * 1.5, jia.gain(), EPS);
        assertEquals(yi.base(), yi.gain(), EPS);
        assertEquals(jia.base(), yi.base(), EPS);
    }

    /** TC-GA13-08：旧祭坛在甲村村界内 / 村界外 80 格处有乙村 / 100 格内没有村子。 */
    @Test
    void migrationPick() {
        assertEquals(Optional.of("甲"), DevoteeMigration.choose(List.of(
                new DevoteeMigration.Candidate<>("乙", false, 20),
                new DevoteeMigration.Candidate<>("甲", true, 40))));
        assertEquals(Optional.of("乙"), DevoteeMigration.choose(List.of(
                new DevoteeMigration.Candidate<>("乙", false, 80),
                new DevoteeMigration.Candidate<>("丙", false, 95))));
        assertEquals(Optional.empty(), DevoteeMigration.choose(List.of(
                new DevoteeMigration.Candidate<>("丙", false, 140))));
        assertEquals(Optional.empty(), DevoteeMigration.choose(List.<DevoteeMigration.Candidate<String>>of()));
        // 恰好 100 格算在内
        assertEquals(Optional.of("丁"), DevoteeMigration.choose(List.of(
                new DevoteeMigration.Candidate<>("丁", false, 100))));
    }
}
