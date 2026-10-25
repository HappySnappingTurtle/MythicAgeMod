package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.core.village.quest.SupplyRules;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC 任务服务（T21 设计文档 §3–§5、§9、§11）：发布、刷新状态、接取 / 交付 / 放弃、结算、委托页视图。
 * <b>任务状态只在这里改。</b>阶段 ① 只有缺料供给（{@link SupplyQuests}）。
 *
 * <p>去重：来源同时只有一个未结束任务；奖励每人每任务一张 {@code UNIQUE} 收据 {@code quest:<村庄>/<任务>}；
 * 点击用 {@code REQUEST} 收据 {@code quest_act:<requestId>}。结算是「事务写收据 → 发绿宝石 → 记贡献 → 标已结算」，同一刻完成；
 * 重启后看到有收据但没标已结算的，只补标记。贡献者不在线时等他进村或打开委托页再结算（§11 待领）。
 */
public final class QuestService {
    public static final String ACCEPT = "accept";
    public static final String DELIVER = "deliver";
    public static final String ABANDON = "abandon";
    /** 结束（都结算了 / 失败）的任务在任务表里保留几天供界面显示。 */
    static final int KEEP_DAYS = 3;
    /** 平时刷新任务状态的间隔（刻）：交付、捐赠、项目建成另有即时刷新。 */
    static final int REFRESH_TICKS = 100;

    private static final Map<UUID, Long> LAST_REFRESH = new ConcurrentHashMap<>();
    @Nullable
    private static volatile VillagesConfig.Quests testConfig;

    private QuestService() {
    }

    public static VillagesConfig.Quests config() {
        VillagesConfig.Quests t = testConfig;
        return t != null ? t : ModConfigs.villages().current().quests();
    }

    /** 只供 GameTest：覆盖配置（null 恢复）。 */
    public static void overrideConfigForTest(@Nullable VillagesConfig.Quests c) {
        testConfig = c;
    }

    static VillageQuests.Book book(MinecraftServer server, UUID village) {
        return VillageQuests.get(server).book(village);
    }

    // ───────────── 调度器挂钩 ─────────────

    /** 日开始（{@code VillageScheduler.dayStart} 末尾）：发新任务、过期、清旧。 */
    public static void dayStart(VillageContext ctx) {
        scan(ctx);
    }

    /** 每轮调度（节流 {@value #REFRESH_TICKS} 刻）：刷新任务状态、给在线的贡献者结算。 */
    public static void tick(VillageContext ctx) {
        Long last = LAST_REFRESH.get(ctx.village().id());
        if (last != null && ctx.now() - last < REFRESH_TICKS && ctx.now() >= last) {
            return;
        }
        LAST_REFRESH.put(ctx.village().id(), ctx.now());
        refresh(ctx);
    }

    static void scan(VillageContext ctx) {
        MinecraftServer server = ctx.level().getServer();
        VillageQuests.Book b = book(server, ctx.village().id());
        VillagesConfig.Quests cfg = config();
        long today = ctx.today();
        // 村庄没在运行的那些天不算进有效期（§3.4）
        if (b.lastScanDay >= 0 && today - b.lastScanDay > 1) {
            long gap = today - b.lastScanDay - 1;
            b.quests.stream().filter(q -> q.state.active() && q.deadline >= 0).forEach(q -> q.deadline += gap);
        }
        b.lastScanDay = today;
        refresh(ctx);
        if (cfg.enabled()) {
            List<QuestRules.Candidate> cands = new ArrayList<>(SupplyQuests.candidates(ctx, b, cfg));
            for (QuestRules.Candidate c : QuestRules.pick(b.quests, cands, cfg.maxOpenPerVillage())) {
                publish(ctx, b, c.source(), cfg);
            }
        }
        b.quests.removeIf(q -> QuestRules.purge(q, today, KEEP_DAYS));
        VillageQuests.get(server).setDirty();
    }

    static Optional<Quest> publish(VillageContext ctx, VillageQuests.Book b, String source, VillagesConfig.Quests cfg) {
        if (QuestRules.openSource(b.quests, source)) {
            return Optional.empty();
        }
        Optional<Quest> q = SupplyQuests.create(ctx, b.next, source, cfg);
        q.ifPresent(x -> {
            b.next++;
            b.quests.add(x);
            AffairsView.Line t = title(ctx, x);
            List<String> args = new ArrayList<>();
            args.add("@@" + t.key());
            args.addAll(t.args());
            ctx.state().log(ctx.today(), "mythic_age.village.log.quest_posted", args.toArray(String[]::new));
        });
        return q;
    }

    /** 刷新全村任务：开关、过期、来源是否还在、完成；然后给在线的贡献者结算。 */
    public static void refresh(VillageContext ctx) {
        MinecraftServer server = ctx.level().getServer();
        VillageQuests.Book b = book(server, ctx.village().id());
        VillagesConfig.Quests cfg = config();
        long today = ctx.today();
        for (Quest q : b.quests) {
            if (!q.state.active()) {
                continue;
            }
            if (!cfg.enabled()) {
                q.move(QuestState.PAUSED, today, "disabled");
                continue;
            }
            if (q.state == QuestState.PAUSED && "disabled".equals(q.reason)) {
                q.move(QuestState.OPEN, today, "");
            }
            if (q.state != QuestState.OPEN) {
                continue;
            }
            if (q.type.equals(SupplyQuests.TYPE)) {
                refreshSupply(ctx, q, today);
            }
        }
        settleAll(server, b);
    }

    private static void refreshSupply(VillageContext ctx, Quest q, long today) {
        // 村库新到的料先托管进项目（同施工每轮的备料），「还差多少」才是真实的
        if (q.source.startsWith(SupplyQuests.PROJECT)) {
            ctx.state().activeProject().filter(p -> p.state == BuildProject.State.STOCKING)
                    .ifPresent(p -> p.stockUp(ctx.state().stock, ctx.data().buildCosts()));
        }
        int rem = SupplyQuests.remaining(ctx, q);
        if (rem < 0 || SupplyQuests.professionGone(ctx, q)) {
            q.move(QuestState.INVALID, today, "gone");
            return;
        }
        if (!q.source.startsWith(SupplyQuests.SHORT)) {
            q.need = q.done + rem;
        }
        if (rem == 0) {
            q.move(q.filled ? QuestState.COMPLETE : QuestState.INVALID, today, q.filled ? "" : "satisfied");
            return;
        }
        if (QuestRules.expired(q, today)) {
            q.move(QuestState.EXPIRED, today, "expired");
        }
    }

    /** 立刻做一次日开始的任务扫描（开发命令、GameTest）。 */
    public static void scanNow(MinecraftServer server, UUID village) {
        context(server, village).ifPresent(QuestService::scan);
    }

    /** 村库被别处改了（捐赠）：当刻刷新这个村的任务。 */
    public static void refreshNow(MinecraftServer server, UUID village) {
        context(server, village).ifPresent(QuestService::refresh);
    }

    // ───────────── 玩家操作 ─────────────

    public static void handleOpen(QuestPayloads.Open payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p && nearKeeper(p, payload.village())) {
                open(p, payload.village());
            }
        });
    }

    public static void handleAct(QuestPayloads.Act payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                act(p, payload.village(), payload.quest(), payload.action(), payload.requestId(), true);
                open(p, payload.village());
            }
        });
    }

    /** 操作结果（GameTest 看）：语言键后缀与交付件数。 */
    public record Outcome(String result, int delivered) {
        static Outcome of(String r) {
            return new Outcome(r, 0);
        }

        public boolean ok() {
            return result.equals("ok");
        }
    }

    /**
     * 接取 / 交付 / 放弃（§5.1）。{@code checkReach}：离谷仓管事 {@value VillageAffairs#REACH} 格内才行（GameTest 可跳过）。
     * 结果提示在动作栏。
     */
    public static Outcome act(ServerPlayer player, UUID village, String questId, String action, String requestId, boolean checkReach) {
        Outcome out = doAct(player, village, questId, action, requestId, checkReach);
        if (!out.result().equals("duplicate")) {
            player.displayClientMessage(out.delivered() > 0
                    ? Component.translatable("mythic_age.quest.act.delivered", out.delivered())
                    : Component.translatable("mythic_age.quest.act." + out.result()), true);
        }
        return out;
    }

    private static Outcome doAct(ServerPlayer player, UUID village, String questId, String action, String requestId, boolean checkReach) {
        MinecraftServer server = player.server;
        if (!requestId.isEmpty() && MythData.transact(player, Transactions.Request.of("quest_act:" + requestId)).status()
                == Transactions.Status.DUPLICATE) {
            return Outcome.of("duplicate");
        }
        if (checkReach && !nearKeeper(player, village)) {
            return Outcome.of("far");
        }
        Optional<VillageContext> ctx = context(server, village);
        if (ctx.isEmpty()) {
            return Outcome.of("closed");
        }
        VillageQuests.Book b = book(server, village);
        refresh(ctx.get());
        Optional<Quest> qo = b.byId(questId);
        if (qo.isEmpty()) {
            return Outcome.of("closed");
        }
        Quest q = qo.get();
        VillagesConfig.Quests cfg = config();
        UUID me = player.getUUID();
        Outcome out = switch (action) {
            case ACCEPT -> QuestRules.canAccept(q, me, QuestRules.activeFor(b.quests, me), cfg.maxActivePerPlayer())
                    .map(r -> Outcome.of(r.name().toLowerCase(java.util.Locale.ROOT))).orElseGet(() -> {
                        q.accepted.add(me);
                        return Outcome.of("ok");
                    });
            case ABANDON -> q.accepted.remove(me) ? Outcome.of("abandoned") : Outcome.of("closed");
            case DELIVER -> deliver(ctx.get(), b, q, player, cfg);
            default -> Outcome.of("closed");
        };
        refresh(ctx.get());
        VillageQuests.get(server).setDirty();
        VillageEconomy.get(server).setDirty();
        return out;
    }

    /** 交付（§5.1）：从背包取、不多收、存进村库（项目当刻托管）、记贡献；补满就完成并结算。 */
    static Outcome deliver(VillageContext ctx, VillageQuests.Book b, Quest q, ServerPlayer player, VillagesConfig.Quests cfg) {
        if (!q.type.equals(SupplyQuests.TYPE) || q.state != QuestState.OPEN) {
            return Outcome.of("closed");
        }
        UUID me = player.getUUID();
        if (!q.accepted.contains(me)) {
            Optional<QuestRules.Refusal> r = QuestRules.canAccept(q, me, QuestRules.activeFor(b.quests, me), cfg.maxActivePerPlayer());
            if (r.isPresent()) {
                return Outcome.of(r.get().name().toLowerCase(java.util.Locale.ROOT));
            }
            q.accepted.add(me);
        }
        int rem = SupplyQuests.remaining(ctx, q);
        if (rem <= 0) {
            return Outcome.of("satisfied");
        }
        VillageEconomy.State s = ctx.state();
        int weight = ctx.data().quests().contribution().item();
        int units = 0;
        boolean anyHeld = false;
        for (String item : SupplyQuests.items(ctx, q)) {
            if (rem <= 0) {
                break;
            }
            int have = count(player, item);
            anyHeld |= have > 0;
            int n = SupplyRules.deliverable(SupplyQuests.acceptable(ctx, q, item, rem), have, s.stock.free());
            if (n <= 0) {
                continue;
            }
            int got = s.stock.deposit(item, n);
            if (got <= 0) {
                continue;
            }
            TradeService.removeItems(player, item, got);
            double value = SupplyQuests.unitValue(ctx, q, item);
            int progress = (int) Math.ceil(got * value - 1.0E-9);
            q.done += progress;
            q.credit(me, got * SupplyQuests.price(q, item), got * weight);
            units += got;
            // 按村库重算还差多少（项目先把新到的料托管进去；口粮按实际份数，半份的食物不会被取整算多）
            s.activeProject().filter(p -> p.state == BuildProject.State.STOCKING)
                    .ifPresent(p -> p.stockUp(s.stock, ctx.data().buildCosts()));
            rem = SupplyQuests.remaining(ctx, q);
        }
        if (units == 0) {
            return Outcome.of(!anyHeld ? "missing" : s.stock.free() == 0 ? "stock_full" : "satisfied");
        }
        s.seenBy.add(me);
        if (rem <= 0) {
            q.filled = true;
        }
        refresh(ctx);
        return new Outcome("ok", units);
    }

    /** 背包里这种物品（不带组件的）有几件。 */
    static int count(ServerPlayer player, String item) {
        int n = 0;
        for (ItemStack st : player.getInventory().items) {
            if (!st.isEmpty() && TradeService.key(st).equals(item)) {
                n += st.getCount();
            }
        }
        return n;
    }

    // ───────────── 结算 ─────────────

    static void settleAll(MinecraftServer server, VillageQuests.Book b) {
        for (Quest q : b.quests) {
            if (!q.state.ended()) {
                continue;
            }
            for (UUID p : q.participants()) {
                if (q.owes(p)) {
                    ServerPlayer sp = OnlinePlayers.find(server, p);
                    if (sp != null) {
                        settle(sp, b, q);
                    }
                }
            }
            if (q.allSettled()) {
                q.move(QuestState.SETTLED, q.endedDay, null);
            }
        }
    }

    /** 给一名贡献者结算一次（收据防重复；已有收据的只补标记）。 */
    static void settle(ServerPlayer player, VillageQuests.Book b, Quest q) {
        String receipt = receiptId(b.village, q);
        UUID me = player.getUUID();
        if (!MythData.get(player).receipts().contains(ReceiptBook.Scope.UNIQUE, receipt)) {
            if (MythData.transact(player, Transactions.Request.unique(receipt)).status() != Transactions.Status.COMMITTED) {
                return;
            }
            int emeralds = q.payout(me);
            for (int left = emeralds; left > 0; left -= 64) {
                TradeService.give(player, new ItemStack(Items.EMERALD, Math.min(64, left)));
            }
            int points = q.contrib.getOrDefault(me, 0);
            if (points > 0) {
                b.contribution.merge(me, points, Integer::sum);
            }
            player.sendSystemMessage(Component.translatable("mythic_age.quest.settled", titleComponent(player.server, b.village, q),
                    emeralds, points));
        }
        q.settled.add(me);
    }

    public static String receiptId(UUID village, Quest q) {
        return "quest:" + village + "/" + q.id();
    }

    // ───────────── 进村、委托页 ─────────────

    /** 进村（{@code VillageService} 的进村标题处）：补发待领的报酬；有没看过的新任务时动作栏提示一次（每人每村每日）。 */
    public static void onEnter(ServerPlayer player, VillageRegistry.Village v) {
        Optional<VillageQuests.Book> bo = VillageQuests.get(player.server).peek(v.id());
        if (bo.isEmpty()) {
            return;
        }
        VillageQuests.Book b = bo.get();
        settleAll(player.server, b);
        long today = io.github.happysnappingturtle.mythicage.guardian.MythDays.today(player.server);
        int seen = b.seen.getOrDefault(player.getUUID(), 0);
        long fresh = b.quests.stream().filter(q -> q.state == QuestState.OPEN && q.scope != Quest.Scope.EVENT && q.number > seen).count();
        if (fresh > 0 && config().enabled() && b.hinted.getOrDefault(player.getUUID(), -1L) != today) {
            b.hinted.put(player.getUUID(), today);
            player.displayClientMessage(Component.translatable("mythic_age.quest.hint.new", fresh), true);
        }
        VillageQuests.get(player.server).setDirty();
    }

    public static void open(ServerPlayer player, UUID village) {
        view(player, village).ifPresent(v -> PacketDistributor.sendToPlayer(player, new QuestPayloads.View(v)));
    }

    public static Optional<QuestView> view(ServerPlayer player, UUID village) {
        MinecraftServer server = player.server;
        Optional<VillageContext> ctx = context(server, village);
        if (ctx.isEmpty()) {
            return Optional.empty();
        }
        VillageQuests.Book b = book(server, village);
        refresh(ctx.get());
        VillagesConfig.Quests cfg = config();
        UUID me = player.getUUID();
        int active = QuestRules.activeFor(b.quests, me);
        long today = ctx.get().today();
        List<QuestView.Row> rows = new ArrayList<>();
        int seen = b.seen.getOrDefault(me, 0);
        for (Quest q : b.quests) {
            if (q.state == QuestState.SETTLED && !q.contributed(me)) {
                continue;
            }
            seen = Math.max(seen, q.number);
            String refusal = QuestRules.canAccept(q, me, active, cfg.maxActivePerPlayer())
                    .map(r -> r.name().toLowerCase(java.util.Locale.ROOT)).orElse("");
            String scope = q.scope == Quest.Scope.PERSONAL && !q.accepted.isEmpty() && !q.accepted.contains(me) ? "taken" : q.scope.id();
            List<String> rewards = new ArrayList<>();
            if (q.prices.values().stream().anyMatch(x -> x > 0)) {
                rewards.add("emerald");
            }
            rewards.add("contribution");
            AffairsView.Line reason = q.reason.isEmpty() || q.state == QuestState.OPEN ? AffairsView.Line.of("")
                    : AffairsView.Line.of("mythic_age.quest.reason." + q.reason);
            boolean canDeliver = q.type.equals(SupplyQuests.TYPE) && q.state == QuestState.OPEN
                    && (q.accepted.contains(me) || refusal.isEmpty());
            rows.add(new QuestView.Row(q.id(), q.type, title(ctx.get(), q), q.done, Math.max(q.need, q.done),
                    q.deadline < 0 || !q.state.active() ? -1 : (int) Math.max(0, q.deadline - today), scope, List.copyOf(rewards),
                    q.state.id(), reason, q.accepted.contains(me), refusal, canDeliver, q.contrib.getOrDefault(me, 0)));
        }
        b.seen.put(me, seen);
        VillageQuests.get(server).setDirty();
        VillageRegistry.Village v = ctx.get().village();
        AffairsView.Line title = v.patron().isEmpty() ? AffairsView.Line.of("mythic_age.village.title.plain")
                : AffairsView.Line.of("mythic_age.village.title", "@mythic_age.god." + v.patron());
        return Optional.of(new QuestView(village, title, List.copyOf(rows), active, cfg.maxActivePerPlayer(), b.contribution(me),
                cfg.enabled()));
    }

    static AffairsView.Line title(VillageContext ctx, Quest q) {
        return SupplyQuests.title(ctx, q);
    }

    static Component titleComponent(MinecraftServer server, UUID village, Quest q) {
        return context(server, village).map(c -> {
            AffairsView.Line l = title(c, q);
            return AffairsView.toComponent(l);
        }).orElse(Component.literal(q.id()));
    }

    // ───────────── 查询（对话、GameTest） ─────────────

    /** 本职缺料有没有对应的进行中的任务（居民对话的「委托」一级，§9.2）。 */
    public static boolean hasShortQuest(MinecraftServer server, UUID village, List<String> items, boolean food) {
        return VillageQuests.get(server).peek(village).map(b -> b.quests.stream().anyMatch(q -> q.state == QuestState.OPEN
                && (items.stream().anyMatch(i -> q.source.equals(SupplyQuests.SHORT + i)) || food && q.source.equals(SupplyQuests.FOOD))))
                .orElse(false);
    }

    public static List<Quest> quests(MinecraftServer server, UUID village) {
        return VillageQuests.get(server).peek(village).map(b -> List.copyOf(b.quests)).orElse(List.of());
    }

    public static int contribution(MinecraftServer server, UUID village, UUID player) {
        return VillageQuests.get(server).peek(village).map(b -> b.contribution(player)).orElse(0);
    }

    // ───────────── 工具 ─────────────

    static Optional<VillageContext> context(MinecraftServer server, UUID village) {
        Optional<VillageRegistry.Village> v = VillageRegistry.get(server).byId(village);
        if (v.isEmpty()) {
            return Optional.empty();
        }
        ServerLevel level = server.getLevel(v.get().bell().dimension());
        return level == null ? Optional.empty() : VillageScheduler.contextOf(level, village);
    }

    /** 离谷仓管事（名册里的 keeper）{@value VillageAffairs#REACH} 格内。 */
    public static boolean nearKeeper(ServerPlayer player, UUID village) {
        Optional<UUID> keeper = VillageEconomy.get(player.server).peek(village).flatMap(VillageEconomy.State::keeper)
                .map(VillageEconomy.Member::id);
        if (keeper.isEmpty()) {
            return false;
        }
        for (ResidentEntity r : VillageScheduler.loaded(village)) {
            if (r.getUUID().equals(keeper.get()) && r.distanceToSqr(player) <= VillageAffairs.REACH * VillageAffairs.REACH) {
                return true;
            }
        }
        return false;
    }

    static String itemKey(String id) {
        if (id.startsWith("#")) {
            return "mythic_age.village.cost_group." + id.substring(1);
        }
        return BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)).getDescriptionId();
    }

    /** 只供 GameTest：清掉节流记录。 */
    public static void clearForTest(UUID village) {
        LAST_REFRESH.remove(village);
    }
}
