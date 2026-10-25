package io.github.happysnappingturtle.mythicage.core.village.life;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.time.VillageCalendar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * 节庆的纯逻辑（T19 设计文档 §1.10 J5）：今天过不过节、此刻在哪一段（含前夜段）、口粮倍数、游行按排放行、宴饮与祭品消耗。
 * 节日 = 历法特殊日的标签同名的 {@link LifeData.Festival}；村庄阶段不到 {@code minAge} 不举行。
 */
public final class FestivalRules {
    private FestivalRules() {
    }

    /** 此刻进行中的节庆段。{@code eve}：这是明天节日的前夜段。 */
    public record Active(LifeData.Festival festival, LifeData.Phase phase, boolean eve) {
    }

    public static boolean open(String minAge, Age stage) {
        return minAge == null || minAge.isEmpty() || Age.byId(minAge).map(stage::atLeast).orElse(true);
    }

    /** 某一 MC 日的节日（没有或阶段不够为 empty）。 */
    public static Optional<LifeData.Festival> on(LifeData life, VillageCalendar cal, int offset, long mcDay, Age stage) {
        for (String tag : cal.date(mcDay, offset).special()) {
            LifeData.Festival f = life.festivals().get(tag);
            if (f != null && open(f.minAge(), stage)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    /** 此刻的节庆段：先看今天的节日，再看明天节日的前夜段。 */
    public static Optional<Active> at(LifeData life, VillageCalendar cal, int offset, long mcDay, int tod, Age stage) {
        Optional<LifeData.Festival> today = on(life, cal, offset, mcDay, stage);
        if (today.isPresent()) {
            for (LifeData.Phase p : today.get().phases()) {
                if (!p.eve() && p.contains(tod)) {
                    return Optional.of(new Active(today.get(), p, false));
                }
            }
        }
        Optional<LifeData.Festival> tomorrow = on(life, cal, offset, mcDay + 1, stage);
        if (tomorrow.isPresent()) {
            for (LifeData.Phase p : tomorrow.get().phases()) {
                if (p.eve() && p.contains(tod)) {
                    return Optional.of(new Active(tomorrow.get(), p, true));
                }
            }
        }
        return Optional.empty();
    }

    /** 今天每人几份口粮（不过节 1）。 */
    public static double rations(LifeData life, VillageCalendar cal, int offset, long mcDay, Age stage) {
        return on(life, cal, offset, mcDay, stage).map(LifeData.Festival::rations).orElse(1.0);
    }

    /** 游行：第 i 人在开始后多少刻放行（每排 {@code perRow} 人，每排间隔 {@code gap} 刻；第 0 人是领头）。 */
    public static List<Integer> release(int people, int perRow, int gap) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < people; i++) {
            out.add(i / Math.max(1, perRow) * gap);
        }
        return out;
    }

    /** 宴饮消耗：每 {@code per} 人 1 份（向上取整），库存不足就有多少用多少。 */
    public static int feast(int people, int per, int stock) {
        if (people <= 0 || per <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(stock, (people + per - 1) / per));
    }

    /** 祭品：依次找村库里有的第一种。 */
    public static Optional<String> offering(List<String> items, ToIntFunction<String> stock) {
        return items.stream().filter(i -> stock.applyAsInt(i) > 0).findFirst();
    }
}
