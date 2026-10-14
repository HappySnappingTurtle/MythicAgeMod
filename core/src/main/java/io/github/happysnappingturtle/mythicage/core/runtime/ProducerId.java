package io.github.happysnappingturtle.mythicage.core.runtime;

import java.util.Objects;

/** 产出点标识，例如 {@code shrine:minecraft:overworld:12,64,-30}。 */
public record ProducerId(String value) {
    public ProducerId {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException("产出点 id 不能为空");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
