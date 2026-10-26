package io.github.happysnappingturtle.mythicage.core.structure;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T22：遗迹宝库规则（单元测试用例文档 TC-T22-10～14）。 */
class VaultRulesTest {
    static final String CROWN = "mythic_age:rheia_crown";
    static final String STATUE = "mythic_age:pygmalion_statue";

    @Test
    void tc10ReceiptIds() {
        assertEquals("vault:minecraft:overworld:1,64,-3", VaultRules.receipt("minecraft:overworld", 1, 64, -3));
        assertEquals("relic:" + CROWN, VaultRules.relicReceipt(CROWN));
    }

    @Test
    void tc11PrefersUnowned() {
        for (int seed = 0; seed < 50; seed++) {
            assertEquals(Optional.of(STATUE), VaultRules.pickRelic(List.of(CROWN, STATUE), CROWN::equals, new Random(seed)),
                    "拿过冠，就抽到雕像");
        }
    }

    @Test
    void tc12AllOwnedStillGivesOne() {
        Set<String> seen = new java.util.HashSet<>();
        Random random = new Random(7);
        for (int i = 0; i < 50; i++) {
            Optional<String> r = VaultRules.pickRelic(List.of(CROWN, STATUE), x -> true, random);
            assertTrue(r.isPresent(), "两件都拿过也给一件");
            seen.add(r.get());
        }
        assertEquals(2, seen.size(), "在整个池里随机");
    }

    @Test
    void tc13EmptyPool() {
        assertFalse(VaultRules.pickRelic(List.of(), x -> false, new Random(1)).isPresent());
    }

    @Test
    void tc14OpenedListCapped() {
        LinkedHashSet<UUID> opened = new LinkedHashSet<>();
        UUID first = UUID.randomUUID();
        VaultRules.remember(opened, first);
        for (int i = 0; i < VaultRules.MAX_OPENED; i++) {
            VaultRules.remember(opened, UUID.randomUUID());
        }
        assertEquals(VaultRules.MAX_OPENED, opened.size());
        assertFalse(opened.contains(first), "满了丢掉最早的");
    }
}
