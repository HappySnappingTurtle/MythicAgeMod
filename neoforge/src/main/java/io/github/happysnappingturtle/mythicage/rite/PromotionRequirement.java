package io.github.happysnappingturtle.mythicage.rite;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.rite.RequirementScope;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 晋升的建筑条件（T11a 设计文档 §4.3；用户修订 U4、U9）：玩家的（如自己神坛的神庙等级）或村庄的
 * （举行地村庄由村民建成的建筑，T19 起）。每种条件一个实现，逐条给出满足 / 缺少与进度。
 */
public interface PromotionRequirement {
    /** {@code <命名空间>:<名字>}，与晋升表 requirements[].type 对应。 */
    String type();

    RequirementScope scope();

    /**
     * @param target 目标时代
     * @param params 晋升表里这一条的原样参数
     */
    Line check(ServerPlayer player, Civilization civ, Age target, VillageRegistry.Village village, JsonObject params);

    /**
     * 晋升成功界面「下一时代需要」里的预告（12 号 §4.3 A：只预告、不判定是否满足）。默认用类型名的语言键。
     */
    default Component preview(JsonObject params) {
        return Component.translatable("mythic_age.rite.req." + type().replace(':', '.'));
    }

    /** 晋升页上的一行：「你的神庙：中神庙」✓、「本村青铜工坊 0 / 2」✗。 */
    record Line(Component label, boolean met, Component progress) {
    }
}
