package io.github.happysnappingturtle.mythicage.content.greek.cyclops;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 地上的一碗马洛涅烈酒（D10）：不是掉落物，玩家、羊都捡不起来、推不动；{@value #LIFE} 刻后消失。
 * 独眼巨人·牧人走过来喝完后由他移除。
 */
public class WineBowl extends Entity implements ItemSupplier {
    public static final int LIFE = 400;
    private int age;

    public WineBowl(EntityType<? extends WineBowl> type, Level level) {
        super(type, level);
    }

    /** 在某处放一只酒碗（扔出的酒落地时，GameTest 也直接用）。 */
    public static WineBowl place(ServerLevel level, Vec3 at) {
        WineBowl bowl = new WineBowl(GreekCyclops.WINE_BOWL.get(), level);
        bowl.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360, 0);
        level.addFreshEntity(bowl);
        return bowl;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().multiply(0.9, 0.98, 0.9));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (level() instanceof ServerLevel sl) {
            if (++age >= LIFE) {
                discard();
            } else if (age % 20 == 0) {
                // 甜酒的香气：一缕淡紫色的烟，远处也看得见碗在哪
                sl.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.3, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(GreekCyclops.WINE_BOWL_ITEM.get());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
    }
}
