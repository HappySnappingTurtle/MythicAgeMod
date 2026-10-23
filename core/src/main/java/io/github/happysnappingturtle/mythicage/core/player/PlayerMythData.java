package io.github.happysnappingturtle.mythicage.core.player;

import io.github.happysnappingturtle.mythicage.core.relic.RelicBook;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 一个玩家的神话数据（T4 设计文档 §2）。可变聚合，只应在服务端主线程上修改；
 * 每次通过 {@link #markChanged()} 递增 {@link #dataVersion()}，用于同步去重。
 */
public final class PlayerMythData {
    /** 善恶最近事件最多保留的条数。 */
    public static final int RECENT_CONDUCT_LIMIT = 20;

    /** 每个文明的状态。 */
    public static final class CivState {
        private long favor;
        private Age age = Age.NONE;
        /** 已结识的村庄主神（T10a）：只作图鉴记录（18 号 §6），不参与候选计算。 */
        private final Set<String> knownPatrons = new LinkedHashSet<>();
        /** 各时代供奉的次神（T11a 写入）：时代 → 次神 id。 */
        private final EnumMap<Age, String> minorGods = new EnumMap<>(Age.class);
        /** 主神认可（T11a）：主神 → 在奉祀该主神的村庄以之晋升或立祀过的最高时代。 */
        private final Map<String, Age> approvals = new LinkedHashMap<>();
        /** Boss 凭证（T11a，按试炼档）：classical = 古典试炼（用于英雄级），heroic = 英雄试炼（用于神话级）。 */
        private final EnumMap<Age, Integer> trialVouchers = new EnumMap<>(Age.class);
        /** 最近一次改奉的 MC 日；−1 = 从未。 */
        private long rededicatedDay = -1;
        /** 举行过仪式的村庄（T11a，信仰面板「你的村庄」）。 */
        private final Map<UUID, RiteMark> riteVillages = new LinkedHashMap<>();
        /** 已晋升、还没弹过成功界面的时代（按晋升顺序）。 */
        private final List<Age> pendingUnlocks = new java.util.ArrayList<>();
        /** 本文明神力的共享短冷却到期时刻（世界时钟刻，T12）；0 = 无。 */
        private long powerCooldownUntil;

        public long favor() {
            return favor;
        }

        public Age age() {
            return age;
        }

        /** 直接设定恩宠（只供事务步骤、存档读取与运维命令使用）；负数按 0 处理。 */
        public void setFavor(long favor) {
            this.favor = Math.max(0, favor);
        }

        public void setAge(Age age) {
            this.age = Objects.requireNonNull(age);
        }

        public Set<String> knownPatrons() {
            return knownPatrons;
        }

        public Map<Age, String> minorGods() {
            return minorGods;
        }

        /** 当前是否供奉某位次神（任一时代）。 */
        public boolean worships(String minorGod) {
            return minorGods.containsValue(minorGod);
        }

        public Map<String, Age> approvals() {
            return approvals;
        }

        /** 某位主神认可到的最高时代；没有时为 NONE。 */
        public Age approval(String patron) {
            return approvals.getOrDefault(patron, Age.NONE);
        }

        /** 认可只升不降。 */
        public void approve(String patron, Age age) {
            if (age.atLeast(approval(patron)) && age != Age.NONE) {
                approvals.put(patron, age);
            }
        }

        public Map<Age, Integer> trialVouchers() {
            return trialVouchers;
        }

        public int vouchers(Age tier) {
            return trialVouchers.getOrDefault(tier, 0);
        }

        public void setVouchers(Age tier, int count) {
            if (count <= 0) {
                trialVouchers.remove(tier);
            } else {
                trialVouchers.put(tier, count);
            }
        }

        public long rededicatedDay() {
            return rededicatedDay;
        }

        public void setRededicatedDay(long day) {
            this.rededicatedDay = day;
        }

        public Map<UUID, RiteMark> riteVillages() {
            return riteVillages;
        }

        /** 记下在某村举行过的仪式：最高时代取大，晋升过则一直标为晋升地。 */
        public void markRiteVillage(UUID village, Age age, boolean promotion) {
            RiteMark old = riteVillages.get(village);
            Age highest = old == null || age.atLeast(old.highest()) ? age : old.highest();
            riteVillages.put(village, new RiteMark(highest, promotion || (old != null && old.promotedHere())));
        }

        public List<Age> pendingUnlocks() {
            return pendingUnlocks;
        }

        public long powerCooldownUntil() {
            return powerCooldownUntil;
        }

        public void setPowerCooldownUntil(long tick) {
            this.powerCooldownUntil = Math.max(0, tick);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CivState c && c.favor == favor && c.age == age
                    && List.copyOf(c.knownPatrons).equals(List.copyOf(knownPatrons)) && c.minorGods.equals(minorGods)
                    && c.approvals.equals(approvals) && c.trialVouchers.equals(trialVouchers)
                    && c.rededicatedDay == rededicatedDay && c.riteVillages.equals(riteVillages)
                    && c.pendingUnlocks.equals(pendingUnlocks) && c.powerCooldownUntil == powerCooldownUntil;
        }

        @Override
        public int hashCode() {
            return Objects.hash(favor, age, knownPatrons, minorGods, approvals, trialVouchers, rededicatedDay);
        }

        void copyFrom(CivState o) {
            favor = o.favor;
            age = o.age;
            knownPatrons.addAll(o.knownPatrons);
            minorGods.putAll(o.minorGods);
            approvals.putAll(o.approvals);
            trialVouchers.putAll(o.trialVouchers);
            rededicatedDay = o.rededicatedDay;
            riteVillages.putAll(o.riteVillages);
            pendingUnlocks.addAll(o.pendingUnlocks);
            powerCooldownUntil = o.powerCooldownUntil;
        }
    }

    /** 在某村举行过的仪式：最高时代、是否在此晋升过（否则是立祀或改奉）。 */
    public record RiteMark(Age highest, boolean promotedHere) {
    }

    /** 神器锻造记录：generation 每次重铸 +1（决策 D2）。 */
    public record ForgeRecord(int generation) {
        public ForgeRecord {
            if (generation < 1) {
                throw new IllegalArgumentException("generation 从 1 开始：" + generation);
            }
        }
    }

    /** 一条善恶事件记录（T18 决定 reason 的取值）。 */
    public record ConductEntry(String reason, int delta, long gameTime) {
    }

    private final EnumMap<Civilization, CivState> civs = new EnumMap<>(Civilization.class);
    private final Set<String> firstKills = new LinkedHashSet<>();
    /** 试炼 Boss 合法击败次数（T11a）：Boss 实体 id → 次数。 */
    private final Map<String, Integer> trialKills = new LinkedHashMap<>();
    /** 各神力的长冷却到期时刻（世界时钟刻，T12）：神力 id → 刻。 */
    private final Map<String, Long> powerCooldowns = new LinkedHashMap<>();
    private final Set<String> discoveries = new LinkedHashSet<>();
    private final Set<String> blueprints = new LinkedHashSet<>();
    private final Map<String, ForgeRecord> forgeRecords = new LinkedHashMap<>();
    private int conductScore;
    private final Deque<ConductEntry> recentConduct = new ArrayDeque<>();
    private final ReceiptBook receipts = new ReceiptBook();
    private final DailyLedger tradeDaily = new DailyLedger();
    /** 圣物绑定表（T16）。 */
    private final RelicBook relics = new RelicBook();
    private long dataVersion;

    public PlayerMythData() {
        for (Civilization c : Civilization.values()) {
            civs.put(c, new CivState());
        }
    }

    public CivState civ(Civilization c) {
        return civs.get(c);
    }

    public Set<String> firstKills() {
        return firstKills;
    }

    public Map<String, Long> powerCooldowns() {
        return powerCooldowns;
    }

    public Map<String, Integer> trialKills() {
        return trialKills;
    }

    public Set<String> discoveries() {
        return discoveries;
    }

    public Set<String> blueprints() {
        return blueprints;
    }

    public Map<String, ForgeRecord> forgeRecords() {
        return forgeRecords;
    }

    /** 记录一次锻造或重铸：已有记录时 generation +1。 */
    public ForgeRecord recordForge(String artifactId) {
        ForgeRecord next = forgeRecords.containsKey(artifactId)
                ? new ForgeRecord(forgeRecords.get(artifactId).generation() + 1)
                : new ForgeRecord(1);
        forgeRecords.put(artifactId, next);
        return next;
    }

    public int conductScore() {
        return conductScore;
    }

    public void setConductScore(int score) {
        this.conductScore = score;
    }

    /** 从旧到新排列，最多 {@link #RECENT_CONDUCT_LIMIT} 条。 */
    public List<ConductEntry> recentConduct() {
        return List.copyOf(recentConduct);
    }

    public void addConductEntry(ConductEntry entry) {
        recentConduct.addLast(entry);
        while (recentConduct.size() > RECENT_CONDUCT_LIMIT) {
            recentConduct.removeFirst();
        }
    }

    public ReceiptBook receipts() {
        return receipts;
    }

    /** 每日限购台账（T9）。 */
    public DailyLedger tradeDaily() {
        return tradeDaily;
    }

    /** 圣物绑定表（T16 设计文档 §2.2）。 */
    public RelicBook relics() {
        return relics;
    }

    public long dataVersion() {
        return dataVersion;
    }

    /** 读档时恢复版本号。 */
    public void setDataVersion(long dataVersion) {
        this.dataVersion = dataVersion;
    }

    public void markChanged() {
        dataVersion++;
    }

    /** 深拷贝。 */
    public PlayerMythData copy() {
        PlayerMythData c = new PlayerMythData();
        for (Civilization civ : Civilization.values()) {
            c.civ(civ).copyFrom(civ(civ));
        }
        c.firstKills.addAll(firstKills);
        c.trialKills.putAll(trialKills);
        c.powerCooldowns.putAll(powerCooldowns);
        c.discoveries.addAll(discoveries);
        c.blueprints.addAll(blueprints);
        c.forgeRecords.putAll(forgeRecords);
        c.conductScore = conductScore;
        c.recentConduct.addAll(recentConduct);
        ReceiptBook r = receipts.copy();
        for (ReceiptBook.Scope scope : ReceiptBook.Scope.values()) {
            for (String id : r.list(scope)) {
                c.receipts.record(scope, id);
            }
        }
        c.tradeDaily.entriesFrom(tradeDaily);
        c.relics.copyFrom(relics);
        c.dataVersion = dataVersion;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlayerMythData d
                && civs.equals(d.civs)
                && List.copyOf(firstKills).equals(List.copyOf(d.firstKills))
                && trialKills.equals(d.trialKills) && powerCooldowns.equals(d.powerCooldowns)
                && List.copyOf(discoveries).equals(List.copyOf(d.discoveries))
                && List.copyOf(blueprints).equals(List.copyOf(d.blueprints))
                && forgeRecords.equals(d.forgeRecords)
                && conductScore == d.conductScore
                && List.copyOf(recentConduct).equals(List.copyOf(d.recentConduct))
                && receipts.equals(d.receipts)
                && tradeDaily.equals(d.tradeDaily)
                && relics.equals(d.relics)
                && dataVersion == d.dataVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(civs, firstKills, discoveries, blueprints, forgeRecords, conductScore, dataVersion);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PlayerMythData{v").append(dataVersion);
        civs.forEach((k, v) -> sb.append(' ').append(k.id()).append('=').append(v.favor()).append('/').append(v.age().id()));
        return sb.append('}').toString();
    }

    /** 只读视图，供序列化遍历。 */
    public Map<Civilization, CivState> civsView() {
        return Collections.unmodifiableMap(civs);
    }
}
