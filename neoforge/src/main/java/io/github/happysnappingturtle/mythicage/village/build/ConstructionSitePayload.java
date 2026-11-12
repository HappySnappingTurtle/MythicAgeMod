package io.github.happysnappingturtle.mythicage.village.build;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public record ConstructionSitePayload(UUID owner, ResourceLocation dimension, BlockPos min, BlockPos max, boolean active,
                                      List<BlockPos> blocked) implements CustomPacketPayload {
    public static final Type<ConstructionSitePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mythic_age", "construction_site"));
    public static final StreamCodec<FriendlyByteBuf, ConstructionSitePayload> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeUUID(p.owner); b.writeResourceLocation(p.dimension); b.writeBlockPos(p.min); b.writeBlockPos(p.max); b.writeBoolean(p.active);
        b.writeCollection(p.blocked, (buf, pos) -> buf.writeBlockPos(pos));
    }, b -> new ConstructionSitePayload(b.readUUID(), b.readResourceLocation(), b.readBlockPos(), b.readBlockPos(), b.readBoolean(),
            b.readList(buf -> buf.readBlockPos())));
    public ConstructionSitePayload { blocked = List.copyOf(blocked); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
