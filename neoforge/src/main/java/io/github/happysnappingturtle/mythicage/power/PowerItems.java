package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 神力道具的识别：{@link GodPowerItem} 自带神力 id；任何物品也可以带数据组件 {@code mythic_age:god_power}（测试与数据驱动用）。
 */
public final class PowerItems {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MythicAgeIds.MOD_ID);
    public static final Supplier<DataComponentType<String>> GOD_POWER = COMPONENTS.registerComponentType("god_power",
            b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private PowerItems() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    public static Optional<String> powerOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (stack.getItem() instanceof GodPowerItem g) {
            return Optional.of(g.powerId());
        }
        return Optional.ofNullable(stack.get(GOD_POWER.get()));
    }
}
