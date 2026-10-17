package io.github.happysnappingturtle.mythicage.core.power;

import java.util.Locale;

/** 施放的结果或拒绝 / 取消原因；语言键 {@code mythic_age.power.reason.<小写名>}。 */
public enum PowerReason {
    OK, COMMITTED, NOT_REGISTERED, ON_COOLDOWN, NO_ITEM, NO_TARGET, INVALID_TARGET, OUT_OF_RANGE, NO_LINE_OF_SIGHT,
    PLACEMENT_INVALID, SHIELDED, INTERRUPTED_DAMAGE, INTERRUPTED_SWITCH, INTERRUPTED_SCREEN, INTERRUPTED_DEATH,
    TARGET_LOST, AIM_TIMEOUT, CANCELLED, DUPLICATE;

    public String langKey() {
        return "mythic_age.power.reason." + name().toLowerCase(Locale.ROOT);
    }
}
