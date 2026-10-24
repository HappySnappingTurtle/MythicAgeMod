package io.github.happysnappingturtle.mythicage.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;

/** 治愈僵尸村民的善恶奖励（T18）：读取原版记录的治愈者；GameTest 直接完成转化。 */
@Mixin(ZombieVillager.class)
public interface ZombieVillagerAccessor {
    @Accessor("conversionStarter")
    UUID mythicAge$conversionStarter();

    @Accessor("conversionStarter")
    void mythicAge$setConversionStarter(UUID starter);

    @Invoker("finishConversion")
    void mythicAge$finishConversion(ServerLevel level);
}
