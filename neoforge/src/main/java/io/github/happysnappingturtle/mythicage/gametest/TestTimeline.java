package io.github.happysnappingturtle.mythicage.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 一个测试自己的时间线：只向 GameTest 登记一次 {@code onEachTick}，之后的「延后 N 刻」「等到某条件」都排在这里。
 * 原版 GameTest 在遍历待执行表时不允许回调里再登记（{@code runAfterDelay} 套 {@code runAfterDelay} 会让服务器崩溃），
 * 需要多段等待的测试用它。
 */
final class TestTimeline {
    private record Step(long due, BooleanSupplier ready, Runnable action) {
    }

    private final GameTestHelper h;
    private final List<Step> steps = new ArrayList<>();

    TestTimeline(GameTestHelper h) {
        this.h = h;
        h.onEachTick(this::tick);
    }

    /** 从现在起 delay 刻后执行。 */
    TestTimeline later(long delay, Runnable action) {
        steps.add(new Step(h.getTick() + delay, () -> true, action));
        return this;
    }

    /** 条件成立的那一刻执行一次。 */
    TestTimeline when(BooleanSupplier ready, Runnable action) {
        steps.add(new Step(h.getTick(), ready, action));
        return this;
    }

    private void tick() {
        for (Step s : List.copyOf(steps)) {
            if (s.due() <= h.getTick() && s.ready().getAsBoolean()) {
                steps.remove(s);
                s.action().run();
            }
        }
    }
}
