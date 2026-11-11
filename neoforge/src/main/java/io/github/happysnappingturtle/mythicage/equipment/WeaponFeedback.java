package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.core.equipment.WeaponMotion;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** 通用动作反馈；事件名按 GA-8 保留，选择只依赖动作，不判断文明或物品 id。 */
public final class WeaponFeedback {
    public static final String THRUST = "item.xiphos.thrust";
    public static final String CLEAVE = "item.xiphos.cleave";
    public static final String MISS = "item.xiphos.miss";

    private WeaponFeedback() {
    }

    /** 仅在伤害成功后由服务器调用，避免两端重复播放或无敌目标冒出命中反馈。 */
    public static void hit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        WeaponMotion motion = WeaponMotionState.current(attacker, stack);
        if (motion == WeaponMotion.SLASH) {
            return;
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                PowerRegistries.sound(motion == WeaponMotion.THRUST ? THRUST : CLEAVE), attacker.getSoundSource(), 0.8F, 1);
        Vec3 at = target.getBoundingBox().getCenter();
        if (motion == WeaponMotion.CLEAVE) {
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.18, 0.25, 0.18, 0.1);
        } else {
            Vec3 direction = at.subtract(attacker.getEyePosition()).normalize();
            for (int i = 0; i < 4; i++) {
                Vec3 point = at.subtract(direction.scale(0.12 * i));
                // count=0 按给定方向发射，不变成各向随机散射。
                level.sendParticles(PowerRegistries.THRUST_LINE.get(), point.x, point.y, point.z,
                        0, direction.x, direction.y, direction.z, 0.16);
            }
        }
    }
}
