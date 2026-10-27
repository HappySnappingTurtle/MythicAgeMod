package io.github.happysnappingturtle.mythicage.conduct;

import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * 「善恶与贡献」界面的快照（T24 设计文档 §3）：只含请求者本人的数据（D6）。
 *
 * @param tier          显示档名（{@code ConductDisplay.Tier#id}）
 * @param positiveToday 今天已得的正分（每日上限 positiveCap）
 * @param recent        最近有效事件，新的在前
 * @param villages      本人有贡献的村庄，按贡献从高到低
 */
public record ConductView(boolean enabled, int score, int min, int max, String tier, int positiveToday, int positiveCap,
                          List<Event> recent, List<Village> villages) {
    /** @param daysAgo 距今几个游戏日（0 = 今天） */
    public record Event(String reason, int delta, long daysAgo) {
    }

    public record Village(AffairsView.Line title, int points, List<Event> recent) {
    }

    private static final StreamCodec<FriendlyByteBuf, Event> EVENT = StreamCodec.of(
            (buf, e) -> {
                buf.writeUtf(e.reason());
                buf.writeVarInt(e.delta());
                buf.writeVarLong(e.daysAgo());
            },
            buf -> new Event(buf.readUtf(), buf.readVarInt(), buf.readVarLong()));

    public static final StreamCodec<FriendlyByteBuf, ConductView> STREAM_CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeBoolean(v.enabled());
                buf.writeVarInt(v.score());
                buf.writeVarInt(v.min());
                buf.writeVarInt(v.max());
                buf.writeUtf(v.tier());
                buf.writeVarInt(v.positiveToday());
                buf.writeVarInt(v.positiveCap());
                writeEvents(buf, v.recent());
                buf.writeVarInt(v.villages().size());
                for (Village vi : v.villages()) {
                    AffairsView.LINE.encode(buf, vi.title());
                    buf.writeVarInt(vi.points());
                    writeEvents(buf, vi.recent());
                }
            },
            buf -> {
                boolean enabled = buf.readBoolean();
                int score = buf.readVarInt();
                int min = buf.readVarInt();
                int max = buf.readVarInt();
                String tier = buf.readUtf();
                int today = buf.readVarInt();
                int cap = buf.readVarInt();
                List<Event> recent = readEvents(buf);
                int n = buf.readVarInt();
                List<Village> villages = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    villages.add(new Village(AffairsView.LINE.decode(buf), buf.readVarInt(), readEvents(buf)));
                }
                return new ConductView(enabled, score, min, max, tier, today, cap, recent, List.copyOf(villages));
            });

    private static void writeEvents(FriendlyByteBuf buf, List<Event> events) {
        buf.writeVarInt(events.size());
        events.forEach(e -> EVENT.encode(buf, e));
    }

    private static List<Event> readEvents(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Event> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(EVENT.decode(buf));
        }
        return List.copyOf(out);
    }
}
