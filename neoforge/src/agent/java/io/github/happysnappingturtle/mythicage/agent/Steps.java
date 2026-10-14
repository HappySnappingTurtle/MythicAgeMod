package io.github.happysnappingturtle.mythicage.agent;

import java.util.function.BooleanSupplier;

/** {@link ScenarioStep} 的常用工厂方法；每次调用都返回全新实例，内部进度互不干扰。 */
public final class Steps {
    private Steps() {
    }

    public static ScenarioStep waitTicks(int n) {
        int[] remaining = {n};
        return mc -> {
            if (remaining[0] <= 0) {
                return true;
            }
            remaining[0]--;
            return remaining[0] <= 0;
        };
    }

    /** 以玩家身份执行一条命令（不带斜杠）。 */
    public static ScenarioStep runCommand(String command) {
        return mc -> {
            if (mc.player != null) {
                mc.player.connection.sendCommand(command);
            } else {
                AgentEntrypoint.LOG.warn("[agent] 玩家为空，跳过命令：{}", command);
            }
            return true;
        };
    }

    public static ScenarioStep action(Runnable r) {
        return mc -> {
            r.run();
            return true;
        };
    }

    /** 轮询直到条件成立；超时调用 onTimeout，仍算完成（不阻塞后续步骤）。 */
    public static ScenarioStep until(BooleanSupplier check, int timeoutTicks, Runnable onTimeout) {
        int[] elapsed = {0};
        return mc -> {
            if (check.getAsBoolean()) {
                return true;
            }
            if (++elapsed[0] > timeoutTicks) {
                onTimeout.run();
                return true;
            }
            return false;
        };
    }
}
