package io.github.happysnappingturtle.mythicage.core.faith;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T10b-01～05：文明档案。 */
class FaithProfileTest {
    /** 只有这两种物品存在；神坛最大堆叠 64，权杖 1。 */
    private static final Function<String, OptionalInt> ITEMS = id -> switch (id) {
        case "mythic_age:greek_altar" -> OptionalInt.of(64);
        case "mythic_age:pharaoh_sceptre" -> OptionalInt.of(1);
        default -> OptionalInt.empty();
    };

    private static final String GREEK = """
            {"schemaVersion":1,"civilization":"greek",
             "guardian":"entity.mythic_age.greek_guardian","shrine":"block.mythic_age.greek_altar",
             "favor":"mythic_age.favor.greek",
             "hint":"mythic_age.faith.greek.hint",
             "firstMeetGifts":[{"item":"mythic_age:greek_altar","count":1}],
             "reissue":{"item":"mythic_age:greek_altar","shrineKind":"greek:altar"}}""";

    @Test
    void tc01FullProfile() {
        List<String> w = new ArrayList<>();
        FaithProfile p = FaithProfile.parse(GREEK, ITEMS, w);
        assertTrue(w.isEmpty(), w::toString);
        assertEquals(Civilization.GREEK, p.civ());
        assertEquals("entity.mythic_age.greek_guardian", p.guardian());
        assertEquals("block.mythic_age.greek_altar", p.shrine());
        assertEquals("mythic_age.favor.greek", p.favor());
        assertEquals("mythic_age.faith.greek.hint", p.hint());
        assertEquals(List.of(new FaithProfile.Gift("mythic_age:greek_altar", 1)), p.firstMeetGifts());
        assertEquals(Optional.of(new FaithProfile.Reissue("mythic_age:greek_altar", "greek:altar")), p.reissue());
    }

    @Test
    void tc02OnlyCivilization() {
        List<String> w = new ArrayList<>();
        FaithProfile p = FaithProfile.parse("{\"schemaVersion\":1,\"civilization\":\"norse\"}", ITEMS, w);
        assertTrue(w.isEmpty(), w::toString);
        assertEquals(FaithProfile.defaults(Civilization.NORSE), p);
        assertEquals("mythic_age.favor.norse", p.favor());
        assertTrue(p.firstMeetGifts().isEmpty());
        assertTrue(p.reissue().isEmpty());
    }

    @Test
    void tc03WholeFileRejected() {
        assertThrows(FaithProfile.FaithProfileException.class,
                () -> FaithProfile.parse("{\"schemaVersion\":1,\"civilization\":\"elves\"}", ITEMS, new ArrayList<>()));
        assertThrows(FaithProfile.FaithProfileException.class,
                () -> FaithProfile.parse("{\"schemaVersion\":2,\"civilization\":\"greek\"}", ITEMS, new ArrayList<>()));
        assertThrows(FaithProfile.FaithProfileException.class,
                () -> FaithProfile.parse("[1]", ITEMS, new ArrayList<>()));
    }

    @Test
    void tc04BadGiftsSkipped() {
        List<String> w = new ArrayList<>();
        FaithProfile p = FaithProfile.parse("""
                {"schemaVersion":1,"civilization":"egyptian","firstMeetGifts":[
                  {"item":"mythic_age:nope","count":1},{"item":"mythic_age:greek_altar","count":0},
                  {"item":"mythic_age:pharaoh_sceptre","count":2},{"item":"mythic_age:pharaoh_sceptre"}],
                 "reissue":{"item":"mythic_age:greek_altar","shrineKind":"greek:altar"}}""", ITEMS, w);
        assertEquals(List.of(new FaithProfile.Gift("mythic_age:pharaoh_sceptre", 1)), p.firstMeetGifts());
        assertTrue(p.reissue().isEmpty(), "补领的神庙种类必须属于本文明");
        assertEquals(4, w.size(), w::toString);
        assertTrue(w.get(0).startsWith("firstMeetGifts[0].item"), w.get(0));
        assertTrue(w.get(1).startsWith("firstMeetGifts[1].count"), w.get(1));
        assertTrue(w.get(2).startsWith("firstMeetGifts[2].count"), w.get(2));
        assertTrue(w.get(3).startsWith("reissue.shrineKind"), w.get(3));
    }

    @Test
    void tc05UnknownFieldWarns() {
        List<String> w = new ArrayList<>();
        FaithProfile p = FaithProfile.parse("{\"schemaVersion\":1,\"civilization\":\"greek\",\"colour\":\"blue\",\"hint\":3}",
                ITEMS, w);
        assertEquals(FaithProfile.defaults(Civilization.GREEK), p);
        assertEquals(List.of("colour: 未知字段，已忽略", "hint: 应为非空字符串，已使用默认值"), w);
    }
}
