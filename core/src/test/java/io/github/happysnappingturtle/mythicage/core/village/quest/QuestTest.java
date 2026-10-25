package io.github.happysnappingturtle.mythicage.core.village.quest;

import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T21 阶段 ①：任务框架与缺料供给的纯逻辑（单元测试用例文档 TC-T21-01～24）。 */
class QuestTest {
    static final UUID A = UUID.nameUUIDFromBytes("a".getBytes());
    static final UUID B = UUID.nameUUIDFromBytes("b".getBytes());

    static Quest quest(int n, String source) {
        return new Quest(n, "supply", source, "minecraft:copper_ingot", Quest.Scope.PUBLIC, 10, 6, 13);
    }

    // ───────────── 生命周期 ─────────────

    @Test
    void tc01LegalMoves() {
        Quest q = quest(1, "s");
        assertTrue(q.move(QuestState.PAUSED, 10, "disabled"));
        assertTrue(q.move(QuestState.OPEN, 10, ""));
        assertTrue(q.move(QuestState.COMPLETE, 11, ""));
        assertTrue(q.move(QuestState.SETTLED, 11, null));
        for (QuestState end : List.of(QuestState.INVALID, QuestState.EXPIRED)) {
            Quest x = quest(2, "s");
            assertTrue(x.move(end, 12, "r"));
            assertTrue(x.move(QuestState.SETTLED, 12, null));
        }
        assertTrue(quest(3, "s").move(QuestState.FAILED, 12, "dead"));
    }

    @Test
    void tc02IllegalMoves() {
        Quest settled = quest(1, "s");
        settled.move(QuestState.COMPLETE, 11, "");
        settled.move(QuestState.SETTLED, 11, null);
        for (QuestState to : QuestState.values()) {
            assertFalse(settled.move(to, 12, ""), "SETTLED → " + to);
        }
        Quest failed = quest(2, "s");
        failed.move(QuestState.FAILED, 11, "dead");
        for (QuestState to : QuestState.values()) {
            assertFalse(failed.move(to, 12, ""), "FAILED → " + to);
        }
        Quest paused = quest(3, "s");
        paused.move(QuestState.PAUSED, 11, "disabled");
        assertFalse(paused.move(QuestState.COMPLETE, 11, ""), "暂停时不能直接完成");
        Quest done = quest(4, "s");
        done.move(QuestState.COMPLETE, 11, "");
        assertFalse(done.move(QuestState.INVALID, 11, "x"));
        assertEquals(QuestState.COMPLETE, done.state);
    }

    @Test
    void tc03PauseExtendsDeadline() {
        Quest q = quest(1, "s");
        q.move(QuestState.PAUSED, 11, "disabled");
        q.move(QuestState.OPEN, 13, "");
        assertEquals(15, q.deadline, "暂停 2 日，截止日 +2");
        assertEquals(-1, q.pausedSince);
    }

    // ───────────── 上限与去重 ─────────────

    @Test
    void tc04AcceptLimit() {
        List<Quest> qs = List.of(quest(1, "a"), quest(2, "b"), quest(3, "c"), quest(4, "d"));
        qs.get(0).accepted.add(A);
        qs.get(1).accepted.add(A);
        assertEquals(2, QuestRules.activeFor(qs, A));
        assertEquals(Optional.empty(), QuestRules.canAccept(qs.get(2), A, QuestRules.activeFor(qs, A), 3), "第 3 个可以");
        qs.get(2).accepted.add(A);
        assertEquals(Optional.of(QuestRules.Refusal.FULL), QuestRules.canAccept(qs.get(3), A, QuestRules.activeFor(qs, A), 3),
                "第 4 个拒绝");
        assertEquals(Optional.of(QuestRules.Refusal.ALREADY), QuestRules.canAccept(qs.get(0), A, 3, 3));
    }

    @Test
    void tc05EndedFreesSlot() {
        List<Quest> qs = List.of(quest(1, "a"), quest(2, "b"), quest(3, "c"));
        qs.forEach(q -> q.accepted.add(A));
        qs.get(0).move(QuestState.COMPLETE, 11, "");
        qs.get(0).move(QuestState.SETTLED, 11, null);
        assertEquals(2, QuestRules.activeFor(qs, A), "终态不占名额");
    }

    @Test
    void tc06EventNoSlot() {
        Quest rescue = new Quest(9, "rescue", "rescue:1:x", "x", Quest.Scope.EVENT, 10, 1, -1);
        rescue.accepted.add(A);
        List<Quest> qs = List.of(quest(1, "a"), quest(2, "b"), quest(3, "c"), rescue);
        qs.forEach(q -> q.accepted.add(A));
        assertEquals(3, QuestRules.activeFor(qs, A), "事件任务不占名额");
        assertEquals(Optional.of(QuestRules.Refusal.EVENT), QuestRules.canAccept(rescue, B, 0, 3), "事件任务不用接");
    }

    @Test
    void tc07SourceDedup() {
        List<Quest> qs = List.of(quest(1, "supply:short:tin"));
        assertTrue(QuestRules.openSource(qs, "supply:short:tin"));
        assertEquals(List.of(), QuestRules.pick(qs, List.of(new QuestRules.Candidate("supply:short:tin", 9)), 5));
    }

    @Test
    void tc08SettledSourceCanReopen() {
        Quest old = quest(1, "supply:short:tin");
        old.move(QuestState.COMPLETE, 11, "");
        old.move(QuestState.SETTLED, 11, null);
        assertFalse(QuestRules.openSource(List.of(old), "supply:short:tin"));
        assertEquals(1, QuestRules.pick(List.of(old), List.of(new QuestRules.Candidate("supply:short:tin", 1)), 5).size());
    }

    @Test
    void tc09OpenCapAndOrder() {
        List<Quest> qs = List.of(quest(1, "x1"), quest(2, "x2"));
        List<QuestRules.Candidate> c = List.of(new QuestRules.Candidate("a", 1), new QuestRules.Candidate("b", 5),
                new QuestRules.Candidate("c", 3), new QuestRules.Candidate("d", 5), new QuestRules.Candidate("e", 0));
        List<QuestRules.Candidate> got = QuestRules.pick(qs, c, 5);
        assertEquals(List.of("b", "d", "c"), got.stream().map(QuestRules.Candidate::source).toList(),
                "已开 2 个，补到 5：按权重、同权重按来源键");
        assertEquals(0, QuestRules.pick(List.of(quest(1, "1"), quest(2, "2"), quest(3, "3"), quest(4, "4"), quest(5, "5")), c, 5).size(),
                "**边界**：已满 5 个");
    }

    @Test
    void tc10PersonalTaken() {
        Quest escort = new Quest(1, "escort", "escort:m", "m", Quest.Scope.PERSONAL, 10, 1, 11);
        escort.accepted.add(A);
        assertEquals(Optional.of(QuestRules.Refusal.TAKEN), QuestRules.canAccept(escort, B, 0, 3));
        escort.accepted.remove(A);
        assertEquals(Optional.empty(), QuestRules.canAccept(escort, B, 0, 3), "A 放弃后 B 可接");
    }

    // ───────────── 发布条件 ─────────────

    @Test
    void tc12StockingGrace() {
        assertFalse(QuestRules.stockingDue(10, 10, 1), "不到 1 日");
        assertTrue(QuestRules.stockingDue(10, 11, 1), "**边界**：恰好 1 日算到期");
        assertTrue(QuestRules.stockingDue(10, 12, 1));
        assertTrue(QuestRules.stockingDue(10, 10, 0), "宽限 0 = 立刻发");
    }

    @Test
    void tcExpiryAndPurge() {
        Quest q = quest(1, "s");
        assertFalse(QuestRules.expired(q, 12));
        assertTrue(QuestRules.expired(q, 13), "到截止日当天就过期");
        q.move(QuestState.FAILED, 13, "dead");
        assertFalse(QuestRules.purge(q, 15, 3));
        assertTrue(QuestRules.purge(q, 16, 3), "结束满 3 日清掉");
        Quest owing = quest(2, "s");
        owing.credit(A, 1, 1);
        owing.move(QuestState.COMPLETE, 13, "");
        assertFalse(QuestRules.purge(owing, 100, 3), "还有人没结算的不清");
    }

    // ───────────── 缺料供给 ─────────────

    @Test
    void tc16To19Deliverable() {
        assertEquals(12, SupplyRules.deliverable(12, 64, 100), "不多收");
        assertEquals(5, SupplyRules.deliverable(12, 5, 100));
        assertEquals(3, SupplyRules.deliverable(12, 64, 3), "村库只剩 3");
        assertEquals(0, SupplyRules.deliverable(0, 64, 100), "已被他人补上");
        assertEquals(0, SupplyRules.deliverable(12, 0, 100));
    }

    @Test
    void tcFoodUnits() {
        assertEquals(3, SupplyRules.foodUnits(5, 2.0), "还差 5 份、每件 2 份：收 3 件（最后一件可以补过头）");
        assertEquals(5, SupplyRules.foodUnits(5, 1.0));
        assertEquals(0, SupplyRules.foodUnits(0, 2.0));
        assertEquals(0, SupplyRules.foodUnits(4, 0));
    }

    @Test
    void tc21Payout() {
        Quest q = quest(1, "s");
        q.credit(A, 7 * 0.25, 7);
        q.credit(B, 5 * 0.25, 5);
        assertEquals(2, q.payout(A), "⌈1.75⌉");
        assertEquals(2, q.payout(B), "⌈1.25⌉");
        assertEquals(7, q.contrib.get(A));
        assertEquals(5, q.contrib.get(B));
        q.credit(A, 0.25, 1);
        assertEquals(2, q.payout(A), "累计 2.0 恰好 2");
    }

    @Test
    void tc22ZeroNoPayout() {
        Quest q = quest(1, "s");
        assertEquals(0, q.payout(A), "**边界**：没交的人不给");
        assertFalse(q.contributed(A));
        q.credit(B, 0, 3);
        assertEquals(0, q.payout(B), "单价 0：只有贡献");
        assertTrue(q.contributed(B));
    }

    @Test
    void tc23InvalidSettlesDelivered() {
        Quest q = quest(1, "s");
        q.credit(A, 3 * 0.25, 3);
        q.move(QuestState.INVALID, 12, "satisfied");
        assertTrue(q.owes(A), "失效也按已交的结算");
        assertEquals(1, q.payout(A));
        q.settled.add(A);
        assertTrue(q.allSettled());
        assertFalse(q.owes(A));
    }

    // ───────────── 文明数据与配置 ─────────────

    @Test
    void tcCivRulesParse() {
        QuestCivRules r = QuestCivRules.parse(JsonParser.parseString("""
                {"supplyItems": {"mythic_age:tin_ingot": 3}, "prices": {"default": 0.5, "mythic_age:tin_ingot": 2},
                 "escortReward": 4, "contribution": {"escort": 6}}""").getAsJsonObject());
        assertEquals(3, r.supplyItems().get("mythic_age:tin_ingot"));
        assertEquals(2.0, r.price("mythic_age:tin_ingot"));
        assertEquals(0.5, r.price("minecraft:oak_planks"), "没登记的用默认单价");
        assertEquals(4, r.escortReward());
        assertEquals(0, r.rescueReward(), "D5：救援默认不给绿宝石");
        assertEquals(6, r.contribution().escort());
        assertEquals(1, r.contribution().item(), "没写的权重用默认");
        assertEquals(0.125, r.repairPrice());
    }

    @Test
    void tcConfigDefaultsAndBounds() {
        VillagesConfig d = VillagesSchema.INSTANCE.defaults();
        assertEquals(VillagesConfig.Quests.DEFAULTS, d.quests());
        assertEquals(3, d.quests().maxActivePerPlayer(), "02 号 §6：每人每村 3 个");
        List<io.github.happysnappingturtle.mythicage.core.config.ConfigIssue> issues = new java.util.ArrayList<>();
        VillagesConfig c = VillagesSchema.INSTANCE.read(io.github.happysnappingturtle.mythicage.core.config.ObjectReader.root(
                JsonParser.parseString("""
                        {"quests": {"enabled": false, "maxActivePerPlayer": 99, "supplyDays": 7}}""").getAsJsonObject(), issues),
                VillagesSchema.INSTANCE.defaults());
        assertFalse(issues.isEmpty(), "越界给警告");
        assertFalse(c.quests().enabled());
        assertEquals(3, c.quests().maxActivePerPlayer(), "越界用默认值");
        assertEquals(7, c.quests().supplyDays());
    }
}
