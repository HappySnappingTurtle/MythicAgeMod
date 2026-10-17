package io.github.happysnappingturtle.mythicage.faith;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 恩宠来源表：各文明在内容初始化时登记。 */
public final class GraceSources {
    private static final Map<String, GraceSource> SOURCES = new LinkedHashMap<>();
    private static final Map<Civilization, List<GraceSource>> TEST = new java.util.EnumMap<>(Civilization.class);

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
        List<GraceSource> test = TEST.get(civ);
        if (test != null) {
            return test;
        }
        return SOURCES.values().stream().filter(s -> s.civ() == civ).toList();
    }

    /** 仅供 GameTest：替换某文明的来源列表；传 null 恢复。 */
    public static synchronized void overrideForTest(Civilization civ, @Nullable List<GraceSource> sources) {
        if (sources == null) {
            TEST.remove(civ);
        } else {
            TEST.put(civ, List.copyOf(sources));
        }
    }
}
