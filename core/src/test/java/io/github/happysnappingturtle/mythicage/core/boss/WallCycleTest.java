package io.github.happysnappingturtle.mythicage.core.boss;

import io.github.happysnappingturtle.mythicage.core.boss.WallCycle.Action;
import io.github.happysnappingturtle.mythicage.core.boss.WallCycle.Group;
import io.github.happysnappingturtle.mythicage.core.boss.WallCycle.Kind;
import io.github.happysnappingturtle.mythicage.core.boss.WallCycle.Layout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T23：机关墙循环（TC-T23-23～28）。 */
class WallCycleTest {
    static final Predicate<Group> FREE = g -> false;

    /** 从 from 推进到 to（含），收集全部动作。 */
    static List<Action> run(WallCycle w, long from, long to, Predicate<Group> occ) {
        List<Action> out = new ArrayList<>();
        for (long t = from; t <= to; t++) {
            out.addAll(w.tick(t, occ));
        }
        return out;
    }

    @Test
    void tc23to24Cycle() {
        WallCycle w = new WallCycle();
        assertEquals(List.of(new Action(Kind.WARN, Group.A)), w.start(0));
        assertTrue(run(w, 1, 23, FREE).isEmpty(), "预警 24 刻内不动");
        assertEquals(List.of(new Action(Kind.RAISE, Group.A)), w.tick(24, FREE));
        assertEquals(Layout.A, w.current());
        assertTrue(run(w, 25, 523, FREE).isEmpty(), "保持 500 刻");
        assertEquals(List.of(new Action(Kind.WARN, Group.B)), w.tick(524, FREE), "预警升 B");
        assertEquals(List.of(new Action(Kind.RAISE, Group.B)), run(w, 525, 548, FREE));
        assertEquals(Layout.AB, w.current());
        List<Action> down = run(w, 549, 548 + 500 + 24, FREE);
        assertTrue(down.contains(new Action(Kind.LOWER, Group.A)) && down.contains(new Action(Kind.LOWER, Group.B)), "A+B → 无");
        assertEquals(Layout.NONE, w.current());
        List<Action> again = run(w, 548 + 500 + 25, 548 + 1000 + 48, FREE);
        assertTrue(again.contains(new Action(Kind.RAISE, Group.A)), "无 → A");
    }

    @Test
    void tc25to26Postpone() {
        WallCycle w = new WallCycle();
        w.start(0);
        boolean[] occupied = {true};
        Predicate<Group> occ = g -> g == Group.A && occupied[0];
        assertTrue(run(w, 1, 24, occ).isEmpty(), "A 组有人：不升");
        assertEquals(Layout.NONE, w.current());
        assertTrue(w.pending().contains(Group.A));
        occupied[0] = false;
        List<Action> later = run(w, 25, 34, occ);
        assertEquals(List.of(new Action(Kind.RAISE, Group.A)), later, "人走开后下一次重查升起");

        WallCycle stuck = new WallCycle();
        stuck.start(0);
        Predicate<Group> always = g -> true;
        assertTrue(run(stuck, 1, 24 + 99, always).isEmpty(), "等 99 刻还不取消");
        assertEquals(List.of(new Action(Kind.CANCEL, Group.A)), stuck.tick(24 + 100, always), "100 刻取消");
        assertEquals(Layout.NONE, stuck.current(), "保持当前布局");
        List<Action> retry = run(stuck, 125, 124 + 500, FREE);
        assertEquals(List.of(new Action(Kind.WARN, Group.A)), retry, "下一轮再试同一个目标");
    }

    @Test
    void tc27to28LowerAndStop() {
        WallCycle w = new WallCycle();
        w.start(0);
        run(w, 1, 24, FREE);
        run(w, 25, 548, FREE);
        assertEquals(Layout.AB, w.current());
        Predicate<Group> all = g -> true;
        List<Action> down = run(w, 549, 548 + 524, all);
        assertTrue(down.contains(new Action(Kind.LOWER, Group.A)), "落下不检查占用");
        WallCycle v = new WallCycle();
        v.start(0);
        run(v, 1, 24, FREE);
        assertEquals(List.of(new Action(Kind.LOWER, Group.A)), v.stop(), "胜利 / 复原全部落下");
        assertFalse(v.running());
        WallCycle saved = new WallCycle();
        saved.load(w.save());
        assertEquals(w.current(), saved.current(), "存档往返");
    }
}
