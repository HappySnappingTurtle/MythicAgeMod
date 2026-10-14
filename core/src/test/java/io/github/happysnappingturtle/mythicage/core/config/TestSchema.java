package io.github.happysnappingturtle.mythicage.core.config;

import com.google.gson.JsonObject;

import java.util.Map;

/** 测试用的最小 schema：覆盖整数、小数、枚举、嵌套对象和锁定字段。 */
final class TestSchema implements ConfigSchema<TestSchema.Value> {
    enum Mode { FAST, SLOW }

    record Inner(int count, boolean flag) {
    }

    record Value(int amount, double ratio, Mode mode, Inner inner, boolean locked) {
    }

    static final TestSchema INSTANCE = new TestSchema();
    static final Value DEFAULTS = new Value(10, 0.5, Mode.FAST, new Inner(3, true), false);
    private static final Map<String, Mode> MODES = Map.of("fast", Mode.FAST, "slow", Mode.SLOW);

    @Override
    public String name() {
        return "test";
    }

    @Override
    public int currentVersion() {
        return 1;
    }

    @Override
    public Value defaults() {
        return DEFAULTS;
    }

    @Override
    public Value read(ObjectReader root, Value base) {
        ObjectReader inner = root.object("inner");
        return new Value(
                root.intIn("amount", base.amount(), 0, 100),
                root.doubleIn("ratio", base.ratio(), 0, 1),
                root.choice("mode", base.mode(), MODES),
                new Inner(inner.intIn("count", base.inner().count(), 0, 9), inner.bool("flag", base.inner().flag())),
                root.lockedBool("locked", false));
    }

    @Override
    public JsonObject toJson(Value v) {
        JsonObject root = new JsonObject();
        root.addProperty("amount", v.amount());
        root.addProperty("ratio", v.ratio());
        root.addProperty("mode", v.mode() == Mode.FAST ? "fast" : "slow");
        JsonObject inner = new JsonObject();
        inner.addProperty("count", v.inner().count());
        inner.addProperty("flag", v.inner().flag());
        root.add("inner", inner);
        root.addProperty("locked", v.locked());
        return root;
    }
}
