package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * 村民的祈祷记录（T10 设计文档 §3.1，附件 {@code mythic_age:greek_prayer}）。信仰来自村庄登记，
 * 这里只存「最近一次计入的祈祷」：哪一天、为哪位主人、在哪座神坛。
 * <p>
 * 另有几个只在内存里的字段（不存档）：行为是否在运行、下次检查的时刻、已同步给客户端的状态。
 */
public final class PrayerRecord {
    public static final long NEVER = -1;

    private long lastPrayDay = NEVER;
    @Nullable
    private UUID lastPrayOwner;
    @Nullable
    private GlobalPos lastAltar;

    // ───── 只在内存里 ─────
    /** 同一村民的工作包与闲逛包各有一个行为实例，只允许一个在跑。 */
    boolean running;
    /** 下一次允许检查开始条件的 gameTime（节流与放弃后的冷却）。 */
    long nextCheck;
    /** 已发给追踪者的状态位（{@link BelieverStatePayload}）；客户端默认 0。 */
    int syncedFlags;

    public long lastPrayDay() {
        return lastPrayDay;
    }

    @Nullable
    public UUID lastPrayOwner() {
        return lastPrayOwner;
    }

    @Nullable
    public GlobalPos lastAltar() {
        return lastAltar;
    }

    /** 正在跪拜（已同步给客户端的状态）。 */
    public boolean praying() {
        return (syncedFlags & BelieverStatePayload.PRAYING) != 0;
    }

    public boolean prayedOn(long day) {
        return lastPrayDay == day;
    }

    private long creditedDay = NEVER;

    /** 今天已经祈祷满一次并入账（GA-4：一天只来一次）。 */
    public boolean creditedOn(long day) {
        return creditedDay == day;
    }

    public void markCredited(long day) {
        creditedDay = day;
    }

    /** 一次计入的祈祷。 */
    public void record(long day, UUID owner, GlobalPos altar) {
        this.lastPrayDay = day;
        this.lastPrayOwner = owner;
        this.lastAltar = altar;
    }

    /** 神坛被拆后清掉指向它的记录（祈祷日与主人保留：同日一主照样生效）。 */
    public void clearAltar() {
        this.lastAltar = null;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("LastPrayDay", lastPrayDay);
        tag.putLong("CreditedDay", creditedDay);
        if (lastPrayOwner != null) {
            tag.putUUID("LastPrayOwner", lastPrayOwner);
        }
        if (lastAltar != null) {
            tag.putString("AltarDim", lastAltar.dimension().location().toString());
            tag.putLong("AltarPos", lastAltar.pos().asLong());
        }
        return tag;
    }

    public static PrayerRecord load(CompoundTag tag) {
        PrayerRecord r = new PrayerRecord();
        r.lastPrayDay = tag.contains("LastPrayDay") ? tag.getLong("LastPrayDay") : NEVER;
        r.creditedDay = tag.contains("CreditedDay") ? tag.getLong("CreditedDay") : NEVER;
        r.lastPrayOwner = tag.hasUUID("LastPrayOwner") ? tag.getUUID("LastPrayOwner") : null;
        ResourceLocation dim = ResourceLocation.tryParse(tag.getString("AltarDim"));
        r.lastAltar = dim == null || !tag.contains("AltarPos") ? null
                : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(tag.getLong("AltarPos")));
        return r;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PrayerRecord r && lastPrayDay == r.lastPrayDay
                && Objects.equals(lastPrayOwner, r.lastPrayOwner) && Objects.equals(lastAltar, r.lastAltar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastPrayDay, lastPrayOwner, lastAltar);
    }

    @Override
    public String toString() {
        return "PrayerRecord[day=" + lastPrayDay + ", owner=" + lastPrayOwner + ", altar=" + lastAltar
                + (running ? ", running" : "") + ", nextCheck=" + nextCheck + "]";
    }

    /** 默认记录（从没祈祷过）不写进存档。 */
    static final IAttachmentSerializer<CompoundTag, PrayerRecord> SERIALIZER = new IAttachmentSerializer<>() {
        @Override
        public PrayerRecord read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            return load(tag);
        }

        @Override
        @Nullable
        public CompoundTag write(PrayerRecord data, HolderLookup.Provider provider) {
            return data.lastPrayDay == NEVER && data.lastAltar == null ? null : data.save();
        }
    };
}
