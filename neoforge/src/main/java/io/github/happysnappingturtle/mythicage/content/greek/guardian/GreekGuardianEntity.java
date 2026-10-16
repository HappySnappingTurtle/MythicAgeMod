package io.github.happysnappingturtle.mythicage.content.greek.guardian;

import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianGesture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 希腊祭司：神庙祭司 Hiereus（外观见 VI-GR-PRI-01，GeckoLib 模型与动画见 T9b 设计文档）。
 * 行为、身份与交易在 {@link GuardianEntity}；这里只有模型、动画与手势时长。
 */
public class GreekGuardianEntity extends GuardianEntity implements GeoEntity, MythVariantHolder {
    /** 客户端判定「在走路」的水平速度阈值（格/刻）。 */
    public static final double WALK_THRESHOLD = 0.02;
    /** 行走动画每循环前进的距离（格）：大腿摆幅 ±14°、腿长 14u，两步约 0.85 格。 */
    public static final double WALK_CYCLE_BLOCKS = 0.85;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int clientSeenGestureStart = -1;

    public GreekGuardianEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** 动画名与时长（T9b 设计文档 §3）。 */
    @Override
    protected int gestureTicks(GuardianGesture g) {
        return switch (g) {
            case GREET -> 32;
            case ANNOUNCE -> 44;
            case POINT -> 28;
            case NONE -> 0;
        };
    }

    private static String animation(GuardianGesture g) {
        return g.id();
    }

    @Override
    public String variant() {
        return "";
    }

    @Override
    public float variantScale() {
        return 1.0F;
    }

    /** 宣告时抬头望天：不叠加视线方向。 */
    @Override
    public boolean headLookLocked() {
        return gesture() == GuardianGesture.ANNOUNCE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<GreekGuardianEntity> main = new AnimationController<>(this, "main", 4, state -> {
            GuardianGesture g = gesture();
            int start = gestureStart();
            if (start != clientSeenGestureStart) {
                clientSeenGestureStart = start;
                if (g != GuardianGesture.NONE) {
                    state.getController().forceAnimationReset();
                }
            }
            if (g != GuardianGesture.NONE) {
                return state.setAndContinue(RawAnimation.begin().thenPlayAndHold(animation(g)));
            }
            return state.setAndContinue(horizontalSpeed() > WALK_THRESHOLD ? WALK : IDLE);
        });
        main.setAnimationSpeedHandler(e -> e.animationSpeed());
        controllers.add(main);
    }

    private double horizontalSpeed() {
        double dx = getX() - xo;
        double dz = getZ() - zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 行走时按实际步速调整播放速度，避免滑步。 */
    private double animationSpeed() {
        if (gesture() == GuardianGesture.NONE && horizontalSpeed() > WALK_THRESHOLD) {
            return Mth.clamp(horizontalSpeed() * 20.0 / WALK_CYCLE_BLOCKS, 0.4, 3.0);
        }
        return 1.0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
