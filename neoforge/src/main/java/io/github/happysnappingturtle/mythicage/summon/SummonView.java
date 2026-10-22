package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 召唤管理界面的快照（T14 设计文档 §7、§11.2）。全部由服务端组装，客户端只展示。
 *
 * @param occupied 占用（存活与等待重生的常驻单位 cost 之和）
 * @param cap      上限
 */
public record SummonView(int occupied, int cap, List<Row> rows) {

    /** 单位在哪里。 */
    public enum Where {
        /** 与主人同维度且已载入：{@code value} 是距离（格）。 */
        NEAR,
        /** 在别的维度：{@code dimension} 是维度 id。 */
        DIMENSION,
        /** 区块未载入（同维度）。 */
        UNLOADED,
        /** 等待重生：{@code value} 是剩余秒数。 */
        RESPAWNING
    }

    /**
     * 一个单位。
     *
     * @param entity    实体类型 id（客户端用它取名字）
     * @param token     召唤符物品 id（作头像）
     * @param health    当前生命；未载入时为 −1
     * @param dimension {@link Where#DIMENSION} 时的维度 id，其余为空串
     */
    public record Row(UUID unitId, String card, String entity, String token, boolean combat, float health, float maxHealth,
                      SummonMode mode, Where where, int value, String dimension, boolean recallable) {
    }

    public static final StreamCodec<FriendlyByteBuf, SummonView> STREAM_CODEC = StreamCodec.of(SummonView::write, SummonView::read);

    private static void write(FriendlyByteBuf buf, SummonView v) {
        buf.writeVarInt(v.occupied);
        buf.writeVarInt(v.cap);
        buf.writeVarInt(v.rows.size());
        for (Row r : v.rows) {
            buf.writeUUID(r.unitId);
            buf.writeUtf(r.card);
            buf.writeUtf(r.entity);
            buf.writeUtf(r.token);
            buf.writeBoolean(r.combat);
            buf.writeFloat(r.health);
            buf.writeFloat(r.maxHealth);
            buf.writeEnum(r.mode);
            buf.writeEnum(r.where);
            buf.writeVarInt(r.value);
            buf.writeUtf(r.dimension);
            buf.writeBoolean(r.recallable);
        }
    }

    private static SummonView read(FriendlyByteBuf buf) {
        int occupied = buf.readVarInt();
        int cap = buf.readVarInt();
        int n = buf.readVarInt();
        List<Row> rows = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            rows.add(new Row(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readFloat(),
                    buf.readFloat(), buf.readEnum(SummonMode.class), buf.readEnum(Where.class), buf.readVarInt(), buf.readUtf(),
                    buf.readBoolean()));
        }
        return new SummonView(occupied, cap, List.copyOf(rows));
    }
}
