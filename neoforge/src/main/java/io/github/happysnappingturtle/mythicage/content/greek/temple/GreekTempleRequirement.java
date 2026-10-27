package io.github.happysnappingturtle.mythicage.content.greek.temple;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.rite.RequirementScope;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.rite.PromotionRequirement;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 希腊晋升条件「你的神庙达到某级」（T11b 设计文档 §1）：取玩家全部希腊神坛登记里缓存的结构等级的最大值，
 * 再按玩家当前的希腊时代封顶（与神坛检视界面的认定等级同一规则）。只读登记，神坛不在加载区块也能判定。
 */
public final class GreekTempleRequirement implements PromotionRequirement {
    public static final String TYPE = "greek:temple_level";
    public static final GreekTempleRequirement INSTANCE = new GreekTempleRequirement();

    private GreekTempleRequirement() {
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public RequirementScope scope() {
        return RequirementScope.PLAYER;
    }

    /** 玩家的最高认定等级；没有希腊神坛时为 −1。 */
    public static int bestLevel(ServerPlayer player, TempleRules rules) {
        ShrineRegistry reg = ShrineRegistry.get(player.server);
        int structural = -1;
        for (ShrineRegistry.Entry e : reg.ownedBy(player.getUUID(), Civilization.GREEK)) {
            if (e.kind().equals(GreekTemple.ALTAR_KIND.id())) {
                structural = Math.max(structural, reg.level(e.pos()));
            }
        }
        if (structural < 0) {
            return -1;
        }
        return Math.min(structural, rules.ageCap(MythData.get(player).civ(Civilization.GREEK).age()));
    }

    @Override
    public Component preview(JsonObject params) {
        TempleRules rules = TempleService.rules();
        int need = Math.max(0, Math.min(rules.maxLevel(), params != null && params.has("level") ? params.get("level").getAsInt() : 1));
        return Component.translatable("mythic_age.rite.preview_temple", Component.translatable("mythic_age.temple.tier." + rules.tier(need).name()));
    }

    @Override
    public Line check(ServerPlayer player, Civilization civ, Age target, VillageRegistry.Village village, JsonObject params) {
        TempleRules rules = TempleService.rules();
        int need = Math.max(0, Math.min(rules.maxLevel(), params != null && params.has("level") ? params.get("level").getAsInt() : 1));
        int best = bestLevel(player, rules);
        Component tier = Component.translatable("mythic_age.temple.tier." + rules.tier(need).name());
        if (best < 0) {
            return new Line(Component.translatable("mythic_age.rite.line.temple_none", tier), false,
                    Component.literal("— / " + need));
        }
        return new Line(Component.translatable("mythic_age.rite.line.temple", tier, need), best >= need,
                Component.literal(best + " / " + need));
    }
}
