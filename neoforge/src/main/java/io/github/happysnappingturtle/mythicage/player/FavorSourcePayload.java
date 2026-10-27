package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：刚才这笔恩宠的来源（T24 §5.4，HUD 写「+5 祈祷」）。数值仍以同步包为准。 */
public record FavorSourcePayload(Civilization civ, String sourceKey) implements CustomPacketPayload {
    public static final Type<FavorSourcePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "favor_source"));
    public static final StreamCodec<FriendlyByteBuf, FavorSourcePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.civ);
                buf.writeUtf(p.sourceKey);
            }, buf -> new FavorSourcePayload(buf.readEnum(Civilization.class), buf.readUtf()));

    /** 台账 id 的前缀（产出点种类）→ 来源语言键。 */
    public static String ledgerSource(String producerId) {
        int i = producerId.indexOf(':');
        return "mythic_age.favor.source." + (i < 0 ? producerId : producerId.substring(0, i));
    }

    @Override
    public Type<FavorSourcePayload> type() {
        return TYPE;
    }
}
