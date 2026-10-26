package io.github.happysnappingturtle.mythicage.content.greek.ruin;

import io.github.happysnappingturtle.mythicage.content.greek.power.SentinelEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 废弃神殿的守卫：敌对的哨兵石像（T22 设计文档 §4.2，D5）。复用神力版的模型、动作与射击（视线被方块挡住或路上有不该打的
 * 生物就不射），但没有主人、不会到期：
 * <ul>
 *   <li>打 {@value #RANGE} 格内的玩家（创造、旁观除外；和平难度不打）与玩家的召唤物、宠物；</li>
 *   <li>生命 60、护甲 8，每 2 秒一箭 3 点；</li>
 *   <li>打碎不重生（结构只登记一次）；掉大理石与少量青铜（战利品表 {@code entities/ruin_sentinel}）；</li>
 *   <li>不是神话单位（16 号 A6：固定构造物），不实现 {@code Enemy}，村庄的警戒与士兵不管它。</li>
 * </ul>
 */
public class RuinSentinelEntity extends SentinelEntity {
    public static final double RANGE = 14;
    public static final int SHOT_INTERVAL = 40;
    public static final float ARROW_DAMAGE = 3;

    public RuinSentinelEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createRuinAttributes() {
        return SentinelEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.FOLLOW_RANGE, RANGE);
    }

    /** 生成在基座上时已经立着（不播破土）。 */
    public void standing() {
        setPoseState(Pose.IDLE, 0);
    }

    @Override
    protected double range() {
        return RANGE;
    }

    @Override
    protected int shotInterval() {
        return SHOT_INTERVAL;
    }

    @Override
    protected float arrowDamage() {
        return ARROW_DAMAGE;
    }

    @Override
    protected boolean shouldShoot(ServerLevel server, LivingEntity e) {
        if (e == this || !e.isAlive() || server.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (e instanceof Player p) {
            return !p.isCreative() && !p.isSpectator();
        }
        return e instanceof io.github.happysnappingturtle.mythicage.power.OwnedSummon s && s.summonOwner() != null
                || e instanceof OwnableEntity o && o.getOwnerUUID() != null;
    }

    /** 只供 GameTest。 */
    public boolean shouldShootForTest(ServerLevel server, LivingEntity e) {
        return shouldShoot(server, e);
    }

    @Override
    protected boolean shouldDropLoot() {
        return true;
    }
}
