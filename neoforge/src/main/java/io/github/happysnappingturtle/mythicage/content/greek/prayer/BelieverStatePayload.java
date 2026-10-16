package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：村民的信仰状态（T10 设计文档 §1、§2 D2），驱动橄榄枝饰与跪姿。
 * 只在状态变化时发给追踪该村民的玩家；开始追踪时补发一次。
 *
 * @param flags {@link #BELIEVER} | {@link #PRAYING}
 */
public record BelieverStatePayload(int entityId, byte flags) implements CustomPacketPayload {
    public static final int BELIEVER = 1;
    public static final int PRAYING = 2;

    public static final Type<BelieverStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "believer_state"));

    public static final StreamCodec<FriendlyByteBuf, BelieverStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entityId());
                buf.writeByte(p.flags());
            },
            buf -> new BelieverStatePayload(buf.readVarInt(), buf.readByte()));

    public static int flags(boolean believer, boolean praying) {
        return (believer ? BELIEVER : 0) | (praying ? PRAYING : 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
