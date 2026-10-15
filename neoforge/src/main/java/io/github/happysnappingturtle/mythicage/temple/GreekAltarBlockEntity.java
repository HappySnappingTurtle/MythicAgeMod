package io.github.happysnappingturtle.mythicage.temple;

import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.temple.TempleSurvey;
import io.github.happysnappingturtle.mythicage.core.temple.TempleVerdict;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 奥林匹斯祭坛的方块实体（T8a 设计文档 §5）：主人、主人时代缓存、最近一次有效扫描的统计与祈祷位。
 * 服务器重启后不信任缓存：加载时重新入队扫描。
 */
public class GreekAltarBlockEntity extends BlockEntity {
    private UUID owner;
    private String ownerName = "";
    private Age ownerAge = Age.NONE;
    private boolean hasResult;
    private TempleSurvey survey = TempleSurvey.EMPTY;
    /** 可用祈祷位（按最高等级的数量选出，按优先顺序排列；实际使用前 N 个）。 */
    private final List<BlockPos> slots = new ArrayList<>();
    private long lastScanTick = Long.MIN_VALUE;
    private boolean unverified;
    private int rulesHash;
    /** 只在内存中保留，供检视界面高亮。 */
    private TempleScan.Highlights highlights;

    public GreekAltarBlockEntity(BlockPos pos, BlockState state) {
        super(GreekTemple.GREEK_ALTAR_BE.get(), pos, state);
    }

    // ───────────── 主人 ─────────────

    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    public String ownerName() {
        return ownerName;
    }

    public Age ownerAge() {
        return ownerAge;
    }

    public void bind(UUID owner, String name, Age age) {
        this.owner = owner;
        this.ownerName = name;
        this.ownerAge = age;
        setChanged();
    }

    void refreshOwnerAge(Age age) {
        if (age != ownerAge) {
            ownerAge = age;
            setChanged();
        }
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
        return rules.evaluate(survey, ownerAge);
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

    void markUnverified(long gameTime) {
        this.unverified = true;
        this.lastScanTick = gameTime;
        setChanged();
    }

    // ───────────── 生命周期 ─────────────

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            TempleService.onAltarLoaded(server, this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("OwnerName", ownerName);
        tag.putString("OwnerAge", ownerAge.id());
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        ownerAge = Age.byId(tag.getString("OwnerAge")).orElse(Age.NONE);
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
    }
}
