package io.github.happysnappingturtle.mythicage.core.village.resource;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;

/**
 * 登记的采集点（T19 设计文档 §6.2，V1 §5.3）：黏土坑、采石点、壳堆……NPC 采集与玩家在该点挖掘共用同一份余量。
 * 耗尽后显示耗尽外观，不随区块重载刷新；按 MC 日缓慢恢复（采石点不恢复）。
 *
 * @param lastDay 上次结算恢复的 MC 日
 */
public record ResourcePoint(String id, String kind, Pos pos, int remaining, int max, int recoverPerDay, long lastDay) {
    public enum Tier { FULL, HALF, LOW, EMPTY }

    public ResourcePoint {
        remaining = Math.max(0, Math.min(max, remaining));
    }

    public static ResourcePoint fresh(String id, String kind, Pos pos, int max, int recoverPerDay, long day) {
        return new ResourcePoint(id, kind, pos, max, max, recoverPerDay, day);
    }

    /** 推进到 {@code today}：每过一日恢复 recoverPerDay，不超过上限。 */
    public ResourcePoint recover(long today) {
        if (today <= lastDay) {
            return this;
        }
        long add = (today - lastDay) * (long) recoverPerDay;
        return new ResourcePoint(id, kind, pos, (int) Math.min(max, remaining + add), max, recoverPerDay, today);
    }

    /** 取走最多 n。返回新状态；实取数 = 旧余量 − 新余量。 */
    public ResourcePoint take(int n) {
        return new ResourcePoint(id, kind, pos, remaining - Math.max(0, Math.min(n, remaining)), max, recoverPerDay, lastDay);
    }

    public Tier tier() {
        if (remaining <= 0) {
            return Tier.EMPTY;
        }
        if (remaining * 3 <= max) {
            return Tier.LOW;
        }
        return remaining * 3 <= max * 2 ? Tier.HALF : Tier.FULL;
    }
}
