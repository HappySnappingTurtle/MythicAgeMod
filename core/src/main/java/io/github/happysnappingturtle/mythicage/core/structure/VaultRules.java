package io.github.happysnappingturtle.mythicage.core.structure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * 遗迹宝库（T22 设计文档 §3.4、§4.4）：每名玩家每座宝库开一次（09 合同 §3「两名玩家先后开宝库各得一份」）；
 * 圣物从池里抽，优先抽这名玩家还没拿过的（01_希腊 §9）。纯函数。
 *
 * <p>防重复靠玩家收据：{@link #receipt} 记「开过这座」，{@link #relicReceipt} 记「拿到过这件圣物」。
 * 方块实体里的已开名单只用于提示，满 {@value #MAX_OPENED} 人时丢掉最早的。
 */
public final class VaultRules {
    public static final int MAX_OPENED = 256;

    private VaultRules() {
    }

    /** 这座宝库的收据：{@code vault:<维度>:<x>,<y>,<z>}。 */
    public static String receipt(String dimension, int x, int y, int z) {
        return "vault:" + dimension + ":" + x + "," + y + "," + z;
    }

    /** 拿到过这件圣物的收据：{@code relic:<物品>}。 */
    public static String relicReceipt(String item) {
        return "relic:" + item;
    }

    /** 抽一件圣物：先在没拿过的里随机，都拿过了在整个池里随机；池空时没有。 */
    public static Optional<String> pickRelic(List<String> pool, Predicate<String> owned, RandomGenerator random) {
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        List<String> fresh = new ArrayList<>();
        for (String r : pool) {
            if (!owned.test(r)) {
                fresh.add(r);
            }
        }
        List<String> from = fresh.isEmpty() ? pool : fresh;
        return Optional.of(from.get(random.nextInt(from.size())));
    }

    /** 记下开过的玩家（最多 {@value #MAX_OPENED} 人，超出丢掉最早的）。 */
    public static void remember(LinkedHashSet<UUID> opened, UUID player) {
        opened.remove(player);
        opened.add(player);
        while (opened.size() > MAX_OPENED) {
            opened.remove(opened.iterator().next());
        }
    }
}
