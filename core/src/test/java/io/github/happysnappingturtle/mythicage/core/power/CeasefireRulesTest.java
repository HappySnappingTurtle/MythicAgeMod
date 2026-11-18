package io.github.happysnappingturtle.mythicage.core.power;

import io.github.happysnappingturtle.mythicage.core.power.CeasefireRules.Party;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T25a 停火（TC-T25a-01～12；05、09 的归属与 Boss 计数的键在平台层，GameTest 覆盖）。 */
class CeasefireRulesTest {
    private static final Party RELEASED = new Party(true, true);

    @Test
    void tc01BothInsideBlocked() {
        assertTrue(CeasefireRules.blocks(Party.INSIDE, Party.INSIDE, false));
    }

    @Test
    void tc02EnvironmentPasses() {
        assertFalse(CeasefireRules.blocks(Party.INSIDE, Party.INSIDE, true), "岩浆、摔落、溺水、饥饿照常");
    }

    @Test
    void tc03OneSideInside() {
        assertTrue(CeasefireRules.blocks(Party.OUTSIDE, Party.INSIDE, false), "区外打区内");
        assertTrue(CeasefireRules.blocks(Party.INSIDE, Party.OUTSIDE, false), "区内打区外");
        assertTrue(CeasefireRules.breaches(Party.OUTSIDE, Party.INSIDE), "区外攻击区内目标也算破约");
        assertFalse(CeasefireRules.blocks(Party.OUTSIDE, Party.OUTSIDE, false), "两边都在区外不管");
        assertFalse(CeasefireRules.breaches(Party.OUTSIDE, Party.OUTSIDE));
    }

    @Test
    void tc04AttackInsideBreaches() {
        assertTrue(CeasefireRules.breaches(Party.INSIDE, Party.INSIDE));
        assertTrue(CeasefireRules.breaches(Party.INSIDE, Party.OUTSIDE), "射向区外也算");
        // 破约后休战结束：active 为假，后续伤害平台层不再拦
        assertFalse(CeasefireRules.active(100, 600, true));
    }

    @Test
    void tc05SameRuleAfterAttribution() {
        // 召唤、宠物、投射物、神力、士兵都由平台层归到主人或射手，归属后的处境照同一规则
        Party owner = Party.INSIDE;
        assertTrue(CeasefireRules.breaches(owner, Party.OUTSIDE));
        assertTrue(CeasefireRules.blocks(owner, Party.OUTSIDE, false));
    }

    @Test
    void tc06ProjectileFromBefore() {
        // 休战前射出的箭命中区内目标：拦（伤害取消）；射出时休战还没开始，没有破约事件
        assertTrue(CeasefireRules.blocks(Party.OUTSIDE, Party.INSIDE, false));
    }

    @Test
    void tc07BossFirstCast() {
        int held = CeasefireRules.bossTicks(0);
        assertEquals(160, held);
        long start = 1000;
        assertTrue(CeasefireRules.bossHeld(start + 159, start + held));
        assertFalse(CeasefireRules.bossHeld(start + 160, start + held), "第 8 秒后退出");
        // 退出后：Boss 按区外算，Boss 打参与者不拦也不算破约
        assertFalse(CeasefireRules.blocks(RELEASED, RELEASED, false));
        assertFalse(CeasefireRules.breaches(RELEASED, RELEASED));
        // 原版 Boss 没有参与者：已放出的 Boss 与区内的玩家之间也不拦、不算破约
        Party boss = Party.freeBoss(true);
        assertFalse(CeasefireRules.blocks(boss, Party.INSIDE, false), "Boss 照常打人");
        assertFalse(CeasefireRules.blocks(Party.INSIDE, boss, false), "人照常打 Boss");
        assertFalse(CeasefireRules.breaches(Party.INSIDE, boss), "打 Boss 不算破约");
        assertFalse(CeasefireRules.breaches(boss, Party.INSIDE), "Boss 攻击不算破约");
    }

    @Test
    void tc08BossSecondCast() {
        assertEquals(0, CeasefireRules.bossTicks(1));
        assertEquals(0, CeasefireRules.bossTicks(3));
        // 场边小怪照常 30 秒：被放出的参与者打它仍拦、仍算破约
        assertTrue(CeasefireRules.blocks(RELEASED, Party.INSIDE, false));
        assertTrue(CeasefireRules.breaches(RELEASED, Party.INSIDE));
    }

    @Test
    void tc10EliteFull30Seconds() {
        assertTrue(CeasefireRules.active(599, 600, false));
        assertFalse(CeasefireRules.active(600, 600, false));
        assertFalse(CeasefireRules.active(300, 600, true), "可被破约提前结束");
    }

    @Test
    void tc11CasterBuff() {
        assertTrue(CeasefireRules.casterBuff(true, true));
        assertFalse(CeasefireRules.casterBuff(false, true), "结束或破约");
        assertFalse(CeasefireRules.casterBuff(true, false), "离开区域");
    }

    @Test
    void tc12Spawns() {
        for (String t : new String[]{"NATURAL", "CHUNK_GENERATION", "PATROL", "SPAWNER", "TRIAL_SPAWNER", "REINFORCEMENT", "EVENT",
                "STRUCTURE", "JOCKEY", "MOB_SUMMONED", "TRIGGERED"}) {
            assertTrue(CeasefireRules.blocksSpawn(true, t), t);
        }
        for (String t : new String[]{"BREEDING", "COMMAND", "SPAWN_EGG", "DISPENSER", "BUCKET", "CONVERSION"}) {
            assertFalse(CeasefireRules.blocksSpawn(true, t), t);
        }
        assertFalse(CeasefireRules.blocksSpawn(false, "NATURAL"), "非敌对不拦");
    }
}
