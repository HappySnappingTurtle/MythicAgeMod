package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.Objects;
import java.util.UUID;

/**
 * 一个私人恩宠产出点（神坛、纪念碑、水晶塔……）的台账（T5 设计文档 §3）。
 * 只在服务端主线程上修改；{@link #lastSettledTick()} 是去重的唯一依据。
 */
public final class ProducerLedger {
    /** 标记「没有进行中的计时」。 */
    public static final long NONE = -1;

    private final ProducerId id;
    private final UUID owner;
    private final Civilization civ;
    private double estimatedRatePerDay;
    private double snapshotRatePerDay;
    private boolean verified;
    private boolean snapshotValid = true;
    private long lastSettledTick;
    private long lastVisitTick;
    private double carry;
    private double offlineBuffer;
    private long nearbySinceTick = NONE;
    private long realStreakStartTick = NONE;
    private double realStreakYield;
    private long awayActiveTicks;

    /** 新建：结算与照看时刻都从 now 开始，快照产率先取公式估计。 */
    public ProducerLedger(ProducerId id, UUID owner, Civilization civ, double estimatedRatePerDay, long now) {
        this.id = Objects.requireNonNull(id);
        this.owner = Objects.requireNonNull(owner);
        this.civ = Objects.requireNonNull(civ);
        this.estimatedRatePerDay = Math.max(0, estimatedRatePerDay);
        this.snapshotRatePerDay = this.estimatedRatePerDay;
        this.lastSettledTick = now;
        this.lastVisitTick = now;
    }

    public ProducerId id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public Civilization civ() {
        return civ;
    }

    /** 公式估计的日产率（由产出点按当前结构计算）。 */
    public double estimatedRatePerDay() {
        return estimatedRatePerDay;
    }

    /** 快照日产率：未验证时等于公式估计；验证后取估计与实测的较小值。 */
    public double snapshotRatePerDay() {
        return snapshotRatePerDay;
    }

    public boolean verified() {
        return verified;
    }

    public boolean snapshotValid() {
        return snapshotValid;
    }

    public long lastSettledTick() {
        return lastSettledTick;
    }

    public long lastVisitTick() {
        return lastVisitTick;
    }

    public double carry() {
        return carry;
    }

    public double offlineBuffer() {
        return offlineBuffer;
    }

    public long nearbySinceTick() {
        return nearbySinceTick;
    }

    public long realStreakStartTick() {
        return realStreakStartTick;
    }

    public double realStreakYield() {
        return realStreakYield;
    }

    /** 方案 C：离开期间主人活跃的 tick 数（待补算）。 */
    public long awayActiveTicks() {
        return awayActiveTicks;
    }

    /** 产出点结构变化后更新公式估计；已验证的快照不会因此升高。 */
    public void setEstimatedRatePerDay(double rate) {
        estimatedRatePerDay = Math.max(0, rate);
        snapshotRatePerDay = verified ? Math.min(snapshotRatePerDay, estimatedRatePerDay) : estimatedRatePerDay;
    }

    /** 完整观测日的实测产率：快照取估计与实测的较小值，并标记已验证。 */
    public void recordObservation(double measuredRatePerDay) {
        snapshotRatePerDay = Math.min(estimatedRatePerDay, Math.max(0, measuredRatePerDay));
        verified = true;
    }

    /** 拆除、核对失败或规则升级：快照不可再用。 */
    public void invalidate() {
        snapshotValid = false;
    }

    // ---- 以下供结算引擎与存档读取使用 ----

    public void setSnapshotRatePerDay(double v) {
        snapshotRatePerDay = Math.max(0, v);
    }

    public void setVerified(boolean v) {
        verified = v;
    }

    public void setSnapshotValid(boolean v) {
        snapshotValid = v;
    }

    public void setLastSettledTick(long v) {
        lastSettledTick = v;
    }

    public void setLastVisitTick(long v) {
        lastVisitTick = v;
    }

    public void setCarry(double v) {
        carry = Math.max(0, v);
    }

    public void setOfflineBuffer(double v) {
        offlineBuffer = Math.max(0, v);
    }

    public void setNearbySinceTick(long v) {
        nearbySinceTick = v;
    }

    public void setRealStreak(long startTick, double yield) {
        realStreakStartTick = startTick;
        realStreakYield = Math.max(0, yield);
    }

    public void setAwayActiveTicks(long v) {
        awayActiveTicks = Math.max(0, v);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProducerLedger l
                && id.equals(l.id) && owner.equals(l.owner) && civ == l.civ
                && Double.compare(estimatedRatePerDay, l.estimatedRatePerDay) == 0
                && Double.compare(snapshotRatePerDay, l.snapshotRatePerDay) == 0
                && verified == l.verified && snapshotValid == l.snapshotValid
                && lastSettledTick == l.lastSettledTick && lastVisitTick == l.lastVisitTick
                && Double.compare(carry, l.carry) == 0 && Double.compare(offlineBuffer, l.offlineBuffer) == 0
                && nearbySinceTick == l.nearbySinceTick && realStreakStartTick == l.realStreakStartTick
                && Double.compare(realStreakYield, l.realStreakYield) == 0
                && awayActiveTicks == l.awayActiveTicks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, owner, civ, lastSettledTick);
    }

    @Override
    public String toString() {
        return "ProducerLedger[" + id + ", owner=" + owner + ", civ=" + civ.id()
                + ", rate=" + snapshotRatePerDay + "/" + estimatedRatePerDay + (verified ? " verified" : "")
                + (snapshotValid ? "" : " INVALID") + ", settled=" + lastSettledTick + ", visit=" + lastVisitTick
                + ", carry=" + carry + ", buffer=" + offlineBuffer + ", away=" + awayActiveTicks + "]";
    }
}
