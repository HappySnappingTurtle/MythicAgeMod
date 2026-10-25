package io.github.happysnappingturtle.mythicage.trade;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.faith.FaithProfile;
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
import io.github.happysnappingturtle.mythicage.faith.FaithProfiles;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianGesture;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.statue.PatronStatues;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * 祭司与主神像的兑换（T9 设计文档 §5、§6；T10b：按文明；T11a：地点为村庄祭司或玩家自己的主神像）。服务端权威，按 12 号 §2 的顺序：
 * 验证 → 预留背包 → 提交事务（收据防重复）→ 发货 → 反馈。全部在服务端主线程的同一刻内完成。
 * <p>
 * 玩家打开界面时登记一个会话；会话每 10 刻校验一次（祭司是否还活着、是否在交互距离内），失效就通知客户端关闭。
 * 会话的文明来自祭司或神庙；通用代码里不写任何文明的名字，与文明有关的文本与赠礼来自文明档案。
 */
public final class TradeService {
    private static final int CHECK_INTERVAL = 10;

    /**
     * 一次打开的界面。
     *
     * @param guardianEntityId 祭司实体 id；在主神像处为 −1
     * @param statue           主神像核心坐标；在祭司处为 null
     * @param villageId        祭司所属村庄；在主神像处或无村庄的祭司为 null
     * @param patron           本村主神 / 此像主神
     */
    public record Session(Civilization civ, TradeOffer.Venue venue, int guardianEntityId, @Nullable BlockPos statue,
                          ResourceKey<Level> dim, @Nullable UUID villageId, String patron) {
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

    /** 与祭司交谈；该文明未开放（没有文明档案）时不打开。 */
    public static boolean openGuardian(ServerPlayer player, GuardianEntity guardian) {
        if (!FaithProfiles.open(guardian.civ())) {
            return false;
        }
        Optional<VillageRegistry.Village> village = GuardianSpawner.villageOf(guardian);
        String patron = village.map(VillageRegistry.Village::patron).orElse("");
        // 神谕日在神谕所坐诊的祭司：只开神谕兑换（T19 §1.10 J6）
        TradeOffer.Venue venue = io.github.happysnappingturtle.mythicage.village.life.OracleService.seated(guardian)
                ? TradeOffer.Venue.ORACLE : TradeOffer.Venue.GUARDIAN;
        Session s = new Session(guardian.civ(), venue, guardian.getId(), null,
                guardian.level().dimension(), village.map(VillageRegistry.Village::id).orElse(null), patron);
        SESSIONS.put(player.getUUID(), s);
        learnPatron(player, guardian.civ(), patron);
        guardian.playGesture(GuardianGesture.GREET);
        send(player, new TradePayloads.Open(view(player, s)));
        return true;
    }

    /** 在自己的主神像前兑换（T11a 修订 1）：主神像必须属于该玩家、完整且在交互距离内。 */
    public static boolean openStatue(ServerPlayer player, BlockPos core) {
        Optional<PatronStatues.Info> info = PatronStatues.info(player.serverLevel(), core);
        if (info.isEmpty() || !FaithProfiles.open(info.get().civ())) {
            return false;
        }
        Session s = new Session(info.get().civ(), TradeOffer.Venue.STATUE, -1, core.immutable(),
                player.level().dimension(), null, info.get().patron());
        if (!valid(player, s)) {
            return false;
        }
        SESSIONS.put(player.getUUID(), s);
        send(player, new TradePayloads.Open(view(player, s)));
        return true;
    }

    /** 当前会话（仪式页用）；没有时为空。 */
    public static Optional<Session> session(ServerPlayer player) {
        return Optional.ofNullable(SESSIONS.get(player.getUUID()));
    }

    /** 会话所在的村庄；在主神像处、无村庄的祭司为空。 */
    public static Optional<VillageRegistry.Village> village(ServerPlayer player, Session s) {
        return s.villageId() == null ? Optional.empty() : VillageRegistry.get(player.server).byId(s.villageId());
    }

    /** 兑换上下文：每次重新取村庄阶段与次神祠（可能刚被别人推进过）。 */
    public static TradeRules.VenueContext context(ServerPlayer player, Session s) {
        if (s.venue() == TradeOffer.Venue.STATUE) {
            Set<String> gods = new java.util.LinkedHashSet<>();
            io.github.happysnappingturtle.mythicage.village.PantheonLoader.get(s.civ()).minorGods().stream()
                    .filter(g -> g.patrons().contains(s.patron())).forEach(g -> gods.add(g.id()));
            return TradeRules.VenueContext.statue(s.patron(), gods);
        }
        Optional<VillageRegistry.Village> v = village(player, s);
        if (s.venue() == TradeOffer.Venue.ORACLE) {
            return TradeRules.VenueContext.oracle(v.map(VillageRegistry.Village::stage).orElse(Age.ARCHAIC), s.patron(),
                    v.map(VillageRegistry.Village::shrineGods).orElse(Set.of()));
        }
        // 不属于任何村庄的祭司（刷怪蛋、命令生成）：按远古阶段出售
        return TradeRules.VenueContext.guardian(v.map(VillageRegistry.Village::stage).orElse(Age.ARCHAIC), s.patron(),
                v.map(VillageRegistry.Village::shrineGods).orElse(Set.of()));
    }

    public static void close(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static boolean hasSession(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** 正在与该祭司交易的玩家中最近的一位；没有时为 null。 */
    @Nullable
    public static ServerPlayer nearestViewer(GuardianEntity guardian) {
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
    public static void onGuardianGone(GuardianEntity guardian) {
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

    /** 只供 GameTest：该玩家当前会话的界面数据；没有会话时为空。 */
    public static Optional<TradeView> viewForTest(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        return s == null ? Optional.empty() : Optional.of(view(player, s));
    }

    /** 只供 GameTest：清掉某玩家（例如 FakePlayer）的会话。 */
    public static void clearForTest() {
        SESSIONS.clear();
    }

    public static boolean valid(ServerPlayer player, Session s) {
        if (!player.isAlive() || player.level().dimension() != s.dim()) {
            return false;
        }
        double max = (double) range() * range();
        if (s.venue() == TradeOffer.Venue.GUARDIAN || s.venue() == TradeOffer.Venue.ORACLE) {
            Entity e = player.level().getEntity(s.guardianEntityId());
            return e instanceof GuardianEntity g && g.civ() == s.civ() && g.isAlive() && !g.isRemoved()
                    && g.distanceToSqr(player) <= max
                    && (s.venue() != TradeOffer.Venue.ORACLE || io.github.happysnappingturtle.mythicage.village.life.OracleService.seated(g));
        }
        BlockPos statue = s.statue();
        return statue != null && PatronStatues.info(player.serverLevel(), statue)
                .map(i -> i.owner().equals(player.getUUID()) && i.civ() == s.civ() && i.intact()).orElse(false)
                && player.distanceToSqr(statue.getCenter()) <= max;
    }

    /**
     * 结识村庄主神（T10a 设计文档 §4；18 号 §6：只作图鉴记录，不再决定晋升候选）：与村庄祭司交谈即记入。
     * 用 UNIQUE 收据保证提示只出现一次。
     */
    static void learnPatron(ServerPlayer player, Civilization civ, String patron) {
        if (patron.isEmpty() || MythData.get(player).civ(civ).knownPatrons().contains(patron)) {
            return;
        }
        Transactions.Result r = MythData.transact(player, Transactions.Request.unique(
                "known_patron:" + civ.id() + ":" + patron, TxSteps.knowPatron(civ, patron)));
        if (r.committed()) {
            player.sendSystemMessage(Component.translatable("mythic_age.village.patron_known",
                    Component.translatable("mythic_age.god." + patron)));
        }
    }

    // ───────────── 界面数据 ─────────────

    static long priceOf(ServerPlayer player, Civilization civ, TradeOffer offer, TradeRules.VenueContext ctx) {
        if (!offer.price().isFavor()) {
            return 0;
        }
        return PriceCalculator.favor(offer.price().favor(), allDiscounts(player, civ, offer, ctx));
    }

    /** 物品价折后的数量（T11b D1：向下取整，至少 1 个）。 */
    static List<TradeOffer.ItemAmount> itemPriceOf(ServerPlayer player, Civilization civ, TradeOffer offer,
                                                   TradeRules.VenueContext ctx) {
        if (offer.price().isFavor()) {
            return List.of();
        }
        List<Double> all = allDiscounts(player, civ, offer, ctx);
        return offer.price().items().stream()
                .map(a -> new TradeOffer.ItemAmount(a.item(), PriceCalculator.items(a.count(), all))).toList();
    }

    private static List<Double> allDiscounts(ServerPlayer player, Civilization civ, TradeOffer offer,
                                             TradeRules.VenueContext ctx) {
        List<Double> all = new ArrayList<>(discounts.apply(player, offer));
        all.addAll(TradeRules.discounts(MythData.get(player), civ, offer, ctx));
        return all;
    }

    /** 列在界面上但不能买的原因（时代、次神、阶段……）；可买时为 OK。 */
    private static final Set<TradeReason> LOCKS = Set.of(TradeReason.AGE_LOCKED, TradeReason.MINOR_GOD_LOCKED,
            TradeReason.STAGE_LOCKED, TradeReason.NO_SHRINE, TradeReason.WRONG_PATRON);
    /** 主神像道具：结果按本村主神带上数据组件（T11a §5.2）。 */
    static final String STATUE_ITEM = "mythic_age:patron_statue";

    /** 主神像的额外条件：已被本村主神认可到古典、还没有这位主神的像。 */
    static TradeReason statueLock(ServerPlayer player, Civilization civ, String patron) {
        if (patron.isEmpty() || !MythData.get(player).civ(civ).approval(patron).atLeast(Age.CLASSICAL)) {
            return TradeReason.NEED_APPROVAL;
        }
        return PatronStatues.owns(player, civ, patron) ? TradeReason.HAS_STATUE : TradeReason.OK;
    }

    static ItemStack resultOf(TradeOffer o, Civilization civ, Session s) {
        if (o.id().equals(VILLAGE_GUIDE)) {
            return io.github.happysnappingturtle.mythicage.village.VillageGuide.displayStack(civ);
        }
        if (o.id().startsWith(ORACLE_PREFIX)) {
            return io.github.happysnappingturtle.mythicage.village.life.OracleService.tablet(o.id());
        }
        return o.result().item().equals(STATUE_ITEM) ? PatronStatues.stack(civ, s.patron())
                : stack(o.result().item(), o.result().count());
    }

    static TradeView view(ServerPlayer player, Session s) {
        Civilization civ = s.civ();
        PlayerMythData data = MythData.get(player);
        long today = MythDays.today(player.server);
        TradeTable table = TradeTableLoader.table(civ);
        TradeRules.VenueContext ctx = context(player, s);
        List<TradeView.OfferView> offers = new ArrayList<>();
        InventoryPlan inv = plan(player);
        for (TradeOffer o : table.offers()) {
            if (!TradeRules.visible(o, ctx)) {
                continue;
            }
            long price = priceOf(player, civ, o, ctx);
            List<TradeOffer.ItemAmount> items = itemPriceOf(player, civ, o, ctx);
            boolean affordable = o.price().isFavor() ? data.civ(civ).favor() >= price
                    : items.stream().allMatch(a -> inv.count(a.item()) >= a.count());
            List<ItemStack> itemPrice = items.stream().map(a -> stack(a.item(), a.count())).toList();
            TradeReason r = TradeRules.check(data, civ, o, ctx, price, price, today);
            TradeReason lock = LOCKS.contains(r) ? r : TradeReason.OK;
            if (lock == TradeReason.OK && o.result().item().equals(STATUE_ITEM)) {
                lock = statueLock(player, civ, s.patron());
            }
            String lockArg = switch (lock) {
                case NEED_APPROVAL, HAS_STATUE -> s.patron();
                case MINOR_GOD_LOCKED, NO_SHRINE -> o.minorGod();
                case AGE_LOCKED, STAGE_LOCKED -> o.minAge().id();
                case WRONG_PATRON -> o.patron().isEmpty() ? o.minorGod() : o.patron();
                default -> "";
            };
            offers.add(new TradeView.OfferView(o.id(), o.category(), resultOf(o, civ, s), price,
                    itemPrice, o.minAge(), lock, lockArg, TradeRules.remainingToday(data, civ, o, today), affordable));
        }
        FaithProfile profile = FaithProfiles.get(civ).orElse(FaithProfile.defaults(civ));
        Optional<VillageRegistry.Village> village = village(player, s);
        io.github.happysnappingturtle.mythicage.rite.RitesView rites = s.venue() == TradeOffer.Venue.GUARDIAN
                ? village.map(v -> io.github.happysnappingturtle.mythicage.rite.RiteService.view(player, civ, v)).orElse(null)
                : null;
        return new TradeView(civ, new TradeView.Labels(profile.favor(), profile.shrine()), s.venue(),
                s.guardianEntityId(), dialogue(player, s, today), data.civ(civ).favor(), MythData.cap(player, civ),
                data.civ(civ).age(), offers, s.patron(), ctx.villageStage(), Optional.ofNullable(rites),
                s.venue() == TradeOffer.Venue.GUARDIAN ? villageNotes(village) : List.of());
    }

    /** 祭司对话页的本村提示。 */
    private static List<Component> villageNotes(Optional<VillageRegistry.Village> village) {
        List<Component> out = new ArrayList<>();
        village.filter(v -> io.github.happysnappingturtle.mythicage.village.VillagePopulation.needsHousing(v.id()))
                .ifPresent(v -> out.add(Component.translatable("mythic_age.village.needs_housing")));
        return out;
    }

    private static TradeView.Dialogue dialogue(ServerPlayer player, Session s, long today) {
        if (s.venue() != TradeOffer.Venue.GUARDIAN) {
            return TradeView.Dialogue.NONE;
        }
        PlayerMythData data = MythData.get(player);
        if (data.civ(s.civ()).age() == Age.NONE) {
            return TradeView.Dialogue.FIRST;
        }
        Optional<FaithProfile.Reissue> reissue = FaithProfiles.get(s.civ()).flatMap(FaithProfile::reissue);
        boolean can = reissue.isPresent()
                && GuardianRules.canReissue(ShrineRegistry.get(player.server).countOwned(player.getUUID(),
                reissue.get().shrineKind()), hasItem(player, reissue.get().item()))
                && !data.receipts().contains(ReceiptBook.Scope.REQUEST, GuardianRules.reissueReceipt(s.civ().id(), today));
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
        Civilization civ = s.civ();
        TradeOffer offer = TradeTableLoader.table(civ).offer(offerId).orElse(null);
        if (offer == null) {
            return Outcome.of(TradeReason.UNKNOWN_OFFER);
        }
        TradeRules.VenueContext ctx = context(player, s);
        long price = priceOf(player, civ, offer, ctx);
        long today = MythDays.today(player.server);
        // 1 验证
        TradeReason reason = TradeRules.check(data, civ, offer, ctx, price, expectedPrice, today);
        if (reason == TradeReason.PRICE_CHANGED) {
            return new Outcome(reason, price, ItemStack.EMPTY);
        }
        if (reason == TradeReason.NOT_ENOUGH_FAVOR) {
            return new Outcome(reason, price - data.civ(civ).favor(), ItemStack.EMPTY);
        }
        if (reason == TradeReason.OK && offer.result().item().equals(STATUE_ITEM)) {
            reason = statueLock(player, civ, s.patron());
        }
        if (reason != TradeReason.OK) {
            return Outcome.of(reason);
        }
        // 2 预留：先在快照上扣物品价，再看产物放不放得下
        InventoryPlan plan = plan(player);
        List<TradeOffer.ItemAmount> items = itemPriceOf(player, civ, offer, ctx);
        for (TradeOffer.ItemAmount a : items) {
            if (!plan.remove(a.item(), a.count())) {
                return Outcome.of(TradeReason.NOT_ENOUGH_ITEMS);
            }
        }
        if (offer.id().equals(VILLAGE_GUIDE)) {
            return beginGuide(player, civ, offer, items, plan, requestId);
        }
        ItemStack result = offer.id().startsWith(ORACLE_PREFIX)
                ? io.github.happysnappingturtle.mythicage.village.life.OracleService.answer(player, offer.id())
                : resultOf(offer, civ, s);
        if (result.isEmpty() || !plan.insert(offer.result().item(), result.getCount(), result.getMaxStackSize())) {
            return Outcome.of(TradeReason.INVENTORY_FULL);
        }
        // 3 提交
        List<TxStep> steps = new ArrayList<>();
        if (offer.category() == TradeOffer.Category.GOD_POWER || offer.category() == TradeOffer.Category.SUMMON) {
            steps.add(TxSteps.requireAge(civ, offer.minAge()));
        }
        if (offer.price().isFavor()) {
            steps.add(TxSteps.spendFavor(civ, price));
        }
        steps.add(TxSteps.recordDaily(offer.key(civ), today, offer.dailyLimit()));
        Transactions.Result r = MythData.transact(player, new Transactions.Request(receipt, ReceiptBook.Scope.REQUEST, steps));
        if (r.status() == Transactions.Status.DUPLICATE) {
            return Outcome.of(TradeReason.DUPLICATE);
        }
        if (!r.committed()) {
            MythicAge.LOGGER.warn("交易 {} 在预检通过后被事务拒绝：{}", offerId, r.reason());
            return Outcome.of(TradeReason.DAILY_LIMIT);
        }
        // 4 发货（同一刻内，背包已预留）
        for (TradeOffer.ItemAmount a : items) {
            removeItems(player, a.item(), a.count());
        }
        give(player, result.copy());
        return new Outcome(TradeReason.OK, 0, result);
    }

    // ───────────── 村庄指引（T13b 设计文档 §7） ─────────────

    /** 神谕的商品 id 前缀：结果是写着答案的神谕铅板（T19 §1.10 J6）。 */
    public static final String ORACLE_PREFIX = "oracle_";

    /** 村庄指引的商品 id：找到村庄才成交，搜索在后台进行。 */
    public static final String VILLAGE_GUIDE = "village_guide";
    private static final Map<UUID, Outcome> LAST_GUIDE = new java.util.concurrent.ConcurrentHashMap<>();

    private static Outcome beginGuide(ServerPlayer player, Civilization civ, TradeOffer offer, List<TradeOffer.ItemAmount> items,
                                      InventoryPlan plan, String requestId) {
        if (!plan.insert("minecraft:filled_map", 1, 1)) {
            return Outcome.of(TradeReason.INVENTORY_FULL);
        }
        BlockPos from = player.blockPosition();
        long today = MythDays.today(player.server);
        LAST_GUIDE.remove(player.getUUID());
        io.github.happysnappingturtle.mythicage.village.VillageGuide.begin(player, civ,
                found -> finishGuide(player, civ, offer, items, requestId, from, today, found));
        return Outcome.of(TradeReason.SEARCHING);
    }

    /** 主线程：找到就再验一次、扣绿宝石、给地图；找不到不扣钱。会话还开着就推送结果，否则发聊天消息。 */
    private static void finishGuide(ServerPlayer player, Civilization civ, TradeOffer offer, List<TradeOffer.ItemAmount> items,
                                    String requestId, BlockPos from, long today,
                                    Optional<io.github.happysnappingturtle.mythicage.village.VillageGuide.Found> found) {
        if (player.hasDisconnected()) {
            return;
        }
        Outcome o = found.isEmpty() ? Outcome.of(TradeReason.NO_VILLAGE_NEARBY)
                : settleGuide(player, civ, offer, items, requestId, from, today, found.get());
        LAST_GUIDE.put(player.getUUID(), o);
        Session s = SESSIONS.get(player.getUUID());
        if (s != null && s.civ() == civ) {
            respond(player, requestId, o);
        } else if (o.ok()) {
            player.sendSystemMessage(Component.translatable("mythic_age.village_guide.delivered", o.given().getHoverName()));
        } else {
            player.sendSystemMessage(Component.translatable(o.reason().langKey(), o.arg(), ""));
        }
    }

    private static Outcome settleGuide(ServerPlayer player, Civilization civ, TradeOffer offer, List<TradeOffer.ItemAmount> items,
                                       String requestId, BlockPos from, long today,
                                       io.github.happysnappingturtle.mythicage.village.VillageGuide.Found f) {
        InventoryPlan plan = plan(player);
        for (TradeOffer.ItemAmount a : items) {
            if (!plan.remove(a.item(), a.count())) {
                return Outcome.of(TradeReason.NOT_ENOUGH_ITEMS);
            }
        }
        if (!plan.insert("minecraft:filled_map", 1, 1)) {
            return Outcome.of(TradeReason.INVENTORY_FULL);
        }
        Transactions.Result r = MythData.transact(player, new Transactions.Request("trade:" + requestId,
                ReceiptBook.Scope.REQUEST, List.of(TxSteps.recordDaily(offer.key(civ), today, offer.dailyLimit()))));
        if (r.status() == Transactions.Status.DUPLICATE) {
            return Outcome.of(TradeReason.DUPLICATE);
        }
        if (!r.committed()) {
            return Outcome.of(TradeReason.DAILY_LIMIT);
        }
        for (TradeOffer.ItemAmount a : items) {
            removeItems(player, a.item(), a.count());
        }
        ItemStack map = io.github.happysnappingturtle.mythicage.village.VillageGuide.map(player.serverLevel(), civ, from, f);
        give(player, map.copy());
        return new Outcome(TradeReason.OK, 0, map);
    }

    /** 只供 GameTest：最近一次村庄指引搜索的结果。 */
    public static Optional<Outcome> lastGuideForTest(ServerPlayer player) {
        return Optional.ofNullable(LAST_GUIDE.get(player.getUUID()));
    }

    // ───────────── 初见与补领 ─────────────

    /**
     * 初见祝福（进入远古时代并领取文明档案里的赠礼）或补领神庙。赠礼一次预留全部，放不下则整单失败。
     */
    public static Outcome blessing(ServerPlayer player, boolean reissue) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.venue() != TradeOffer.Venue.GUARDIAN || !valid(player, s)) {
            SESSIONS.remove(player.getUUID());
            return Outcome.of(TradeReason.NPC_GONE);
        }
        Civilization civ = s.civ();
        FaithProfile profile = FaithProfiles.get(civ).orElse(null);
        if (profile == null) {
            return Outcome.of(TradeReason.NPC_GONE);
        }
        PlayerMythData data = MythData.get(player);
        List<ItemStack> gifts = new ArrayList<>();
        Transactions.Request request;
        if (!reissue) {
            String receipt = GuardianRules.blessingReceipt(civ.id());
            if (data.receipts().contains(ReceiptBook.Scope.UNIQUE, receipt)) {
                return Outcome.of(TradeReason.DUPLICATE);
            }
            if (data.civ(civ).age() != Age.NONE) {
                return Outcome.of(TradeReason.ALREADY_BLESSED);
            }
            profile.firstMeetGifts().forEach(g -> gifts.add(stack(g.item(), g.count())));
            request = Transactions.Request.unique(receipt, TxSteps.requireAgeExactly(civ, Age.NONE),
                    TxSteps.setAge(civ, Age.ARCHAIC));
        } else {
            if (data.civ(civ).age() == Age.NONE) {
                return Outcome.of(TradeReason.NOT_BLESSED);
            }
            FaithProfile.Reissue r = profile.reissue().orElse(null);
            if (r == null || !GuardianRules.canReissue(ShrineRegistry.get(player.server).countOwned(player.getUUID(),
                    r.shrineKind()), hasItem(player, r.item()))) {
                return Outcome.of(TradeReason.HAS_SHRINE);
            }
            String receipt = GuardianRules.reissueReceipt(civ.id(), MythDays.today(player.server));
            if (data.receipts().contains(ReceiptBook.Scope.REQUEST, receipt)) {
                return Outcome.of(TradeReason.REISSUED_TODAY);
            }
            gifts.add(stack(r.item(), 1));
            request = Transactions.Request.of(receipt, TxSteps.requireAge(civ, Age.ARCHAIC));
        }
        InventoryPlan plan = plan(player);
        for (ItemStack g : gifts) {
            if (g.isEmpty() || !plan.insert(key(g), g.getCount(), g.getMaxStackSize())) {
                return Outcome.of(TradeReason.INVENTORY_FULL);
            }
        }
        Transactions.Result r = MythData.transact(player, request);
        if (r.status() == Transactions.Status.DUPLICATE) {
            return Outcome.of(TradeReason.DUPLICATE);
        }
        if (!r.committed()) {
            return Outcome.of(reissue ? TradeReason.NOT_BLESSED : TradeReason.ALREADY_BLESSED);
        }
        gifts.forEach(g -> give(player, g.copy()));
        if (!reissue && player.level().getEntity(s.guardianEntityId()) instanceof GuardianEntity g) {
            g.playGesture(GuardianGesture.ANNOUNCE);
        }
        if (!reissue) {
            io.github.happysnappingturtle.mythicage.advancement.MythEvents.fire(player, io.github.happysnappingturtle.mythicage.advancement.MythEvents.BLESSED, civ);
            io.github.happysnappingturtle.mythicage.advancement.MythEvents.fire(player, io.github.happysnappingturtle.mythicage.advancement.MythEvents.AGE_REACHED, civ, Age.ARCHAIC.ordinal());
        }
        return new Outcome(TradeReason.OK, 0, gifts.isEmpty() ? ItemStack.EMPTY : gifts.getFirst());
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

    /** 某会话的界面数据（仪式结果回发用）。 */
    public static TradeView viewOf(ServerPlayer player, Session s) {
        return view(player, s);
    }

    /** 仪式等改变了界面数据后刷新（不改会话）。 */
    public static void refresh(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        if (s != null) {
            send(player, new TradePayloads.Open(view(player, s)));
        }
    }

    private static void respond(ServerPlayer player, String requestId, Outcome o) {
        Session s = SESSIONS.get(player.getUUID());
        if (o.reason() == TradeReason.NPC_GONE || s == null) {
            send(player, new TradePayloads.Close());
            return;
        }
        send(player, new TradePayloads.Result(requestId, o.reason(), o.arg(), o.given().copy(), view(player, s)));
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && !(player instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    // ───────────── 背包 ─────────────

    /** 物品价与产物都是不带组件的普通物品；背包里带组件的同种物品不参与扣除与合堆。 */
    public static String key(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        return s.getComponentsPatch().isEmpty() ? id : id + "#" + s.getComponentsPatch().hashCode();
    }

    public static InventoryPlan plan(ServerPlayer player) {
        List<InventoryPlan.Slot> slots = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) {
            slots.add(s.isEmpty() ? InventoryPlan.Slot.empty() : new InventoryPlan.Slot(key(s), s.getCount(), s.getMaxStackSize()));
        }
        return new InventoryPlan(slots);
    }

    private static boolean hasItem(ServerPlayer player, String itemId) {
        ItemStack probe = stack(itemId, 1);
        return !probe.isEmpty() && player.getInventory().contains(probe);
    }

    public static void removeItems(ServerPlayer player, String itemId, int amount) {
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

    public static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) || !stack.isEmpty()) {
            MythicAge.LOGGER.warn("预留后仍放不下 {}，掉落在 {} 脚下", stack, player.getScoreboardName());
            player.drop(stack, false);
        }
        player.getInventory().setChanged();
    }

    public static ItemStack stack(String itemId, int count) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        Item item = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }
}
