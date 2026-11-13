package io.github.happysnappingturtle.mythicage.relic;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.relic.RelicBinding;
import io.github.happysnappingturtle.mythicage.core.relic.RelicBook;
import io.github.happysnappingturtle.mythicage.core.relic.RelicCard;
import io.github.happysnappingturtle.mythicage.core.relic.RelicSlots;
import io.github.happysnappingturtle.mythicage.core.relic.RelicState;
import io.github.happysnappingturtle.mythicage.core.relic.TempleRelicRules;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.temple.TempleBook;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 村庄神庙的圣物供奉（GA-13 §2.5）：每位供奉者在每个村子有自己的供奉位，圣物存在这个村子的神庙簿里，
 * 绑定表记 {@code village:<村>}。效果以村庄神庙的圆心算 48 格、只对放的人生效（{@link OfferedIndex} 每人一条）；
 * 全局类效果（祈祷 R、兑换价折扣）照旧按绑定表算，与放在哪无关。
 */
public final class TempleRelics {
    public static final String WHERE_PREFIX = "village:";
    /** 定期刷新索引的间隔（刻）：神庙所在区块加载、卸载后跟上。 */
    public static final int INDEX_PERIOD = 100;

    private TempleRelics() {
    }

    public static String where(UUID village) {
        return WHERE_PREFIX + village;
    }

    static Optional<UUID> villageOf(String where) {
        if (where == null || !where.startsWith(WHERE_PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(where.substring(WHERE_PREFIX.length())));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ───────────── 放入、取出 ─────────────

    /** 拒绝原因；可以供奉时为空。 */
    public static Optional<Component> offerError(ServerPlayer player, VillageTemple t, RelicCard card, ItemStack held) {
        TempleBook book = VillageTemples.book(player.server, t.village());
        Civilization civ = Civilization.byId(t.civ()).orElse(Civilization.GREEK);
        PlayerMythData data = MythData.get(player);
        Optional<RelicBinding> b = data.relics().get(card.id());
        int used = book.relics(player.getUUID()).size();
        int cap = RelicSlots.offerSlots(data.civ(civ).age());
        return switch (TempleRelicRules.check(book.isDevotee(player.getUUID()), card.civ() == civ, b.isPresent(), used, cap)) {
            case NONE -> Optional.empty();
            case NOT_DEVOTEE -> Optional.of(Component.translatable("mythic_age.relic.not_devotee"));
            case WRONG_CIV -> Optional.of(Component.translatable("mythic_age.relic.wrong_civ",
                    Component.translatable("mythic_age.civ." + card.civ().id())));
            case OCCUPIED -> Optional.of(RelicService.occupied(held, b.orElseThrow()));
            case FULL -> Optional.of(Component.translatable("mythic_age.relic.full", used, cap));
        };
    }

    /** 手持圣物潜行右键神庙祭坛：放进自己在这个村子的下一个空位。返回是否处理了（不是圣物为 false）。 */
    public static boolean offer(ServerPlayer player, ServerLevel level, VillageTemple t, ItemStack held) {
        Optional<RelicCard> card = RelicCards.of(held);
        if (card.isEmpty()) {
            return false;
        }
        Optional<Component> error = offerError(player, t, card.get(), held);
        if (error.isPresent()) {
            player.displayClientMessage(error.get(), true);
            return true;
        }
        UUID inst = RelicItems.ensureInstance(held);
        RelicBook book = MythData.get(player).relics();
        book.bind(card.get().id(), new RelicBinding(RelicState.OFFERED, inst, card.get().civ(), where(t.village())));
        ItemStack one = held.copyWithCount(1);
        VillageTemples.book(player.server, t.village()).addRelic(player.getUUID(), one);
        VillageTemples.changed(player.server);
        held.shrink(1);
        var a = t.altar().pos();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, a.getX() + 0.5, a.getY() + 1.2, a.getZ() + 0.5,
                16, 0.35, 0.15, 0.35, 0.01);
        level.playSound(null, a, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.2F);
        index(player.server, t);
        RelicService.changed(player);
        player.displayClientMessage(Component.translatable("mythic_age.relic.offered", one.getHoverName()), true);
        sendShelf(player, t);
        TempleRelicDisplay.broadcast(level, t);
        return true;
    }

    /** 取出自己在这个村子的第 slot 件，放回背包（满了掉在脚下）。 */
    public static boolean retrieve(ServerPlayer player, VillageTemple t, int slot) {
        ItemStack s = VillageTemples.book(player.server, t.village()).removeRelic(player.getUUID(), slot);
        if (s.isEmpty()) {
            return false;
        }
        VillageTemples.changed(player.server);
        release(player, s);
        index(player.server, t);
        player.displayClientMessage(Component.translatable("mythic_age.relic.retrieved", s.getHoverName()), true);
        give(player, s);
        RelicService.changed(player);
        sendShelf(player, t);
        if (player.server.getLevel(t.altar().dimension()) instanceof ServerLevel level) {
            TempleRelicDisplay.broadcast(level, t);
        }
        return true;
    }

    /** 退出供奉时：交回的圣物解绑并放回背包（GA-13 D3）。 */
    public static void returnAll(ServerPlayer player, UUID village, List<ItemStack> stacks) {
        for (ItemStack s : stacks) {
            release(player, s);
            give(player, s);
        }
        VillageTemples.of(player.server, village).ifPresent(t -> index(player.server, t));
        if (!stacks.isEmpty()) {
            RelicService.changed(player);
        }
    }

    private static void release(ServerPlayer player, ItemStack s) {
        RelicCards.of(s).ifPresent(c -> RelicItems.instance(s).ifPresent(i -> MythData.get(player).relics().release(c.id(), i)));
    }

    private static void give(ServerPlayer player, ItemStack s) {
        if (!player.getInventory().add(s)) {
            player.drop(s, false);
        }
    }

    // ───────────── 对账（§5 的 OFFERED 一支） ─────────────

    /** 绑定表里「供奉在村子」的一条是否还对得上：村子还在、这位玩家在那里的圣物里有这一件。 */
    static boolean bindingValid(MinecraftServer server, UUID player, RelicBinding b) {
        Optional<UUID> v = villageOf(b.where());
        if (v.isEmpty()) {
            return false;
        }
        Optional<VillageEconomy.State> s = VillageEconomy.get(server).peek(v.get());
        if (s.isEmpty()) {
            return false;
        }
        for (ItemStack st : s.get().temple.relics(player)) {
            if (b.instance().equals(RelicItems.instance(st).orElse(null))) {
                return true;
            }
        }
        return false;
    }

    // ───────────── 效果索引 ─────────────

    /** 按神庙簿重建这个村子的索引条目：每位供奉者一条，位置是神庙圆心。祭坛所在区块没加载时去掉。 */
    public static void index(MinecraftServer server, VillageTemple t) {
        OfferedIndex.removeVillage(t.village());
        ServerLevel level = server.getLevel(t.altar().dimension());
        if (level == null || !level.isLoaded(t.center())) {
            return;
        }
        Civilization civ = Civilization.byId(t.civ()).orElse(Civilization.GREEK);
        TempleBook book = VillageTemples.book(server, t.village());
        for (UUID d : book.devotees()) {
            Set<RelicCard> cards = new LinkedHashSet<>();
            for (ItemStack s : book.relics(d)) {
                RelicCards.of(s).ifPresent(cards::add);
            }
            OfferedIndex.putVillage(t.village(), d, GlobalPos.of(t.altar().dimension(), t.center()), civ, List.copyOf(cards));
        }
    }

    /** 每 {@link #INDEX_PERIOD} 刻：有圣物的村子重建一次索引（区块加载卸载后跟上）。 */
    public static void refreshIndex(MinecraftServer server) {
        for (VillageEconomy.State s : VillageEconomy.get(server).all()) {
            if (s.temple.allRelics().isEmpty()) {
                OfferedIndex.removeVillage(s.village);
                continue;
            }
            Optional<VillageTemple> t = VillageTemples.of(server, s.village);
            if (t.isPresent()) {
                index(server, t.get());
                if (server.getLevel(t.get().altar().dimension()) instanceof ServerLevel level) {
                    TempleRelicDisplay.broadcast(level, t.get());
                }
            } else {
                OfferedIndex.removeVillage(s.village);
            }
        }
    }

    // ───────────── 界面 ─────────────

    /** 供奉栏：看的人在这个村子的圣物、已用 / 上限（按这个村子算）。 */
    public static RelicPayloads.Shelf shelf(ServerPlayer viewer, VillageTemple t) {
        TempleBook book = VillageTemples.book(viewer.server, t.village());
        Civilization civ = Civilization.byId(t.civ()).orElse(Civilization.GREEK);
        List<ItemStack> mine = new ArrayList<>(book.relics(viewer.getUUID()));
        return new RelicPayloads.Shelf(t.altar().pos(), book.isDevotee(viewer.getUUID()), mine.size(),
                RelicSlots.offerSlots(MythData.get(viewer).civ(civ).age()), List.copyOf(mine));
    }

    public static void sendShelf(ServerPlayer viewer, VillageTemple t) {
        if (viewer.connection != null && !(viewer instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            PacketDistributor.sendToPlayer(viewer, shelf(viewer, t));
        }
    }
}
