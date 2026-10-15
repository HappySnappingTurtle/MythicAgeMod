package io.github.happysnappingturtle.mythicage.entity;

import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** 实体类型。碰撞尺寸与 {@code data/mythic_age/mythic_age/rig/*.json} 一致（check_rig.py 校验）。 */
public final class ModEntities {
    public static final DeferredHolder<EntityType<?>, EntityType<MinotaurEntity>> MINOTAUR = ModRegistries.ENTITY_TYPES.register("minotaur",
            () -> EntityType.Builder.of(MinotaurEntity::new, MobCategory.CREATURE)
                    .sized(1.1F, 2.5F)
                    .eyeHeight(2.2F)
                    .clientTrackingRange(10)
                    .build("mythic_age:minotaur"));

    private ModEntities() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ModEntities::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MINOTAUR.get(), MinotaurEntity.createAttributes().build());
    }
}
