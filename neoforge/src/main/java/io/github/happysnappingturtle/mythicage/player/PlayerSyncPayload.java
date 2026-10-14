package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：五个文明的恩宠、时代与上限（上限已含圣物加成），以及数据版本号（T4 设计文档 §4）。
 */
public record PlayerSyncPayload(long dataVersion, List<CivEntry> civs) implements CustomPacketPayload {
    public record CivEntry(Civilization civ, long favor, Age age, long cap) {
    }

    public static final Type<PlayerSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "player_sync"));

    public static final StreamCodec<FriendlyByteBuf, PlayerSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarLong(p.dataVersion());
                buf.writeVarInt(p.civs().size());
                for (CivEntry e : p.civs()) {
                    buf.writeEnum(e.civ());
                    buf.writeVarLong(e.favor());
                    buf.writeEnum(e.age());
                    buf.writeVarLong(e.cap());
                }
            },
            buf -> {
                long version = buf.readVarLong();
                int n = buf.readVarInt();
                List<CivEntry> civs = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    civs.add(new CivEntry(buf.readEnum(Civilization.class), buf.readVarLong(),
                            buf.readEnum(Age.class), buf.readVarLong()));
                }
                return new PlayerSyncPayload(version, civs);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
