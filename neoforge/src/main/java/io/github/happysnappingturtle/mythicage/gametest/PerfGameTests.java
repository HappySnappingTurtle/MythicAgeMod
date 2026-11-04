package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.perf.PerfStats;
import io.github.happysnappingturtle.mythicage.perf.Perf;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** T24 阶段 ④：性能探针（TC-T24-87）。采样 40 刻后自动停止，各分类与合计都有样本，报告能出。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PerfGameTests {
    private PerfGameTests() {
    }

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = "t24_perf", timeoutTicks = 200)
    public static void perf_samples_and_stops(GameTestHelper h) {
        Perf.start(40);
        h.runAfterDelay(60, () -> {
            h.assertFalse(Perf.running(), "40 刻后自动停止");
            h.assertTrue(Perf.stats().summary(PerfStats.TOTAL).samples() == 40, "合计 40 个样本：" + Perf.stats().summary(PerfStats.TOTAL));
            h.assertTrue(Perf.stats().categories().contains("village_scheduler"), "各处理器都在：" + Perf.stats().categories());
            List<String> lines = Perf.lines(h.getLevel().getServer());
            h.assertTrue(lines.size() >= 3 && lines.get(1).startsWith("已测每刻合计"), "报告：" + lines);
            h.succeed();
        });
    }
}
