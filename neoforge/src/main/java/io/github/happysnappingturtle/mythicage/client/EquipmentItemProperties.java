package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.content.greek.equipment.GreekEquipment;
import io.github.happysnappingturtle.mythicage.equipment.DrawBowItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** 文明弓与盾的模型属性（T17）：原版只给 minecraft:bow / shield 注册了 pull、pulling、blocking。 */
final class EquipmentItemProperties {
    private EquipmentItemProperties() {
    }

    static void register() {
        bow(GreekEquipment.COMPOSITE_BOW.get());
        shield(GreekEquipment.ASPIS.get());
    }

    /** pull 按这把弓自己的满弓刻数归一化（复合弓 18 刻），三段拉弓贴图与服务器的力度一致。 */
    private static void bow(DrawBowItem bow) {
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                entity == null || entity.getUseItem() != stack ? 0.0F
                        : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / bow.fullTicks());
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    private static void shield(Item shield) {
        ItemProperties.register(shield, ResourceLocation.withDefaultNamespace("blocking"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }
}
