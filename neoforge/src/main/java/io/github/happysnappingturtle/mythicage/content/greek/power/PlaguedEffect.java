package io.github.happysnappingturtle.mythicage.content.greek.power;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「瘟疫」标记（05 号 GP-GR-06）：带这个效果的实体不能开弓或施法（见 {@link PestilencePower} 的事件），
 * 本身不造成伤害；瘟疫区域每 20 刻续上，离开区域后 2 秒消失。Boss 不会得到它（远程封禁不适用于 Boss）。
 */
public final class PlaguedEffect extends MobEffect {
    public PlaguedEffect() {
        super(MobEffectCategory.HARMFUL, 0x6E1A1A);
    }
}
