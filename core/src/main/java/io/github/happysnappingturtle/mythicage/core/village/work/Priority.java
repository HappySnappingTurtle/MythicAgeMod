package io.github.happysnappingturtle.mythicage.core.village.work;

/**
 * 工作单优先级（11 号 §4，V1 §3），从高到低：紧急撤离 / 自卫 > 恢复口粮与安全 > 已开工的施工 > 缺口生产 > 新施工 > 装饰活动。
 * 生活作息（回家睡觉、吃饭）属于 ROUTINE，排在新施工之后、装饰之前。
 */
public enum Priority {
    EMERGENCY,
    FOOD_SAFETY,
    ACTIVE_BUILD,
    SHORTAGE,
    NEW_BUILD,
    ROUTINE,
    DECOR
}
