package io.github.happysnappingturtle.mythicage.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayAtAltar;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在村民的工作包与闲逛包末尾追加祈祷行为（T10 设计文档 §4）。没有职业的村民白天跑的是闲逛包。
 * 注入点非必需（require = 0）：与其他改村民 AI 的模组冲突时不崩溃，由 GreekPrayer 启动自检报错。
 */
@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesMixin {
    @Inject(method = "getWorkPackage", at = @At("RETURN"), cancellable = true, require = 0)
    private static void mythicAge$prayAtWork(VillagerProfession profession, float speedModifier,
                                             CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        cir.setReturnValue(mythicAge$withPrayer(cir.getReturnValue()));
    }

    @Inject(method = "getIdlePackage", at = @At("RETURN"), cancellable = true, require = 0)
    private static void mythicAge$prayWhenIdle(VillagerProfession profession, float speedModifier,
                                               CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        cir.setReturnValue(mythicAge$withPrayer(cir.getReturnValue()));
    }

    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> mythicAge$withPrayer(
            ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original) {
        return ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>builder()
                .addAll(original)
                .add(Pair.of(PrayAtAltar.PRIORITY, new PrayAtAltar()))
                .build();
    }
}
