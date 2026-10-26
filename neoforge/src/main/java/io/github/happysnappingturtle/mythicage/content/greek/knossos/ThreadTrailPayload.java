package io.github.happysnappingturtle.mythicage.content.greek.knossos;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：本人的回程线（T23 §5.1）。replace 为真时整条重画（截断、清空、刚取线），否则把 points 接在已有的线后面。
 */
public record ThreadTrailPayload(boolean replace, List<Vec3> points) implements CustomPacketPayload {
    public static final Type<ThreadTrailPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "thread_trail"));

    public static final StreamCodec<FriendlyByteBuf, ThreadTrailPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.replace());
                buf.writeVarInt(p.points().size());
                for (Vec3 v : p.points()) {
                    buf.writeFloat((float) v.x);
                    buf.writeFloat((float) v.y);
                    buf.writeFloat((float) v.z);
                }
            },
            buf -> {
                boolean replace = buf.readBoolean();
                int n = Math.min(buf.readVarInt(), 4096);
                List<Vec3> pts = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    pts.add(new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()));
                }
                return new ThreadTrailPayload(replace, pts);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
