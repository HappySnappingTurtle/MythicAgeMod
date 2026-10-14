package io.github.happysnappingturtle.mythicage.core.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigHolderTest {
    private static ConfigHolder<TestSchema.Value> holder() {
        return ConfigHolder.create(TestSchema.INSTANCE, ConfigLoader.load(TestSchema.INSTANCE, "{}", null));
    }

    @Test
    void tc_t3_20_applyCallsHooksAndListeners() {
        var h = holder();
        List<String> calls = new ArrayList<>();
        h.addPreApplyHook((o, n) -> calls.add("hook " + o.amount() + "->" + n.amount()));
        h.addListener((o, n, v) -> calls.add("listener v" + v));
        var out = h.reload("{\"amount\":20}");
        assertEquals(ConfigHolder.Status.APPLIED, out.status());
        assertEquals(2, out.version());
        assertEquals(20, h.current().amount());
        assertEquals(List.of("hook 10->20", "listener v2"), calls);
    }

    @Test
    void tc_t3_21_equalValueIsUnchanged() {
        var h = holder();
        AtomicBoolean called = new AtomicBoolean();
        h.addPreApplyHook((o, n) -> called.set(true));
        h.addListener((o, n, v) -> called.set(true));
        var out = h.reload("{\"amount\":10}");
        assertEquals(ConfigHolder.Status.UNCHANGED, out.status());
        assertEquals(1, h.version());
        assertFalse(called.get());
    }

    @Test
    void tc_t3_22_unparsableKeepsOld() {
        var h = holder();
        AtomicBoolean called = new AtomicBoolean();
        h.addPreApplyHook((o, n) -> called.set(true));
        var out = h.reload("{ broken");
        assertEquals(ConfigHolder.Status.KEPT_OLD, out.status());
        assertEquals(1, h.version());
        assertEquals(TestSchema.DEFAULTS, h.current());
        assertFalse(called.get());
    }

    @Test
    void tc_t3_23_failingHookAbortsWithoutApplying() {
        var h = holder();
        List<String> calls = new ArrayList<>();
        h.addPreApplyHook((o, n) -> calls.add("first"));
        h.addPreApplyHook((o, n) -> {
            throw new IllegalStateException("settlement failed");
        });
        h.addListener((o, n, v) -> calls.add("listener"));
        var out = h.reload("{\"amount\":30}");
        assertEquals(ConfigHolder.Status.ABORTED, out.status());
        assertTrue(out.reason().contains("settlement failed"));
        assertEquals(1, h.version());
        assertEquals(10, h.current().amount());
        assertEquals(List.of("first"), calls);
    }

    @Test
    void tc_t3_24_failingListenerDoesNotRollBack() {
        var h = holder();
        h.addListener((o, n, v) -> {
            throw new IllegalStateException("listener boom");
        });
        var out = h.reload("{\"amount\":40}");
        assertEquals(ConfigHolder.Status.APPLIED, out.status());
        assertEquals(40, h.current().amount());
        assertTrue(out.issues().stream().anyMatch(i -> i.message().contains("listener boom")));
    }

    @Test
    void tc_t3_26_appliedResultCarriesWarnings() {
        var h = holder();
        var out = h.reload("{\"amount\":50,\"ratio\":7}");
        assertEquals(ConfigHolder.Status.APPLIED, out.status());
        assertEquals(1, out.issues().size());
        assertEquals("ratio", out.issues().get(0).path());
    }

    /** 测试类型：两个字段必须始终相等，用来检测读者是否看到「撕裂」的值。 */
    private record Pair(int a, int b) {
    }

    private static final class PairSchema implements ConfigSchema<Pair> {
        @Override
        public String name() {
            return "pair";
        }

        @Override
        public int currentVersion() {
            return 1;
        }

        @Override
        public Pair defaults() {
            return new Pair(0, 0);
        }

        @Override
        public Pair read(ObjectReader root, Pair base) {
            int v = root.intIn("v", 0, 0, Integer.MAX_VALUE);
            return new Pair(v, v);
        }

        @Override
        public JsonObject toJson(Pair value) {
            JsonObject o = new JsonObject();
            o.addProperty("v", value.a());
            return o;
        }
    }

    @Test
    void tc_t3_25_readersNeverSeeTornValues() throws InterruptedException {
        PairSchema schema = new PairSchema();
        ConfigHolder<Pair> h = ConfigHolder.create(schema, ConfigLoader.load(schema, "{}", null));
        AtomicBoolean stop = new AtomicBoolean();
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        List<Thread> readers = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Thread t = new Thread(() -> {
                try {
                    while (!stop.get()) {
                        Pair p = h.current();
                        if (p.a() != p.b()) {
                            failures.add(new AssertionError("torn value " + p));
                        }
                    }
                } catch (Throwable e) {
                    failures.add(e);
                }
            });
            readers.add(t);
            t.start();
        }
        for (int i = 1; i <= 2000; i++) {
            h.reload("{\"v\":" + i + "}");
        }
        stop.set(true);
        for (Thread t : readers) {
            t.join();
        }
        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals(2001, h.version());
    }
}
