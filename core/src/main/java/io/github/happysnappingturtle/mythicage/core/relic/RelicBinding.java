package io.github.happysnappingturtle.mythicage.core.relic;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.Objects;
import java.util.UUID;

/**
 * 绑定表的一条（06 号 §0.1）：这名玩家的这件圣物现在处于哪种激活态、是哪一个物品、在哪里。
 *
 * @param instance 物品实例 UUID（数据组件 {@code mythic_age:relic_instance}），用来认出绑定的是哪一个物品
 * @param civ      圣物所属文明；供奉位按它计数
 * @param where    位置：佩戴为 {@code curios:<槽位序号>}，供奉为 {@code <维度>|x,y,z}
 */
public record RelicBinding(RelicState state, UUID instance, Civilization civ, String where) {
    public RelicBinding {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(civ);
        Objects.requireNonNull(where);
        if (state == null || state == RelicState.INACTIVE) {
            throw new IllegalArgumentException("未激活的圣物不入绑定表");
        }
    }

    public static String shrineWhere(String dimension, int x, int y, int z) {
        return dimension + "|" + x + "," + y + "," + z;
    }

    public static String curioWhere(int slot) {
        return "curios:" + slot;
    }
}
