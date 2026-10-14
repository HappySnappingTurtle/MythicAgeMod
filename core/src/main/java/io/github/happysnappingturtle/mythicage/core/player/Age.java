package io.github.happysnappingturtle.mythicage.core.player;

import java.util.Locale;
import java.util.Optional;

/**
 * 信仰时代（总纲 §3.4）。每个玩家在每个文明上各有独立的时代；NONE 表示尚未与该文明守护者交谈。
 * 基础恩宠上限见总纲 §3.1。
 */
public enum Age {
    NONE(0), ARCHAIC(100), CLASSICAL(150), HEROIC(200), MYTHIC(300);

    private final long baseCap;

    Age(long baseCap) {
        this.baseCap = baseCap;
    }

    public long baseCap() {
        return baseCap;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean atLeast(Age other) {
        return ordinal() >= other.ordinal();
    }

    public static Optional<Age> byId(String id) {
        for (Age a : values()) {
            if (a.id().equals(id)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }
}
