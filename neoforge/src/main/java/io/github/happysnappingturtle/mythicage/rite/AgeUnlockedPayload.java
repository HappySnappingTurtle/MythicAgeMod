package io.github.happysnappingturtle.mythicage.rite;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 服务端 → 客户端：晋升成功界面（{@code mythic_age:age_unlocked}，12 号 §4.3-A；T11a 设计文档 §6.2）。
 *
 * @param caps   上限变化（只含有变化的项）
 * @param extras 晋升表的额外解锁项
 * @param next   下一时代需要（只预告）；已是最高时代或没有晋升表时为空
 */
public record AgeUnlockedPayload(Civilization civ, Age age, String minorGod, List<ItemStack> powers, List<ItemStack> summons,
                                 List<ItemStack> recipes, List<Cap> caps, List<Extra> extras, Optional<Next> next)
        implements CustomPacketPayload {
    public static final Type<AgeUnlockedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "age_unlocked"));

    public record Cap(String key, long before, long after) {
    }

    public record Extra(ItemStack icon, String key) {
    }

    /** @param requirements 建筑条件的预告（如「神庙达到前柱式神庙」），不判定是否满足 */
    public record Next(Age age, long favor, List<ItemStack> offerings, Age trial, List<net.minecraft.network.chat.Component> requirements) {
    }

    private static void stacks(RegistryFriendlyByteBuf buf, List<ItemStack> l) {
        buf.writeVarInt(l.size());
        l.forEach(s -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, s));
    }

    private static List<ItemStack> stacks(RegistryFriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), 256);
        List<ItemStack> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return out;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, AgeUnlockedPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.civ);
                buf.writeEnum(p.age);
                buf.writeUtf(p.minorGod);
                stacks(buf, p.powers);
                stacks(buf, p.summons);
                stacks(buf, p.recipes);
                buf.writeVarInt(p.caps.size());
                p.caps.forEach(c -> {
                    buf.writeUtf(c.key());
                    buf.writeVarLong(c.before());
                    buf.writeVarLong(c.after());
                });
                buf.writeVarInt(p.extras.size());
                p.extras.forEach(e -> {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.icon());
                    buf.writeUtf(e.key());
                });
                buf.writeBoolean(p.next.isPresent());
                p.next.ifPresent(n -> {
                    buf.writeEnum(n.age());
                    buf.writeVarLong(n.favor());
                    stacks(buf, n.offerings());
                    buf.writeEnum(n.trial());
                    buf.writeVarInt(n.requirements().size());
                    n.requirements().forEach(c -> net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buf, c));
                });
            },
            buf -> {
                Civilization civ = buf.readEnum(Civilization.class);
                Age age = buf.readEnum(Age.class);
                String god = buf.readUtf();
                List<ItemStack> powers = stacks(buf);
                List<ItemStack> summons = stacks(buf);
                List<ItemStack> recipes = stacks(buf);
                int nc = Math.min(buf.readVarInt(), 16);
                List<Cap> caps = new ArrayList<>(nc);
                for (int i = 0; i < nc; i++) {
                    caps.add(new Cap(buf.readUtf(), buf.readVarLong(), buf.readVarLong()));
                }
                int ne = Math.min(buf.readVarInt(), 64);
                List<Extra> extras = new ArrayList<>(ne);
                for (int i = 0; i < ne; i++) {
                    extras.add(new Extra(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readUtf()));
                }
                Optional<Next> next = Optional.empty();
                if (buf.readBoolean()) {
                    Age na = buf.readEnum(Age.class);
                    long favor = buf.readVarLong();
                    List<ItemStack> offerings = stacks(buf);
                    Age trial = buf.readEnum(Age.class);
                    int nr = Math.min(buf.readVarInt(), 16);
                    List<net.minecraft.network.chat.Component> reqs = new ArrayList<>(nr);
                    for (int i = 0; i < nr; i++) {
                        reqs.add(net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(buf));
                    }
                    next = Optional.of(new Next(na, favor, offerings, trial, List.copyOf(reqs)));
                }
                return new AgeUnlockedPayload(civ, age, god, powers, summons, recipes, caps, extras, next);
            });

    @Override
    public Type<AgeUnlockedPayload> type() {
        return TYPE;
    }
}
