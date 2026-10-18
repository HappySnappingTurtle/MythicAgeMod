package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：某个实体身上「有外观的状态」（{@code mythic_age:aura}，T13 设计文档 §1.4）。
 * 原版只把药水效果同步给玩家自己，别人看不到怪物身上有什么效果，所以外观状态单独同步。
 *
 * @param remainingTicks 剩余刻数；0 表示移除
 */
public record AuraPayload(int entityId, String aura, int remainingTicks) implements CustomPacketPayload {
    public static final Type<AuraPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "aura"));

    public static final StreamCodec<FriendlyByteBuf, AuraPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entityId);
                buf.writeUtf(p.aura, 32);
                buf.writeVarInt(p.remainingTicks);
            },
            buf -> new AuraPayload(buf.readVarInt(), buf.readUtf(32), buf.readVarInt()));

    @Override
    public Type<AuraPayload> type() {
        return TYPE;
    }
}
