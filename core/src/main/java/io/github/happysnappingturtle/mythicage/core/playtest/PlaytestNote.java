package io.github.happysnappingturtle.mythicage.core.playtest;

import java.time.LocalTime;
import java.util.Locale;

/**
 * 一条试玩反馈 → 记录文件里的一行 Markdown（GA-3 设计文档 §5）。
 * 文字原样写入，不做格式化（{@code %} 不会被当成占位符）。
 */
public final class PlaytestNote {
    public static final int MAX_CODE_POINTS = 300;

    /**
     * 记反馈时玩家的状态。
     *
     * @param held    手持物品 id；空串表示空手
     * @param looking 看着的方块或实体 id；空串表示没看着东西
     */
    public record State(int x, int y, int z, String dimension, String age, double favor, String held, String looking) {
    }

    private PlaytestNote() {
    }

    /** 清理玩家输入：换行变空格、Markdown 记号转义、超长截断。全空白时抛 IllegalArgumentException。 */
    public static String clean(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("反馈文字为空");
        }
        String oneLine = text.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ').strip();
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = 0; i < oneLine.length(); ) {
            int cp = oneLine.codePointAt(i);
            if (count == MAX_CODE_POINTS) {
                out.append('…');
                break;
            }
            if (cp == '#' || cp == '|' || cp == '`' || cp == '\\') {
                out.append('\\');
            }
            out.appendCodePoint(cp);
            count++;
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    public static String line(LocalTime time, String text, State s) {
        StringBuilder b = new StringBuilder();
        b.append("- ").append(hhmm(time)).append(" [笔记] ").append(clean(text))
                .append(" — 坐标 (").append(s.x()).append(", ").append(s.y()).append(", ").append(s.z()).append(") ")
                .append(s.dimension())
                .append(" · ").append(s.age())
                .append(" · 恩宠 ").append(String.format(Locale.ROOT, "%.1f", s.favor()));
        if (!s.held().isEmpty()) {
            b.append(" · 手持 ").append(s.held());
        }
        if (!s.looking().isEmpty()) {
            b.append(" · 看着 ").append(s.looking());
        }
        return b.toString();
    }

    /** 里程碑一行。 */
    public static String milestone(LocalTime time, String what) {
        return "- " + hhmm(time) + " [里程碑] " + what;
    }

    public static String hhmm(LocalTime t) {
        return String.format(Locale.ROOT, "%02d:%02d", t.getHour(), t.getMinute());
    }
}
