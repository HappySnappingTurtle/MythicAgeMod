package io.github.happysnappingturtle.mythicage.rite;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 建筑条件注册表（各文明在内容初始化时登记）。 */
public final class PromotionRequirements {
    private static final Map<String, PromotionRequirement> ALL = new LinkedHashMap<>();
    private static final Map<String, PromotionRequirement> TEST = new LinkedHashMap<>();

    private PromotionRequirements() {
    }

    public static synchronized void register(PromotionRequirement r) {
        if (ALL.putIfAbsent(r.type(), r) != null) {
            throw new IllegalStateException("重复的建筑条件：" + r.type());
        }
    }

    public static synchronized Optional<PromotionRequirement> get(String type) {
        PromotionRequirement t = TEST.get(type);
        return Optional.ofNullable(t != null ? t : ALL.get(type));
    }

    public static boolean known(String type) {
        return get(type).isPresent();
    }

    /** 只供 GameTest：临时登记；传 null 清除全部测试条件。 */
    public static synchronized void overrideForTest(@Nullable PromotionRequirement r) {
        if (r == null) {
            TEST.clear();
        } else {
            TEST.put(r.type(), r);
        }
    }
}
