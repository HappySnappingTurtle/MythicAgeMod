package io.github.happysnappingturtle.mythicage.rite;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.rite.RiteKind;
import io.github.happysnappingturtle.mythicage.trade.TradeView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** 仪式的网络负载（T11a 设计文档 §7.4）。请求一律由服务端重新校验。 */
public final class RitePayloads {
    private RitePayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    /**
     * 客户端 → 服务端：举行仪式。
     *
     * @param age           改奉时要改的时代；其余忽略
     * @param expectedFavor 界面上显示的恩宠代价
     */
    public record Perform(RiteKind kind, Age age, String god, long expectedFavor, String requestId)
            implements CustomPacketPayload {
        public static final Type<Perform> TYPE = payloadType("rite_perform");
        public static final StreamCodec<FriendlyByteBuf, Perform> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeEnum(p.kind);
                    buf.writeEnum(p.age);
                    buf.writeUtf(p.god, 64);
                    buf.writeVarLong(p.expectedFavor);
                    buf.writeUtf(p.requestId, 64);
                },
                buf -> new Perform(buf.readEnum(RiteKind.class), buf.readEnum(Age.class), buf.readUtf(64), buf.readVarLong(),
                        buf.readUtf(64)));

        @Override
        public Type<Perform> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：结果、不可举行的原因与刷新后的界面数据。 */
    public record Result(String requestId, RiteService.Reason reason, List<Component> issues, TradeView view)
            implements CustomPacketPayload {
        public static final Type<Result> TYPE = payloadType("rite_result");
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    buf.writeUtf(r.requestId);
                    buf.writeEnum(r.reason);
                    buf.writeVarInt(r.issues.size());
                    r.issues.forEach(c -> ComponentSerialization.STREAM_CODEC.encode(buf, c));
                    TradeView.STREAM_CODEC.encode(buf, r.view);
                },
                buf -> {
                    String id = buf.readUtf();
                    RiteService.Reason reason = buf.readEnum(RiteService.Reason.class);
                    int n = Math.min(buf.readVarInt(), 64);
                    List<Component> issues = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        issues.add(ComponentSerialization.STREAM_CODEC.decode(buf));
                    }
                    return new Result(id, reason, issues, TradeView.STREAM_CODEC.decode(buf));
                });

        @Override
        public Type<Result> type() {
            return TYPE;
        }
    }
}
