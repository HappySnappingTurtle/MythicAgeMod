package io.github.happysnappingturtle.mythicage.shrine;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 神庙种类表：各文明在内容初始化时登记（T10b 设计文档 §3.1）。 */
public final class ShrineKinds {
    private static final Map<String, ShrineKind> KINDS = new LinkedHashMap<>();
    private static final Map<String, ShrineKind> TEST = new LinkedHashMap<>();

    private ShrineKinds() {
    }

    public static synchronized ShrineKind register(ShrineKind kind) {
        if (KINDS.putIfAbsent(kind.id(), kind) != null) {
            throw new IllegalStateException("重复的神庙种类：" + kind.id());
        }
        return kind;
    }

    public static Optional<ShrineKind> byId(String id) {
        ShrineKind t = TEST.get(id);
        return Optional.ofNullable(t != null ? t : KINDS.get(id));
    }

    /** 方块对应的神庙种类；不是神庙方块时为空。 */
    public static Optional<ShrineKind> byBlock(Block block) {
        for (ShrineKind k : all()) {
            if (k.block().get() == block) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }

    public static List<ShrineKind> of(Civilization civ) {
        return all().stream().filter(k -> k.civ() == civ).toList();
    }

    public static List<ShrineKind> all() {
        Map<String, ShrineKind> m = new LinkedHashMap<>(KINDS);
        m.putAll(TEST);
        return List.copyOf(m.values());
    }

    /** 只供 GameTest：临时登记一个测试种类；传 null 清除全部测试种类。 */
    public static synchronized void overrideForTest(@Nullable ShrineKind kind) {
        if (kind == null) {
            TEST.clear();
        } else {
            TEST.put(kind.id(), kind);
        }
    }
}
