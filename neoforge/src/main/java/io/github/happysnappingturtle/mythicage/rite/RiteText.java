package io.github.happysnappingturtle.mythicage.rite;

import io.github.happysnappingturtle.mythicage.core.rite.RiteRules;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 仪式缺项的显示文本（语言键 {@code mythic_age.rite.issue.<key>}；参数里的时代、次神、物品换成名字）。 */
final class RiteText {
    private RiteText() {
    }

    static Component issue(RiteRules.Issue i) {
        List<Object> a = i.args();
        Object[] args = switch (i.key()) {
            case "village_caught_up", "not_open", "data_error", "age_not_reached" -> new Object[]{age(a.get(0))};
            case "not_candidate", "god_unavailable" -> new Object[]{god(a.get(0))};
            case "offering" -> new Object[]{TradeService.stack(String.valueOf(a.get(0)), 1).getHoverName(), a.get(1), a.get(2)};
            case "voucher" -> new Object[]{Component.translatable("mythic_age.trial." + a.get(0)), a.get(1), a.get(2)};
            default -> a.toArray();
        };
        return Component.translatable("mythic_age.rite.issue." + i.key(), args);
    }

    static Component age(Object id) {
        return Component.translatable("mythic_age.age." + id);
    }

    static Component god(Object id) {
        return Component.translatable("mythic_age.god." + id);
    }
}
