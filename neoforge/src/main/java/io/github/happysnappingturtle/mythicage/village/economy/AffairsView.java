package io.github.happysnappingturtle.mythicage.village.economy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 村务界面的快照（T19 设计文档 §4.3；T24 §4 改为分页：总览 / 库存与捐献，委托另有 {@code QuestView}）。
 * 文本行都用语言键 + 参数，由客户端翻译。
 *
 * @param tab      打开哪一页
 * @param title    语言键行：村庄名称
 * @param date     语言键行：今天是第几期第几日
 * @param notes    历法提示：节日倒数、下一个神谕日（§1.10 J8）
 * @param blockers 缺口，按三类分开（T24 §4.3）
 */
public record AffairsView(UUID village, int tab, Line title, Line date, List<Line> notes, int total, int capacity,
                          List<Entry> stock, List<Blocker> blockers, List<Line> log, Overview overview) {
    public static final int TAB_OVERVIEW = 0;
    public static final int TAB_STOCK = 1;

    /** @param cls 分类 id：self_resolving、rare_material、occupied */
    public record Blocker(String cls, Line line) {
    }

    /** 计数：key 是语言键（岗位）或字面名字（协作者，{@code literal} 为真）。 */
    public record Count(String key, boolean literal, int n) {
    }

    /**
     * 总览分页（T24 §4.2）。
     *
     * @param stage      村庄阶段（时代 id）
     * @param freeBeds   空床数
     * @param jobs       按职业计数（语言键）
     * @param alerts     警报：正在警戒、最近两日的饥荒 / 移民失踪 / 停工
     * @param food       口粮行（含底线）；没有口粮规则时为空行
     * @param project    在建项目的状态行（不含缺料明细，缺料在 blockers）
     * @param collaborators 本村贡献 &gt; 0 的玩家，前 8 名（D7）
     * @param quests     进行中的委托数；publicQuests 其中公共的
     */
    public record Overview(String stage, int residents, int freeBeds, int soldiers, int soldierCap, List<Count> jobs,
                           List<Line> alerts, Line food, List<Line> project, List<Count> collaborators, int quests,
                           int publicQuests) {
        public static final Overview EMPTY = new Overview("none", 0, 0, 0, 0, List.of(), List.of(), Line.of(""), List.of(),
                List.of(), 0, 0);
    }

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

    private static final StreamCodec<FriendlyByteBuf, Count> COUNT = StreamCodec.of(
            (buf, c) -> {
                buf.writeUtf(c.key());
                buf.writeBoolean(c.literal());
                buf.writeVarInt(c.n());
            },
            buf -> new Count(buf.readUtf(), buf.readBoolean(), buf.readVarInt()));

    private static final StreamCodec<FriendlyByteBuf, Overview> OVERVIEW = StreamCodec.of(
            (buf, o) -> {
                buf.writeUtf(o.stage());
                buf.writeVarInt(o.residents());
                buf.writeVarInt(o.freeBeds());
                buf.writeVarInt(o.soldiers());
                buf.writeVarInt(o.soldierCap());
                writeList(buf, o.jobs(), COUNT);
                writeLines(buf, o.alerts());
                LINE.encode(buf, o.food());
                writeLines(buf, o.project());
                writeList(buf, o.collaborators(), COUNT);
                buf.writeVarInt(o.quests());
                buf.writeVarInt(o.publicQuests());
            },
            buf -> new Overview(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    readList(buf, COUNT), readLines(buf), LINE.decode(buf), readLines(buf), readList(buf, COUNT),
                    buf.readVarInt(), buf.readVarInt()));

    private static final StreamCodec<FriendlyByteBuf, Blocker> BLOCKER = StreamCodec.of(
            (buf, b) -> {
                buf.writeUtf(b.cls());
                LINE.encode(buf, b.line());
            },
            buf -> new Blocker(buf.readUtf(), LINE.decode(buf)));

    public static final StreamCodec<FriendlyByteBuf, AffairsView> STREAM_CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUUID(v.village());
                buf.writeVarInt(v.tab());
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
                writeList(buf, v.blockers(), BLOCKER);
                writeLines(buf, v.log());
                OVERVIEW.encode(buf, v.overview());
            },
            buf -> {
                UUID id = buf.readUUID();
                int tab = buf.readVarInt();
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
                List<Blocker> blockers = readList(buf, BLOCKER);
                List<Line> log = readLines(buf);
                return new AffairsView(id, tab, title, date, notes, total, cap, List.copyOf(stock), blockers, log, OVERVIEW.decode(buf));
            });

    private static <T> void writeList(FriendlyByteBuf buf, List<T> list, StreamCodec<FriendlyByteBuf, T> codec) {
        buf.writeVarInt(list.size());
        list.forEach(x -> codec.encode(buf, x));
    }

    private static <T> List<T> readList(FriendlyByteBuf buf, StreamCodec<FriendlyByteBuf, T> codec) {
        int n = buf.readVarInt();
        List<T> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(codec.decode(buf));
        }
        return List.copyOf(out);
    }

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
