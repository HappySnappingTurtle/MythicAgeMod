package io.github.happysnappingturtle.mythicage.village.economy;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.trade.ResidentOffer;
import io.github.happysnappingturtle.mythicage.core.village.trade.ResidentTradeRules;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * 居民交易（T19 设计文档 §11.3、§1.8 I1–I7）：卖的东西从村库出货、收的东西收进村库；不是祭司的恩宠兑换（D8）。
 * <ul>
 *   <li>卖：村库不够一份就缺货；口粮卖到底线为止；杂货（{@code source: import}）不走村库；</li>
 *   <li>收：村库装不下一份就拒收，玩家的东西不动；</li>
 *   <li>每条商品每名居民每 MC 日有额度，当天祈祷过的居民减半（F7）；次神特色商品只在立过该次神祠的村庄出现，供奉者打折。</li>
 * </ul>
 * 成交在服务端一刻内完成：先验证（距离、库存、额度、绿宝石 / 物品、背包），再扣、再给。
 */
public final class ResidentTrade {
    public static final double REACH = 8;
    /** 居民今天祈祷过没有（F7）；由各文明的祈祷模块设置（希腊：T10 的祈祷记录）。 */
    private static volatile BiPredicate<ResidentEntity, Long> prayed = (r, day) -> false;

    private ResidentTrade() {
    }

    public static void setPrayedToday(BiPredicate<ResidentEntity, Long> p) {
        prayed = p;
    }

    /** 这名居民有没有交易表。 */
    public static boolean trades(ResidentEntity r) {
        return !offers(r).isEmpty();
    }

    static List<ResidentOffer> offers(ResidentEntity r) {
        return VillageData.civ(r.civ()).map(d -> d.professions().get(r.profession()))
                .map(CivVillageData.Profession::trades).orElse(List.of());
    }

    static Set<String> playerGods(ServerPlayer player, String civ) {
        return Civilization.byId(civ).map(c -> (Set<String>) new LinkedHashSet<>(MythData.get(player).civ(c).minorGods().values()))
                .orElse(Set.of());
    }

    public static void open(ServerPlayer player, ResidentEntity r) {
        view(player, r).ifPresent(v -> PacketDistributor.sendToPlayer(player, new AffairsPayloads.Trade(v)));
    }

    public static Optional<ResidentTradeView> view(ServerPlayer player, ResidentEntity r) {
        Optional<Context> c = context(player, r);
        if (c.isEmpty()) {
            return Optional.empty();
        }
        List<ResidentTradeView.Row> rows = new ArrayList<>();
        for (ResidentOffer o : c.get().visible) {
            rows.add(row(player, r, c.get(), o));
        }
        return Optional.of(new ResidentTradeView(r.getId(), r.getCustomName() == null ? "" : r.getCustomName().getString(),
                VillageAffairs.professionKey(r.profession()), List.copyOf(rows)));
    }

    private record Context(VillageRegistry.Village village, VillageEconomy.State state, CivVillageData data, ResidentTradeRules rules,
                           Set<String> gods, long today, boolean prayedToday, List<ResidentOffer> visible) {
    }

    private static Optional<Context> context(ServerPlayer player, ResidentEntity r) {
        Optional<VillageRegistry.Village> v = r.village().flatMap(id -> VillageRegistry.get(player.server).byId(id));
        Optional<CivVillageData> data = VillageData.civ(r.civ());
        if (v.isEmpty() || data.isEmpty()) {
            return Optional.empty();
        }
        long today = MythDays.today(player.server);
        Set<String> shrines = v.get().shrineGods();
        VillageEconomy.State state = VillageEconomy.get(player.server).state(v.get().id());
        String festival = io.github.happysnappingturtle.mythicage.core.village.life.FestivalRules.on(data.get().life(),
                data.get().calendar(), VillageData.offset(data.get(), state), today, v.get().stage())
                .map(io.github.happysnappingturtle.mythicage.core.village.life.LifeData.Festival::id).orElse("");
        List<ResidentOffer> visible = offers(r).stream()
                .filter(o -> ResidentTradeRules.visible(o, v.get().stage(), shrines, festival)).toList();
        return Optional.of(new Context(v.get(), VillageEconomy.get(player.server).state(v.get().id()), data.get(), data.get().trade(),
                playerGods(player, r.civ()), today, prayed.test(r, today), visible));
    }

    private static ResidentTradeView.Row row(ServerPlayer player, ResidentEntity r, Context c, ResidentOffer o) {
        int price = c.rules.price(o, c.gods);
        int remaining = Math.max(0, c.rules.dailyCap(o, c.prayedToday, c.gods) - r.tradedToday(o.id(), c.today));
        if (o.villageCap() > 0) {
            // 节庆商品：每村每节限量（§1.10 J5）
            remaining = Math.min(remaining, Math.max(0, o.villageCap() - c.state.dailyCount(c.today, goodsKey(o))));
        }
        int stock = o.sell() ? ResidentTradeRules.stockShares(o, c.state.stock, c.data.rations()) : 0;
        ResidentTradeView.State state;
        if (remaining <= 0) {
            state = ResidentTradeView.State.DAILY_DONE;
        } else if (o.sell()) {
            state = stock <= 0 ? ResidentTradeView.State.OUT_OF_STOCK
                    : count(player.getInventory(), Items.EMERALD) < price ? ResidentTradeView.State.NO_EMERALD : ResidentTradeView.State.OK;
        } else {
            state = !ResidentTradeRules.canAccept(o, c.state.stock) ? ResidentTradeView.State.FULL
                    : count(player.getInventory(), item(o)) < o.count() ? ResidentTradeView.State.NO_ITEM : ResidentTradeView.State.OK;
        }
        return new ResidentTradeView.Row(o.id(), o.sell(), o.item(), o.count(), price, o.price(),
                o.imported() ? -1 : Math.min(stock, 999), remaining, state);
    }

    static String goodsKey(ResidentOffer o) {
        return "goods:" + o.id();
    }

    static Item item(ResidentOffer o) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(o.item()));
    }

    /** 背包里这种物品（没有耐久损耗、没有附加数据的）有几件。 */
    static int count(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && plain(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static boolean plain(ItemStack s) {
        return !s.isDamaged() && s.getComponentsPatch().isEmpty();
    }

    private static void remove(Inventory inv, Item item, int n) {
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && plain(s)) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
    }

    private static void give(ServerPlayer player, Item item, int n) {
        while (n > 0) {
            int k = Math.min(n, item.getDefaultMaxStackSize());
            ItemStack st = new ItemStack(item, k);
            if (!player.getInventory().add(st)) {
                player.drop(st, false);
            }
            n -= k;
        }
    }

    public static void handleDeal(AffairsPayloads.Deal payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p && p.level().getEntity(payload.resident()) instanceof ResidentEntity r
                    && r.isAlive() && r.distanceToSqr(p) <= REACH * REACH) {
                deal(p, r, payload.offer());
                open(p, r);
            }
        });
    }

    /** 做一份交易（GameTest 直接调用，不看距离）。返回此时这条商品的状态（OK 表示成交了）。 */
    public static ResidentTradeView.State deal(ServerPlayer player, ResidentEntity r, String offerId) {
        Optional<Context> co = context(player, r);
        if (co.isEmpty()) {
            return ResidentTradeView.State.OUT_OF_STOCK;
        }
        Context c = co.get();
        Optional<ResidentOffer> oo = c.visible.stream().filter(o -> o.id().equals(offerId)).findFirst();
        if (oo.isEmpty()) {
            return ResidentTradeView.State.OUT_OF_STOCK;
        }
        ResidentOffer o = oo.get();
        ResidentTradeView.Row row = row(player, r, c, o);
        if (row.state() != ResidentTradeView.State.OK) {
            return row.state();
        }
        Item it = item(o);
        if (o.sell()) {
            if (!o.imported() && c.state.stock.take(o.item(), o.count(), 0) < o.count()) {
                return ResidentTradeView.State.OUT_OF_STOCK;
            }
            remove(player.getInventory(), Items.EMERALD, row.price());
            give(player, it, o.count());
        } else {
            remove(player.getInventory(), it, o.count());
            c.state.stock.deposit(o.item(), o.count());
            give(player, Items.EMERALD, row.price());
        }
        r.addTraded(o.id(), c.today);
        if (o.villageCap() > 0) {
            c.state.addDaily(c.today, goodsKey(o), 1);
        }
        VillageEconomy.get(player.server).setDirty();
        return ResidentTradeView.State.OK;
    }
}
