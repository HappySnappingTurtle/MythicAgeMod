package io.github.happysnappingturtle.mythicage.core.village;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;

/** 村庄阶段提升前后的数值差（T24 设计文档 §4.5，同 12 号 §4.3 A「旧 → 新」的做法）：只列有变化的项。 */
public final class StageChanges {
    private StageChanges() {
    }

    public record Change(String key, int before, int after) {
    }

    /** 键按 before 的顺序，之后是只在 after 里的键；只在一边出现的键，另一边按 0 算。 */
    public static List<Change> diff(Map<String, Integer> before, Map<String, Integer> after) {
        LinkedHashSet<String> keys = new LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        List<Change> out = new ArrayList<>();
        for (String k : keys) {
            int b = before.getOrDefault(k, 0);
            int a = after.getOrDefault(k, 0);
            if (a != b) {
                out.add(new Change(k, b, a));
            }
        }
        return List.copyOf(out);
    }
}
