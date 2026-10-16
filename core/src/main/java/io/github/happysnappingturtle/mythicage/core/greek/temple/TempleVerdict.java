package io.github.happysnappingturtle.mythicage.core.greek.temple;

import io.github.happysnappingturtle.mythicage.core.player.Age;

import java.util.List;
import java.util.Optional;

/**
 * 判定结果（T8a 设计文档 §3.3）。
 *
 * @param structuralLevel 结构满足的最高等级
 * @param grantedLevel    受主人时代封顶后的认定等级
 * @param missing         下一级（structuralLevel + 1）尚未满足的条目；已是最高级时为空
 * @param ageRequired     结构等级高于认定等级时，认定下一级所需的时代
 */
public record TempleVerdict(int structuralLevel, int grantedLevel, List<Missing> missing, Optional<Age> ageRequired) {
    public TempleVerdict {
        missing = List.copyOf(missing);
    }

    /**
     * 一条缺失项，例如 {@code columns 3/4}。
     *
     * @param key  统计项：columns、roof、litBraziers、statues、pediments、marbleFloor
     * @param have 当前值
     * @param need 需要的值
     */
    public record Missing(String key, int have, int need) {
    }
}
