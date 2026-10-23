package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.content.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** 装备机制用的物品标签（T17，五文明通用）。 */
public final class EquipmentTags {
    /** 钝器：涅墨亚狮子等「锐器减伤」的目标对它不减伤。 */
    public static final TagKey<Item> BLUNT = ModTags.item("blunt");
    /** 青铜圆盾一族：方阵与格挡反击认这个标签（以后的抛光镜盾也加进来）。 */
    public static final TagKey<Item> ASPIS_FAMILY = ModTags.item("aspis_family");
    /** 举盾时仍可出手的武器（长矛刺击；以后维京圆盾的撞盾也用同一入口）。 */
    public static final TagKey<Item> SHIELD_THRUST = ModTags.item("shield_thrust");

    private EquipmentTags() {
    }

    /** 某套护甲的套装标签 {@code mythic_age:set/<套>}。 */
    public static TagKey<Item> set(String armorSet) {
        return ModTags.item("set/" + armorSet);
    }
}
