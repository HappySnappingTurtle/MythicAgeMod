package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** 召唤管理界面的网络负载（T14 设计文档 §11.2）。 */
public final class SummonPayloads {
    private SummonPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    /** 客户端 → 服务端：打开（或刷新）管理界面。 */
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = payloadType("summon_request");
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.unit(new Request());

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：快照。 */
    public record View(SummonView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = payloadType("summon_view");
        public static final StreamCodec<FriendlyByteBuf, View> STREAM_CODEC = SummonView.STREAM_CODEC.map(View::new, View::view);

        @Override
        public Type<View> type() {
            return TYPE;
        }
    }

    public enum Action {
        /** 单个单位改模式（{@code mode}）。 */
        MODE,
        /** 单个单位召回到主人身边（改为跟随）。 */
        RECALL,
        /** 单个单位遣散。 */
        DISMISS,
        /** 全部改为跟随。 */
        ALL_FOLLOW,
        /** 32 格内的单位驻守主人脚下。 */
        ALL_GUARD_HERE
    }

    /** 客户端 → 服务端：指令。{@code unitId} 在 ALL_* 时忽略；{@code mode} 只在 MODE 时有意义。 */
    public record Command(Action action, UUID unitId, SummonMode mode) implements CustomPacketPayload {
        private static final UUID NONE = new UUID(0, 0);
        public static final Type<Command> TYPE = payloadType("summon_command");
        public static final StreamCodec<FriendlyByteBuf, Command> STREAM_CODEC = StreamCodec.of(
                (buf, c) -> {
                    buf.writeEnum(c.action);
                    buf.writeUUID(c.unitId);
                    buf.writeEnum(c.mode);
                },
                buf -> new Command(buf.readEnum(Action.class), buf.readUUID(), buf.readEnum(SummonMode.class)));

        public static Command all(Action action) {
            return new Command(action, NONE, SummonMode.FOLLOW);
        }

        @Override
        public Type<Command> type() {
            return TYPE;
        }
    }
}
