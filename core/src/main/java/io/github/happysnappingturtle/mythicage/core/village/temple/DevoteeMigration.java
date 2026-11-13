package io.github.happysnappingturtle.mythicage.core.village.temple;

import java.util.Collection;
import java.util.Optional;

/**
 * 旧存档迁移时给受过祝福的玩家选村（GA-13 §2.7）：旧祭坛在哪个村的村界内就是那个村；
 * 都不在就取 {@link #RANGE} 格内最近的村子；再没有就不指定。
 */
public final class DevoteeMigration {
    /** 旧祭坛不在任何村界内时，找最近村子的水平距离上限。 */
    public static final double RANGE = 100;

    private DevoteeMigration() {
    }

    /**
     * 一个候选村子。
     *
     * @param inBounds 旧祭坛在它的村界内
     * @param distance 旧祭坛到村中心的水平距离
     */
    public record Candidate<K>(K village, boolean inBounds, double distance) {
    }

    public static <K> Optional<K> choose(Collection<Candidate<K>> candidates) {
        Candidate<K> inside = null;
        Candidate<K> nearest = null;
        for (Candidate<K> c : candidates) {
            if (c.inBounds() && (inside == null || c.distance() < inside.distance())) {
                inside = c;
            }
            if (c.distance() <= RANGE && (nearest == null || c.distance() < nearest.distance())) {
                nearest = c;
            }
        }
        if (inside != null) {
            return Optional.of(inside.village());
        }
        return nearest == null ? Optional.empty() : Optional.of(nearest.village());
    }
}
