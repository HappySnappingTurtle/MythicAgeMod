package io.github.happysnappingturtle.mythicage.core.village.time;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 作息（T19 设计文档 §9.1，V1 §6.1）：按 MC 一日的刻数（0 = 日出）分段；段可以跨午夜（如 23000–24000、13000–23000）。
 * {@code overlays} 是叠加在主段上的时段（如希腊祈祷窗口 2000–9000，与工作重叠）。
 *
 * @param segments 主段：一天内任一刻恰好落在一段里（解析时校验）
 */
public record Schedule(List<Segment> segments, List<Segment> overlays) {
    public static final int DAY = 24000;

    /** 常用段名。 */
    public static final String DAWN = "dawn", WORK = "work", LUNCH = "lunch", EVENING = "evening", SUPPER = "supper",
            SLEEP = "sleep";

    /** @param to 不含；from > to 表示跨午夜 */
    public record Segment(String id, int from, int to) {
        public boolean contains(int t) {
            if (from == to) {
                return true;
            }
            return from < to ? t >= from && t < to : t >= from || t < to;
        }
    }

    public Schedule {
        segments = List.copyOf(segments);
        overlays = List.copyOf(overlays);
    }

    public static int timeOfDay(long dayTime) {
        return (int) Math.floorMod(dayTime, (long) DAY);
    }

    public String segmentAt(int tick) {
        int t = Math.floorMod(tick, DAY);
        for (Segment s : segments) {
            if (s.contains(t)) {
                return s.id();
            }
        }
        return WORK;
    }

    public boolean in(String overlay, int tick) {
        int t = Math.floorMod(tick, DAY);
        return overlays.stream().anyMatch(s -> s.id().equals(overlay) && s.contains(t));
    }

    public Optional<Segment> overlay(String id) {
        return overlays.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    /** 这一刻是不是某段的第一刻（每日事件，如晚饭扣口粮）。 */
    public boolean startsAt(String id, int tick) {
        int t = Math.floorMod(tick, DAY);
        return segments.stream().anyMatch(s -> s.id().equals(id) && s.from() == t);
    }

    public static Schedule parse(JsonObject o) {
        List<Segment> seg = list(o, "segments");
        for (int t = 0; t < DAY; t += 100) {
            int tt = t;
            if (seg.stream().noneMatch(s -> s.contains(tt))) {
                throw new IllegalArgumentException("作息的主段没有覆盖第 " + t + " 刻");
            }
        }
        return new Schedule(seg, list(o, "overlays"));
    }

    private static List<Segment> list(JsonObject o, String key) {
        List<Segment> r = new ArrayList<>();
        if (o.has(key)) {
            for (JsonElement e : o.getAsJsonArray(key)) {
                JsonObject s = e.getAsJsonObject();
                int from = s.get("from").getAsInt();
                int to = s.get("to").getAsInt();
                r.add(new Segment(s.get("id").getAsString(), Math.floorMod(from, DAY), to == DAY ? 0 : Math.floorMod(to, DAY)));
            }
        }
        return r;
    }

    /** 测试与回退用的默认作息：0–11000 工作，11000–12000 傍晚，12000–13000 晚饭，13000–23000 睡觉，23000 起黎明。 */
    public static Schedule simple() {
        return new Schedule(List.of(new Segment(DAWN, 23000, 0), new Segment(WORK, 0, 11000),
                new Segment(EVENING, 11000, 12000), new Segment(SUPPER, 12000, 13000), new Segment(SLEEP, 13000, 23000)),
                List.of());
    }
}
