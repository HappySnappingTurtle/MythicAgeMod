package io.github.happysnappingturtle.mythicage.core.power;

import java.util.Set;

/**
 * 神力「停火」（GP-GR-05，T25a §2）的纯规则：谁的伤害被拦、什么算破约、Boss 停手几刻、施法者移速、哪些生成被拦。
 * 世界查询（谁在区内、伤害类型、攻击归属）由平台层做完再传进来。
 * <p>
 * 被放出休战的一方（Boss 停手到期后的 Boss 与它的竞技场战斗参与者）按「在区外」算，于是 D18 的「只要有一方在区内就拦」照样适用：
 * 场边的小怪仍受保护，攻击它们仍算破约。涉及「已放出的 Boss」本身的伤害（不论哪一方）一律不拦、也不算破约，Boss 战照常进行
 * （原版 Boss 没有参与者名单，靠这一条恢复战斗）。
 */
public final class CeasefireRules {
    /** 半径（PowerSpec 上限）、持续、吟唱、长冷却（刻）。 */
    public static final int RADIUS = 64;
    public static final int DURATION_TICKS = 600;
    public static final int CAST_TICKS = 40;
    public static final int COOLDOWN_TICKS = 3600;
    /** Boss 第一次被停火时停手多久（8 秒）。 */
    public static final int BOSS_TICKS = 160;
    /** 施法者移速加成。 */
    public static final double CASTER_SPEED = 0.30;

    /**
     * 一方的处境。
     *
     * @param inside   在某个仍有效的休战区里（区内竖直方向 ±16 由平台层判）
     * @param released 已被放出休战（Boss 停手到期后的 Boss 与它的战斗参与者）
     * @param freeBoss 自己就是已放出的 Boss：涉及它的伤害都不拦、不算破约
     */
    public record Party(boolean inside, boolean released, boolean freeBoss) {
        public static final Party OUTSIDE = new Party(false, false, false);
        public static final Party INSIDE = new Party(true, false, false);

        public Party(boolean inside, boolean released) {
            this(inside, released, false);
        }

        /** 已放出的 Boss（停手到期或从没被停手）。 */
        public static Party freeBoss(boolean inside) {
            return new Party(inside, true, true);
        }

        /** 按规则算的「在区内」：被放出的算在区外。 */
        public boolean shielded() {
            return inside && !released;
        }
    }

    private CeasefireRules() {
    }

    /**
     * 这次伤害拦不拦（D18：有一方在区内就拦）。环境伤害（岩浆、摔落、溺水、饥饿……）照常。
     * 休战开始前射出、休战中命中的投射物也拦（D5），只是它不算破约，见 {@link #breaches}。
     */
    public static boolean blocks(Party attacker, Party victim, boolean environmental) {
        return !environmental && !attacker.freeBoss() && !victim.freeBoss() && (attacker.shielded() || victim.shielded());
    }

    /**
     * 这个攻击动作（挥砍、射出投射物、施放伤人的神力、生物发起近战）算不算破约：发起方或目标有一方按规则在区内。
     * 目标未知（射向空处）时只看发起方。
     */
    public static boolean breaches(Party attacker, Party target) {
        return !attacker.freeBoss() && !target.freeBoss() && (attacker.shielded() || target.shielded());
    }

    /** Boss 被这一次停火停手多少刻：同一场 Boss 战的第一次 8 秒，第二次起 0（D3 的「一场」由平台层给出计数）。 */
    public static int bossTicks(int earlierCastsThisFight) {
        return earlierCastsThisFight <= 0 ? BOSS_TICKS : 0;
    }

    /** Boss 现在还在停手吗。 */
    public static boolean bossHeld(long now, long heldUntil) {
        return now < heldUntil;
    }

    /** 施法者的移速加成还该在吗（D2）：休战还有效、没破约、施法者还在区里。 */
    public static boolean casterBuff(boolean truceActive, boolean casterInside) {
        return truceActive && casterInside;
    }

    /** 休战还有效吗：没破约、没到期。 */
    public static boolean active(long now, long endTick, boolean broken) {
        return !broken && now < endTick;
    }

    /** 放行的生成方式（D4）：繁殖、命令、刷怪蛋（含发射器）、桶装、转化；其余（自然、巡逻、刷怪笼、试炼刷怪器、增援……）都判失败。 */
    public static final Set<String> ALLOWED_SPAWNS = Set.of("BREEDING", "COMMAND", "SPAWN_EGG", "DISPENSER", "BUCKET", "CONVERSION");

    /** 区内这次生成拦不拦：只拦敌对生物。spawnType 是原版 {@code MobSpawnType} 的名字。 */
    public static boolean blocksSpawn(boolean hostile, String spawnType) {
        return hostile && !ALLOWED_SPAWNS.contains(spawnType);
    }
}
