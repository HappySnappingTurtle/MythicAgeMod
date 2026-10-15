package io.github.happysnappingturtle.mythicage.trade;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 交易相关的网络负载（T9 设计文档 §3）。请求一律由服务端校验，客户端数据只当提示。 */
public final class TradePayloads {
    private TradePayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    // ───────────── 服务端 → 客户端 ─────────────

    /** 打开（或刷新）交易界面。 */
    public record Open(TradeView view) implements CustomPacketPayload {
        public static final Type<Open> TYPE = payloadType("trade_open");
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC =
                TradeView.STREAM_CODEC.map(Open::new, Open::view);

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    /**
     * 一次请求的结果，附带刷新后的界面数据。
     *
     * @param arg 与原因相关的数字：恩宠不足时为还差多少，改价时为新价格，其余为 0
     */
    public record Result(String requestId, TradeReason reason, long arg, String itemName, int count, TradeView view)
            implements CustomPacketPayload {
        public static final Type<Result> TYPE = payloadType("trade_result");
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    buf.writeUtf(r.requestId);
                    buf.writeEnum(r.reason);
                    buf.writeVarLong(r.arg);
                    buf.writeUtf(r.itemName);
                    buf.writeVarInt(r.count);
                    TradeView.STREAM_CODEC.encode(buf, r.view);
                },
                buf -> new Result(buf.readUtf(), buf.readEnum(TradeReason.class), buf.readVarLong(), buf.readUtf(),
                        buf.readVarInt(), TradeView.STREAM_CODEC.decode(buf)));

        @Override
        public Type<Result> type() {
            return TYPE;
        }
    }

    /** 服务端关闭界面（祭司离开、死亡或超出距离）。 */
    public record Close() implements CustomPacketPayload {
        public static final Type<Close> TYPE = payloadType("trade_close");
        public static final StreamCodec<FriendlyByteBuf, Close> STREAM_CODEC = StreamCodec.unit(new Close());

        @Override
        public Type<Close> type() {
            return TYPE;
        }
    }

    // ───────────── 客户端 → 服务端 ─────────────

    /** 兑换一件商品；expectedPrice 是界面上显示的价格，requestId 每次点击新生成、重试时沿用。 */
    public record Buy(String offerId, long expectedPrice, String requestId) implements CustomPacketPayload {
        public static final Type<Buy> TYPE = payloadType("trade_buy");
        public static final StreamCodec<FriendlyByteBuf, Buy> STREAM_CODEC = StreamCodec.of(
                (buf, b) -> {
                    buf.writeUtf(b.offerId, 64);
                    buf.writeVarLong(b.expectedPrice);
                    buf.writeUtf(b.requestId, 64);
                },
                buf -> new Buy(buf.readUtf(64), buf.readVarLong(), buf.readUtf(64)));

        @Override
        public Type<Buy> type() {
            return TYPE;
        }
    }

    /** 初见祝福（reissue = false）或补领祭坛（reissue = true）。 */
    public record Blessing(boolean reissue, String requestId) implements CustomPacketPayload {
        public static final Type<Blessing> TYPE = payloadType("trade_blessing");
        public static final StreamCodec<FriendlyByteBuf, Blessing> STREAM_CODEC = StreamCodec.of(
                (buf, b) -> {
                    buf.writeBoolean(b.reissue);
                    buf.writeUtf(b.requestId, 64);
                },
                buf -> new Blessing(buf.readBoolean(), buf.readUtf(64)));

        @Override
        public Type<Blessing> type() {
            return TYPE;
        }
    }

    /** 玩家关闭了界面。 */
    public record Closed() implements CustomPacketPayload {
        public static final Type<Closed> TYPE = payloadType("trade_closed");
        public static final StreamCodec<FriendlyByteBuf, Closed> STREAM_CODEC = StreamCodec.unit(new Closed());

        @Override
        public Type<Closed> type() {
            return TYPE;
        }
    }

    /** 从神坛检视界面打开「兑换」页。 */
    public record OpenAltar(BlockPos pos) implements CustomPacketPayload {
        public static final Type<OpenAltar> TYPE = payloadType("trade_open_altar");
        public static final StreamCodec<FriendlyByteBuf, OpenAltar> STREAM_CODEC =
                BlockPos.STREAM_CODEC.map(OpenAltar::new, OpenAltar::pos).cast();

        @Override
        public Type<OpenAltar> type() {
            return TYPE;
        }
    }
}
