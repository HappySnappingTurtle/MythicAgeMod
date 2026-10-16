package io.github.happysnappingturtle.mythicage.trade;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.guardian.GuardianRules;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxStep;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxSteps;
import io.github.happysnappingturtle.mythicage.core.trade.InventoryPlan;
import io.github.happysnappingturtle.mythicage.core.trade.PriceCalculator;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.core.trade.TradeRules;
import io.github.happysnappingturtle.mythicage.core.trade.TradeTable;
import io.github.happysnappingturtle.mythicage.guardian.GreekGuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.temple.AltarRegistry;
import io.github.happysnappingturtle.mythicage.village.PantheonLoader;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * 守护者与神坛的兑换（T9 设计文档 §5、§6）。服务端权威，按 12 号 §2 的顺序：
 * 验证 → 预留背包 → 提交事务（收据防重复）→ 发货 → 反馈。全部在服务端主线程的同一刻内完成。
 * <p>
 * 玩家打开界面时登记一个会话；会话每 10 刻校验一次（祭司是否还活着、是否在交互距离内），失效就通知客户端关闭。
 */
public final class TradeService {
    public static final Civilization CIV = Civilization.GREEK;
    private static final int CHECK_INTERVAL = 10;

    /** 一次打开的界面。guardianEntityId 在祭坛兑换时为 −1；altar 在守护者处为 null。 */
    record Session(TradeOffer.Venue venue, int guardianEntityId, @Nullable BlockPos altar, ResourceKey<Level> dim,
                   String villagePatron) {
    }

    /** 请求结果：原因、附加数字、成功时的产物名与数量。 */
    public record Outcome(TradeReason reason, long arg, ItemStack given) {
        static Outcome of(TradeReason reason) {
            return new Outcome(reason, 0, ItemStack.EMPTY);
        }

        public boolean ok() {
            return reason == TradeReason.OK;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    /** 恩宠折扣来源（T16 圣物接入）：(玩家, 商品) → 折扣列表。 */
    private static volatile BiFunction<ServerPlayer, TradeOffer, List<Double>> discounts = (p, o) -> List.of();

    private TradeService() {
    }

    public static void setDiscountProvider(BiFunction<ServerPlayer, TradeOffer, List<Double>> provider) {
        discounts = provider;
    }

    static int range() {
        return GuardianSpawner.interactRange();
    }

    // ───────────── 会话 ─────────────

    public static void openGuardian(ServerPlayer player, GreekGuardianEntity guardian) {
        String patron = GuardianSpawner.villageOf(guardian).map(VillageRegistry.Village::patron).orElse("");
        Session s = new Session(TradeOffer.Venue.GUARDIAN, guardian.getId(), null, guardian.level().dimension(), patron);
        SESSIONS.put(player.getUUID(), s);
        learnPatron(player, patron);
        guardian.playGesture(GreekGuardianEntity.Gesture.GREET);
        send(player, new TradePayloads.Open(view(player, s)));
    }

    /** 从神坛检视界面打开兑换页：神坛必须属于该玩家且在交互距离内。 */
    public static boolean openAltar(ServerPlayer player, BlockPos pos) {
        Session s = new Session(TradeOffer.Venue.ALTAR, -1, pos.immutable(), player.level().dimension(), "");
        if (!valid(player, s)) {
            return false;
        }
        SESSIONS.put(player.getUUID(), s);
        send(player, new TradePayloads.Open(view(player, s)));
        return true;
    }

    public static void close(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static boolean hasSession(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** 正在与该祭司交易的玩家中最近的一位；没有时为 null。 */
    @Nullable
    public static ServerPlayer nearestViewer(GreekGuardianEntity guardian) {
        if (!(guardian.level() instanceof ServerLevel level) || SESSIONS.isEmpty()) {
            return null;
        }
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (Map.Entry<UUID, Session> e : SESSIONS.entrySet()) {
            if (e.getValue().guardianEntityId() != guardian.getId()) {
                continue;
            }
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p != null && p.level() == level) {
                double d = p.distanceToSqr(guardian);
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
        }
        return best;
    }

    /** 祭司死亡或被移除：关闭所有对它打开的界面。 */
    public static void onGuardianGone(GreekGuardianEntity guardian) {
        if (!(guardian.level() instanceof ServerLevel level)) {
            return;
        }
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> e = it.next();
            if (e.getValue().guardianEntityId() == guardian.getId()) {
                it.remove();
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
                if (p != null) {
                    send(p, new TradePayloads.Close());
                }
            }
        }
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_INTERVAL != 0 || SESSIONS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> e = it.next();
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                it.remove();
            } else if (!valid(p, e.getValue())) {
                it.remove();
                send(p, new TradePayloads.Close());
            }
        }
    }

    /** 只供 GameTest：清掉某玩家（例如 FakePlayer）的会话。 */
    public static void clearForTest() {
        SESSIONS.clear();
    }

    static boolean valid(ServerPlayer player, Session s) {
        if (!player.isAlive() || player.level().dimension() != s.dim()) {
            return false;
        }
        double max = (double) range() * range();
        if (s.venue() == TradeOffer.Venue.GUARDIAN) {
            Entity e = player.level().getEntity(s.guardianEntityId());
            return e instanceof GreekGuardianEntity g && g.isAlive() && !g.isRemoved() && g.distanceToSqr(player) <= max;
        }
        BlockPos altar = s.altar();
        return altar != null && AltarRegistry.get(player.server).get(GlobalPos.of(s.dim(), altar))
                .map(entry -> entry.owner().equals(player.getUUID())).orElse(false)
                && player.distanceToSqr(altar.getCenter()) <= max;
    }

    /**
     * 结识村庄主神（T10a 设计文档 §4）：与村庄祭司交谈即记入；首次结识时提示该主神名下的次神候选。
     * 用 UNIQUE 收据保证提示只出现一次。
     */
    static void learnPatron(ServerPlayer player, String patron) {
        if (patron.isEmpty() || MythData.get(player).civ(CIV).knownPatrons().contains(patron)) {
            return;
        }
        Transactions.Result r = MythData.transact(player, Transactions.Request.unique(
                "known_patron:" + CIV.id() + ":" + patron, TxSteps.knowPatron(CIV, patron)));
        if (r.committed()) {
            var pantheon = PantheonLoader.get(CIV);
            net.minecraft.network.chat.MutableComponent gods = net.minecraft.network.chat.Component.empty();
            boolean first = true;
            for (var g : pantheon.underPatron(patron, Age.CLASSICAL)) {
                if (!first) {
                    gods.append("、");
                }
                gods.append(net.minecraft.network.chat.Component.translatable("mythic_age.god." + g.id()));
                first = false;
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("mythic_age.village.patron_known",
                    net.minecraft.network.chat.Component.translatable("mythic_age.god." + patron), gods));
        }
    }

    // ───────────── 界面数据 ─────────────

    static long priceOf(ServerPlayer player, TradeOffer offer) {
        return priceOf(player, offer, "");
    }

    static long priceOf(ServerPlayer player, TradeOffer offer, String villagePatron) {
        if (!offer.price().isFavor()) {
            return 0;
        }
        List<Double> all = new ArrayList<>(discounts.apply(player, offer));
        all.addAll(TradeRules.discounts(MythData.get(player), CIV, offer, villagePatron));
        return PriceCalculator.favor(offer.price().favor(), all);
    }

    static TradeView view(ServerPlayer player, Session s) {
        PlayerMythData data = MythData.get(player);
        long today = MythDays.today(player.server);
        TradeTable table = TradeTableLoader.table(CIV);
        List<TradeView.OfferView> offers = new ArrayList<>();
        InventoryPlan inv = plan(player);
        for (TradeOffer o : table.offersAt(s.venue())) {
            long price = priceOf(player, o, s.villagePatron());
            boolean affordable = o.price().isFavor() ? data.civ(CIV).favor() >= price
                    : o.price().items().stream().allMatch(a -> inv.count(a.item()) >= a.count());
            List<ItemStack> itemPrice = o.price().items().stream().map(a -> stack(a.item(), a.count())).toList();
            boolean godLocked = !o.minorGod().isEmpty() && !data.civ(CIV).worships(o.minorGod());
            offers.add(new TradeView.OfferView(o.id(), o.category(), stack(o.result().item(), o.result().count()), price,
                    itemPrice, o.minAge(), !data.civ(CIV).age().atLeast(o.minAge()) || godLocked,
                    TradeRules.remainingToday(data, CIV, o, today), affordable, godLocked ? o.minorGod() : ""));
        }
        List<String> patronGods = new ArrayList<>();
        if (!s.villagePatron().isEmpty()) {
            var pantheon = PantheonLoader.get(CIV);
            for (Age a : List.of(Age.CLASSICAL, Age.HEROIC, Age.MYTHIC)) {
                for (var g : pantheon.underPatron(s.villagePatron(), a)) {
                    patronGods.add(a.id() + ":" + g.id());
                }
            }
        }
        return new TradeView(s.venue(), s.guardianEntityId(), dialogue(player, s, today), data.civ(CIV).favor(),
                MythData.cap(player, CIV), data.civ(CIV).age(), offers, s.villagePatron(), patronGods);
    }

    private static TradeView.Dialogue dialogue(ServerPlayer player, Session s, long today) {
        if (s.venue() != TradeOffer.Venue.GUARDIAN) {
            return TradeView.Dialogue.NONE;
        }
        PlayerMythData data = MythData.get(player);
        if (data.civ(CIV).age() == Age.NONE) {
            return TradeView.Dialogue.FIRST;
        }
        boolean can = GuardianRules.canReissue(AltarRegistry.get(player.server).countOwned(player.getUUID()),
                hasAltarItem(player))
                && !data.receipts().contains(ReceiptBook.Scope.REQUEST, GuardianRules.reissueReceipt(CIV.id(), today));
        return can ? TradeView.Dialogue.REISSUE : TradeView.Dialogue.NONE;
    }

    // ───────────── 交易 ─────────────

    public static Outcome buy(ServerPlayer player, String offerId, long expectedPrice, String requestId) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || !valid(player, s)) {
            SESSIONS.remove(player.getUUID());
            return Outcome.of(TradeReason.NPC_GONE);
        }
        PlayerMythData data = MythData.get(player);
        String receipt = "trade:" + requestId;
        if (data.receipts().contains(ReceiptBook.Scope.REQUEST, receipt)) {
            return Outcome.of(TradeReason.DUPLICATE);
        }
        TradeOffer offer = TradeTableLoader.table(CIV).offer(offerId).orElse(null);
        if (offer == null) {
            return Outcome.of(TradeReason.UNKNOWN_OFFER);
        }
        long price = priceOf(player, offer, s.villagePatron());
        long today = MythDays.today(player.server);
        // 1 验证
        TradeReason reason = TradeRules.check(data, CIV, offer, s.venue(), price, expectedPrice, today);
        if (reason == TradeReason.PRICE_CHANGED) {
            return new Outcome(reason, price, ItemStack.EMPTY);
        }
        if (reason == TradeReason.NOT_ENOUGH_FAVOR) {
            return new Outcome(reason, price - data.civ(CIV).favor(), ItemStack.EMPTY);
        }
        if (reason != TradeReason.OK) {
            return Outcome.of(reason);
        }
        // 2 预留：先在快照上扣物品价，再看产物放不放得下
        InventoryPlan plan = plan(player);
        for (TradeOffer.ItemAmount a : offer.price().items()) {
            if (!plan.remove(a.item(), a.count())) {
                return Outcome.of(TradeReason.NOT_ENOUGH_ITEMS);
            }
        }
        ItemStack result = stack(offer.result().item(), offer.result().count());
        if (result.isEmpty() || !plan.insert(offer.result().item(), result.getCount(), result.getMaxStackSize())) {
            return Outcome.of(TradeReason.INVENTORY_FULL);
        }
        // 3 提交
        List<TxStep> steps = new ArrayList<>();
        steps.add(TxSteps.requireAge(CIV, offer.minAge()));
        if (offer.price().isFavor()) {
            steps.add(TxSteps.spendFavor(CIV, price));
        }
        steps.add(TxSteps.recordDaily(offer.key(CIV), today, offer.dailyLimit()));
        Transactions.Result r = MythData.transact(player, new Transactions.Request(receipt, ReceiptBook.Scope.REQUEST, steps));
        if (r.status() == Transactions.Status.DUPLICATE) {
            return Outcome.of(TradeReason.DUPLICATE);
        }
        if (!r.committed()) {
            MythicAge.LOGGER.warn("交易 {} 在预检通过后被事务拒绝：{}", offerId, r.reason());
            return Outcome.of(TradeReason.DAILY_LIMIT);
        }
        // 4 发货（同一刻内，背包已预留）
        for (TradeOffer.ItemAmount a : offer.price().items()) {
            removeItems(player, a.item(), a.count());
        }
        give(player, result.copy());
        return new Outcome(TradeReason.OK, 0, result);
    }

    // ───────────── 初见与补领 ─────────────

    public static Outcome blessing(ServerPlayer player, boolean reissue) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.venue() != TradeOffer.Venue.GUARDIAN || !valid(player, s)) {
            SESSIONS.remove(player.getUUID());
            return Outcome.of(TradeReason.NPC_GONE);
        }
        PlayerMythData data = MythData.get(player);
        ItemStack altar = new ItemStack(GreekTemple.GREEK_ALTAR.get());
        Transactions.Request request;
        if (!reissue) {
            String receipt = GuardianRules.blessingReceipt(CIV.id());
            if (data.receipts().contains(ReceiptBook.Scope.UNIQUE, receipt)) {
                return Outcome.of(TradeReason.DUPLICATE);
            }
            if (data.civ(CIV).age() != Age.NONE) {
                return Outcome.of(TradeReason.ALREADY_BLESSED);
            }
            request = Transactions.Request.unique(receipt, TxSteps.requireAgeExactly(CIV, Age.NONE),
                    TxSteps.setAge(CIV, Age.ARCHAIC));
        } else {
            if (data.civ(CIV).age() == Age.NONE) {
                return Outcome.of(TradeReason.NOT_BLESSED);
            }
            if (!GuardianRules.canReissue(AltarRegistry.get(player.server).countOwned(player.getUUID()), hasAltarItem(player))) {
                return Outcome.of(TradeReason.HAS_ALTAR);
            }
            String receipt = GuardianRules.reissueReceipt(CIV.id(), MythDays.today(player.server));
            if (data.receipts().contains(ReceiptBook.Scope.REQUEST, receipt)) {
                return Outcome.of(TradeReason.REISSUED_TODAY);
            }
            request = Transactions.Request.of(receipt, TxSteps.requireAge(CIV, Age.ARCHAIC));
        }
        if (!plan(player).insert(key(altar), 1, altar.getMaxStackSize())) {
            return Outcome.of(TradeReason.INVENTORY_FULL);
        }
        Transactions.Result r = MythData.transact(player, request);
        if (r.status() == Transactions.Status.DUPLICATE) {
            return Outcome.of(TradeReason.DUPLICATE);
        }
        if (!r.committed()) {
            return Outcome.of(reissue ? TradeReason.NOT_BLESSED : TradeReason.ALREADY_BLESSED);
        }
        give(player, altar.copy());
        if (!reissue && player.level().getEntity(s.guardianEntityId()) instanceof GreekGuardianEntity g) {
            g.playGesture(GreekGuardianEntity.Gesture.ANNOUNCE);
        }
        return new Outcome(TradeReason.OK, 0, altar);
    }

    // ───────────── 网络入口 ─────────────

    public static void handleBuy(TradePayloads.Buy p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            respond(player, p.requestId(), buy(player, p.offerId(), p.expectedPrice(), p.requestId()));
        }
    }

    public static void handleBlessing(TradePayloads.Blessing p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            respond(player, p.requestId(), blessing(player, p.reissue()));
        }
    }

    public static void handleClosed(TradePayloads.Closed p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            close(player);
        }
    }

    public static void handleOpenAltar(TradePayloads.OpenAltar p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            openAltar(player, p.pos());
        }
    }

    private static void respond(ServerPlayer player, String requestId, Outcome o) {
        Session s = SESSIONS.get(player.getUUID());
        if (o.reason() == TradeReason.NPC_GONE || s == null) {
            send(player, new TradePayloads.Close());
            return;
        }
        String name = o.given().isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(o.given().getItem()).toString();
        send(player, new TradePayloads.Result(requestId, o.reason(), o.arg(), name, o.given().getCount(), view(player, s)));
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    // ───────────── 背包 ─────────────

    /** 物品价与产物都是不带组件的普通物品；背包里带组件的同种物品不参与扣除与合堆。 */
    static String key(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        return s.getComponentsPatch().isEmpty() ? id : id + "#" + s.getComponentsPatch().hashCode();
    }

    static InventoryPlan plan(ServerPlayer player) {
        List<InventoryPlan.Slot> slots = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) {
            slots.add(s.isEmpty() ? InventoryPlan.Slot.empty() : new InventoryPlan.Slot(key(s), s.getCount(), s.getMaxStackSize()));
        }
        return new InventoryPlan(slots);
    }

    private static boolean hasAltarItem(ServerPlayer player) {
        return player.getInventory().contains(new ItemStack(GreekTemple.GREEK_ALTAR.get()));
    }

    private static void removeItems(ServerPlayer player, String itemId, int amount) {
        List<ItemStack> items = player.getInventory().items;
        int left = amount;
        for (int i = items.size() - 1; i >= 0 && left > 0; i--) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && key(s).equals(itemId)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        player.getInventory().setChanged();
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) || !stack.isEmpty()) {
            MythicAge.LOGGER.warn("预留后仍放不下 {}，掉落在 {} 脚下", stack, player.getScoreboardName());
            player.drop(stack, false);
        }
        player.getInventory().setChanged();
    }

    static ItemStack stack(String itemId, int count) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        Item item = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }
}
