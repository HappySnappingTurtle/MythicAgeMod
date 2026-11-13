package io.github.happysnappingturtle.mythicage.core.playtest;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.rite.PromotionTable;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static io.github.happysnappingturtle.mythicage.core.playtest.PlaytestStations.Spot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GA-3 站点表（TC-GA3-01～08）。晋升数值读正式数据，不在测试里另写一份。 */
class PlaytestStationsTest {
    static PromotionTable promotion() {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        Path file = root.resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/promotion/greek.json");
        try {
            return PromotionTable.parse(Files.readString(file), id -> OptionalInt.of(64),
                    type -> type.equals("greek:temple_level"), new ArrayList<>());
        } catch (IOException e) {
            throw new IllegalStateException("读不到 " + file, e);
        }
    }

    private static PlaytestStations.Spec spec(String id) {
        return PlaytestStations.byId(id).orElseThrow().spec();
    }

    @Test
    void tc01Ids() {
        List<String> ids = PlaytestStations.ALL.stream().map(PlaytestStations.Station::id).toList();
        assertEquals(List.of("start", "temple", "powers", "village", "rite", "labyrinth", "boss", "hero"), ids);
        for (int i = 0; i < PlaytestStations.ALL.size(); i++) {
            assertEquals(i + 1, PlaytestStations.ALL.get(i).number());
        }
    }

    @Test
    void tc02Minutes() {
        PlaytestStations.ALL.forEach(s -> assertTrue(s.minutes() >= 5 && s.minutes() <= 10, s.id() + " " + s.minutes()));
        assertTrue(PlaytestStations.totalMinutes() <= 60, "合计 " + PlaytestStations.totalMinutes());
    }

    @Test
    void tc03Keys() {
        Set<String> keys = new HashSet<>();
        PlaytestStations.ALL.forEach(s -> {
            assertTrue(keys.add(s.watchKey()));
            assertTrue(keys.add(s.nameKey()));
        });
    }

    @Test
    void tc04FavorWithinCap() {
        PlaytestStations.ALL.forEach(s -> assertTrue(s.spec().favor() <= s.spec().age().baseCap(),
                s.id() + " 恩宠 " + s.spec().favor() + " 超过 " + s.spec().age() + " 上限"));
        PlaytestStations.ALL.stream().filter(s -> s.spec().age() == Age.NONE)
                .forEach(s -> assertEquals(0, s.spec().favor(), s.id()));
    }

    @Test
    void tc05RiteHalf() {
        PromotionTable t = promotion();
        PromotionTable.AgeEntry classical = t.entry(Age.CLASSICAL).orElseThrow();
        List<PlaytestStations.Give> given = PlaytestStations.offerings(spec("rite").offerings(), t);
        assertEquals(classical.offerings().size() / 2, given.size(), "给一半：" + given);
        for (PlaytestStations.Give g : given) {
            assertTrue(classical.offerings().contains(new TradeOffer.ItemAmount(g.item(), g.count())), "数量与表一致：" + g);
        }
        assertEquals(classical.favor(), spec("rite").favor(), "恩宠正好够");
        assertTrue(spec("rite").temple() >= 1, "神庙条件满足");
    }

    @Test
    void tc06HeroAllMet() {
        PromotionTable t = promotion();
        PromotionTable.AgeEntry heroic = t.entry(Age.HEROIC).orElseThrow();
        PlaytestStations.Spec s = spec("hero");
        assertEquals(heroic.offerings().size(), PlaytestStations.offerings(s.offerings(), t).size());
        assertTrue(s.favor() >= heroic.favor());
        assertTrue(s.firstKill(), "凭证来自首杀结算");
        assertEquals(Age.CLASSICAL, heroic.trial());
        int level = heroic.requirements().getFirst().params().get("level").getAsInt();
        assertTrue(s.temple() >= level, "神庙 " + s.temple() + " ≥ " + level);
        assertEquals(Age.CLASSICAL, s.age());
    }

    @Test
    void tc07ById() {
        assertTrue(PlaytestStations.byId("boss").isPresent());
        assertFalse(PlaytestStations.byId("").isPresent());
        assertFalse(PlaytestStations.byId("BOSS ").isPresent());
        assertFalse(PlaytestStations.byId("x").isPresent());
    }

    @Test
    void tc08Anchors() {
        for (String id : List.of("start", "temple", "powers", "village", "rite", "hero")) {
            assertTrue(spec(id).spot().village(), id);
        }
        assertEquals(Spot.LABYRINTH_ENTRANCE, spec("labyrinth").spot());
        assertEquals(Spot.ARENA_GATE, spec("boss").spot());
        assertEquals(-1, spec("start").temple());
        assertEquals(-1, spec("temple").temple(), "第 2 站不预先扩建");
        assertEquals(1, spec("powers").temple());
        assertEquals(1, spec("rite").temple());
        assertEquals(2, spec("hero").temple());
        assertTrue(spec("temple").upgrade(), "第 2 站：神庙扩建（GA-13）");
        PlaytestStations.ALL.stream().filter(s -> !s.id().equals("temple")).forEach(s -> assertFalse(s.spec().upgrade(), s.id()));
        assertTrue(spec("boss").items().stream().anyMatch(g -> g.item().equals("mythic_age:ariadne_thread")));
        assertFalse(spec("labyrinth").items().stream().anyMatch(g -> g.item().equals("mythic_age:ariadne_thread")),
                "迷宫站自己取线团");
        // 晋升到古典的站都选了次神（雅典娜的交易与召唤要用）
        PlaytestStations.ALL.stream().filter(s -> s.spec().age() == Age.CLASSICAL)
                .forEach(s -> assertEquals(PlaytestStations.ATHENA, s.spec().god(), s.id()));
    }
}
