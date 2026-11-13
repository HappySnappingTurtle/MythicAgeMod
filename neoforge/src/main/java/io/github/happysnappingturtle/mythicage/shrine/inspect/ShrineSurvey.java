package io.github.happysnappingturtle.mythicage.shrine.inspect;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：神坛检视（T8a 设计文档 §4；T24 §5.1 抽成五文明通用的外壳）。只带展示用的数字、文本键与坐标，客户端不做判定。
 * 各文明的神坛只负责填数据（希腊见 {@code GreekShrineSurvey}）。
 *
 * @param tierKeys    各级的名称语言键
 * @param rows        下一级的每一项要求（已是最高级时为当前统计，need = 0）
 * @param ageRequired 结构够了但主人时代不够时，要求的时代 id；否则为空
 * @param estimate    预计日产出（估算，不是发放承诺）
 * @param away        离开时的记账（A2 方案 E），没有台账时 present = false
 * @param highlights  「高亮缺失格」的分组，形状不同（不只靠颜色）
 */
public record ShrineSurvey(Civilization civ, BlockPos pos, String ownerName, boolean viewerIsOwner, Age ownerAge,
                           boolean hasResult, boolean unverified, int structuralLevel, int grantedLevel, List<String> tierKeys,
                           double coefficient, int slotsCap, int slotsFound, List<Row> rows, String ageRequired, double estimate,
                           Prayer prayer, Away away, BlockPos scanMin, BlockPos scanMax, List<Highlight> highlights,
                           Temple temple)
        implements CustomPacketPayload {
    /** 玩家神坛（GA-13 之前）的构造：没有村庄神庙段。 */
    public ShrineSurvey(Civilization civ, BlockPos pos, String ownerName, boolean viewerIsOwner, Age ownerAge,
                        boolean hasResult, boolean unverified, int structuralLevel, int grantedLevel, List<String> tierKeys,
                        double coefficient, int slotsCap, int slotsFound, List<Row> rows, String ageRequired, double estimate,
                        Prayer prayer, Away away, BlockPos scanMin, BlockPos scanMax, List<Highlight> highlights) {
        this(civ, pos, ownerName, viewerIsOwner, ownerAge, hasResult, unverified, structuralLevel, grantedLevel, tierKeys,
                coefficient, slotsCap, slotsFound, rows, ageRequired, estimate, prayer, away, scanMin, scanMax, highlights,
                Temple.NONE);
    }

    /**
     * 村庄神庙段（GA-13）：present 为假时是玩家神坛。
     *
     * @param patron           村子主神 id（村名「某某之村」；空为无主神的村子）
     * @param integrityPercent 完好度（百分比）
     * @param devotee          看的人是这里的供奉者
     * @param devotees         供奉者人数
     */
    public record Temple(boolean present, String patron, int integrityPercent, boolean devotee, int devotees) {
        public static final Temple NONE = new Temple(false, "", 100, false, 0);
    }
    public static final int HIGHLIGHT_LIMIT = 4096;

    /** 一项条件：have / need；布尔条件用 0/1，{@code bool} 为真。名称键 {@code mythic_age.altar.row.<key>}。 */
    public record Row(String key, int have, int need, boolean bool) {
        public boolean ok() {
            return need == 0 || have >= need;
        }
    }

    /** 一条说明：语言键与参数，warn 为真时按问题显示。 */
    public record Note(String key, List<String> args, boolean warn) {
        public static Note of(String key, boolean warn, String... args) {
            return new Note(key, List.of(args), warn);
        }
    }

    /** 祈祷与产率：正在祈祷、信徒、今日已得，以及雨天减半、离得太近等说明。 */
    public record Prayer(int praying, int believers, double today, List<Note> notes) {
    }

    /**
     * 离开时的记账（A2 方案 E，T5）：按 ratePerDay 记账；verified 为假时按估算打折；usable 为假时（照看期过了）不再记账；
     * buffer 是攒着、主人回来附近后入账的离线储备。
     */
    public record Away(boolean present, double ratePerDay, boolean verified, boolean usable, double buffer) {
        public static final Away NONE = new Away(false, 0, false, false, 0);
    }

    /** 高亮的形状：格子框、带对角叉的框（缺 / 挡）、虚线框（不合格）、细高柱（整列）、贴地薄片（位置）。 */
    public enum Shape { CELL, CROSS, DASHED, COLUMN, FLAT }

    /** @param kind 图例键后缀 {@code mythic_age.altar.highlight.<kind>} */
    public record Highlight(String kind, Shape shape, int color, List<BlockPos> cells) {
    }

    public List<BlockPos> cells(String kind) {
        return highlights.stream().filter(h -> h.kind().equals(kind)).findFirst().map(Highlight::cells).orElse(List.of());
    }

    public static final Type<ShrineSurvey> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "shrine_survey"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void writeStrings(FriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        list.forEach(buf::writeUtf);
    }

    private static List<String> readStrings(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(buf.readUtf());
        }
        return List.copyOf(out);
    }

    public static final StreamCodec<FriendlyByteBuf, ShrineSurvey> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.civ());
                buf.writeBlockPos(p.pos());
                buf.writeUtf(p.ownerName());
                buf.writeBoolean(p.viewerIsOwner());
                buf.writeEnum(p.ownerAge());
                buf.writeBoolean(p.hasResult());
                buf.writeBoolean(p.unverified());
                buf.writeVarInt(p.structuralLevel());
                buf.writeVarInt(p.grantedLevel());
                writeStrings(buf, p.tierKeys());
                buf.writeDouble(p.coefficient());
                buf.writeVarInt(p.slotsCap());
                buf.writeVarInt(p.slotsFound());
                buf.writeVarInt(p.rows().size());
                for (Row r : p.rows()) {
                    buf.writeUtf(r.key());
                    buf.writeVarInt(r.have());
                    buf.writeVarInt(r.need());
                    buf.writeBoolean(r.bool());
                }
                buf.writeUtf(p.ageRequired());
                buf.writeDouble(p.estimate());
                Prayer pr = p.prayer();
                buf.writeVarInt(pr.praying());
                buf.writeVarInt(pr.believers());
                buf.writeDouble(pr.today());
                buf.writeVarInt(pr.notes().size());
                for (Note n : pr.notes()) {
                    buf.writeUtf(n.key());
                    writeStrings(buf, n.args());
                    buf.writeBoolean(n.warn());
                }
                Away a = p.away();
                buf.writeBoolean(a.present());
                buf.writeDouble(a.ratePerDay());
                buf.writeBoolean(a.verified());
                buf.writeBoolean(a.usable());
                buf.writeDouble(a.buffer());
                buf.writeBlockPos(p.scanMin());
                buf.writeBlockPos(p.scanMax());
                buf.writeVarInt(p.highlights().size());
                for (Highlight h : p.highlights()) {
                    buf.writeUtf(h.kind());
                    buf.writeEnum(h.shape());
                    buf.writeInt(h.color());
                    buf.writeVarInt(h.cells().size());
                    h.cells().forEach(buf::writeBlockPos);
                }
                Temple t = p.temple();
                buf.writeBoolean(t.present());
                buf.writeUtf(t.patron());
                buf.writeVarInt(t.integrityPercent());
                buf.writeBoolean(t.devotee());
                buf.writeVarInt(t.devotees());
            },
            buf -> {
                Civilization civ = buf.readEnum(Civilization.class);
                BlockPos pos = buf.readBlockPos();
                String owner = buf.readUtf();
                boolean isOwner = buf.readBoolean();
                Age age = buf.readEnum(Age.class);
                boolean hasResult = buf.readBoolean();
                boolean unverified = buf.readBoolean();
                int structural = buf.readVarInt();
                int granted = buf.readVarInt();
                List<String> tiers = readStrings(buf);
                double coefficient = buf.readDouble();
                int cap = buf.readVarInt();
                int found = buf.readVarInt();
                int nRows = buf.readVarInt();
                List<Row> rows = new ArrayList<>(nRows);
                for (int i = 0; i < nRows; i++) {
                    rows.add(new Row(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
                }
                String ageRequired = buf.readUtf();
                double estimate = buf.readDouble();
                int praying = buf.readVarInt();
                int believers = buf.readVarInt();
                double today = buf.readDouble();
                int nNotes = buf.readVarInt();
                List<Note> notes = new ArrayList<>(nNotes);
                for (int i = 0; i < nNotes; i++) {
                    notes.add(new Note(buf.readUtf(), readStrings(buf), buf.readBoolean()));
                }
                Away away = new Away(buf.readBoolean(), buf.readDouble(), buf.readBoolean(), buf.readBoolean(), buf.readDouble());
                BlockPos min = buf.readBlockPos();
                BlockPos max = buf.readBlockPos();
                int nH = buf.readVarInt();
                List<Highlight> highlights = new ArrayList<>(nH);
                int total = 0;
                for (int i = 0; i < nH; i++) {
                    String kind = buf.readUtf();
                    Shape shape = buf.readEnum(Shape.class);
                    int color = buf.readInt();
                    int n = buf.readVarInt();
                    total += n;
                    if (total > HIGHLIGHT_LIMIT) {
                        throw new IllegalArgumentException("高亮坐标过多：" + total);
                    }
                    List<BlockPos> cells = new ArrayList<>(n);
                    for (int j = 0; j < n; j++) {
                        cells.add(buf.readBlockPos());
                    }
                    highlights.add(new Highlight(kind, shape, color, List.copyOf(cells)));
                }
                Temple temple = new Temple(buf.readBoolean(), buf.readUtf(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
                return new ShrineSurvey(civ, pos, owner, isOwner, age, hasResult, unverified, structural, granted, tiers,
                        coefficient, cap, found, List.copyOf(rows), ageRequired, estimate,
                        new Prayer(praying, believers, today, List.copyOf(notes)), away, min, max, List.copyOf(highlights), temple);
            });
}
