package io.github.happysnappingturtle.mythicage.relic;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.relic.RelicSlots;
import io.github.happysnappingturtle.mythicage.core.relic.RelicState;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** 神庙供奉栏的网络包（T16 设计文档 §4.2）。各文明的神庙界面通用。 */
public final class RelicPayloads {
    private RelicPayloads() {
    }

    /**
     * 服务端 → 客户端：某座神庙的供奉架，以及看的人在该文明的供奉位用量。
     *
     * @param used 已用（含其他神庙）
     * @param cap  上限（按看的人在该文明的时代）
     */
    public record Shelf(BlockPos pos, boolean viewerIsOwner, int used, int cap, List<ItemStack> relics)
            implements CustomPacketPayload {
        public static final Type<Shelf> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "relic_shelf"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Shelf> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Shelf::pos,
                ByteBufCodecs.BOOL, Shelf::viewerIsOwner,
                ByteBufCodecs.VAR_INT, Shelf::used,
                ByteBufCodecs.VAR_INT, Shelf::cap,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(RelicShelf.MAX)), Shelf::relics,
                Shelf::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 客户端 → 服务端：取出某座神庙供奉架上的第 slot 件。 */
    public record Retrieve(BlockPos pos, int slot) implements CustomPacketPayload {
        public static final Type<Retrieve> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "relic_retrieve"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Retrieve> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Retrieve::pos,
                ByteBufCodecs.VAR_INT, Retrieve::slot,
                Retrieve::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static Shelf shelf(ServerPlayer viewer, ShrineBlockEntity be) {
        PlayerMythData d = MythData.get(viewer);
        var civ = be.kind().civ();
        boolean owner = be.owner().map(viewer.getUUID()::equals).orElse(false);
        return new Shelf(be.getBlockPos(), owner, d.relics().used(RelicState.OFFERED, civ),
                RelicSlots.offerSlots(d.civ(civ).age()), List.copyOf(be.relics().stacks()));
    }

    public static void sendShelf(ServerPlayer viewer, ServerLevel level, BlockPos pos) {
        if (viewer.connection != null && level.getBlockEntity(pos) instanceof ShrineBlockEntity be
                && RelicAnchor.Registry.forKind(be.kind()).isPresent()) {
            PacketDistributor.sendToPlayer(viewer, shelf(viewer, be));
        }
    }

    public static void handleRetrieve(Retrieve payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p
                    && p.distanceToSqr(payload.pos().getCenter()) <= 64) {
                RelicService.retrieve(p, p.serverLevel(), payload.pos(), payload.slot());
            }
        });
    }
}
