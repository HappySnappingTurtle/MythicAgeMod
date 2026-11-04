package io.github.happysnappingturtle.mythicage.core.boss;

import io.github.happysnappingturtle.mythicage.core.boss.MechanicWindows.Impact;
import io.github.happysnappingturtle.mythicage.core.boss.MoveScheduler.Move;
import io.github.happysnappingturtle.mythicage.core.boss.MoveScheduler.Situation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T23：血量、阶段、计时、机制窗口、选招、参战者（单元测试用例文档 TC-T23-01～22、34～39）。 */
class BossRulesTest {
    @Test
    void tc01to04Scaling() {
        assertEquals(320, BossScaling.maxHealth(320, 1));
        assertEquals(480, BossScaling.maxHealth(320, 2));
        assertEquals(800, BossScaling.maxHealth(320, 4));
        assertEquals(320, BossScaling.maxHealth(320, 0), "0 人按 1 算");
        assertEquals(320, BossScaling.maxHealth(320, -3));
        assertEquals(360, BossScaling.rescale(240, 320, 480), 1e-4, "比例 75% 不变");
        assertEquals(480, BossScaling.raise(480, BossScaling.maxHealth(320, 1)), "人数变少不降");
    }

    @Test
    void tc05to06Phases() {
        ArenaPhase p = ArenaPhase.IDLE;
        for (ArenaPhase to : new ArenaPhase[]{ArenaPhase.OPEN, ArenaPhase.FIGHT_P1, ArenaPhase.FIGHT_P2, ArenaPhase.VICTORY,
                ArenaPhase.RESETTING, ArenaPhase.IDLE}) {
            p = p.go(to);
        }
        assertEquals(ArenaPhase.IDLE, p);
        assertThrows(IllegalStateException.class, () -> ArenaPhase.IDLE.go(ArenaPhase.FIGHT_P1));
        assertThrows(IllegalStateException.class, () -> ArenaPhase.VICTORY.go(ArenaPhase.FIGHT_P2));
        assertThrows(IllegalStateException.class, () -> ArenaPhase.FIGHT_P2.go(ArenaPhase.FIGHT_P1));
        assertTrue(ArenaPhase.OPEN.canGo(ArenaPhase.IDLE), "OPEN 超时回 IDLE");
    }

    @Test
    void tc07to08Clock() {
        ArenaClock c = new ArenaClock();
        c.reset(1000);
        assertFalse(c.tick(false, 1599, ArenaClock.EMPTY_RESET), "599 刻不复原");
        assertFalse(c.tick(true, 1599, ArenaClock.EMPTY_RESET), "有人回来清零");
        assertFalse(c.tick(false, 2198, ArenaClock.EMPTY_RESET));
        assertTrue(c.tick(false, 2199, ArenaClock.EMPTY_RESET), "第 600 刻复原");
        ArenaClock o = new ArenaClock();
        o.reset(0);
        assertFalse(o.tick(false, 1199, ArenaClock.OPEN_TIMEOUT));
        assertTrue(o.tick(false, 1200, ArenaClock.OPEN_TIMEOUT), "1200 刻关门");
        ArenaClock away = new ArenaClock();
        away.reset(5000);
        assertTrue(away.tick(false, 90000, ArenaClock.EMPTY_RESET), "区块卸载期间不推进：回来第一刻就已超时");
    }

    @Test
    void reconnectInsideHallCannotReviveExpiredFight() {
        ArenaClock clock = new ArenaClock();
        clock.reset(1000);
        assertTrue(clock.tick(true, 1600, ArenaClock.EMPTY_RESET), "在大厅内重新上线也应先处理已过期的战斗");
        assertEquals(1000, clock.lastPresent(), "不能用新到场时间覆盖超时证据");
        clock.reset(1000);
        assertFalse(clock.tick(true, 1599, ArenaClock.EMPTY_RESET), "30 秒内回场仍可续战");
        assertEquals(1599, clock.lastPresent());
    }

    @Test
    void tc09to14Windows() {
        MechanicWindows w = new MechanicWindows();
        MechanicWindows.Outcome first = w.onImpact(Impact.INTACT_PILLAR, 1000, false);
        assertEquals(60, first.stunTicks());
        assertEquals(2.0F, first.multiplier());
        assertFalse(w.onImpact(Impact.INTACT_PILLAR, 1399, false).stunned(), "间隔 399：只碎柱");
        assertTrue(w.onImpact(Impact.INTACT_PILLAR, 1400, false).stunned(), "间隔 400：硬直");
        assertFalse(w.onImpact(Impact.WALL, 2000, false).stunned(), "还有完整的柱时撞墙不硬直");
        MechanicWindows.Outcome wall = w.onImpact(Impact.WALL, 2000, true);
        assertEquals(40, wall.stunTicks(), "柱全碎后撞墙 40 刻");
        MechanicWindows cap = new MechanicWindows();
        for (int i = 0; i < 12; i++) {
            assertTrue(cap.onImpact(Impact.WALL, i * 400L, true).stunned());
        }
        assertFalse(cap.onImpact(Impact.WALL, 12 * 400L, true).stunned(), "第 13 次不硬直");
        assertTrue(MechanicWindows.PILLAR_TICKS <= MechanicWindows.R1B_MAX_TICKS
                && MechanicWindows.WALL_TICKS <= MechanicWindows.R1B_MAX_TICKS
                && MechanicWindows.DAMAGE_MULTIPLIER <= MechanicWindows.R1B_MAX_MULTIPLIER, "R1b");
        assertTrue(MechanicWindows.PILLAR_TICKS <= 60, "R1：对 Boss 的硬直 ≤ 3 秒（卡片 MIN-06）");
    }

    static Situation s(boolean p2, double d, boolean los, boolean busy, boolean axe, boolean charge, boolean thr) {
        return new Situation(p2, d, los, busy, axe, charge, thr);
    }

    @Test
    void tc15to22Moves() {
        assertEquals(Move.AXE, MoveScheduler.choose(s(false, 3, true, false, true, true, true)));
        assertEquals(Move.CHASE, MoveScheduler.choose(s(false, 3, true, false, false, true, true)));
        assertEquals(Move.CHARGE, MoveScheduler.choose(s(false, 6, true, false, true, true, true)));
        assertEquals(Move.CHARGE, MoveScheduler.choose(s(false, 16, true, false, true, true, true)));
        assertEquals(Move.CHASE, MoveScheduler.choose(s(false, 17, true, false, true, true, true)));
        assertEquals(Move.DOUBLE_CHARGE, MoveScheduler.choose(s(true, 10, true, false, true, true, true)));
        assertEquals(Move.THROW, MoveScheduler.choose(s(true, 10, true, false, true, false, true)));
        assertEquals(Move.CHASE, MoveScheduler.choose(s(false, 10, true, false, true, false, true)), "P1 没有掷石");
        assertEquals(Move.CHASE, MoveScheduler.choose(s(false, 10, false, false, true, true, true)), "视线不通不冲锋");
        assertEquals(Move.NONE, MoveScheduler.choose(s(true, 3, true, true, true, true, true)), "出招中");
    }

    @Test
    void tc34to39Participants() {
        Participants p = new Participants();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID soldierOnly = UUID.randomUUID();
        UUID creative = UUID.randomUUID();
        p.damage(a, true);
        for (int i = 0; i < 100; i++) {
            p.present(a);
        }
        for (int i = 0; i < 599; i++) {
            p.present(b);
        }
        assertTrue(p.legal(a), "打过一下");
        assertFalse(p.legal(b), "599 刻");
        p.present(b);
        assertTrue(p.legal(b), "600 刻");
        p.damage(soldierOnly, false);
        for (int i = 0; i < 200; i++) {
            p.present(soldierOnly);
        }
        assertFalse(p.legal(soldierOnly), "只有士兵打过");
        p.damage(c, true);
        assertTrue(p.legal(c), "狼 / 召唤物 / 神力算本人（调用方传 own = true）");
        p.damage(creative, true);
        assertEquals(java.util.List.of(a, b, c), p.legal(id -> id.equals(creative)), "创造模式排除");
        Participants restored = new Participants();
        p.entries().forEach(restored::restore);
        assertTrue(restored.legal(b), "存档往返后保留（断线 / 重启）");
    }
}
