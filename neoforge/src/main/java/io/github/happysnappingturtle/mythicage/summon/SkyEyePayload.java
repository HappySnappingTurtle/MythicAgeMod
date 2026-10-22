package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 主人：天眼标记（T14 设计文档 §11.4）。客户端让这些实体显示描边 ticks 刻；只发给主人一人，别的玩家看不到。
 */
public record SkyEyePayload(int[] entityIds, int ticks) implements CustomPacketPayload {
    public static final Type<SkyEyePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "sky_eye"));
    public static final StreamCodec<FriendlyByteBuf, SkyEyePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarIntArray(p.entityIds);
                buf.writeVarInt(p.ticks);
            },
            buf -> new SkyEyePayload(buf.readVarIntArray(), buf.readVarInt()));

    @Override
    public Type<SkyEyePayload> type() {
        return TYPE;
    }
}
