package io.github.happysnappingturtle.mythicage.core.wild;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 野外神话生物卡（T15 设计文档 §1.1，数据包 {@code wild_creature/<civ>/<id>.json}）。五文明通用。
 *
 * @param biomes        生成群系标签（不带 #），如 {@code mythic_age:region/greek/arcadia}
 * @param weight        刷新权重（原版僵尸 = 100）；只随结构生成的为 0
 * @param densityArea   区域密度的区域边长（格）
 * @param densityMax    每个区域最多几群
 * @param avoidVillages 离村庄中心多少格内不生成
 */
public record WildCreatureCard(String civ, String id, String entity, Attitude attitude, Spawn spawn, List<String> biomes,
                               int weight, int minGroup, int maxGroup, int densityArea, int densityMax, int avoidVillages) {

    public enum Attitude { PASSIVE, NEUTRAL, HOSTILE, TERRITORIAL;

        /** 敌对、领地型默认离村庄 64 格；中立、被动默认 0（总纲 §4）。 */
        public int defaultVillageAvoid() {
            return this == HOSTILE || this == TERRITORIAL ? 64 : 0;
        }
    }

    public enum Spawn { NATURAL, STRUCTURE }

    public WildCreatureCard {
        Objects.requireNonNull(entity);
        biomes = List.copyOf(biomes);
        if (minGroup < 1 || maxGroup < minGroup) {
            throw new IllegalArgumentException("群大小不合法：[" + minGroup + ", " + maxGroup + "]");
        }
        if (weight < 0 || densityArea < 16 || densityMax < 1 || avoidVillages < 0) {
            throw new IllegalArgumentException("权重 / 密度 / 村庄排除不合法");
        }
        if (spawn == Spawn.NATURAL && (weight == 0 || biomes.isEmpty())) {
            throw new IllegalArgumentException("自然刷新的生物要有权重与群系：" + civ + ":" + id);
        }
    }

    public String key() {
        return civ + ":" + id;
    }

    /** 解析一张卡；字段缺失或不合法时抛出 {@link IllegalArgumentException}（调用方记日志并跳过）。 */
    public static WildCreatureCard parse(String civ, String id, JsonObject o) {
        try {
            Attitude attitude = Attitude.valueOf(o.get("attitude").getAsString().toUpperCase(Locale.ROOT));
            Spawn spawn = Spawn.valueOf(o.get("spawn").getAsString().toUpperCase(Locale.ROOT));
            List<String> biomes = new ArrayList<>();
            if (o.has("biomes")) {
                for (JsonElement e : o.getAsJsonArray("biomes")) {
                    biomes.add(e.getAsString().replaceFirst("^#", ""));
                }
            }
            int min = 1;
            int max = 1;
            if (o.has("group")) {
                min = o.getAsJsonArray("group").get(0).getAsInt();
                max = o.getAsJsonArray("group").get(1).getAsInt();
            }
            int area = 256;
            int dmax = 1;
            if (o.has("density")) {
                JsonObject d = o.getAsJsonObject("density");
                area = d.get("area").getAsInt();
                dmax = d.get("max").getAsInt();
            }
            int avoid = o.has("avoid_villages") ? o.get("avoid_villages").getAsInt() : attitude.defaultVillageAvoid();
            return new WildCreatureCard(civ, id, o.get("entity").getAsString(), attitude, spawn, biomes,
                    o.has("weight") ? o.get("weight").getAsInt() : 0, min, max, area, dmax, avoid);
        } catch (NullPointerException | UnsupportedOperationException | IllegalStateException e) {
            throw new IllegalArgumentException("野外生物卡 " + civ + ":" + id + " 缺少字段或类型不对", e);
        }
    }
}
