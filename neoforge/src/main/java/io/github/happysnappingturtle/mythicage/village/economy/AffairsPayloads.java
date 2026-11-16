package io.github.happysnappingturtle.mythicage.village.economy;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 村务界面的网络负载（T19 设计文档 §4.3）。 */
public final class AffairsPayloads {
    private AffairsPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    /** 服务端 → 客户端：打开或刷新。 */
    public record View(AffairsView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = payloadType("village_affairs_view");
        public static final StreamCodec<FriendlyByteBuf, View> STREAM_CODEC = AffairsView.STREAM_CODEC.map(View::new, View::view);

        @Override
        public Type<View> type() {
            return TYPE;
        }
    }

    /** 客户端 → 服务端：把主手的物品捐进村库。 */
    public record Donate(UUID village) implements CustomPacketPayload {
        public static final Type<Donate> TYPE = payloadType("village_affairs_donate");
        public static final StreamCodec<FriendlyByteBuf, Donate> STREAM_CODEC = StreamCodec.of(
                (buf, d) -> buf.writeUUID(d.village), buf -> new Donate(buf.readUUID()));

        @Override
        public Type<Donate> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：打开或刷新居民交易界面（§1.8 I7）。 */
    public record Trade(ResidentTradeView view) implements CustomPacketPayload {
        public static final Type<Trade> TYPE = payloadType("resident_trade_view");
        public static final StreamCodec<FriendlyByteBuf, Trade> STREAM_CODEC = ResidentTradeView.STREAM_CODEC.map(Trade::new,
                Trade::view);

        @Override
        public Type<Trade> type() {
            return TYPE;
        }
    }

    /** 客户端 → 服务端：做一份交易。 */
    public record Deal(int resident, String offer) implements CustomPacketPayload {
        public static final Type<Deal> TYPE = payloadType("resident_trade_deal");
        public static final StreamCodec<FriendlyByteBuf, Deal> STREAM_CODEC = StreamCodec.of(
                (buf, d) -> {
                    buf.writeVarInt(d.resident);
                    buf.writeUtf(d.offer);
                }, buf -> new Deal(buf.readVarInt(), buf.readUtf()));

        @Override
        public Type<Deal> type() {
            return TYPE;
        }
    }

    /** 客户端 → 服务端：显示村界（T24 §4.4）。 */
    public record BoundsRequest(UUID village) implements CustomPacketPayload {
        public static final Type<BoundsRequest> TYPE = payloadType("village_bounds_request");
        public static final StreamCodec<FriendlyByteBuf, BoundsRequest> STREAM_CODEC = StreamCodec.of(
                (buf, b) -> buf.writeUUID(b.village), buf -> new BoundsRequest(buf.readUUID()));

        @Override
        public Type<BoundsRequest> type() {
            return TYPE;
        }
    }

    /**
     * 服务端 → 客户端：村界（T24 §4.4）。built 是已建村界并集的单位边（实线）；planned 是未建的规划地块（虚线）；
     * occupied 是被占的地块（实线 + 对角叉）。矩形是 {minX, minZ, maxX, maxZ}（格子含两端）。
     */
    public record Bounds(List<io.github.happysnappingturtle.mythicage.core.village.gen.Outline.Edge> built, List<int[]> planned,
                         List<int[]> occupied) implements CustomPacketPayload {
        public static final Type<Bounds> TYPE = payloadType("village_bounds");
        public static final StreamCodec<FriendlyByteBuf, Bounds> STREAM_CODEC = StreamCodec.of(
                (buf, b) -> {
                    buf.writeVarInt(b.built.size());
                    for (var e : b.built) {
                        buf.writeVarInt(e.x());
                        buf.writeVarInt(e.z());
                        buf.writeBoolean(e.alongX());
                    }
                    writeRects(buf, b.planned);
                    writeRects(buf, b.occupied);
                },
                buf -> {
                    int n = buf.readVarInt();
                    List<io.github.happysnappingturtle.mythicage.core.village.gen.Outline.Edge> built = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        built.add(new io.github.happysnappingturtle.mythicage.core.village.gen.Outline.Edge(buf.readVarInt(),
                                buf.readVarInt(), buf.readBoolean()));
                    }
                    return new Bounds(List.copyOf(built), readRects(buf), readRects(buf));
                });

        private static void writeRects(FriendlyByteBuf buf, List<int[]> rects) {
            buf.writeVarInt(rects.size());
            for (int[] r : rects) {
                for (int i = 0; i < 4; i++) {
                    buf.writeVarInt(r[i]);
                }
            }
        }

        private static List<int[]> readRects(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<int[]> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                out.add(new int[]{buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()});
            }
            return List.copyOf(out);
        }

        @Override
        public Type<Bounds> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：村庄升级 toast（T24 §4.5，12 号 §4.3 B）。icon 是物品 id。 */
    public record Toast(String icon, net.minecraft.network.chat.Component title, net.minecraft.network.chat.Component body)
            implements CustomPacketPayload {
        public static final Type<Toast> TYPE = payloadType("village_toast");
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Toast> STREAM_CODEC = StreamCodec.of(
                (buf, t) -> {
                    buf.writeUtf(t.icon);
                    net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buf, t.title);
                    net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buf, t.body);
                }, buf -> new Toast(buf.readUtf(), net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(buf),
                        net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(buf)));

        @Override
        public Type<Toast> type() {
            return TYPE;
        }
    }
}
