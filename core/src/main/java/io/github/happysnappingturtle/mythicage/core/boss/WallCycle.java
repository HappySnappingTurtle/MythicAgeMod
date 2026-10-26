package io.github.happysnappingturtle.mythicage.core.boss;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 阶段二的机关墙（T23 §3、§5.5，D4、D5）：布局按 {@code A → A+B → 无 → A …} 循环，每种保持 {@value #HOLD} 刻；
 * 每次切换先预警 {@value #WARN} 刻；要升起的组若有格子被占（玩家、生物、掉落物）就<b>整组延期</b>，每 {@value #RECHECK} 刻重查，
 * 累计 {@value #POSTPONE_LIMIT} 刻还被占就取消本次切换（保持当前布局，下一轮再试同一个目标），不推人、不传送。落下不检查。
 * 纯状态机：调用方每刻 {@link #tick} 一次，按返回的动作改方块。
 */
public final class WallCycle {
    public static final int WARN = 24;
    public static final int HOLD = 500;
    public static final int RECHECK = 10;
    public static final int POSTPONE_LIMIT = 100;

    public enum Group { A, B }

    public enum Layout {
        NONE(EnumSet.noneOf(Group.class)), A(EnumSet.of(Group.A)), AB(EnumSet.of(Group.A, Group.B));

        private final Set<Group> groups;

        Layout(Set<Group> groups) {
            this.groups = groups;
        }

        public Set<Group> groups() {
            return groups;
        }

        public Layout next() {
            return switch (this) {
                case A -> AB;
                case AB -> NONE;
                case NONE -> A;
            };
        }
    }

    public enum Kind { WARN, RAISE, LOWER, CANCEL }

    public record Action(Kind kind, Group group) {
    }

    private enum State { OFF, WARN, WAIT, STEADY }

    private State state = State.OFF;
    private Layout current = Layout.NONE;
    private Layout target = Layout.A;
    private long since;
    private int waited;

    /** 进入阶段二：立刻开始预警升 A。 */
    public List<Action> start(long now) {
        current = Layout.NONE;
        target = Layout.A;
        return warn(now);
    }

    public List<Action> tick(long now, Predicate<Group> occupied) {
        List<Action> out = new ArrayList<>();
        switch (state) {
            case OFF -> {
            }
            case STEADY -> {
                if (now - since >= HOLD) {
                    out.addAll(warn(now));
                }
            }
            case WARN -> {
                if (now - since >= WARN) {
                    state = State.WAIT;
                    waited = 0;
                    out.addAll(tryApply(now, occupied));
                }
            }
            case WAIT -> {
                waited++;
                if (waited >= POSTPONE_LIMIT) {
                    for (Group g : raising()) {
                        out.add(new Action(Kind.CANCEL, g));
                    }
                    state = State.STEADY;
                    since = now;
                } else if (waited % RECHECK == 0) {
                    out.addAll(tryApply(now, occupied));
                }
            }
        }
        return out;
    }

    /** 胜利 / 复原：全部落下并停止。 */
    public List<Action> stop() {
        List<Action> out = new ArrayList<>();
        for (Group g : current.groups()) {
            out.add(new Action(Kind.LOWER, g));
        }
        current = Layout.NONE;
        state = State.OFF;
        return out;
    }

    public Layout current() {
        return current;
    }

    public boolean running() {
        return state != State.OFF;
    }

    /** 正在等待升起（已预警或延期中）的组。 */
    public Set<Group> pending() {
        return state == State.WARN || state == State.WAIT ? raising() : EnumSet.noneOf(Group.class);
    }

    private List<Action> warn(long now) {
        state = State.WARN;
        since = now;
        List<Action> out = new ArrayList<>();
        for (Group g : Group.values()) {
            if (target.groups().contains(g) != current.groups().contains(g)) {
                out.add(new Action(Kind.WARN, g));
            }
        }
        return out;
    }

    private Set<Group> raising() {
        Set<Group> s = EnumSet.noneOf(Group.class);
        for (Group g : target.groups()) {
            if (!current.groups().contains(g)) {
                s.add(g);
            }
        }
        return s;
    }

    private List<Action> tryApply(long now, Predicate<Group> occupied) {
        Set<Group> up = raising();
        for (Group g : up) {
            if (occupied.test(g)) {
                return List.of();
            }
        }
        List<Action> out = new ArrayList<>();
        for (Group g : current.groups()) {
            if (!target.groups().contains(g)) {
                out.add(new Action(Kind.LOWER, g));
            }
        }
        for (Group g : up) {
            out.add(new Action(Kind.RAISE, g));
        }
        current = target;
        target = current.next();
        state = State.STEADY;
        since = now;
        return out;
    }

    /** 存档用：当前布局、目标、状态名、计时起点、已等待。 */
    public String save() {
        return current + "," + target + "," + state + "," + since + "," + waited;
    }

    public void load(String s) {
        String[] p = s.split(",");
        if (p.length != 5) {
            return;
        }
        current = Layout.valueOf(p[0]);
        target = Layout.valueOf(p[1]);
        state = State.valueOf(p[2]);
        since = Long.parseLong(p[3]);
        waited = Integer.parseInt(p[4]);
    }
}
