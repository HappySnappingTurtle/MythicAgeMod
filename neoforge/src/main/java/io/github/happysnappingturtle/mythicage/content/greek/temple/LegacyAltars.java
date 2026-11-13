package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.relic.RelicSlots;
import io.github.happysnappingturtle.mythicage.core.village.temple.DevoteeMigration;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.relic.OfferedIndex;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageSpace;
import io.github.happysnappingturtle.mythicage.village.temple.TempleBook;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 旧存档里玩家祭坛的迁移（GA-13 §2.7）。神庙归村庄之后，{@code greek_altar} 成了装饰用的「旧祭坛」（同一个方块 id，
 * 背包里的物品不用换）：
 * <ul>
 *   <li>旧祭坛的方块实体加载时（{@link #migrate}）：注销神坛登记与台账；供奉在上面的圣物挪到主人在最近村子的供奉位，
 *   供奉位满了或附近没有村子就记进「待退回」，主人上线时放回背包；主人成为那个村子的供奉者；方块实体清掉主人，以后只是装饰。</li>
 *   <li>受过祝福的玩家第一次上线（{@link #onLogin}）：按登记里的旧祭坛位置成为最近村子的供奉者（区块没加载也行），提示一次。</li>
 * </ul>
 * 选村按 {@link DevoteeMigration}：旧祭坛在哪个村的村界内就是那个村；都不在就取 100 格内最近的；再没有就不指定。
 */
public final class LegacyAltars {
    /** 一次性提示的回执。 */
    public static final String RECEIPT = "ga13:temple_moved";

    private LegacyAltars() {
    }

    /** 旧祭坛位置该归哪个村子。 */
    public static Optional<UUID> villageFor(MinecraftServer server, GlobalPos altar) {
        ServerLevel level = server.getLevel(altar.dimension());
        Optional<UUID> inside = level == null ? Optional.empty()
                : VillageSpace.villageAt(level, altar.pos()).filter(v -> "greek".equals(v.civ())).map(VillageRegistry.Village::id);
        List<DevoteeMigration.Candidate<UUID>> cands = new ArrayList<>();
        for (VillageRegistry.Village v : VillageRegistry.get(server).in(altar.dimension())) {
            if (!"greek".equals(v.civ())) {
                continue;
            }
            BlockPos b = v.bell().pos();
            double d = Math.hypot(b.getX() - altar.pos().getX(), b.getZ() - altar.pos().getZ());
            cands.add(new DevoteeMigration.Candidate<>(v.id(), inside.map(v.id()::equals).orElse(false), d));
        }
        return DevoteeMigration.choose(cands);
    }

    /** 旧祭坛的方块实体加载（或测试里绑定）之后：迁走主人与圣物。没有主人时什么都不做。 */
    public static void migrate(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof GreekAltarBlockEntity be) || be.owner().isEmpty()) {
            return;
        }
        MinecraftServer server = level.getServer();
        UUID owner = be.owner().get();
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        ShrineRegistry.get(server).remove(key);
        PrayerService.resetForTest(key);
        io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers.get(server).remove(new io.github.happysnappingturtle.mythicage.core.runtime.ProducerId(
                PrayerService.PROBE_TYPE + ":" + key.dimension().location() + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ()));
        List<ItemStack> relics = be.relics().clear();
        OfferedIndex.remove(level, pos);
        Age age = be.ownerAge();
        be.clearOwner();
        Optional<UUID> village = villageFor(server, key);
        List<ItemStack> back = new ArrayList<>();
        if (village.isPresent()) {
            VillageTemples.addDevotee(server, village.get(), owner, MythDays.today(server), TempleBook.Via.MIGRATION);
            TempleBook book = VillageTemples.book(server, village.get());
            int cap = RelicSlots.offerSlots(age);
            for (ItemStack s : relics) {
                if (book.relics(owner).size() < cap) {
                    book.addRelic(owner, s);
                } else {
                    back.add(s);
                }
            }
            VillageTemples.changed(server);
        } else {
            back.addAll(relics);
        }
        if (!back.isEmpty()) {
            Returns.get(server).add(owner, back);
        }
        MythicAge.LOGGER.info("GA-13：旧祭坛 {} 已迁移（主人 {}，村子 {}，圣物 {} 件，待退回 {} 件）", pos, owner,
                village.map(UUID::toString).orElse("无"), relics.size(), back.size());
        // 主人在线就立刻对账（圣物绑定改到村子）、退回
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) {
            giveReturns(p);
            io.github.happysnappingturtle.mythicage.relic.RelicService.reconcile(p);
        }
    }

    /** 受过祝福的玩家上线：按登记里的旧祭坛成为供奉者（只做一次）、退回待退回的圣物。 */
    public static void onLogin(ServerPlayer player) {
        MinecraftServer server = player.server;
        giveReturns(player);
        PlayerMythData data = MythData.get(player);
        if (data.civ(Civilization.GREEK).age() == Age.NONE || data.receipts().contains(ReceiptBook.Scope.UNIQUE, RECEIPT)) {
            return;
        }
        data.receipts().record(ReceiptBook.Scope.UNIQUE, RECEIPT);
        boolean had = false;
        for (ShrineRegistry.Entry e : ShrineRegistry.get(server).ownedBy(player.getUUID(), Civilization.GREEK)) {
            if (!e.kind().equals(GreekTemple.ALTAR_KIND.id())) {
                continue;
            }
            had = true;
            villageFor(server, e.pos()).ifPresent(v -> io.github.happysnappingturtle.mythicage.trade.TradeService.becomeDevotee(player, v,
                    TempleBook.Via.MIGRATION));
        }
        // 在哪个村子晋升、立祀过，就是那里的供奉者（与新规则一致）
        data.civ(Civilization.GREEK).riteVillages().forEach((v, mark) -> {
            if (VillageRegistry.get(server).byId(v).isPresent()) {
                io.github.happysnappingturtle.mythicage.trade.TradeService.becomeDevotee(player, v,
                        mark.promotedHere() ? TempleBook.Via.PROMOTION : TempleBook.Via.RITE);
            }
        });
        boolean devoted = !VillageTemples.devotedVillages(server, player.getUUID()).isEmpty();
        player.displayClientMessage(Component.translatable(had || devoted ? "mythic_age.temple.moved" : "mythic_age.temple.moved_join"),
                false);
        MythData.changed(player);
    }

    static void giveReturns(ServerPlayer player) {
        List<ItemStack> back = Returns.get(player.server).take(player.getUUID());
        for (ItemStack s : back) {
            if (!player.getInventory().add(s)) {
                player.drop(s, false);
            }
        }
        if (!back.isEmpty()) {
            player.displayClientMessage(Component.translatable("mythic_age.temple.relics_returned", back.size()), false);
        }
    }

    /** 待退回的圣物（主人不在线、或附近没有村子 / 供奉位满了时）：玩家 → 物品。 */
    public static final class Returns extends SavedData {
        public static final String NAME = "mythic_age_legacy_altar_returns";
        private final Map<UUID, List<ItemStack>> pending = new LinkedHashMap<>();

        public static Returns get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(Returns::new, Returns::load, null), NAME);
        }

        void add(UUID player, List<ItemStack> stacks) {
            pending.computeIfAbsent(player, k -> new ArrayList<>()).addAll(stacks);
            setDirty();
        }

        List<ItemStack> take(UUID player) {
            List<ItemStack> out = pending.remove(player);
            if (out == null) {
                return List.of();
            }
            setDirty();
            return out;
        }

        public List<ItemStack> peek(UUID player) {
            return List.copyOf(pending.getOrDefault(player, List.of()));
        }

        static Returns load(CompoundTag tag, HolderLookup.Provider registries) {
            Returns r = new Returns();
            for (Tag t : tag.getList("players", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                if (!c.hasUUID("id")) {
                    continue;
                }
                List<ItemStack> l = new ArrayList<>();
                for (Tag i : c.getList("items", Tag.TAG_COMPOUND)) {
                    ItemStack.parse(registries, i).filter(s -> !s.isEmpty()).ifPresent(l::add);
                }
                r.pending.put(c.getUUID("id"), l);
            }
            return r;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            pending.forEach((id, l) -> {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", id);
                ListTag items = new ListTag();
                l.forEach(s -> items.add(s.save(registries)));
                c.put("items", items);
                list.add(c);
            });
            tag.put("players", list);
            return tag;
        }
    }
}
