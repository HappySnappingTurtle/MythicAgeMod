package io.github.happysnappingturtle.mythicage.recipe;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时代受限配方的放行判定（T6 设计文档 §7.2）。读取 {@code world_rules.json} 的
 * {@code recipes.ageGated}：关闭时一律放行；没有玩家上下文时按 allowAutomation；
 * 否则要求玩家在该文明的时代不低于要求。
 */
public final class AgeGate {
    /** 同一玩家两次「时代不足」提示的最短间隔（毫秒）。 */
    static final long NOTICE_INTERVAL_MS = 3000;
    private static final Map<UUID, Long> LAST_NOTICE = new ConcurrentHashMap<>();

    private AgeGate() {
    }

    public static boolean allows(Civilization civ, Age age, ItemStack result) {
        WorldRulesConfig.AgeGated cfg = ModConfigs.worldRules().current().recipes().ageGated();
        if (!cfg.enabled()) {
            return true;
        }
        ServerPlayer player = CraftingContext.current();
        if (player == null) {
            return cfg.allowAutomation();
        }
        if (MythData.get(player).civ(civ).age().atLeast(age)) {
            return true;
        }
        notice(player, civ, age, result);
        return false;
    }

    private static void notice(ServerPlayer player, Civilization civ, Age age, ItemStack result) {
        long now = System.currentTimeMillis();
        Long last = LAST_NOTICE.get(player.getUUID());
        if (last != null && now - last < NOTICE_INTERVAL_MS) {
            return;
        }
        LAST_NOTICE.put(player.getUUID(), now);
        player.displayClientMessage(Component.translatable("mythic_age.recipe.age_locked", result.getHoverName(),
                Component.translatable("mythic_age.civ." + civ.id()),
                Component.translatable("mythic_age.age." + age.id())), true);
    }

    /** 供 GameTest：清除提示节流记录。 */
    public static void resetNotices() {
        LAST_NOTICE.clear();
    }

    /** 供 GameTest：该玩家是否在节流窗口内收到过提示。 */
    public static boolean noticedRecently(UUID id) {
        Long last = LAST_NOTICE.get(id);
        return last != null && System.currentTimeMillis() - last < NOTICE_INTERVAL_MS;
    }
}
