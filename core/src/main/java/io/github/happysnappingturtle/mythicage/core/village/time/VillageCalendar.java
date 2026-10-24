package io.github.happysnappingturtle.mythicage.core.village.time;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 村庄历法（T19 设计文档 §9.2，V1 §6.2）：一年 {@code yearDays} 个 MC 日，分若干期；特殊日（节庆、神谕日）按「一年中的第几日」登记。
 * 各村按村庄 id 派生 0 到 yearDays − 1 的偏移，全世界不会同一天过节。历法只改工作权重与节庆，不影响作物生长。
 * MC 日序号从 0 起（{@code MythDays}）：偏移 0 的村庄在第 0 日是一年的第 1 日。
 */
public record VillageCalendar(int yearDays, List<Period> periods, Map<Integer, List<String>> special) {
    public record Period(String id, int days) {
    }

    /** @param dayOfYear 1 起；dayInPeriod 1 起 */
    public record Date(int dayOfYear, String period, int dayInPeriod, List<String> special) {
        public boolean is(String s) {
            return special.contains(s);
        }
    }

    public VillageCalendar {
        periods = List.copyOf(periods);
        Map<Integer, List<String>> m = new LinkedHashMap<>();
        special.forEach((k, v) -> m.put(k, List.copyOf(v)));
        special = java.util.Collections.unmodifiableMap(m);
        int sum = periods.stream().mapToInt(Period::days).sum();
        if (yearDays <= 0 || (!periods.isEmpty() && sum != yearDays)) {
            throw new IllegalArgumentException("历法各期天数之和 " + sum + " 不等于一年 " + yearDays + " 日");
        }
    }

    public static VillageCalendar none() {
        return new VillageCalendar(1, List.of(), Map.of());
    }

    /** 由村庄 id 派生的偏移（splitmix64），永远相同。 */
    public int offset(UUID village) {
        long z = village.getMostSignificantBits() ^ Long.rotateLeft(village.getLeastSignificantBits(), 17) + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (int) Math.floorMod(z, (long) yearDays);
    }

    public Date date(long mcDay, int offset) {
        int doy = (int) Math.floorMod(mcDay + offset, (long) yearDays) + 1;
        String period = "";
        int inPeriod = doy;
        int acc = 0;
        for (Period p : periods) {
            if (doy <= acc + p.days()) {
                period = p.id();
                inPeriod = doy - acc;
                break;
            }
            acc += p.days();
        }
        return new Date(doy, period, inPeriod, special.getOrDefault(doy, List.of()));
    }

    /** 从今天起，下一个带 {@code tag} 的日子还有几天（今天就是则 0）；没有返回 −1。 */
    public int daysUntil(long mcDay, int offset, String tag) {
        for (int i = 0; i < yearDays; i++) {
            if (date(mcDay + i, offset).is(tag)) {
                return i;
            }
        }
        return -1;
    }

    public static VillageCalendar parse(JsonObject o) {
        List<Period> ps = new ArrayList<>();
        if (o.has("periods")) {
            for (JsonElement e : o.getAsJsonArray("periods")) {
                JsonObject p = e.getAsJsonObject();
                ps.add(new Period(p.get("id").getAsString(), p.get("days").getAsInt()));
            }
        }
        Map<Integer, List<String>> sp = new LinkedHashMap<>();
        if (o.has("special")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("special").entrySet()) {
                List<String> tags = new ArrayList<>();
                if (e.getValue().isJsonArray()) {
                    e.getValue().getAsJsonArray().forEach(t -> tags.add(t.getAsString()));
                } else {
                    tags.add(e.getValue().getAsString());
                }
                sp.put(Integer.parseInt(e.getKey()), tags);
            }
        }
        return new VillageCalendar(o.get("yearDays").getAsInt(), ps, sp);
    }
}
