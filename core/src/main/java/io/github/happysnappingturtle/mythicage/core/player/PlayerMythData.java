package io.github.happysnappingturtle.mythicage.core.player;

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

        @Override
        public boolean equals(Object o) {
            return o instanceof CivState c && c.favor == favor && c.age == age;
        }

        @Override
        public int hashCode() {
            return Objects.hash(favor, age);
        }
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
    private final Set<String> discoveries = new LinkedHashSet<>();
    private final Set<String> blueprints = new LinkedHashSet<>();
    private final Map<String, ForgeRecord> forgeRecords = new LinkedHashMap<>();
    private int conductScore;
    private final Deque<ConductEntry> recentConduct = new ArrayDeque<>();
    private final ReceiptBook receipts = new ReceiptBook();
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
            c.civ(civ).setFavor(civ(civ).favor());
            c.civ(civ).setAge(civ(civ).age());
        }
        c.firstKills.addAll(firstKills);
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
        c.dataVersion = dataVersion;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlayerMythData d
                && civs.equals(d.civs)
                && List.copyOf(firstKills).equals(List.copyOf(d.firstKills))
                && List.copyOf(discoveries).equals(List.copyOf(d.discoveries))
                && List.copyOf(blueprints).equals(List.copyOf(d.blueprints))
                && forgeRecords.equals(d.forgeRecords)
                && conductScore == d.conductScore
                && List.copyOf(recentConduct).equals(List.copyOf(d.recentConduct))
                && receipts.equals(d.receipts)
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
