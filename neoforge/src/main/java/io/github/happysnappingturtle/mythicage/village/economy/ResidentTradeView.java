package io.github.happysnappingturtle.mythicage.village.economy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * 居民交易界面的快照（T19 设计文档 §1.8 I7）。
 *
 * @param resident 居民实体 id
 * @param name     居民名字
 * @param profession 职业的语言键
 */
public record ResidentTradeView(int resident, String name, String profession, List<Row> rows) {
    /** 不能成交的原因（客户端灰掉按钮并显示）。 */
    public enum State { OK, OUT_OF_STOCK, DAILY_DONE, NO_EMERALD, NO_ITEM, FULL }

    /**
     * @param stock     村库还能卖出几份；杂货为 −1
     * @param remaining 今天还能做几份
     */
    public record Row(String id, boolean sell, String item, int count, int price, int basePrice, int stock, int remaining,
                      State state) {
    }

    public static final StreamCodec<FriendlyByteBuf, ResidentTradeView> STREAM_CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeVarInt(v.resident());
                buf.writeUtf(v.name());
                buf.writeUtf(v.profession());
                buf.writeVarInt(v.rows().size());
                for (Row r : v.rows()) {
                    buf.writeUtf(r.id());
                    buf.writeBoolean(r.sell());
                    buf.writeUtf(r.item());
                    buf.writeVarInt(r.count());
                    buf.writeVarInt(r.price());
                    buf.writeVarInt(r.basePrice());
                    buf.writeInt(r.stock());
                    buf.writeVarInt(r.remaining());
                    buf.writeEnum(r.state());
                }
            },
            buf -> {
                int id = buf.readVarInt();
                String name = buf.readUtf();
                String prof = buf.readUtf();
                int n = buf.readVarInt();
                List<Row> rows = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    rows.add(new Row(buf.readUtf(), buf.readBoolean(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                            buf.readVarInt(), buf.readInt(), buf.readVarInt(), buf.readEnum(State.class)));
                }
                return new ResidentTradeView(id, name, prof, List.copyOf(rows));
            });
}
