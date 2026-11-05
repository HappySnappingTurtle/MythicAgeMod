package io.github.happysnappingturtle.mythicage.core.greek.prayer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 供奉的供品表（TC-GA4-20～25）。 */
class OfferingTableTest {
    static String real() {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        try {
            return Files.readString(root.resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/offering/greek.json"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String table(String items, String bonus) {
        return "{\"schemaVersion\":1,\"bonus\":" + bonus + ",\"items\":[" + items + "]}";
    }

    @Test
    void tc20Real() {
        List<String> w = new ArrayList<>();
        OfferingTable t = OfferingTable.parse(real(), w);
        assertEquals(7, t.items().size());
        assertEquals(0.5, t.bonus());
        assertTrue(w.isEmpty(), w.toString());
    }

    @Test
    void tc21Lookup() {
        OfferingTable t = OfferingTable.parse(real(), new ArrayList<>());
        assertEquals(1, t.of("mythic_age:libation").orElseThrow().count());
        assertEquals(2, t.of("minecraft:cooked_beef").orElseThrow().count());
        assertEquals("thysia", t.of("minecraft:cooked_beef").orElseThrow().kind());
        assertTrue(t.of("minecraft:diamond").isEmpty());
    }

    @Test
    void tc22BadCount() {
        List<String> w = new ArrayList<>();
        OfferingTable t = OfferingTable.parse(table("{\"item\":\"a:b\",\"count\":0},{\"item\":\"a:c\",\"count\":-2},"
                + "{\"item\":\"a:d\",\"count\":3}", "0.5"), w);
        assertEquals(1, t.items().size());
        assertEquals(2, w.size());
    }

    @Test
    void tc23BadBonus() {
        assertThrows(OfferingTable.OfferingTableException.class, () -> OfferingTable.parse(table("", "2.5"), new ArrayList<>()));
        assertThrows(OfferingTable.OfferingTableException.class, () -> OfferingTable.parse(table("", "-0.1"), new ArrayList<>()));
        assertThrows(OfferingTable.OfferingTableException.class,
                () -> OfferingTable.parse("{\"schemaVersion\":1,\"items\":[]}", new ArrayList<>()));
    }

    @Test
    void tc24Duplicate() {
        List<String> w = new ArrayList<>();
        OfferingTable t = OfferingTable.parse(table("{\"item\":\"a:b\",\"count\":1},{\"item\":\"a:b\",\"count\":5}", "0.5"), w);
        assertEquals(1, t.of("a:b").orElseThrow().count());
        assertEquals(1, w.size());
    }

    @Test
    void tc25Version() {
        assertThrows(OfferingTable.OfferingTableException.class,
                () -> OfferingTable.parse("{\"schemaVersion\":2,\"bonus\":0.5,\"items\":[]}", new ArrayList<>()));
        assertThrows(OfferingTable.OfferingTableException.class, () -> OfferingTable.parse("[]", new ArrayList<>()));
    }
}
