package io.github.happysnappingturtle.mythicage.agent.dev;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * T7 §3 校准样板：不动、不转头，身体朝向始终等于偏航角。只在遥控口开发环境注册，
 * 用来实测 Blockbench → Bedrock 几何 → GeckoLib 渲染的轴向。
 */
public final class CalibrationEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CalibrationEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoAi(true);
    }

    @Override
    public void tick() {
        super.tick();
        yBodyRot = getYRot();
        yBodyRotO = getYRot();
        yHeadRot = getYRot();
        yHeadRotO = getYRot();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
