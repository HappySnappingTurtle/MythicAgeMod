package io.github.happysnappingturtle.mythicage.boss;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：手持竞技场钥匙（克诺索斯：阿里阿德涅线团）时，阶段二每秒指一次 Boss 的方位（T23 §5.1）。
 * 客户端在玩家前方画一段指向 (x, y, z) 的短线，约 1 秒后消失。
 */
public record BossPointerPayload(double x, double y, double z) implements CustomPacketPayload {
    public static final Type<BossPointerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "boss_pointer"));

    public static final StreamCodec<FriendlyByteBuf, BossPointerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.x());
                buf.writeDouble(p.y());
                buf.writeDouble(p.z());
            },
            buf -> new BossPointerPayload(buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
