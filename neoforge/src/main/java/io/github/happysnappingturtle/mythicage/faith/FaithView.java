package io.github.happysnappingturtle.mythicage.faith;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 信仰面板的数据（T11a 设计文档 §7.3）。全部由服务端组装，客户端只展示。
 *
 * @param mythicUnits 神话单位上限（按全部文明的最高时代）
 * @param relicSlots  圣物饰品栏数
 */
public record FaithView(int mythicUnits, int relicSlots, List<Page> pages) {

    public enum Status { UNOPENED, UNCONTACTED, CONTACTED }

    /**
     * 一个文明的页签。
     *
     * @param minorGods  古典、英雄、神话三个时代各自的次神（未到或未选为空串）
     * @param approvals  主神表里的每位主神 → 认可到的时代（NONE = 未认可）
     * @param next       下一次晋升的条件行；已到最高或该文明未开放晋升时为空
     * @param villages   你的村庄
     * @param sources    恩宠来源各行
     * @param features   本文明特色（可选）
     */
    public record Page(Civilization civ, Status status, String hint, String favorKey, long favor, long cap, Age age,
                       List<String> minorGods, List<Approval> approvals, Age nextAge, List<Line> next,
                       List<Row> villages, List<Row> sources, List<Component> features) {
    }

    public record Approval(String patron, Age age) {
    }

    /** 一条条件：图标（可为空）、文字、是否满足、进度。 */
    public record Line(ItemStack icon, Component label, boolean met, Component progress) {
    }

    /** 一行：名称、坐标（可为 null 时用 ZERO 且 hasPos=false）、说明、右侧数字。 */
    public record Row(Component name, boolean hasPos, BlockPos pos, Component note, Component value) {
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, FaithView> STREAM_CODEC = StreamCodec.of(FaithView::write,
            FaithView::read);

    private static <T> void list(RegistryFriendlyByteBuf buf, List<T> l, BiConsumer<RegistryFriendlyByteBuf, T> w) {
        buf.writeVarInt(l.size());
        l.forEach(x -> w.accept(buf, x));
    }

    private static <T> List<T> list(RegistryFriendlyByteBuf buf, Function<RegistryFriendlyByteBuf, T> r) {
        int n = buf.readVarInt();
        if (n > 256) {
            throw new IllegalArgumentException("列表过长：" + n);
        }
        List<T> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(r.apply(buf));
        }
        return out;
    }

    private static void text(RegistryFriendlyByteBuf b, Component c) {
        ComponentSerialization.STREAM_CODEC.encode(b, c);
    }

    private static Component text(RegistryFriendlyByteBuf b) {
        return ComponentSerialization.STREAM_CODEC.decode(b);
    }

    private static void row(RegistryFriendlyByteBuf b, Row r) {
        text(b, r.name());
        b.writeBoolean(r.hasPos());
        b.writeBlockPos(r.pos());
        text(b, r.note());
        text(b, r.value());
    }

    private static Row row(RegistryFriendlyByteBuf b) {
        return new Row(text(b), b.readBoolean(), b.readBlockPos(), text(b), text(b));
    }

    private static void write(RegistryFriendlyByteBuf buf, FaithView v) {
        buf.writeVarInt(v.mythicUnits);
        buf.writeVarInt(v.relicSlots);
        list(buf, v.pages, (b, p) -> {
            b.writeEnum(p.civ());
            b.writeEnum(p.status());
            b.writeUtf(p.hint());
            b.writeUtf(p.favorKey());
            b.writeVarLong(p.favor());
            b.writeVarLong(p.cap());
            b.writeEnum(p.age());
            list(b, p.minorGods(), (bb, s) -> bb.writeUtf(s));
            list(b, p.approvals(), (bb, a) -> {
                bb.writeUtf(a.patron());
                bb.writeEnum(a.age());
            });
            b.writeEnum(p.nextAge());
            list(b, p.next(), (bb, l) -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(bb, l.icon());
                text(bb, l.label());
                bb.writeBoolean(l.met());
                text(bb, l.progress());
            });
            list(b, p.villages(), FaithView::row);
            list(b, p.sources(), FaithView::row);
            list(b, p.features(), FaithView::text);
        });
    }

    private static FaithView read(RegistryFriendlyByteBuf buf) {
        int units = buf.readVarInt();
        int slots = buf.readVarInt();
        List<Page> pages = list(buf, b -> new Page(b.readEnum(Civilization.class), b.readEnum(Status.class), b.readUtf(),
                b.readUtf(), b.readVarLong(), b.readVarLong(), b.readEnum(Age.class), list(b, bb -> bb.readUtf()),
                list(b, bb -> new Approval(bb.readUtf(), bb.readEnum(Age.class))), b.readEnum(Age.class),
                list(b, bb -> new Line(ItemStack.OPTIONAL_STREAM_CODEC.decode(bb), text(bb), bb.readBoolean(), text(bb))),
                list(b, FaithView::row), list(b, FaithView::row), list(b, FaithView::text)));
        return new FaithView(units, slots, pages);
    }
}
