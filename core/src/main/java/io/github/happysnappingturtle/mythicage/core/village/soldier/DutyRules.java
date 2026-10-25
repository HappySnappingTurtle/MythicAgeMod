package io.github.happysnappingturtle.mythicage.core.village.soldier;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 班次与岗位（T20 设计文档 §5.1–§5.2，V3-希腊-居民 §7）：纯函数。
 */
public final class DutyRules {
    /** 巡逻时每个岗位一段的时长（走过去加停留，刻）。 */
    public static final int PATROL_LEG = 400;

    private DutyRules() {
    }

    public enum Shift { DAY, NIGHT }

    /** 此刻的状态：在岗、不值勤（按普通居民作息）、夜班士兵白天补觉。 */
    public enum State { ON_DUTY, OFF_DUTY, SHIFT_SLEEP }

    /**
     * 分班：按转岗先后排好的第 {@code index} 名（0 起）。只有 1 名时只值昼班；≥ 2 名时夜班 ⌈n/2⌉ 人
     * （「夜里至少一半在岗」），最早转岗的几名值昼班。
     */
    public static Shift shift(int index, int total) {
        if (total <= 1) {
            return Shift.DAY;
        }
        int day = total / 2;
        return index < day ? Shift.DAY : Shift.NIGHT;
    }

    /** 时刻 {@code t} 是否在 [from, to) 内（可跨午夜；from == to 表示全天）。 */
    public static boolean within(int t, int from, int to) {
        t = Math.floorMod(t, 24000);
        if (from == to) {
            return true;
        }
        return from < to ? t >= from && t < to : t >= from || t < to;
    }

    public static State state(Shift shift, int timeOfDay, SoldierCivRules.Shifts s) {
        boolean onDuty = shift == Shift.DAY ? within(timeOfDay, s.dayFrom(), s.dayTo()) : within(timeOfDay, s.nightFrom(), s.nightTo());
        if (onDuty) {
            return State.ON_DUTY;
        }
        if (shift == Shift.NIGHT && within(timeOfDay, s.sleepFrom(), s.sleepTo())) {
            return State.SHIFT_SLEEP;
        }
        return State.OFF_DUTY;
    }

    /** 一个岗位：锚点类别与位置。 */
    public record Post(String kind, Pos pos) {
    }

    /**
     * 岗位表：按 {@code kinds} 的顺序展开规划里的锚点；一个都没有时用 {@code fallback}（村钟）。
     */
    public static List<Post> posts(List<String> kinds, Map<String, List<Pos>> anchors, Pos fallback) {
        List<Post> out = new ArrayList<>();
        for (String k : kinds) {
            for (Pos p : anchors.getOrDefault(k, List.of())) {
                out.add(new Post(k, p));
            }
        }
        if (out.isEmpty() && fallback != null) {
            out.add(new Post("bell", fallback));
        }
        return out;
    }

    /**
     * 派岗：在岗士兵（按转岗先后）一人一岗；人比岗位多时，多出的人沿岗位顺序巡逻（{@code patrol = true}）。
     *
     * @return 士兵 → 分配（固定岗位的序号，或巡逻）
     */
    public static Map<UUID, Assignment> assign(List<UUID> onDuty, List<Post> posts) {
        Map<UUID, Assignment> out = new LinkedHashMap<>();
        for (int i = 0; i < onDuty.size(); i++) {
            if (posts.isEmpty()) {
                break;
            }
            if (i < posts.size()) {
                out.put(onDuty.get(i), new Assignment(i, false, 0));
            } else {
                out.put(onDuty.get(i), new Assignment(0, true, i - posts.size()));
            }
        }
        return out;
    }

    /**
     * @param post   固定岗位的序号（巡逻时为 0）
     * @param offset 巡逻者的错开量（几名巡逻者不走在一起）
     */
    public record Assignment(int post, boolean patrol, int offset) {
        /** 此刻该去哪个岗位。 */
        public int target(long now, int postCount) {
            if (!patrol || postCount == 0) {
                return post;
            }
            return (int) Math.floorMod(now / PATROL_LEG + offset, (long) postCount);
        }
    }

    /** 离某点最近的岗位序号（不值勤的士兵自己发现敌人时，拴绳以它为中心）；没有岗位返回 -1。 */
    public static int nearest(List<Post> posts, double x, double z) {
        int best = -1;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < posts.size(); i++) {
            Pos p = posts.get(i).pos();
            double d = (p.x() + 0.5 - x) * (p.x() + 0.5 - x) + (p.z() + 0.5 - z) * (p.z() + 0.5 - z);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }
}
