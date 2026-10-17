package io.github.happysnappingturtle.mythicage.core.rite;

import java.util.Locale;
import java.util.Optional;

/** 三种仪式（T11a 设计文档 §4.1；20 号 §1 通用名）：晋升、立祀、改奉。 */
public enum RiteKind {
    PROMOTION, CONSECRATION, REDEDICATION;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<RiteKind> byId(String id) {
        for (RiteKind k : values()) {
            if (k.id().equals(id)) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }
}
