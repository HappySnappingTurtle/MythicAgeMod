package io.github.happysnappingturtle.mythicage.agent;

import net.minecraft.client.Minecraft;

/** 最小执行单元：每 tick 调用一次，返回 true 表示完成。 */
@FunctionalInterface
public interface ScenarioStep {
    boolean tick(Minecraft mc);
}
