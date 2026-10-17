package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.power.PowerReason;

import java.util.Optional;

/**
 * 一个神力的效果代码（T12 设计文档 §2.5）。数值来自 {@link io.github.happysnappingturtle.mythicage.core.power.PowerSpec}，
 * 这里只写「做什么」。框架保证：调用 {@link #execute} 时道具已扣、冷却已记，且目标在确认与吟唱结束时都校验过。
 */
public interface GodPower {
    String id();

    /** 确认时与吟唱结束时的额外校验（放置点空间、权限……）；返回失败原因，通过时为空。 */
    default Optional<PowerReason> validate(CastContext ctx) {
        return Optional.empty();
    }

    /** 事务提交成功后调用。持续型效果自己登记逐刻任务。 */
    void execute(CastContext ctx);

    /** 两段式神力（冥界通道、流沙）：第一段提交后只产生阶段物或预览；具体随对应神力实现。 */
    default boolean twoStage() {
        return false;
    }
}
