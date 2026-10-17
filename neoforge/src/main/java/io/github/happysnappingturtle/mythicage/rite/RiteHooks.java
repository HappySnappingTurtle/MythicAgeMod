package io.github.happysnappingturtle.mythicage.rite;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.rite.RiteKind;
import io.github.happysnappingturtle.mythicage.core.rite.RiteRules;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 仪式的扩展点（T11a 设计文档 §4.2、§4.4）：晋升后钩子（如亚特兰蒂斯「克罗诺斯的馈赠」）与费用修正（如圣物「泰坦的宝藏」）。
 * 不改仪式流程本身；晋升后钩子抛异常只记日志，不回滚仪式。
 */
public final class RiteHooks {
    @FunctionalInterface
    public interface AfterPromotion {
        void after(ServerPlayer player, Civilization civ, Age newAge, String minorGod, VillageRegistry.Village village);
    }

    @FunctionalInterface
    public interface CostModifier {
        RiteRules.Cost modify(ServerPlayer player, Civilization civ, RiteKind kind, Age age, RiteRules.Cost cost);
    }

    private static final List<AfterPromotion> AFTER = new ArrayList<>();
    private static final List<CostModifier> COST = new ArrayList<>();

    private RiteHooks() {
    }

    public static synchronized void afterPromotion(AfterPromotion hook) {
        AFTER.add(hook);
    }

    public static synchronized void costModifier(CostModifier modifier) {
        COST.add(modifier);
    }

    /** 只供 GameTest：移除一个钩子。 */
    public static synchronized void removeForTest(Object hook) {
        AFTER.remove(hook);
        COST.remove(hook);
    }

    static synchronized void runAfterPromotion(ServerPlayer p, Civilization civ, Age age, String god, VillageRegistry.Village v) {
        for (AfterPromotion h : List.copyOf(AFTER)) {
            try {
                h.after(p, civ, age, god, v);
            } catch (RuntimeException e) {
                MythicAge.LOGGER.error("晋升后钩子出错（仪式已完成，不回滚）", e);
            }
        }
    }

    static synchronized RiteRules.Cost modify(ServerPlayer p, Civilization civ, RiteKind kind, Age age, RiteRules.Cost cost) {
        RiteRules.Cost c = cost;
        for (CostModifier m : COST) {
            c = m.modify(p, civ, kind, age, c);
        }
        return c;
    }
}
