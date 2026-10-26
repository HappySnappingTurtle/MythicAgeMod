package io.github.happysnappingturtle.mythicage.core.village.quest;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 敌袭救援与护送里的「真实危险」（T21 设计文档 §8，02 号 §2、§4）。纯函数。
 */
public final class RescueRules {
    private RescueRules() {
    }

    /**
     * 一只威胁的来历：玩家刷出来的（刷怪蛋、命令、发射器、刷怪笼、试炼刷怪笼）、有主人（召唤物、宠物）、
     * 在把居民当目标之前打过它的玩家、在这段时间里伤过这名居民的玩家。
     */
    public record Danger(boolean playerMade, boolean owned, Set<UUID> provokers, Set<UUID> harmers) {
        public Danger {
            provokers = Set.copyOf(provokers);
            harmers = Set.copyOf(harmers);
        }

        /** 对这名玩家是不是真实危险（§8.3 四种情形都不是才算）。 */
        public boolean realFor(UUID player) {
            return !playerMade && !owned && !provokers.contains(player) && !harmers.contains(player);
        }
    }

    /**
     * 谁算救下（§8.2）：对这只威胁造成致死一击的玩家，加上在它以居民为目标期间造成过它所受伤害<b>一半以上</b>的玩家。
     * {@code total} 含士兵与环境造成的伤害（士兵不归任何玩家，所以士兵打了大半时没人过半）。
     */
    public static Set<UUID> rescuers(Map<UUID, Double> byPlayer, double total, Optional<UUID> killer) {
        Set<UUID> out = new LinkedHashSet<>();
        killer.ifPresent(out::add);
        if (total > 0) {
            byPlayer.forEach((p, d) -> {
                if (d > total / 2) {
                    out.add(p);
                }
            });
        }
        return out;
    }
}
