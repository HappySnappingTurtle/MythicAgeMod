package io.github.happysnappingturtle.mythicage.rig;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.rig.RigDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** 服务端 → 客户端：rig 的视线限幅；登录与数据包重载时发送完整快照。 */
public record RigHeadLookPayload(Map<ResourceLocation, RigDefinition.HeadLook> limits) implements CustomPacketPayload {
    public RigHeadLookPayload {
        limits = Map.copyOf(limits);
    }

    public static final Type<RigHeadLookPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "rig_head_look"));

    public static final StreamCodec<FriendlyByteBuf, RigHeadLookPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.limits().size());
                p.limits().forEach((id, look) -> {
                    buf.writeResourceLocation(id);
                    buf.writeFloat(look.yaw());
                    buf.writeFloat(look.pitch());
                });
            },
            buf -> {
                int n = buf.readVarInt();
                Map<ResourceLocation, RigDefinition.HeadLook> limits = new HashMap<>();
                for (int i = 0; i < n; i++) {
                    limits.put(buf.readResourceLocation(), new RigDefinition.HeadLook(buf.readFloat(), buf.readFloat()));
                }
                return new RigHeadLookPayload(limits);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
