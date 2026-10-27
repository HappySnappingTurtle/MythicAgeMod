package io.github.happysnappingturtle.mythicage.conduct;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 「善恶与贡献」界面的网络负载（T24 设计文档 §3.4）。请求不带玩家参数：服务端只回请求者本人（D6）。 */
public final class ConductPayloads {
    private ConductPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    /** 客户端 → 服务端：打开或刷新。 */
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = payloadType("conduct_request");
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.unit(new Request());

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：界面数据。 */
    public record View(ConductView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = payloadType("conduct_view");
        public static final StreamCodec<FriendlyByteBuf, View> STREAM_CODEC = ConductView.STREAM_CODEC.map(View::new, View::view);

        @Override
        public Type<View> type() {
            return TYPE;
        }
    }
}
