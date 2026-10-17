package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：一次施放的表现事件（{@code mythic_age:power_fx}，T12 设计文档 §2.7 / §7）。
 * 只管「播什么」，不带任何结算结果；客户端按 castId + phase 去重。
 *
 * @param caster       施法者实体 id
 * @param dimension    原点所在维度（客户端不在该维度就忽略）
 * @param targets      目标实体 id，最多 {@link #MAX_TARGETS} 个，多的截断
 * @param startTick    该阶段开始的世界时钟刻（只作对照，客户端按收到的时刻播放）
 * @param duration     {@code CHANNEL_START} 为吟唱刻数，{@code COMMIT} 为（剩余）持续上界，其余为 0
 * @param cancelReason {@code CHANNEL_CANCEL} 的原因（{@code PowerReason} 名），其余为空串
 */
public record PowerFxPayload(String castId, String powerId, Phase phase, int caster, ResourceLocation dimension, Vec3 origin,
                             List<Integer> targets, long startTick, long seed, int duration, String cancelReason)
        implements CustomPacketPayload {
    public static final int MAX_TARGETS = 16;
    public static final Type<PowerFxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "power_fx"));

    public enum Phase { CHANNEL_START, CHANNEL_CANCEL, COMMIT, WARNING, HIT, END }

    public PowerFxPayload {
        targets = List.copyOf(targets.size() > MAX_TARGETS ? targets.subList(0, MAX_TARGETS) : targets);
    }

    /** 同一份表现换一个持续时间（补播时给剩余时长）。 */
    public PowerFxPayload withDuration(int d) {
        return new PowerFxPayload(castId, powerId, phase, caster, dimension, origin, targets, startTick, seed, d, cancelReason);
    }

    public static final StreamCodec<FriendlyByteBuf, PowerFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.castId, 64);
                buf.writeUtf(p.powerId, 128);
                buf.writeEnum(p.phase);
                buf.writeVarInt(p.caster);
                buf.writeResourceLocation(p.dimension);
                buf.writeDouble(p.origin.x);
                buf.writeDouble(p.origin.y);
                buf.writeDouble(p.origin.z);
                buf.writeVarInt(p.targets.size());
                p.targets.forEach(buf::writeVarInt);
                buf.writeVarLong(p.startTick);
                buf.writeLong(p.seed);
                buf.writeVarInt(p.duration);
                buf.writeUtf(p.cancelReason, 64);
            },
            buf -> {
                String castId = buf.readUtf(64);
                String powerId = buf.readUtf(128);
                Phase phase = buf.readEnum(Phase.class);
                int caster = buf.readVarInt();
                ResourceLocation dim = buf.readResourceLocation();
                Vec3 origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                int n = buf.readVarInt();
                if (n < 0 || n > MAX_TARGETS) {
                    throw new IllegalArgumentException("目标数超出上限：" + n);
                }
                List<Integer> targets = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    targets.add(buf.readVarInt());
                }
                return new PowerFxPayload(castId, powerId, phase, caster, dim, origin, targets, buf.readVarLong(),
                        buf.readLong(), buf.readVarInt(), buf.readUtf(64));
            });

    @Override
    public Type<PowerFxPayload> type() {
        return TYPE;
    }
}
