package io.github.happysnappingturtle.mythicage.core.player;

import java.util.Locale;
import java.util.Optional;

/** 五个文明。{@link #id()} 用于存档、网络与命令参数，不随枚举顺序变化。 */
public enum Civilization {
    GREEK, EGYPTIAN, NORSE, CHINESE, ATLANTEAN;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<Civilization> byId(String id) {
        for (Civilization c : values()) {
            if (c.id().equals(id)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
