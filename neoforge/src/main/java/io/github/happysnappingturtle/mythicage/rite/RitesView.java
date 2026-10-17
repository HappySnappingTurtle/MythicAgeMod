package io.github.happysnappingturtle.mythicage.rite;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.rite.RiteKind;
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
 * 祭司「晋升」页的数据（T11a 设计文档 §7.1）。全部由服务端算好，客户端只展示。
 *
 * @param options   可以选的仪式：晋升 1 个、立祀 1 个、改奉每个已达时代 1 个
 */
public record RitesView(String patron, Age stage, List<String> shrines, List<Option> options) {

    /**
     * 一种仪式。
     *
     * @param blocking   结构性的不可举行原因（村庄、冷却、未开放……）；为空时只看条件行是否都满足
     * @param lines      条件行：恩宠、供品、Boss 凭证、建筑条件
     * @param preview    仪式后会发生的变化
     * @param favor      实际要扣的恩宠（已含修正），提交时作为期望值
     * @param currentGod 改奉时：该时代当前供奉的次神
     */
    public record Option(RiteKind kind, Age target, boolean approved, long favor, List<Candidate> candidates,
                         List<Line> lines, List<Component> blocking, List<Component> preview, String currentGod) {
        public boolean ready() {
            return blocking.isEmpty() && lines.stream().allMatch(Line::met);
        }
    }

    /** 候选次神：可供奉的神力、召唤与本村立祠后出售的商品（图标）。 */
    public record Candidate(String id, boolean available, List<ItemStack> powers, List<ItemStack> summons,
                            List<ItemStack> goods) {
    }

    public record Line(Component label, boolean met, Component progress) {
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, RitesView> STREAM_CODEC = StreamCodec.of(RitesView::write,
            RitesView::read);

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

    private static void text(RegistryFriendlyByteBuf buf, Component c) {
        ComponentSerialization.STREAM_CODEC.encode(buf, c);
    }

    private static Component text(RegistryFriendlyByteBuf buf) {
        return ComponentSerialization.STREAM_CODEC.decode(buf);
    }

    private static void stacks(RegistryFriendlyByteBuf buf, List<ItemStack> l) {
        list(buf, l, ItemStack.OPTIONAL_STREAM_CODEC::encode);
    }

    private static List<ItemStack> stacks(RegistryFriendlyByteBuf buf) {
        return list(buf, ItemStack.OPTIONAL_STREAM_CODEC::decode);
    }

    private static void write(RegistryFriendlyByteBuf buf, RitesView v) {
        buf.writeUtf(v.patron);
        buf.writeEnum(v.stage);
        list(buf, v.shrines, (b, s) -> b.writeUtf(s));
        list(buf, v.options, (b, o) -> {
            b.writeEnum(o.kind());
            b.writeEnum(o.target());
            b.writeBoolean(o.approved());
            b.writeVarLong(o.favor());
            list(b, o.candidates(), (bb, c) -> {
                bb.writeUtf(c.id());
                bb.writeBoolean(c.available());
                stacks(bb, c.powers());
                stacks(bb, c.summons());
                stacks(bb, c.goods());
            });
            list(b, o.lines(), (bb, l) -> {
                text(bb, l.label());
                bb.writeBoolean(l.met());
                text(bb, l.progress());
            });
            list(b, o.blocking(), RitesView::text);
            list(b, o.preview(), RitesView::text);
            b.writeUtf(o.currentGod());
        });
    }

    private static RitesView read(RegistryFriendlyByteBuf buf) {
        String patron = buf.readUtf();
        Age stage = buf.readEnum(Age.class);
        List<String> shrines = list(buf, b -> b.readUtf());
        List<Option> options = list(buf, b -> new Option(b.readEnum(RiteKind.class), b.readEnum(Age.class), b.readBoolean(),
                b.readVarLong(),
                list(b, bb -> new Candidate(bb.readUtf(), bb.readBoolean(), stacks(bb), stacks(bb), stacks(bb))),
                list(b, bb -> new Line(text(bb), bb.readBoolean(), text(bb))),
                list(b, RitesView::text), list(b, RitesView::text), b.readUtf()));
        return new RitesView(patron, stage, shrines, options);
    }
}
