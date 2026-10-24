package io.github.happysnappingturtle.mythicage.entity;

import io.github.happysnappingturtle.mythicage.content.greek.guardian.GreekGuardianEntity;
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
    /** 希腊守护者（神庙祭司，T9）：人形，碰撞 0.6 × 1.95（VI-GR-PRI-01）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<GreekGuardianEntity>> GREEK_GUARDIAN =
            ModRegistries.ENTITY_TYPES.register("greek_guardian",
                    () -> EntityType.Builder.of(GreekGuardianEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.95F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(10)
                            .build("mythic_age:greek_guardian"));

    /** 文明村庄的居民（T19）：人形，碰撞同原版村民 0.6 × 1.95。 */
    public static final DeferredHolder<EntityType<?>, EntityType<io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity>> RESIDENT =
            ModRegistries.ENTITY_TYPES.register("resident",
                    () -> EntityType.Builder.of(io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.95F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(10)
                            .build("mythic_age:resident"));

    private ModEntities() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ModEntities::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MINOTAUR.get(), MinotaurEntity.createAttributes().build());
        event.put(GREEK_GUARDIAN.get(), GreekGuardianEntity.createAttributes().build());
        event.put(RESIDENT.get(), io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity.createAttributes().build());
    }
}
