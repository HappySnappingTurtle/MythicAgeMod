package io.github.happysnappingturtle.mythicage.boss;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：试炼 Boss 血条下的一行提示（T24 §5.3，12 号 §4「关键机制数、个人首杀状态」）。五文明通用：
 * 机制计数取竞技场声明的柱（以后别的竞技场换成自己的计数）。
 *
 * @param active    false = 离开大厅或战斗结束，清掉这一行
 * @param intact    完整的机制物（柱）数；total 为 0 时不显示计数
 * @param walls     机关墙状态：空串（阶段一）、none / a / ab
 * @param firstKill 这名玩家是否已有本试炼的首杀记录
 */
public record ArenaHudPayload(boolean active, int intact, int total, String walls, boolean firstKill) implements CustomPacketPayload {
    public static final ArenaHudPayload OFF = new ArenaHudPayload(false, 0, 0, "", false);
    public static final Type<ArenaHudPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "arena_hud"));
    public static final StreamCodec<FriendlyByteBuf, ArenaHudPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.active);
                buf.writeVarInt(p.intact);
                buf.writeVarInt(p.total);
                buf.writeUtf(p.walls);
                buf.writeBoolean(p.firstKill);
            },
            buf -> new ArenaHudPayload(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readBoolean()));

    @Override
    public Type<ArenaHudPayload> type() {
        return TYPE;
    }
}
