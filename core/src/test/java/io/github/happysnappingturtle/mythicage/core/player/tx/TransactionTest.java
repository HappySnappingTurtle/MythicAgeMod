package io.github.happysnappingturtle.mythicage.core.player.tx;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    private static PlayerMythData withFavor(long favor) {
        PlayerMythData d = new PlayerMythData();
        d.civ(GREEK).setAge(Age.ARCHAIC);
        d.civ(GREEK).setFavor(favor);
        return d;
    }

    /** 记录调用顺序的测试步骤。 */
    private static TxStep recording(List<String> log, String name, String failure) {
        return new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                log.add("check " + name);
                return Optional.ofNullable(failure);
            }

            @Override
            public void apply(PlayerMythData data) {
                log.add("apply " + name);
            }
        };
    }

    @Test
    void tc_t4_20_commitAppliesAllAndWritesReceipt() {
        PlayerMythData d = withFavor(50);
        long v = d.dataVersion();
        var r = Transactions.execute(d, Transactions.Request.of("req-1",
                TxSteps.spendFavor(GREEK, 20), TxSteps.unlockBlueprint("harpe")));
        assertEquals(Transactions.Status.COMMITTED, r.status());
        assertEquals(30, d.civ(GREEK).favor());
        assertTrue(d.blueprints().contains("harpe"));
        assertEquals(v + 1, d.dataVersion());
        assertTrue(d.receipts().contains(ReceiptBook.Scope.REQUEST, "req-1"));
    }

    @Test
    void tc_t4_21_duplicateRequestIsIgnored() {
        PlayerMythData d = withFavor(50);
        Transactions.execute(d, Transactions.Request.of("req-1", TxSteps.spendFavor(GREEK, 20)));
        long v = d.dataVersion();
        var r = Transactions.execute(d, Transactions.Request.of("req-1", TxSteps.spendFavor(GREEK, 20)));
        assertEquals(Transactions.Status.DUPLICATE, r.status());
        assertEquals(30, d.civ(GREEK).favor());
        assertEquals(v, d.dataVersion());
    }

    @Test
    void tc_t4_22_rejectedAppliesNothing() {
        PlayerMythData d = withFavor(10);
        var r = Transactions.execute(d, Transactions.Request.of("req-1",
                TxSteps.spendFavor(GREEK, 20), TxSteps.unlockBlueprint("harpe")));
        assertEquals(Transactions.Status.REJECTED, r.status());
        assertTrue(r.reason().contains("恩宠不足"), r.reason());
        assertEquals(10, d.civ(GREEK).favor());
        assertFalse(d.blueprints().contains("harpe"));
        assertFalse(d.receipts().contains(ReceiptBook.Scope.REQUEST, "req-1"));
    }

    @Test
    void tc_t4_23_allChecksRunBeforeAnyApply() {
        List<String> log = new ArrayList<>();
        var r = Transactions.execute(withFavor(0), Transactions.Request.of("req",
                recording(log, "a", null), recording(log, "b", null), recording(log, "c", "不行")));
        assertEquals(Transactions.Status.REJECTED, r.status());
        assertEquals(List.of("check a", "check b", "check c"), log);
    }

    @Test
    void tc_t4_24_requireAge() {
        PlayerMythData d = withFavor(100);
        var r = Transactions.execute(d, Transactions.Request.of("req",
                TxSteps.requireAge(GREEK, Age.CLASSICAL)));
        assertEquals(Transactions.Status.REJECTED, r.status());
        assertTrue(r.reason().contains("时代不足"));
    }

    @Test
    void tc_t4_25_rejectedRequestCanBeRetried() {
        PlayerMythData d = withFavor(10);
        assertEquals(Transactions.Status.REJECTED,
                Transactions.execute(d, Transactions.Request.of("req", TxSteps.spendFavor(GREEK, 20))).status());
        d.civ(GREEK).setFavor(40);
        assertEquals(Transactions.Status.COMMITTED,
                Transactions.execute(d, Transactions.Request.of("req", TxSteps.spendFavor(GREEK, 20))).status());
        assertEquals(20, d.civ(GREEK).favor());
    }

    @Test
    void tc_t4_26_requestReceiptsAreBounded() {
        PlayerMythData d = withFavor(0);
        for (int i = 0; i <= ReceiptBook.REQUEST_CAPACITY; i++) {
            Transactions.execute(d, Transactions.Request.of("r" + i));
        }
        assertEquals(ReceiptBook.REQUEST_CAPACITY, d.receipts().size(ReceiptBook.Scope.REQUEST));
        assertFalse(d.receipts().contains(ReceiptBook.Scope.REQUEST, "r0"), "最早的一条应被淘汰");
        for (int i = 1; i <= ReceiptBook.REQUEST_CAPACITY; i++) {
            assertEquals(Transactions.Status.DUPLICATE,
                    Transactions.execute(d, Transactions.Request.of("r" + i)).status(), "r" + i);
        }
    }

    @Test
    void tc_t4_27_uniqueReceiptsAreKept() {
        PlayerMythData d = withFavor(0);
        for (int i = 0; i < 1000; i++) {
            Transactions.execute(d, Transactions.Request.unique("reward-" + i));
        }
        assertEquals(1000, d.receipts().size(ReceiptBook.Scope.UNIQUE));
    }

    @Test
    void tc_t4_28_applyFailurePropagatesWithoutReceipt() {
        PlayerMythData d = withFavor(0);
        long v = d.dataVersion();
        TxStep boom = new TxStep() {
            @Override
            public Optional<String> check(PlayerMythData data) {
                return Optional.empty();
            }

            @Override
            public void apply(PlayerMythData data) {
                throw new IllegalStateException("背包写入失败");
            }
        };
        assertThrows(IllegalStateException.class, () -> Transactions.execute(d, Transactions.Request.of("req", boom)));
        assertFalse(d.receipts().contains(ReceiptBook.Scope.REQUEST, "req"));
        assertEquals(v, d.dataVersion());
    }

    @Test
    void tc_t4_29_scopesAreIndependent() {
        PlayerMythData d = withFavor(0);
        Transactions.execute(d, Transactions.Request.unique("same-id"));
        assertEquals(Transactions.Status.COMMITTED, Transactions.execute(d, Transactions.Request.of("same-id")).status());
    }

    @Test
    void addFavorRespectsCapAndBonus() {
        PlayerMythData d = withFavor(95);
        Transactions.execute(d, Transactions.Request.of("a", TxSteps.addFavor(GREEK, 30, 0)));
        assertEquals(100, d.civ(GREEK).favor());
        Transactions.execute(d, Transactions.Request.of("b", TxSteps.addFavor(GREEK, 30, 20)));
        assertEquals(120, d.civ(GREEK).favor());
        assertEquals(0, d.civ(Civilization.NORSE).favor(), "其他文明不受影响");
    }
}
