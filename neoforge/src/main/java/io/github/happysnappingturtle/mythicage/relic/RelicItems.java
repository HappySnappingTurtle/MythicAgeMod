package io.github.happysnappingturtle.mythicage.relic;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** 圣物物品的共用部分（T16 设计文档 §2.2、§3）：实例 UUID 数据组件、Curios 槽位 id 与物品标签。 */
public final class RelicItems {
    /** Curios 圣物槽的 id（D2：不用通用的 relic，避免和其他模组的槽合并）。 */
    public static final String SLOT = "mythic_relic";
    /** Curios 槽位默认校验用的物品标签。 */
    public static final TagKey<Item> CURIOS_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", SLOT));

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MythicAgeIds.MOD_ID);
    /** 物品第一次被佩戴或供奉时写入，之后不变；用来认出绑定的是哪一个物品。 */
    public static final Supplier<DataComponentType<UUID>> INSTANCE = COMPONENTS.registerComponentType("relic_instance",
            b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    private RelicItems() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    public static Optional<UUID> instance(ItemStack stack) {
        return Optional.ofNullable(stack.get(INSTANCE.get()));
    }

    /** 读出实例 UUID；没有就写入一个新的。 */
    public static UUID ensureInstance(ItemStack stack) {
        UUID id = stack.get(INSTANCE.get());
        if (id == null) {
            id = UUID.randomUUID();
            stack.set(INSTANCE.get(), id);
        }
        return id;
    }
}
