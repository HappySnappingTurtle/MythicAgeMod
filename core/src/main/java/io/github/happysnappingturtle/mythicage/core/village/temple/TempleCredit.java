package io.github.happysnappingturtle.mythicage.core.village.temple;

import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * 一次祈祷给村里每位供奉者的入账（GA-13 §2.3）：每人各算一笔，互不分摊。
 * 某位供奉者的 n 是他今天在所有村子合计的信徒数（已含这一位），第 25 名起为 0（{@link PrayerFormula#N_CAP}）。
 */
public final class TempleCredit {
    private TempleCredit() {
    }

    /**
     * 一笔入账的输入。
     *
     * @param tier     这座村庄神庙的系数（已含完好度修正）
     * @param weather  天气系数
     * @param mult     服务器倍率
     * @param bonus    献供加成（供品表的 bonus）
     */
    public record Inputs(double tier, double weather, double mult, double bonus) {
    }

    /** 一位供奉者的这一笔：{@code base} 不含献供，{@code gain} 是实际入账。 */
    public record Share(int n, double base, double gain) {
    }

    /**
     * @param devotees  这个村子的供奉者
     * @param nOf       供奉者今天合计的信徒数（已含这一位）
     * @param relicR    供奉者的圣物加成 R
     * @param offered   供奉者今天在这个村子献过供：1，否则 0
     */
    public static Map<UUID, Share> split(Collection<UUID> devotees, ToIntFunction<UUID> nOf, ToDoubleFunction<UUID> relicR,
                                         ToIntFunction<UUID> offered, Inputs in) {
        Map<UUID, Share> out = new LinkedHashMap<>();
        for (UUID d : devotees) {
            int n = nOf.applyAsInt(d);
            double r = relicR.applyAsDouble(d);
            double base = PrayerFormula.credit(n, in.tier(), in.weather(), r, 0, in.mult());
            double gain = offered.applyAsInt(d) > 0
                    ? PrayerFormula.credit(n, in.tier(), in.weather(), r, in.bonus(), in.mult()) : base;
            out.put(d, new Share(n, base, gain));
        }
        return out;
    }
}
