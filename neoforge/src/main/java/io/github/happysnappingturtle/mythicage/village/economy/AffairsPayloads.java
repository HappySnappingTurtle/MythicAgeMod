package io.github.happysnappingturtle.mythicage.village.economy;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
}
