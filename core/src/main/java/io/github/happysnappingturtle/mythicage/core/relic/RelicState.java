package io.github.happysnappingturtle.mythicage.core.relic;

/**
 * 圣物的四种状态（06 号 §0.1）。{@link #INACTIVE} 不入绑定表：没有绑定就是未激活。
 * 三种激活态互斥，彼此之间必须经过未激活。
 */
public enum RelicState {
    INACTIVE, WORN, OFFERED, HERO
}
