package io.github.happysnappingturtle.mythicage.runtime;

import io.github.happysnappingturtle.mythicage.core.runtime.ProducerId;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerLedger;
import io.github.happysnappingturtle.mythicage.core.runtime.Ticks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试产出点（类型 {@code test}）：验证运行时框架用，没有方块。
 * 是否真实运行由命令或 GameTest 设定（只在内存中，默认否）；真实运行时按公式估计产出。
 */
public final class TestProducers {
    public static final String TYPE = "test";
    private static final Map<ProducerId, Boolean> REAL = new ConcurrentHashMap<>();

    private TestProducers() {
    }

    public static void register() {
        ProducerProbes.register(TYPE, (server, ledger, owner, now, interval) -> observe(ledger, interval));
    }

    public static void setRealRunning(ProducerId id, boolean real) {
        REAL.put(id, real);
    }

    public static void clear() {
        REAL.clear();
    }

    private static ProducerProbe.Observation observe(ProducerLedger ledger, long interval) {
        if (!REAL.getOrDefault(ledger.id(), false)) {
            return ProducerProbe.Observation.NOT_RUNNING;
        }
        return new ProducerProbe.Observation(true, false, Ticks.perDay(ledger.estimatedRatePerDay(), interval));
    }
}
