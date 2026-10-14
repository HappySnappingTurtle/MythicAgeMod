package io.github.happysnappingturtle.mythicage.content.wood;

import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import java.util.function.Supplier;

/**
 * 船类型（T6b 设计文档 §7.2）：通过 NeoForge 的枚举扩展（META-INF/enumextensions.json）给 {@link Boat.Type}
 * 增加 MYTHIC_AGE_OLIVE。贴图位于 {@code mythic_age:textures/entity/boat|chest_boat/olive.png}。
 */
public final class ModBoats {
    @SuppressWarnings("unused") // 由 enumextensions.json 引用
    public static final EnumProxy<Boat.Type> OLIVE_PROXY = new EnumProxy<>(Boat.Type.class,
            (Supplier<Block>) () -> GreekAgriculture.OLIVE_PLANKS.get(),
            "mythic_age:olive",
            (Supplier<Item>) () -> GreekAgriculture.OLIVE_BOAT.get(),
            (Supplier<Item>) () -> GreekAgriculture.OLIVE_CHEST_BOAT.get(),
            (Supplier<Item>) () -> Items.STICK,
            false);

    private ModBoats() {
    }

    public static Boat.Type olive() {
        return OLIVE_PROXY.getValue();
    }
}
