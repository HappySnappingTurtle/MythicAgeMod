package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;

/** 主人活跃判定（T5 设计文档 §2）：在线，且距最近一次有效输入不足宽限期。 */
public final class ActivityRules {
    private ActivityRules() {
    }

    public static boolean isActive(boolean online, long now, long lastActiveTick, RuntimeConfig.Idle idle) {
        if (!online) {
            return false;
        }
        return !idle.enabled() || now - lastActiveTick < idle.graceTicks();
    }
}
