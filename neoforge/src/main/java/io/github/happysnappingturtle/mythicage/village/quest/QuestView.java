package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 委托页的快照（T21 设计文档 §9.1）：按看的人算好的——能不能接、已接几个、本村贡献。文本都是语言键 + 参数。
 *
 * @param active       这名玩家在本村已接的未结束任务数
 * @param max          每人每村上限
 * @param contribution 这名玩家在本村的贡献
 * @param enabled      任务总开关
 */
public record QuestView(UUID village, AffairsView.Line title, List<Row> rows, int active, int max, int contribution,
                        boolean enabled) {
    /**
     * 一条任务。
     *
     * @param daysLeft  剩余 MC 日；没有截止日为 −1
     * @param scope     public / personal / event；个人任务已被别人接时是 taken
     * @param rewards   奖励类别：emerald、contribution、conduct
     * @param state     open / paused / complete / invalid / expired / failed / settled
     * @param reason    暂停、失效、失败的原因（语言键行；没有时 key 为空）
     * @param refusal   不能接的原因（语言键后缀；能接为空）
     * @param mine      这名玩家在这个任务里的份额（交了几件 / 修了几格）
     */
    public record Row(String id, String type, AffairsView.Line title, int done, int need, int daysLeft, String scope,
                      List<String> rewards, String state, AffairsView.Line reason, boolean accepted, String refusal,
                      boolean canDeliver, int mine, String sourceKey) {
    }

    public static final StreamCodec<FriendlyByteBuf, QuestView> STREAM_CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUUID(v.village());
                AffairsView.LINE.encode(buf, v.title());
                buf.writeVarInt(v.rows().size());
                for (Row r : v.rows()) {
                    buf.writeUtf(r.id());
                    buf.writeUtf(r.type());
                    AffairsView.LINE.encode(buf, r.title());
                    buf.writeVarInt(r.done());
                    buf.writeVarInt(r.need());
                    buf.writeVarInt(r.daysLeft() + 1);
                    buf.writeUtf(r.scope());
                    buf.writeVarInt(r.rewards().size());
                    r.rewards().forEach(buf::writeUtf);
                    buf.writeUtf(r.state());
                    AffairsView.LINE.encode(buf, r.reason());
                    buf.writeBoolean(r.accepted());
                    buf.writeUtf(r.refusal());
                    buf.writeBoolean(r.canDeliver());
                    buf.writeVarInt(r.mine());
                    buf.writeUtf(r.sourceKey());
                }
                buf.writeVarInt(v.active());
                buf.writeVarInt(v.max());
                buf.writeVarInt(v.contribution());
                buf.writeBoolean(v.enabled());
            },
            buf -> {
                UUID id = buf.readUUID();
                AffairsView.Line title = AffairsView.LINE.decode(buf);
                int n = buf.readVarInt();
                List<Row> rows = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    String rid = buf.readUtf();
                    String type = buf.readUtf();
                    AffairsView.Line t = AffairsView.LINE.decode(buf);
                    int done = buf.readVarInt();
                    int need = buf.readVarInt();
                    int days = buf.readVarInt() - 1;
                    String scope = buf.readUtf();
                    int rn = buf.readVarInt();
                    List<String> rewards = new ArrayList<>(rn);
                    for (int k = 0; k < rn; k++) {
                        rewards.add(buf.readUtf());
                    }
                    String state = buf.readUtf();
                    AffairsView.Line reason = AffairsView.LINE.decode(buf);
                    rows.add(new Row(rid, type, t, done, need, days, scope, List.copyOf(rewards), state, reason, buf.readBoolean(),
                            buf.readUtf(), buf.readBoolean(), buf.readVarInt(), buf.readUtf()));
                }
                return new QuestView(id, title, List.copyOf(rows), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                        buf.readBoolean());
            });
}
