package io.github.happysnappingturtle.mythicage.village.found;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** 奠基石的网络包（T19 §1.13 K2）：服务端让建立者选主神；客户端回选好的主神。 */
public final class FoundationPayloads {
    private FoundationPayloads() {
    }

    /** 一位可选的主神与他名下的次神。 */
    public record Option(String god, List<String> minors) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Option> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Option::god,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), Option::minors,
                Option::new);
    }

    /** 服务端 → 客户端：打开选主神界面。 */
    public record Choose(BlockPos pos, String civ, List<Option> options) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "foundation_choose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Choose::pos,
                ByteBufCodecs.STRING_UTF8, Choose::civ,
                Option.STREAM_CODEC.apply(ByteBufCodecs.list(8)), Choose::options,
                Choose::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 客户端 → 服务端：选定主神，立村。 */
    public record Found(BlockPos pos, String patron) implements CustomPacketPayload {
        public static final Type<Found> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "foundation_found"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Found> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Found::pos,
                ByteBufCodecs.STRING_UTF8, Found::patron,
                Found::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
