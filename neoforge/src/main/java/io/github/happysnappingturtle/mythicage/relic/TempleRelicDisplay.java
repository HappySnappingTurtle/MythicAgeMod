package io.github.happysnappingturtle.mythicage.relic;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.village.temple.TempleBook;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
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

import java.util.List;

/**
 * 村庄神庙祭坛上方的圣物展示（GA-13 §2.5）：最近放进来的 3 件（所有供奉者合在一起），客户端让它们悬在祭坛上方慢慢转。
 * 供奉、取出时和每 {@link TempleRelics#INDEX_PERIOD} 刻发给祭坛 {@link #RANGE} 格内的玩家；客户端收不到更新就过期不画。
 */
public final class TempleRelicDisplay {
    public static final int SHOWN = 3;
    public static final double RANGE = 64;

    private TempleRelicDisplay() {
    }

    /** 服务端 → 客户端：某座祭坛上方要画的圣物（空表 = 不画）。 */
    public record Display(BlockPos altar, List<ItemStack> relics) implements CustomPacketPayload {
        public static final Type<Display> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "temple_relics"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Display> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Display::altar,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(SHOWN)), Display::relics,
                Display::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 最近放进来的几件（按放入先后取最后 {@link #SHOWN} 件）。 */
    public static List<ItemStack> recent(TempleBook book) {
        List<ItemStack> all = book.allRelics();
        return List.copyOf(all.subList(Math.max(0, all.size() - SHOWN), all.size()).stream().map(ItemStack::copy).toList());
    }

    public static void broadcast(ServerLevel level, VillageTemple t) {
        Display d = new Display(t.altar().pos(), recent(VillageTemples.book(level.getServer(), t.village())));
        for (ServerPlayer p : level.players()) {
            if (p.connection != null && !(p instanceof net.neoforged.neoforge.common.util.FakePlayer)
                    && p.blockPosition().closerThan(t.altar().pos(), RANGE)) {
                PacketDistributor.sendToPlayer(p, d);
            }
        }
    }
}
