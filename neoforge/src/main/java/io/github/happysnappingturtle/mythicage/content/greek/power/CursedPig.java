package io.github.happysnappingturtle.mythicage.content.greek.power;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * 被诅咒的猪（05 号 GP-GR-09；D8）：敌对生物被阿佛洛狄忒的诅咒变成的猪。行为同原版猪，但：
 * 不能繁殖、不吃饲料、不能上鞍、被雷劈不变僵尸猪灵；永久存在（不会自然消失）；只掉猪肉（战利品表）；
 * 实现 {@link Enemy}，目标矩阵里仍是「敌对生物」（铁傀儡会攻击它，杀它不算杀动物）。
 */
public class CursedPig extends Pig implements Enemy {
    public CursedPig(EntityType<? extends Pig> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public boolean isSaddleable() {
        return false;
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        // 不转化
    }

    @Nullable
    @Override
    public Pig getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
