package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleSurvey;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleVerdict;
import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 奥林匹斯祭坛的方块实体（T8a 设计文档 §5）：最近一次有效扫描的统计与祈祷位；主人与时代缓存在
 * {@link ShrineBlockEntity}（T10b）；近 7 日在此计入祈祷的信徒（T10 设计文档 §3.2）。
 * 服务器重启后不信任缓存：加载时重新入队扫描。
 */
public class GreekAltarBlockEntity extends ShrineBlockEntity {
    private boolean hasResult;
    private TempleSurvey survey = TempleSurvey.EMPTY;
    /** 可用祈祷位（按最高等级的数量选出，按优先顺序排列；实际使用前 N 个）。 */
    private final List<BlockPos> slots = new ArrayList<>();
    private long lastScanTick = Long.MIN_VALUE;
    private boolean unverified;
    private int rulesHash;
    /** 只在内存中保留，供检视界面高亮。 */
    private TempleScan.Highlights highlights;
    /** 近 {@link #RECENT_DAYS} 个 MC 日在此计入祈祷的村民 → 最近一次的日序号；按时间先后，最多 {@link #RECENT_LIMIT} 个。 */
    private final LinkedHashMap<UUID, Long> recentPrayers = new LinkedHashMap<>();
    public static final int RECENT_DAYS = 7;
    public static final int RECENT_LIMIT = 64;

    public GreekAltarBlockEntity(BlockPos pos, BlockState state) {
        super(GreekTemple.GREEK_ALTAR_BE.get(), pos, state);
    }

    @Override
    public ShrineKind kind() {
        return GreekTemple.ALTAR_KIND;
    }

    // ───────────── 判定结果 ─────────────

    public boolean hasResult() {
        return hasResult;
    }

    public TempleSurvey survey() {
        return survey;
    }

    public boolean unverified() {
        return unverified;
    }

    public long lastScanTick() {
        return lastScanTick;
    }

    int rulesHash() {
        return rulesHash;
    }

    TempleScan.Highlights highlights() {
        return highlights;
    }

    public TempleVerdict verdict(TempleRules rules) {
        return rules.evaluate(survey, ownerAge());
    }

    /** 认定等级下实际可用的祈祷位。 */
    public List<BlockPos> prayerSlots(TempleRules rules) {
        int n = rules.tier(verdict(rules).grantedLevel()).prayerSlots();
        return List.copyOf(slots.subList(0, Math.min(n, slots.size())));
    }

    void applyScan(TempleScan.Outcome outcome, long gameTime, int rulesHash) {
        this.survey = outcome.survey();
        this.slots.clear();
        this.slots.addAll(outcome.prayerSlotsAll());
        this.highlights = outcome.highlights();
        this.hasResult = true;
        this.unverified = false;
        this.lastScanTick = gameTime;
        this.rulesHash = rulesHash;
        setChanged();
    }

    // ───────────── 信徒（T10） ─────────────

    /** 记下一次计入的祈祷；返回近 7 日信徒数是否因此变化。 */
    public boolean notePrayer(UUID villager, long day) {
        Long before = recentPrayers.remove(villager);
        recentPrayers.put(villager, day);
        boolean changed = before == null;
        changed |= prune(day);
        while (recentPrayers.size() > RECENT_LIMIT) {
            recentPrayers.remove(recentPrayers.keySet().iterator().next());
            changed = true;
        }
        if (before == null || before != day) {
            setChanged();
        }
        return changed;
    }

    /** 测试与检视用的只读副本。 */
    public Map<UUID, Long> recentPrayers() {
        return Map.copyOf(recentPrayers);
    }

    /** 近 7 日（含今天）在此计入祈祷的信徒数。 */
    public int believers(long today) {
        if (prune(today)) {
            setChanged();
        }
        return recentPrayers.size();
    }

    private boolean prune(long today) {
        boolean removed = false;
        for (Iterator<Map.Entry<UUID, Long>> it = recentPrayers.entrySet().iterator(); it.hasNext(); ) {
            if (today - it.next().getValue() >= RECENT_DAYS) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    void markUnverified(long gameTime) {
        this.unverified = true;
        this.lastScanTick = gameTime;
        setChanged();
    }

    // ───────────── 生命周期 ─────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("HasResult", hasResult);
        tag.putIntArray("Survey", new int[]{survey.columns(), survey.raisedColumns(), survey.roofCovered(),
                survey.roofArea(), survey.openAltar() ? 1 : 0, survey.platformReachable() ? 1 : 0, survey.litBraziers(),
                survey.statues(), survey.pediments(), survey.marbleFloor()});
        ListTag list = new ListTag();
        for (BlockPos p : slots) {
            list.add(LongTag.valueOf(p.asLong()));
        }
        tag.put("Slots", list);
        tag.putLong("LastScan", lastScanTick);
        tag.putBoolean("Unverified", unverified);
        tag.putInt("RulesHash", rulesHash);
        ListTag recent = new ListTag();
        recentPrayers.forEach((id, day) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Id", id);
            c.putLong("Day", day);
            recent.add(c);
        });
        tag.put("RecentPrayers", recent);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hasResult = tag.getBoolean("HasResult");
        int[] s = tag.getIntArray("Survey");
        // 格式不符（含修订 01 之前的 6 项旧格式）时不信任，等加载后的重扫
        survey = s.length == 10 ? new TempleSurvey(s[0], s[1], s[2], s[3], s[4] != 0, s[5] != 0, s[6], s[7], s[8], s[9])
                : TempleSurvey.EMPTY;
        slots.clear();
        for (Tag t : tag.getList("Slots", Tag.TAG_LONG)) {
            slots.add(BlockPos.of(((LongTag) t).getAsLong()));
        }
        lastScanTick = tag.getLong("LastScan");
        unverified = tag.getBoolean("Unverified");
        rulesHash = tag.getInt("RulesHash");
        recentPrayers.clear();
        for (Tag t : tag.getList("RecentPrayers", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.hasUUID("Id")) {
                recentPrayers.put(c.getUUID("Id"), c.getLong("Day"));
            }
        }
    }
}
