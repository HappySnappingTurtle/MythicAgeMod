package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** 委托页的网络负载（T21 设计文档 §9.3）。 */
public final class QuestPayloads {
    private QuestPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    /** 服务端 → 客户端：打开或刷新委托页。 */
    public record View(QuestView view) implements CustomPacketPayload {
        public static final Type<View> TYPE = payloadType("village_quests_view");
        public static final StreamCodec<FriendlyByteBuf, View> STREAM_CODEC = QuestView.STREAM_CODEC.map(View::new, View::view);

        @Override
        public Type<View> type() {
            return TYPE;
        }
    }

    /** 客户端 → 服务端：从村务界面打开委托页。 */
    public record Open(UUID village) implements CustomPacketPayload {
        public static final Type<Open> TYPE = payloadType("village_quests_open");
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.of(
                (buf, o) -> buf.writeUUID(o.village), buf -> new Open(buf.readUUID()));

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    /**
     * 客户端 → 服务端：接取、交付、放弃。{@code requestId} 每次点击新生成、重发时不变（防双击、网络重发，§3.3）。
     */
    public record Act(UUID village, String quest, String action, String requestId) implements CustomPacketPayload {
        public static final Type<Act> TYPE = payloadType("village_quests_act");
        public static final StreamCodec<FriendlyByteBuf, Act> STREAM_CODEC = StreamCodec.of(
                (buf, a) -> {
                    buf.writeUUID(a.village);
                    buf.writeUtf(a.quest);
                    buf.writeUtf(a.action);
                    buf.writeUtf(a.requestId);
                },
                buf -> new Act(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf()));

        @Override
        public Type<Act> type() {
            return TYPE;
        }
    }
}
