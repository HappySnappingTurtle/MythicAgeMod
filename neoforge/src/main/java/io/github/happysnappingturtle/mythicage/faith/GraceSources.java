package io.github.happysnappingturtle.mythicage.faith;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 恩宠来源表：各文明在内容初始化时登记。 */
public final class GraceSources {
    private static final Map<String, GraceSource> SOURCES = new LinkedHashMap<>();

    private GraceSources() {
    }

    public static synchronized GraceSource register(GraceSource source) {
        if (!source.id().startsWith(source.civ().id() + ":")) {
            throw new IllegalArgumentException("恩宠来源 " + source.id() + " 不属于文明 " + source.civ().id());
        }
        if (SOURCES.putIfAbsent(source.id(), source) != null) {
            throw new IllegalStateException("重复的恩宠来源：" + source.id());
        }
        return source;
    }

    public static synchronized List<GraceSource> of(Civilization civ) {
        return SOURCES.values().stream().filter(s -> s.civ() == civ).toList();
    }
}
