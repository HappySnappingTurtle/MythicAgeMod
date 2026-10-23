package io.github.happysnappingturtle.mythicage.core.equipment;

/**
 * 格挡后的反击窗口（EQ-GR-11）：格挡成功的那一刻打开，之后 ticks 刻内（含第 ticks 刻）的第一次近战命中加成，只结算一次。
 * 通用：以后别的盾的「格挡后……」也用它。
 */
public final class CounterWindow {
    private long openedAt = Long.MIN_VALUE;
    private int ticks;
    private boolean used = true;

    public void open(long now, int ticks) {
        this.openedAt = now;
        this.ticks = ticks;
        this.used = false;
    }

    /** 现在命中能否吃到加成；能的话同时把窗口用掉。 */
    public boolean consume(long now) {
        if (used || now < openedAt || now - openedAt > ticks) {
            return false;
        }
        used = true;
        return true;
    }

    public boolean isOpen(long now) {
        return !used && now >= openedAt && now - openedAt <= ticks;
    }
}
