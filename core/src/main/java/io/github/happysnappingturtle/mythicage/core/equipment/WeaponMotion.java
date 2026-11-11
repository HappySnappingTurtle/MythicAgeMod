package io.github.happysnappingturtle.mythicage.core.equipment;

/** 通用武器动作；长度仅影响挥手动画，不改变攻击冷却。 */
public enum WeaponMotion {
    THRUST(5), SLASH(6), CLEAVE(6);

    private final int swingTicks;

    WeaponMotion(int swingTicks) {
        this.swingTicks = swingTicks;
    }

    public int swingTicks() {
        return swingTicks;
    }
}
