package io.github.happysnappingturtle.mythicage.power;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 神力效果注册表：各文明在内容初始化时登记（T13 起）。 */
public final class GodPowers {
    private static final Map<String, GodPower> ALL = new LinkedHashMap<>();
    private static final Map<String, GodPower> TEST = new HashMap<>();

    private GodPowers() {
    }

    public static synchronized void register(GodPower p) {
        if (ALL.putIfAbsent(p.id(), p) != null) {
            throw new IllegalStateException("重复的神力：" + p.id());
        }
    }

    public static synchronized Optional<GodPower> get(String id) {
        GodPower t = TEST.get(id);
        return Optional.ofNullable(t != null ? t : ALL.get(id));
    }

    /** 只供 GameTest：临时登记；传 null 清除全部测试神力。 */
    public static synchronized void overrideForTest(@Nullable GodPower p) {
        if (p == null) {
            TEST.clear();
        } else {
            TEST.put(p.id(), p);
        }
    }
}
