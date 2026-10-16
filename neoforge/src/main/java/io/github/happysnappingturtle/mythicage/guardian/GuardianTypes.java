package io.github.happysnappingturtle.mythicage.guardian;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 文明 → 祭司实体类型（T10b 设计文档 §4.2）。各文明在内容初始化时登记；
 * 没有登记的文明不生成祭司。
 */
public final class GuardianTypes {
    private static final Map<Civilization, Supplier<? extends EntityType<? extends GuardianEntity>>> TYPES =
            new EnumMap<>(Civilization.class);
    private static final Map<Civilization, Supplier<? extends EntityType<? extends GuardianEntity>>> TEST =
            new EnumMap<>(Civilization.class);

    private GuardianTypes() {
    }

    public static synchronized void register(Civilization civ, Supplier<? extends EntityType<? extends GuardianEntity>> type) {
        if (TYPES.putIfAbsent(civ, type) != null) {
            throw new IllegalStateException("文明 " + civ.id() + " 已登记祭司类型");
        }
    }

    public static synchronized Optional<EntityType<? extends GuardianEntity>> typeFor(Civilization civ) {
        var t = TEST.containsKey(civ) ? TEST.get(civ) : TYPES.get(civ);
        return t == null ? Optional.empty() : Optional.of(t.get());
    }

    /** 实体类型所属的文明（只看正式登记；测试替换不影响）。没有登记的祭司类型是编程错误。 */
    public static synchronized Civilization civOf(EntityType<?> type) {
        for (var e : TYPES.entrySet()) {
            if (e.getValue().get() == type) {
                return e.getKey();
            }
        }
        throw new IllegalStateException("祭司实体类型 " + EntityType.getKey(type) + " 没有在 GuardianTypes 登记文明");
    }

    /** 只供 GameTest：让某文明临时使用给定的祭司类型；type 为 null 时清除全部替换。 */
    public static synchronized void overrideForTest(Civilization civ,
                                                    @Nullable Supplier<? extends EntityType<? extends GuardianEntity>> type) {
        if (type == null) {
            TEST.clear();
        } else {
            TEST.put(civ, type);
        }
    }
}
