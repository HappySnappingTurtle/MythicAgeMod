package io.github.happysnappingturtle.mythicage.core.power;

import java.util.Locale;
import java.util.Optional;

/**
 * 公共目标入口（05 号 §5.1）：决定确认时选的是单个实体、一个点，还是以施法者为中心。
 */
public enum TargetProfile {
    HOSTILE_1(Pick.ENTITY), HOSTILE_AREA(Pick.POINT), ALLY_AREA(Pick.POINT), ANY_AUTH_ENTITY(Pick.ENTITY),
    ANIMAL_AREA(Pick.POINT), OWNER_SUMMON(Pick.ENTITY), PLACEMENT(Pick.BLOCK), STRUCTURE_OWNED(Pick.BLOCK),
    WORLD_AREA(Pick.POINT), TWO_STAGE(Pick.POINT), FREE_FOR_ALL(Pick.POINT);

    /** 确认时选什么：单个实体、一个点（方块或空中落点）、一个方块面（放置）。 */
    public enum Pick { ENTITY, POINT, BLOCK }

    private final Pick pick;

    TargetProfile(Pick pick) {
        this.pick = pick;
    }

    public Pick pick() {
        return pick;
    }

    public String id() {
        return name();
    }

    public static Optional<TargetProfile> byId(String id) {
        for (TargetProfile p : values()) {
            if (p.name().equals(id.toUpperCase(Locale.ROOT))) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
