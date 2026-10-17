package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.power.PowerSpec;
import io.github.happysnappingturtle.mythicage.core.power.TargetProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 施法者：瞄准预览（{@code mythic_age:power_aim}，T12 设计文档 §7）。几何由客户端每帧自己算，
 * 合法性由服务端判：{@code START} 带预览参数，{@code HOVER} 给悬停实体的判定（没有悬停实体时 id 为 -1），
 * {@code STOP} 结束预览（确认进入吟唱或会话结束）。
 */
public record PowerAimPayload(Kind kind, String castId, String powerId, TargetProfile.Pick pick, boolean selfCentered,
                              float range, float radius, int entityId, boolean valid) implements CustomPacketPayload {
    public static final Type<PowerAimPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "power_aim"));

    public enum Kind { START, HOVER, STOP }

    public static PowerAimPayload start(String castId, PowerSpec spec) {
        return new PowerAimPayload(Kind.START, castId, spec.id(), spec.profile().pick(), spec.selfCentered(),
                (float) spec.range(), (float) spec.radius(), -1, false);
    }

    public static PowerAimPayload hover(String castId, int entityId, boolean valid) {
        return new PowerAimPayload(Kind.HOVER, castId, "", TargetProfile.Pick.ENTITY, false, 0, 0, entityId, valid);
    }

    public static PowerAimPayload stop(String castId) {
        return new PowerAimPayload(Kind.STOP, castId, "", TargetProfile.Pick.ENTITY, false, 0, 0, -1, false);
    }

    public static final StreamCodec<FriendlyByteBuf, PowerAimPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.kind);
                buf.writeUtf(p.castId, 64);
                buf.writeUtf(p.powerId, 128);
                buf.writeEnum(p.pick);
                buf.writeBoolean(p.selfCentered);
                buf.writeFloat(p.range);
                buf.writeFloat(p.radius);
                buf.writeVarInt(p.entityId);
                buf.writeBoolean(p.valid);
            },
            buf -> new PowerAimPayload(buf.readEnum(Kind.class), buf.readUtf(64), buf.readUtf(128),
                    buf.readEnum(TargetProfile.Pick.class), buf.readBoolean(), buf.readFloat(), buf.readFloat(),
                    buf.readVarInt(), buf.readBoolean()));

    @Override
    public Type<PowerAimPayload> type() {
        return TYPE;
    }
}
