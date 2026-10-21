package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 召唤单位实体身上的登记副本（T14 设计文档 §3.2）：主人、单位 id、单位卡、模式、驻守点。
 * 登记（{@link SummonRegistry}）是真相；指令同时改两边，实体载入时以登记为准。攻击指令的目标只在内存里。
 */
public final class SummonLink {
    @Nullable
    public UUID owner;
    @Nullable
    public UUID unitId;
    public String card = "";
    public SummonMode mode = SummonMode.FOLLOW;
    /** 驻守点（与实体同维度）。 */
    @Nullable
    public BlockPos guard;
    /** 传令杖指定的攻击目标（不存档）。 */
    @Nullable
    public UUID forcedTarget;

    public boolean bound() {
        return owner != null && unitId != null;
    }

    public void save(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        if (owner != null) {
            t.putUUID("owner", owner);
        }
        if (unitId != null) {
            t.putUUID("unit", unitId);
        }
        t.putString("card", card);
        t.putString("mode", mode.id());
        if (guard != null) {
            t.putLong("guard", guard.asLong());
        }
        tag.put("MythSummon", t);
    }

    public void load(CompoundTag tag) {
        if (!tag.contains("MythSummon")) {
            return;
        }
        CompoundTag t = tag.getCompound("MythSummon");
        owner = t.hasUUID("owner") ? t.getUUID("owner") : null;
        unitId = t.hasUUID("unit") ? t.getUUID("unit") : null;
        card = t.getString("card");
        mode = SummonMode.byId(t.getString("mode")).orElse(SummonMode.FOLLOW);
        guard = t.contains("guard") ? BlockPos.of(t.getLong("guard")) : null;
    }
}
