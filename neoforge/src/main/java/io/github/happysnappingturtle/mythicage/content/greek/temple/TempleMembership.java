package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.relic.TempleRelics;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 退出供奉（GA-13 D3）：在神庙检视里点「不再供奉此村」。不再从这里入账、台账注销、供奉在这里的圣物退回背包；
 * 之后在这里的守护者处「在这里供奉」就能回来。
 */
public final class TempleMembership {
    private TempleMembership() {
    }

    /** 客户端 → 服务端：退出这座祭坛所属村子的供奉。 */
    public record Leave(BlockPos altar) implements CustomPacketPayload {
        public static final Type<Leave> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "temple_leave"));
        public static final StreamCodec<FriendlyByteBuf, Leave> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Leave::altar, Leave::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void handleLeave(Leave p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.distanceToSqr(p.altar().getCenter()) <= 64) {
                VillageTemples.byAltar(player.server, GlobalPos.of(player.level().dimension(), p.altar()))
                        .ifPresent(t -> leave(player, t.village()));
            }
        });
    }

    /** 退出；不是这里的供奉者时返回 false。 */
    public static boolean leave(ServerPlayer player, UUID village) {
        var book = VillageTemples.book(player.server, village);
        if (!book.isDevotee(player.getUUID())) {
            return false;
        }
        List<ItemStack> relics = book.removeDevotee(player.getUUID());
        VillageTemples.changed(player.server);
        TemplePrayer.dropLedger(player.server, village, player.getUUID());
        TempleRelics.returnAll(player, village, relics);
        player.displayClientMessage(Component.translatable("mythic_age.temple.left"), false);
        Optional<VillageTemple> t = VillageTemples.of(player.server, village);
        t.ifPresent(x -> {
            TempleRelics.sendShelf(player, x);
            VillageTempleSurvey.send(player, x);
            if (player.server.getLevel(x.altar().dimension()) instanceof net.minecraft.server.level.ServerLevel level) {
                io.github.happysnappingturtle.mythicage.relic.TempleRelicDisplay.broadcast(level, x);
            }
        });
        return true;
    }
}
