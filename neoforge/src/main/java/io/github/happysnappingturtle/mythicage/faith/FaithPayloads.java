package io.github.happysnappingturtle.mythicage.faith;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 信仰面板的网络负载（T11a 设计文档 §7.4）。 */
public final class FaithPayloads {
    private FaithPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    /** 客户端 → 服务端：打开（或刷新）信仰面板。 */
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = payloadType("faith_request");
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.unit(new Request());

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    /** 客户端 → 服务端：重看某文明某时代的晋升成功界面。 */
    public record Replay(Civilization civ, Age age) implements CustomPacketPayload {
        public static final Type<Replay> TYPE = payloadType("faith_replay");
        public static final StreamCodec<FriendlyByteBuf, Replay> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    buf.writeEnum(r.civ);
                    buf.writeEnum(r.age);
                },
                buf -> new Replay(buf.readEnum(Civilization.class), buf.readEnum(Age.class)));

        @Override
        public Type<Replay> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：面板数据。 */
    public record View(FaithView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = payloadType("faith_view");
        public static final StreamCodec<RegistryFriendlyByteBuf, View> STREAM_CODEC =
                FaithView.STREAM_CODEC.map(View::new, View::view);

        @Override
        public Type<View> type() {
            return TYPE;
        }
    }
}
