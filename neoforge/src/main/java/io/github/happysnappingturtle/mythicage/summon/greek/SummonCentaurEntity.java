package io.github.happysnappingturtle.mythicage.summon.greek;

import io.github.happysnappingturtle.mythicage.entity.centaur.AbstractCentaurEntity;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import io.github.happysnappingturtle.mythicage.summon.SummonGoals;
import io.github.happysnappingturtle.mythicage.summon.SummonLink;
import io.github.happysnappingturtle.mythicage.summon.SummonTargets;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 召唤版半人马（T14 设计文档 §6.3、§11.5；制作卡 06）：在共同的弓箭、踢击、风筝之上，多归属与指令、
 * 特殊攻击「掷松」（蓄力 20 刻后掷出松树干，伤害 ×2、穿过 3 个目标，冷却 15 秒）。
 */
public class SummonCentaurEntity extends AbstractCentaurEntity implements MythSummon {
    private final SummonLink link = new SummonLink();

    public SummonCentaurEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createSummonAttributes() {
        return createCentaurAttributes();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new SummonGoals.Follow(this, 1.15));
        goalSelector.addGoal(3, new SummonGoals.Guard(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new SummonGoals.Commanded(this));
        targetSelector.addGoal(1, new SummonGoals.DefendOwner(this));
        targetSelector.addGoal(2, new Retaliate());
        targetSelector.addGoal(3, new SummonGoals.NearestEnemy(this));
    }

    @Override
    protected boolean mayAttack(LivingEntity target) {
        return SummonTargets.canAttack(this, target);
    }

    @Override
    protected boolean hurlEnabled() {
        return true;
    }

    // ───────────── MythSummon ─────────────

    @Override
    public SummonLink link() {
        return link;
    }

    @Override
    public boolean combatUnit() {
        return true;
    }

    @Override
    public void applyStats(SummonUnits.Stats s) {
        applyCentaurStats(s, 70, 0.32);
    }

    /** 传令杖潜行右键：立即掷松（冷却中、正在出手或目标太远时拒绝）。 */
    @Override
    public boolean requestSpecial(LivingEntity target) {
        if (level().getGameTime() < pineReadyAt || action == 3 || distanceToSqr(target) > 24 * 24) {
            return false;
        }
        startHurl(target);
        return true;
    }

    // ───────────── 阵营、存档 ─────────────

    @Override
    public boolean isAlliedTo(Entity other) {
        if (link.owner != null && (other.getUUID().equals(link.owner)
                || (other instanceof MythSummon s && link.owner.equals(s.link().owner)))) {
            return true;
        }
        Player owner = link.owner == null ? null : level().getPlayerByUUID(link.owner);
        return owner != null && owner.isAlliedTo(other) || super.isAlliedTo(other);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        link.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        link.load(tag);
    }
}
