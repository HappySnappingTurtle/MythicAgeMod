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
import java.util.Set;
import java.util.Map;

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
        // TC-T10b-13：通用群系加成与 T10a 的沿海加成结果相同
        Map<String, Integer> w = VillageRules.biomeBonus(GREEK, Map.of("poseidon", 2.0));
        assertEquals(70, w.get("poseidon"));
        assertEquals(70.0 / 135, histogram(w, 10000, 7).get("poseidon") / 10000.0, 0.02);
        assertEquals(GREEK, VillageRules.biomeBonus(GREEK, Map.of()));
        assertEquals(GREEK, VillageRules.biomeBonus(GREEK, Map.of("odin", 3.0)), "不在权重表里的主神忽略");
        assertEquals(1, VillageRules.biomeBonus(Map.of("zeus", 1), Map.of("zeus", 0.1)).get("zeus"), "至少为 1");
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
        VillagesConfig.BiomeBonus b = c.biomeBonus("greek").get("poseidon");
        assertEquals(List.of("#minecraft:is_ocean", "#minecraft:is_beach"), b.biomes());
        assertEquals(32, b.radius());
        assertEquals(2.0, b.multiplier());
    }

    @Test
    void tc_t10b_10_patronWeightsMergeByCiv() {
        VillagesConfig base = new VillagesConfig(VillagesConfig.Mode.AFFILIATE, Map.of(), true,
                Map.of("greek", GREEK, "egyptian", Map.of("ra", 40, "isis", 35, "set", 25)), Map.of(), true);
        List<io.github.happysnappingturtle.mythicage.core.config.ConfigIssue> issues = new java.util.ArrayList<>();
        VillagesConfig v = VillagesSchema.INSTANCE.read(io.github.happysnappingturtle.mythicage.core.config.ObjectReader.root(
                com.google.gson.JsonParser.parseString("{\"patronWeights\":{\"greek\":{\"zeus\":1}}}").getAsJsonObject(),
                issues), base);
        assertEquals(Map.of("zeus", 1), v.patronWeights().get("greek"));
        assertEquals(Map.of("ra", 40, "isis", 35, "set", 25), v.patronWeights().get("egyptian"), "文件没写的文明保留默认");
        assertTrue(issues.isEmpty(), issues::toString);
    }

    @Test
    void tc_t10b_11_biomeBonusParse() {
        ConfigLoadResult<VillagesConfig> ok = ConfigLoader.load(VillagesSchema.INSTANCE, """
                {"patronBiomeBonus":{"greek":{"hades":{"biomes":["minecraft:dark_forest"],"radius":16,"multiplier":1.5}}}}""",
                null);
        assertTrue(ok.issues().isEmpty(), () -> ok.issues().toString());
        assertEquals(new VillagesConfig.BiomeBonus(List.of("minecraft:dark_forest"), 16, 1.5),
                ok.value().biomeBonus("greek").get("hades"));
        assertEquals(2.0, ok.value().biomeBonus("greek").get("poseidon").multiplier(), "同文明其他主神保留默认");
        ConfigLoadResult<VillagesConfig> bad = ConfigLoader.load(VillagesSchema.INSTANCE, """
                {"patronBiomeBonus":{"greek":{"poseidon":{"biomes":["Not A Biome"],"radius":200,"multiplier":0}},
                 "elves":{"x":{}}}}""", null);
        List<String> paths = bad.issues().stream().map(i -> i.path()).toList();
        assertTrue(paths.containsAll(List.of("patronBiomeBonus.greek.poseidon.biomes", "patronBiomeBonus.greek.poseidon.radius",
                "patronBiomeBonus.greek.poseidon.multiplier", "patronBiomeBonus.elves")), paths::toString);
        assertEquals(VillagesSchema.INSTANCE.defaults().biomeBonus("greek"), bad.value().biomeBonus("greek"));
    }

    @Test
    void tc_t10b_12_version1CoastalIsMigrated() {
        ConfigLoadResult<VillagesConfig> r = ConfigLoader.load(VillagesSchema.INSTANCE,
                "{\"schemaVersion\":1,\"coastalPoseidonBonus\":3.0}", null);
        assertEquals(3.0, r.value().biomeBonus("greek").get("poseidon").multiplier());
        assertEquals(List.of("coastalPoseidonBonus"), r.issues().stream().map(i -> i.path()).toList());
        String out = ConfigLoader.toPrettyJson(VillagesSchema.INSTANCE, r.value());
        assertFalse(out.contains("coastalPoseidonBonus"), "toJson 只输出新结构");
        assertTrue(out.contains("patronBiomeBonus"));
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
        // 18 号 §4：候选看举行地村庄的主神
        Pantheon p = greek();
        assertEquals(List.of("athena", "hermes"), ids(p.candidates("zeus", Age.CLASSICAL)));
        assertEquals(List.of("athena", "ares"), ids(p.candidates("hades", Age.CLASSICAL)));
        assertEquals(List.of("dionysus"), ids(p.candidates("zeus", Age.HEROIC)), "阿波罗未开放");
        assertEquals(List.of(), ids(p.candidates("odin", Age.CLASSICAL)));
        assertEquals(List.of("apollo", "dionysus"), ids(p.underPatron("zeus", Age.HEROIC)), "对话页列出含未开放的");
    }

    @Test
    void tc_t10b_14_duplicateGodIdsAcrossCivs() {
        Pantheon other = Pantheon.parse("""
                {"schemaVersion":1,"civilization":"atlantean","patrons":["kronos","zeus"],
                 "minorGods":[{"id":"athena","age":"classical","patrons":["kronos"]}]}""");
        assertEquals(List.of("athena", "zeus"), Pantheon.duplicateIds(List.of(greek(), other)));
        assertEquals(List.of(), Pantheon.duplicateIds(List.of(greek())));
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
        // T11a：补给类次神商品只在立了该次神祠的村庄出售（对所有人），供奉者有折扣
        TradeRules.VenueContext noShrine = TradeRules.VenueContext.guardian(Age.CLASSICAL, "zeus", Set.of());
        TradeRules.VenueContext athenaShrine = TradeRules.VenueContext.guardian(Age.CLASSICAL, "zeus", Set.of("athena"));
        assertEquals(TradeReason.NO_SHRINE, TradeRules.check(d, Civilization.GREEK, shield, noShrine, 10, 10, 0));
        assertEquals(TradeReason.OK, TradeRules.check(d, Civilization.GREEK, shield, athenaShrine, 10, 10, 0));
        assertEquals(List.of(), TradeRules.discounts(d, Civilization.GREEK, shield, athenaShrine));
        d.civ(Civilization.GREEK).minorGods().put(Age.CLASSICAL, "athena");
        assertEquals(List.of(0.1), TradeRules.discounts(d, Civilization.GREEK, shield, athenaShrine));
        assertEquals(List.of(0.1), TradeRules.discounts(d, Civilization.GREEK, bolt, noShrine));
        assertEquals(List.of(), TradeRules.discounts(d, Civilization.GREEK, bolt,
                TradeRules.VenueContext.guardian(Age.CLASSICAL, "hades", Set.of())));
        assertThrows(TradeTable.TradeTableException.class, () -> TradeTable.parse(json.replace("0.1}", "0.6}"),
                TradeTable.ItemLookup.ANY));
        assertThrows(TradeTable.TradeTableException.class, () -> TradeTable.parse(
                json.replace("\"patron\":\"zeus\",", ""), TradeTable.ItemLookup.ANY), "patronDiscount 需要 patron");
    }
}
