package io.github.happysnappingturtle.mythicage.mixin;

import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 试炼刷怪器的两个绝对时刻（停摆时后推用；GameTest 也读它）。 */
@Mixin(TrialSpawnerData.class)
public interface TrialSpawnerDataAccessor {
    @Accessor("cooldownEndsAt")
    long mythicAge$getCooldownEndsAt();

    @Accessor("cooldownEndsAt")
    void mythicAge$setCooldownEndsAt(long value);

    @Accessor("nextMobSpawnsAt")
    long mythicAge$getNextMobSpawnsAt();

    @Accessor("nextMobSpawnsAt")
    void mythicAge$setNextMobSpawnsAt(long value);
}
