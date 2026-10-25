package io.github.happysnappingturtle.mythicage.village.economy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 村务界面的快照（T19 设计文档 §4.3）：库存、托管、容量、缺口、发展记录。文本行都用语言键 + 参数，由客户端翻译。
 *
 * @param title    语言键行：村庄名称
 * @param date     语言键行：今天是第几期第几日
 * @param notes    历法提示：节日倒数、下一个神谕日（§1.10 J8）
 */
public record AffairsView(UUID village, Line title, Line date, List<Line> notes, int total, int capacity, List<Entry> stock,
                          List<Line> shortages, List<Line> log) {
    public record Entry(String item, int count, int escrowed) {
    }

    public record Line(String key, List<String> args) {
        public static Line of(String key, String... args) {
            return new Line(key, List.of(args));
        }
    }

    /**
     * 文本行 → 文本组件：参数以 {@code @} 开头的当语言键翻译；以 {@code @@} 开头的当语言键、并把后面所有参数都交给它。
     * 服务端发聊天消息用（客户端界面有同样的规则）。
     */
    public static net.minecraft.network.chat.Component toComponent(Line l) {
        return net.minecraft.network.chat.Component.translatable(l.key(), componentArgs(l.args()));
    }

    private static Object[] componentArgs(List<String> in) {
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            String a = in.get(i);
            if (a.startsWith("@@")) {
                out.add(net.minecraft.network.chat.Component.translatable(a.substring(2), componentArgs(in.subList(i + 1, in.size()))));
                break;
            }
            out.add(a.startsWith("@") ? net.minecraft.network.chat.Component.translatable(a.substring(1)) : a);
        }
        return out.toArray();
    }

    public static final StreamCodec<FriendlyByteBuf, Line> LINE = StreamCodec.of(
            (buf, l) -> {
                buf.writeUtf(l.key());
                buf.writeVarInt(l.args().size());
                l.args().forEach(buf::writeUtf);
            },
            buf -> {
                String k = buf.readUtf();
                int n = buf.readVarInt();
                List<String> a = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    a.add(buf.readUtf());
                }
                return new Line(k, List.copyOf(a));
            });

    public static final StreamCodec<FriendlyByteBuf, AffairsView> STREAM_CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUUID(v.village());
                LINE.encode(buf, v.title());
                LINE.encode(buf, v.date());
                writeLines(buf, v.notes());
                buf.writeVarInt(v.total());
                buf.writeVarInt(v.capacity());
                buf.writeVarInt(v.stock().size());
                for (Entry e : v.stock()) {
                    buf.writeUtf(e.item());
                    buf.writeVarInt(e.count());
                    buf.writeVarInt(e.escrowed());
                }
                writeLines(buf, v.shortages());
                writeLines(buf, v.log());
            },
            buf -> {
                UUID id = buf.readUUID();
                Line title = LINE.decode(buf);
                Line date = LINE.decode(buf);
                List<Line> notes = readLines(buf);
                int total = buf.readVarInt();
                int cap = buf.readVarInt();
                int n = buf.readVarInt();
                List<Entry> stock = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    stock.add(new Entry(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
                }
                return new AffairsView(id, title, date, notes, total, cap, List.copyOf(stock), readLines(buf), readLines(buf));
            });

    private static void writeLines(FriendlyByteBuf buf, List<Line> lines) {
        buf.writeVarInt(lines.size());
        lines.forEach(l -> LINE.encode(buf, l));
    }

    private static List<Line> readLines(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Line> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(LINE.decode(buf));
        }
        return List.copyOf(out);
    }
}
