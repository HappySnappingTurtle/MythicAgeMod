package io.github.happysnappingturtle.mythicage.core.playtest;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 试玩反馈的一行格式（TC-GA3-10～17）。 */
class PlaytestNoteTest {
    private static final LocalTime T = LocalTime.of(21, 19);
    private static final PlaytestNote.State FULL = new PlaytestNote.State(120, 71, -340, "主世界", "远古", 2.4,
            "mythic_age:marble_pillar", "mythic_age:greek_altar");

    @Test
    void tc10Full() {
        assertEquals("- 21:19 [笔记] 高亮看不清 — 坐标 (120, 71, -340) 主世界 · 远古 · 恩宠 2.4 · 手持 mythic_age:marble_pillar"
                + " · 看着 mythic_age:greek_altar", PlaytestNote.line(T, "高亮看不清", FULL));
    }

    @Test
    void tc11Newlines() {
        String l = PlaytestNote.line(T, "第一行\n第二行\r\n第三行\r第四", FULL);
        assertFalse(l.contains("\n") || l.contains("\r"));
        assertTrue(l.contains("第一行 第二行 第三行 第四"));
    }

    @Test
    void tc12Escape() {
        assertEquals("\\# a \\| b \\` c \\\\", PlaytestNote.clean("# a | b ` c \\"));
    }

    @Test
    void tc13Percent() {
        assertEquals("100% 恩宠 %s", PlaytestNote.clean("100% 恩宠 %s"));
    }

    @Test
    void tc14Truncate() {
        String exact = "字".repeat(300);
        assertEquals(exact, PlaytestNote.clean(exact));
        assertEquals(exact + "…", PlaytestNote.clean(exact + "多"));
        // 代理对按一个码点计，不会被切开
        String emoji = "😀".repeat(301);
        String cut = PlaytestNote.clean(emoji);
        assertEquals(300, cut.codePointCount(0, cut.length() - 1));
        assertTrue(cut.endsWith("…"));
    }

    @Test
    void tc15Blank() {
        assertThrows(IllegalArgumentException.class, () -> PlaytestNote.clean(""));
        assertThrows(IllegalArgumentException.class, () -> PlaytestNote.clean(" \n\t"));
        assertThrows(IllegalArgumentException.class, () -> PlaytestNote.clean(null));
    }

    @Test
    void tc16NoHeldNoTarget() {
        String l = PlaytestNote.line(T, "x", new PlaytestNote.State(0, 64, 0, "主世界", "古典", 0, "", ""));
        assertFalse(l.contains("手持") || l.contains("看着") || l.contains("null"), l);
    }

    @Test
    void tc17DecimalPoint() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertTrue(PlaytestNote.line(T, "x", FULL).contains("恩宠 2.4"));
        } finally {
            Locale.setDefault(old);
        }
    }
}
