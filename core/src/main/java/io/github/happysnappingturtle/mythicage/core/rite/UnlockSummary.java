package io.github.happysnappingturtle.mythicage.core.rite;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 晋升成功界面的解锁清单（T11a 设计文档 §6.2；12 号 §4.3-A）：全部从注册数据汇总，不手写。
 *
 * @param powers  神力（交易表结果物品 id）
 * @param summons 召唤
 * @param recipes 本文明本时代的时代受限配方产物（去重）
 * @param caps    上限变化（只含有变化的项）
 * @param extras  晋升表的 extraUnlocks
 */
public record UnlockSummary(Civilization civ, Age age, String minorGod, List<String> powers, List<String> summons,
                            List<String> recipes, List<CapChange> caps, List<PromotionTable.ExtraUnlock> extras) {
    public UnlockSummary {
        powers = List.copyOf(powers);
        summons = List.copyOf(summons);
        recipes = List.copyOf(recipes);
        caps = List.copyOf(caps);
        extras = List.copyOf(extras);
    }

    /** @param key 语言键后缀：favor_cap、shrines、mythic_units、relic_slots */
    public record CapChange(String key, long before, long after) {
    }

    /** 一条时代受限配方：产物、所属文明与时代。 */
    public record GatedRecipe(String result, Civilization civ, Age age) {
    }

    /**
     * 一个时代的清单。神力、召唤：交易表中该时代、无次神或次神为所选次神的商品；
     * 配方：该文明该时代的受限配方，同产物去重。
     */
    public static UnlockSummary of(Civilization civ, Age age, String minorGod, List<TradeOffer> offers,
                                   List<GatedRecipe> recipes, List<CapChange> caps, List<PromotionTable.ExtraUnlock> extras) {
        List<String> powers = new ArrayList<>();
        List<String> summons = new ArrayList<>();
        for (TradeOffer o : offers) {
            if (o.minAge() != age || !(o.minorGod().isEmpty() || o.minorGod().equals(minorGod))) {
                continue;
            }
            if (o.category() == TradeOffer.Category.GOD_POWER) {
                addOnce(powers, o.result().item());
            } else if (o.category() == TradeOffer.Category.SUMMON) {
                addOnce(summons, o.result().item());
            }
        }
        Set<String> rs = new LinkedHashSet<>();
        for (GatedRecipe r : recipes) {
            if (r.civ() == civ && r.age() == age) {
                rs.add(r.result());
            }
        }
        List<CapChange> changed = caps.stream().filter(c -> c.before() != c.after()).toList();
        return new UnlockSummary(civ, age, minorGod, powers, summons, List.copyOf(rs), changed, extras);
    }

    /** 跨级（管理命令）：按时代顺序合并多份清单为一份，取最后一级的时代与次神，上限变化取首尾。 */
    public static UnlockSummary merge(List<UnlockSummary> parts) {
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("没有可合并的清单");
        }
        UnlockSummary last = parts.get(parts.size() - 1);
        List<String> powers = new ArrayList<>();
        List<String> summons = new ArrayList<>();
        List<String> recipes = new ArrayList<>();
        List<PromotionTable.ExtraUnlock> extras = new ArrayList<>();
        Map<String, CapChange> caps = new java.util.LinkedHashMap<>();
        for (UnlockSummary p : parts) {
            p.powers().forEach(x -> addOnce(powers, x));
            p.summons().forEach(x -> addOnce(summons, x));
            p.recipes().forEach(x -> addOnce(recipes, x));
            p.extras().forEach(x -> {
                if (!extras.contains(x)) {
                    extras.add(x);
                }
            });
            for (CapChange c : p.caps()) {
                CapChange old = caps.get(c.key());
                caps.put(c.key(), new CapChange(c.key(), old == null ? c.before() : old.before(), c.after()));
            }
        }
        return new UnlockSummary(last.civ(), last.age(), last.minorGod(), powers, summons, recipes,
                caps.values().stream().filter(c -> c.before() != c.after()).toList(), extras);
    }

    private static void addOnce(List<String> list, String v) {
        if (!list.contains(v)) {
            list.add(v);
        }
    }
}
