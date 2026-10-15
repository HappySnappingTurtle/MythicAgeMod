package io.github.happysnappingturtle.mythicage.agent.dev;

import io.github.happysnappingturtle.mythicage.agent.AgentMode;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 只在遥控口开发环境注册的实体（T7 §3）。生成：{@code /summon mythic_age:dev_calibration ~ ~ ~ {Rotation:[0f,0f]}}。
 * 客户端与内置服务端在同一进程中，所以两边注册表一致；发布包不含 agent 源集。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID)
public final class DevEntities {
    public static final ResourceLocation CALIBRATION_ID = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "dev_calibration");
    static EntityType<CalibrationEntity> calibration;

    private DevEntities() {
    }

    @SubscribeEvent
    static void onRegister(RegisterEvent event) {
        if (!AgentMode.isRemote()) {
            return;
        }
        event.register(Registries.ENTITY_TYPE, CALIBRATION_ID, () -> {
            calibration = EntityType.Builder.of(CalibrationEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .build(CALIBRATION_ID.toString());
            return calibration;
        });
    }

    @SubscribeEvent
    static void onAttributes(EntityAttributeCreationEvent event) {
        if (calibration != null) {
            event.put(calibration, Mob.createMobAttributes().build());
        }
    }
}
