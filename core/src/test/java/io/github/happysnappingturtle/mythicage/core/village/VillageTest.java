package io.github.happysnappingturtle.mythicage.core.village;

import io.github.happysnappingturtle.mythicage.core.config.ConfigLoadResult;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesSchema;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.core.trade.TradeRules;
import io.github.happysnappingturtle.mythicage.core.trade.TradeTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T10a-01～14：确定性抽取、villages.json、主神/次神表、玩家数据与交易表新字段。 */
class VillageTest {
    private static final Map<String, Integer> GREEK = Map.of("zeus", 40, "poseidon", 35, "hades", 25);

    static Pantheon greek() {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        try {
            return Pantheon.parse(Files.readString(root.resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/pantheon/greek.json")));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Integer> histogram(Map<String, Integer> weights, int n, long salt) {
        Map<String, Integer> h = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String k = VillageRules.pick(weights, VillageRules.unit(i * 37L, 64, -i * 91L, salt)).orElseThrow();
            h.merge(k, 1, Integer::sum);
        }
        return h;
    }

    @Test
    void tc01Deterministic() {
        double a = VillageRules.unit(218, 66, -1243, VillageRules.SALT_PATRON);
        assertEquals(a, VillageRules.unit(218, 66, -1243, VillageRules.SALT_PATRON));
        assertEquals(VillageRules.pick(GREEK, a), VillageRules.pick(new java.util.TreeMap<>(GREEK), a), "与 Map 迭代顺序无关");
    }

    @Test
    void tc02WeightsHold() {
        Map<String, Integer> h = histogram(GREEK, 10000, VillageRules.SALT_PATRON);
        assertEquals(0.40, h.get("zeus") / 10000.0, 0.02);
        assertEquals(0.35, h.get("poseidon") / 10000.0, 0.02);
        assertEquals(0.25, h.get("hades") / 10000.0, 0.02);
    }

    @Test
    void tc03CoastalPoseidon() {
        Map<String, Integer> w = VillageRules.coastal(GREEK, true, 2.0);
        assertEquals(70, w.get("poseidon"));
        assertEquals(70.0 / 135, histogram(w, 10000, 7).get("poseidon") / 10000.0, 0.02);
        assertEquals(GREEK, VillageRules.coastal(GREEK, false, 2.0));
    }

    @Test
    void tc04TypeWeights() {
        assertEquals("greek", VillageRules.pick(Map.of("greek", 1), 0.99).orElseThrow());
        Map<String, Integer> h = histogram(Map.of("greek", 70, "chinese", 30), 10000, VillageRules.SALT_CIV);
        assertEquals(0.30, h.get("chinese") / 10000.0, 0.02);
        assertTrue(VillageRules.pick(Map.of(), 0.5).isEmpty());
    }

    @Test
    void tc05DefaultConfig() {
        ConfigLoadResult<VillagesConfig> r = ConfigLoader.load(VillagesSchema.INSTANCE,
                ConfigLoader.defaultFileContent(VillagesSchema.INSTANCE), null);
        assertTrue(r.issues().isEmpty(), () -> r.issues().toString());
        VillagesConfig c = r.value();
        assertEquals(VillagesConfig.Mode.AFFILIATE, c.vanillaVillages());
        assertEquals(Map.of("greek", 1), c.vanillaTypeToCiv().get("plains"));
        assertEquals(Map.of("norse", 1), c.vanillaTypeToCiv().get("snowy"));
        assertEquals(GREEK, c.patronWeights().get("greek"));
        assertEquals(2.0, c.coastalPoseidonBonus());
    }

    @Test
    void tc06BadValuesWarnWithPath() {
        ConfigLoadResult<VillagesConfig> r = ConfigLoader.load(VillagesSchema.INSTANCE, """
                {"vanillaVillages":"maybe","vanillaTypeToCiv":{"plains":"atlantis","jungle":"chinese","desert":{"egyptian":-3}},
                 "patronWeights":{"greek":{"zeus":-1,"poseidon":10},"elves":{"x":1}},"coastalPoseidonBonus":0}""", null);
        List<String> paths = r.issues().stream().map(i -> i.path()).toList();
        assertTrue(paths.contains("vanillaVillages"), paths::toString);
        assertTrue(paths.contains("vanillaTypeToCiv.plains"), paths::toString);
        assertTrue(paths.contains("vanillaTypeToCiv.jungle"), paths::toString);
        assertTrue(paths.contains("vanillaTypeToCiv.desert.egyptian"), paths::toString);
        assertTrue(paths.contains("patronWeights.greek.zeus"), paths::toString);
        assertTrue(paths.contains("patronWeights.elves"), paths::toString);
        assertTrue(paths.contains("coastalPoseidonBonus"), paths::toString);
        VillagesConfig c = r.value();
        assertEquals(VillagesConfig.Mode.AFFILIATE, c.vanillaVillages(), "非法值回退默认");
        assertEquals(Map.of("greek", 1), c.vanillaTypeToCiv().get("plains"));
        assertEquals(Map.of("poseidon", 10), c.patronWeights().get("greek"));
        ConfigLoadResult<VillagesConfig> weighted = ConfigLoader.load(VillagesSchema.INSTANCE,
                "{\"vanillaTypeToCiv\":{\"plains\":{\"greek\":70,\"chinese\":30}},\"vanillaVillages\":\"neutral\"}", null);
        assertEquals(Map.of("greek", 70, "chinese", 30), weighted.value().vanillaTypeToCiv().get("plains"));
        assertEquals(VillagesConfig.Mode.NEUTRAL, weighted.value().vanillaVillages());
    }

    @Test
    void tc07ParsesGreekPantheon() {
        Pantheon p = greek();
        assertEquals(Civilization.GREEK, p.civ());
        assertEquals(List.of("zeus", "poseidon", "hades"), p.patrons());
        assertEquals(9, p.minorGods().size());
    }

    @Test
    void tc08PantheonErrors() {
        String base = "{\"schemaVersion\":1,\"civilization\":\"greek\",\"patrons\":[\"zeus\"],\"minorGods\":[%s]}";
        assertThrows(Pantheon.PantheonException.class, () -> Pantheon.parse(base.formatted(
                "{\"id\":\"athena\",\"age\":\"classical\",\"patrons\":[\"odin\"]}")));
        assertThrows(Pantheon.PantheonException.class, () -> Pantheon.parse(base.formatted(
                "{\"id\":\"athena\",\"age\":\"archaic\",\"patrons\":[\"zeus\"]}")));
        assertThrows(Pantheon.PantheonException.class, () -> Pantheon.parse(base.formatted(
                "{\"id\":\"a\",\"age\":\"classical\",\"patrons\":[\"zeus\"]},{\"id\":\"a\",\"age\":\"heroic\",\"patrons\":[\"zeus\"]}")));
        var e = assertThrows(Pantheon.PantheonException.class, () -> Pantheon.parse(base.formatted(
                "{\"id\":\"athena\",\"age\":\"classical\",\"patrons\":[\"odin\"]}")));
        assertTrue(e.getMessage().startsWith("minorGods[0].patrons:"), e.getMessage());
    }

    private static List<String> ids(List<Pantheon.MinorGod> gods) {
        return gods.stream().map(Pantheon.MinorGod::id).toList();
    }

    @Test
    void tc09to12Candidates() {
        Pantheon p = greek();
        assertEquals(List.of("athena", "hermes"), ids(p.candidates(Set.of("zeus"), Age.CLASSICAL)));
        assertEquals(List.of("athena", "hermes", "ares"), ids(p.candidates(Set.of("zeus", "hades"), Age.CLASSICAL)));
        assertEquals(List.of("dionysus"), ids(p.candidates(Set.of("zeus"), Age.HEROIC)), "阿波罗未开放");
        assertEquals(List.of(), ids(p.candidates(Set.of(), Age.CLASSICAL)));
        assertEquals(List.of("apollo", "dionysus"), ids(p.underPatron("zeus", Age.HEROIC)), "对话页列出含未开放的");
    }

    @Test
    void tc13PlayerDataCopyEquals() {
        PlayerMythData d = new PlayerMythData();
        d.civ(Civilization.GREEK).knownPatrons().add("zeus");
        d.civ(Civilization.GREEK).minorGods().put(Age.CLASSICAL, "athena");
        PlayerMythData c = d.copy();
        assertEquals(d, c);
        c.civ(Civilization.GREEK).knownPatrons().add("hades");
        assertFalse(d.equals(c));
        assertTrue(d.civ(Civilization.GREEK).worships("athena"));
        assertFalse(d.civ(Civilization.GREEK).worships("ares"));
    }

    @Test
    void tc14TradeFields() {
        String json = """
                {"schemaVersion":1,"civilization":"greek","offers":[
                 {"id":"bolt","category":"god_power","result":{"item":"a:b","count":1},"price":{"favor":20},
                  "minAge":"archaic","venues":["guardian"],"patron":"zeus","patronDiscount":0.1},
                 {"id":"shield","category":"supplies","result":{"item":"a:c","count":1},"price":{"favor":10},
                  "minAge":"classical","venues":["guardian"],"minorGod":"athena","minorGodDiscount":{"athena":0.1}}]}""";
        TradeTable t = TradeTable.parse(json, TradeTable.ItemLookup.ANY);
        TradeOffer bolt = t.offer("bolt").orElseThrow();
        TradeOffer shield = t.offer("shield").orElseThrow();
        assertEquals("zeus", bolt.patron());
        assertEquals("athena", shield.minorGod());
        PlayerMythData d = new PlayerMythData();
        d.civ(Civilization.GREEK).setAge(Age.CLASSICAL);
        d.civ(Civilization.GREEK).setFavor(99);
        assertEquals(TradeReason.MINOR_GOD_LOCKED, TradeRules.check(d, Civilization.GREEK, shield,
                TradeOffer.Venue.GUARDIAN, 10, 10, 0));
        d.civ(Civilization.GREEK).minorGods().put(Age.CLASSICAL, "athena");
        assertEquals(TradeReason.OK, TradeRules.check(d, Civilization.GREEK, shield, TradeOffer.Venue.GUARDIAN, 10, 10, 0));
        assertEquals(List.of(0.1), TradeRules.discounts(d, Civilization.GREEK, shield, ""));
        assertEquals(List.of(0.1), TradeRules.discounts(d, Civilization.GREEK, bolt, "zeus"));
        assertEquals(List.of(), TradeRules.discounts(d, Civilization.GREEK, bolt, "hades"));
        assertThrows(TradeTable.TradeTableException.class, () -> TradeTable.parse(json.replace("0.1}", "0.6}"),
                TradeTable.ItemLookup.ANY));
        assertThrows(TradeTable.TradeTableException.class, () -> TradeTable.parse(
                json.replace("\"patron\":\"zeus\",", ""), TradeTable.ItemLookup.ANY), "patronDiscount 需要 patron");
    }
}
