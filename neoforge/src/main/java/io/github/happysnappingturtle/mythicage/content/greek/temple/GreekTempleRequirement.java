package io.github.happysnappingturtle.mythicage.content.greek.temple;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.rite.RequirementScope;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.rite.PromotionRequirement;
import io.github.happysnappingturtle.mythicage.core.village.temple.TempleStanding;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 希腊晋升条件「神庙达到某级」（T11b 设计文档 §1；GA-13 §2.8 改为看举行仪式的这个村子）：村庄神庙的等级够、
 * 且完好度 ≥ 80%（{@link TempleStanding#meets}）。等级随卫城扩建提升，不看玩家时代。
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

    /** 玩家供奉的村子里神庙等级最高的一座；一座都没有为 −1（登录补发进度用）。 */
    public static int bestLevel(ServerPlayer player, TempleRules rules) {
        int best = -1;
        for (java.util.UUID v : VillageTemples.devotedVillages(player.server, player.getUUID())) {
            best = Math.max(best, VillageTemples.of(player.server, v).map(VillageTemple::level).orElse(-1));
        }
        return best;
    }

    static int need(JsonObject params, TempleRules rules) {
        return Math.max(0, Math.min(rules.maxLevel(), params != null && params.has("level") ? params.get("level").getAsInt() : 1));
    }

    @Override
    public Component preview(JsonObject params) {
        TempleRules rules = TempleService.rules();
        return Component.translatable("mythic_age.rite.preview_temple",
                Component.translatable("mythic_age.temple.tier." + rules.tier(need(params, rules)).name()));
    }

    @Override
    public Line check(ServerPlayer player, Civilization civ, Age target, VillageRegistry.Village village, JsonObject params) {
        TempleRules rules = TempleService.rules();
        int need = need(params, rules);
        Component tier = Component.translatable("mythic_age.temple.tier." + rules.tier(need).name());
        java.util.Optional<VillageTemple> t = village == null ? java.util.Optional.empty()
                : VillageTemples.of(player.server, village);
        if (t.isEmpty()) {
            return new Line(Component.translatable("mythic_age.rite.line.temple_none", tier), false,
                    Component.literal("— / " + need));
        }
        int level = t.get().level();
        int percent = (int) Math.round(t.get().integrity() * 100);
        boolean ok = TempleStanding.meets(level, t.get().integrity(), need);
        if (level >= need && !ok) {
            return new Line(Component.translatable("mythic_age.rite.line.temple_damaged", tier, percent), false,
                    Component.literal(percent + "% / " + Math.round(TempleStanding.PROMOTION_INTEGRITY * 100) + "%"));
        }
        return new Line(Component.translatable("mythic_age.rite.line.temple", tier, need), ok,
                Component.literal(level + " / " + need));
    }
}
