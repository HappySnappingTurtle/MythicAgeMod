package io.github.happysnappingturtle.mythicage.village.economy;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.VillageBlockers;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.core.village.time.VillageCalendar;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 村务（T19 设计文档 §4.3，D4）：右键居民先说一句话（{@link ResidentDialogue}）；谷仓管事接着打开村务界面（库存、缺口、记录、捐赠），
 * 有交易表的职业接着打开交易界面（{@link ResidentTrade}）。
 * 捐赠是真实转移：满仓退回、记贡献（{@code seenBy}），<b>不加善恶、不加恩宠</b>（11 号 §8）。
 * 文本行的参数以 {@code @} 开头时，客户端把它当语言键翻译；{@code @@} 开头的把后面的参数都交给它（记录行）。
 */
public final class VillageAffairs {
    public static final double REACH = 8;

    private VillageAffairs() {
    }

    public static void onResidentInteract(ServerPlayer player, ResidentEntity r) {
        player.sendSystemMessage(Component.translatable("mythic_age.village.resident.says",
                r.getCustomName() == null ? Component.empty() : r.getCustomName(),
                Component.translatable(professionKey(r.profession())), ResidentDialogue.line(player.server, r)));
        Optional<UUID> v = r.village();
        if (v.isEmpty() || r.inTransit()) {
            return;
        }
        boolean keeper = VillageEconomy.get(player.server).peek(v.get())
                .map(s -> s.roster.get(r.getUUID())).map(VillageEconomy.Member::keeper).orElse(false);
        if (keeper) {
            open(player, v.get());
        } else if (ResidentTrade.trades(r)) {
            ResidentTrade.open(player, r);
        }
    }

    public static String professionKey(String profession) {
        return "mythic_age.profession." + profession.replace(':', '.');
    }

    public static void open(ServerPlayer player, UUID village) {
        open(player, village, AffairsView.TAB_OVERVIEW);
    }

    public static void open(ServerPlayer player, UUID village, int tab) {
        view(player, village, tab).ifPresent(v -> PacketDistributor.sendToPlayer(player, new AffairsPayloads.View(v)));
    }

    public static Optional<AffairsView> view(ServerPlayer player, UUID village) {
        return view(player, village, AffairsView.TAB_OVERVIEW);
    }

    /** @param tab 打开哪一页（{@link AffairsView#TAB_OVERVIEW}、{@link AffairsView#TAB_STOCK}） */
    public static Optional<AffairsView> view(ServerPlayer player, UUID village, int tab) {
        Optional<VillageRegistry.Village> vo = VillageRegistry.get(player.server).byId(village);
        if (vo.isEmpty()) {
            return Optional.empty();
        }
        VillageRegistry.Village v = vo.get();
        VillageEconomy.State s = VillageEconomy.get(player.server).state(village);
        Optional<CivVillageData> data = VillageData.civ(v.civ());
        List<AffairsView.Entry> stock = new ArrayList<>();
        s.stock.items().forEach((k, n) -> stock.add(new AffairsView.Entry(k, n, s.stock.escrowed(k))));
        s.stock.escrows().values().forEach(m -> m.keySet().forEach(k -> {
            if (s.stock.count(k) == 0 && stock.stream().noneMatch(e -> e.item().equals(k))) {
                stock.add(new AffairsView.Entry(k, 0, s.stock.escrowed(k)));
            }
        }));
        List<AffairsView.Blocker> blockers = blockers(village, s, data);
        List<AffairsView.Line> log = new ArrayList<>();
        for (int i = s.log.size() - 1; i >= 0 && log.size() < 8; i--) {
            VillageEconomy.LogEntry e = s.log.get(i);
            List<String> args = new ArrayList<>();
            args.add(String.valueOf(e.day() + 1));
            args.add("@@" + e.key());
            args.addAll(e.args());
            log.add(new AffairsView.Line("mythic_age.village.log.line", List.copyOf(args)));
        }
        AffairsView.Line title = titleLine(v);
        AffairsView.Line date = data.map(d -> {
            VillageCalendar.Date dt = d.calendar().date(MythDays.today(player.server), VillageData.offset(d, s));
            return dt.period().isEmpty() ? AffairsView.Line.of("mythic_age.village.date.plain", String.valueOf(dt.dayOfYear()))
                    : AffairsView.Line.of("mythic_age.village.date", "@mythic_age.village.period." + v.civ() + "." + dt.period(),
                    String.valueOf(dt.dayInPeriod()));
        }).orElse(AffairsView.Line.of("mythic_age.village.date.plain", "?"));
        List<AffairsView.Line> notes = data.map(d -> calendarNotes(player.server, v, d, s)).orElse(List.of());
        AffairsView.Overview overview = VillageOverview.build(player, v, s, data, blockers);
        return Optional.of(new AffairsView(village, tab, title, date, notes, s.stock.total(), s.stock.capacity(), stock, blockers, log,
                overview));
    }

    /**
     * 缺口按三类分开（T24 设计文档 §4.3）：自主会解决 / 需要稀有材料 / 被占地。「能否自产」看文明数据，与委托发布同一规则。
     */
    public static List<AffairsView.Blocker> blockers(UUID village, VillageEconomy.State s, Optional<CivVillageData> data) {
        List<AffairsView.Blocker> out = new ArrayList<>();
        s.activeProject().ifPresent(p -> {
            if (p.state == io.github.happysnappingturtle.mythicage.core.village.build.BuildProject.State.STOCKING) {
                p.missing().forEach((k, n) -> out.add(blocker(VillageBlockers.Kind.MISSING_MATERIAL,
                        data.map(d -> d.producible(k)).orElse(false),
                        AffairsView.Line.of("mythic_age.village.short.material", "@" + itemKey(k), String.valueOf(n)))));
            } else if (p.state == io.github.happysnappingturtle.mythicage.core.village.build.BuildProject.State.BLOCKED) {
                io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec spec =
                        io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings.all().get(p.building);
                String name = spec == null ? p.building
                        : io.github.happysnappingturtle.mythicage.village.build.Construction.nameArg(spec, p.god);
                String at = p.blockedAt == null ? "?" : p.blockedAt.x() + ", " + p.blockedAt.y() + ", " + p.blockedAt.z();
                out.add(blocker(VillageBlockers.Kind.PROJECT_BLOCKED, false,
                        AffairsView.Line.of("mythic_age.village.project.blocked_line", name, at)));
            }
        });
        data.ifPresent(d -> {
            if (d.rations().belowFloor(s.stock)) {
                out.add(blocker(VillageBlockers.Kind.FOOD_LOW, false, AffairsView.Line.of("mythic_age.village.short.food",
                        String.valueOf((int) d.rations().total(s.stock)), String.valueOf(d.rations().floor()))));
            }
            for (ResourcePoint p : s.points.values()) {
                if (p.remaining() <= 0) {
                    out.add(blocker(VillageBlockers.Kind.POINT_DEPLETED, false,
                            AffairsView.Line.of("mythic_age.village.short.point", "@mythic_age.village.point." + p.kind())));
                }
            }
        });
        if (s.stock.free() == 0) {
            out.add(blocker(VillageBlockers.Kind.STOCK_FULL, false, AffairsView.Line.of("mythic_age.village.short.full")));
        }
        if (io.github.happysnappingturtle.mythicage.village.VillagePopulation.needsHousing(village)) {
            out.add(blocker(VillageBlockers.Kind.NEEDS_HOUSING, false, AffairsView.Line.of("mythic_age.village.short.housing")));
        }
        String space = VillageScheduler.runtime(village).needsSpace();
        io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec spaceSpec =
                io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings.all().get(space);
        if (spaceSpec != null && s.activeProject().isEmpty()) {
            out.add(blocker(VillageBlockers.Kind.NEEDS_SPACE, false, AffairsView.Line.of("mythic_age.village.short.space",
                    io.github.happysnappingturtle.mythicage.village.build.Construction.nameArg(spaceSpec, ""))));
        }
        return List.copyOf(out);
    }

    private static AffairsView.Blocker blocker(VillageBlockers.Kind kind, boolean producible, AffairsView.Line line) {
        return new AffairsView.Blocker(VillageBlockers.classify(kind, producible).id(), line);
    }

    /** 村名（主神之村）的文本行。 */
    public static AffairsView.Line titleLine(VillageRegistry.Village v) {
        return v.patron().isEmpty() ? AffairsView.Line.of("mythic_age.village.title.plain")
                : AffairsView.Line.of("mythic_age.village.title", "@mythic_age.god." + v.patron());
    }

    /** 历法提示（§1.10 J8）：英雄起显示下一个节日还有几日（今天就是则说今天）；有神谕所时显示下一个神谕日。 */
    public static List<AffairsView.Line> calendarNotes(net.minecraft.server.MinecraftServer server, VillageRegistry.Village v,
                                                      CivVillageData d, VillageEconomy.State s) {
        List<AffairsView.Line> out = new ArrayList<>();
        long today = MythDays.today(server);
        int offset = VillageData.offset(d, s);
        int best = Integer.MAX_VALUE;
        String next = "";
        for (io.github.happysnappingturtle.mythicage.core.village.life.LifeData.Festival f : d.life().festivals().values()) {
            if (!io.github.happysnappingturtle.mythicage.core.village.life.FestivalRules.open(f.minAge(), v.stage())) {
                continue;
            }
            int n = d.calendar().daysUntil(today, offset, f.id());
            if (n >= 0 && n < best) {
                best = n;
                next = f.id();
            }
        }
        if (!next.isEmpty()) {
            out.add(best == 0 ? AffairsView.Line.of("mythic_age.village.date.festival_today", "@mythic_age.festival." + next)
                    : AffairsView.Line.of("mythic_age.village.date.festival_in", "@mythic_age.festival." + next, String.valueOf(best)));
        }
        io.github.happysnappingturtle.mythicage.village.life.OracleService.status(server, v).ifPresent(st -> out.add(
                st.state() == io.github.happysnappingturtle.mythicage.core.village.life.OracleRules.State.OPEN
                        ? AffairsView.Line.of("mythic_age.village.date.oracle_today")
                        : AffairsView.Line.of("mythic_age.village.date.oracle_next", String.valueOf(st.nextDay()))));
        return out;
    }

    /** 施工项目（T19 §1.4 E7）：进度、缺料、停工坐标。 */
    public static List<AffairsView.Line> projectLines(VillageEconomy.State s) {
        List<AffairsView.Line> out = new ArrayList<>();
        s.activeProject().ifPresent(p -> {
            io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec spec =
                    io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings.all().get(p.building);
            if (spec == null) {
                return;
            }
            String name = io.github.happysnappingturtle.mythicage.village.build.Construction.nameArg(spec, p.god);
            switch (p.state) {
                case STOCKING -> {
                    out.add(AffairsView.Line.of("mythic_age.village.project.stocking", name));
                    p.missing().forEach((k, n) -> out.add(AffairsView.Line.of("mythic_age.village.project.missing",
                            "@" + itemKey(k), String.valueOf(n))));
                }
                case BUILDING -> out.add(AffairsView.Line.of("mythic_age.village.project.building", name,
                        String.valueOf(p.placed.cardinality()), String.valueOf(p.segments)));
                case BLOCKED -> out.add(AffairsView.Line.of("mythic_age.village.project.blocked_line", name,
                        p.blockedAt.x() + ", " + p.blockedAt.y() + ", " + p.blockedAt.z()));
                default -> {
                }
            }
        });
        return out;
    }

    /** 物品（或 {@code #组}）的语言键。 */
    public static String itemKeyOf(String id) {
        return itemKey(id);
    }

    static String itemKey(String id) {
        if (id.startsWith("#")) {
            return "mythic_age.village.cost_group." + id.substring(1);
        }
        return BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)).getDescriptionId();
    }

    /**
     * 切换分页 / 从祭司对话页打开（T24 §4.1，D8）：要在谷仓管事 {@value #REACH} 格内，或者正和本村祭司说话。
     * village 为全零时用当前祭司会话所在的村。
     */
    public static void handleOpen(AffairsPayloads.Open payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                resolve(p, payload.village()).ifPresent(v -> open(p, v, payload.tab()));
            }
        });
    }

    public static void handleBounds(AffairsPayloads.BoundsRequest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                // 村界不是隐私：能看村务的人，或站在钟附近的人都能看
                Optional<UUID> v = resolve(p, payload.village()).or(() -> nearBell(p, payload.village()) ? Optional.of(payload.village())
                        : Optional.empty());
                v.flatMap(id -> io.github.happysnappingturtle.mythicage.village.VillageBounds.build(p.server, id))
                        .ifPresent(b -> PacketDistributor.sendToPlayer(p, b));
            }
        });
    }

    static boolean nearBell(ServerPlayer player, UUID village) {
        return VillageRegistry.get(player.server).byId(village)
                .filter(v -> v.bell().dimension() == player.level().dimension())
                .map(v -> player.blockPosition().closerThan(v.bell().pos(),
                        io.github.happysnappingturtle.mythicage.village.VillageService.LEAVE_RANGE))
                .orElse(false);
    }

    /** 能看哪个村的村务：谷仓管事身边，或本村祭司的会话里。 */
    public static Optional<UUID> resolve(ServerPlayer player, UUID requested) {
        Optional<UUID> priest = io.github.happysnappingturtle.mythicage.trade.TradeService.session(player)
                .filter(s -> io.github.happysnappingturtle.mythicage.trade.TradeService.valid(player, s))
                .map(io.github.happysnappingturtle.mythicage.trade.TradeService.Session::villageId);
        if (requested.equals(AffairsPayloads.Open.FROM_PRIEST)) {
            return priest;
        }
        // 管理员（权限 2，可以用 /mythicage village affairs）在哪都能看
        if (priest.filter(requested::equals).isPresent() || nearKeeper(player, requested)
                || (player.hasPermissions(2) && VillageRegistry.get(player.server).byId(requested).isPresent())) {
            return Optional.of(requested);
        }
        return Optional.empty();
    }

    /** 捐赠主手物品：容量够多少收多少，其余留在手上。 */
    public static void handleDonate(AffairsPayloads.Donate payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                donate(p, payload.village());
                open(p, payload.village(), AffairsView.TAB_STOCK);
            }
        });
    }

    /** 返回实收数。 */
    public static int donate(ServerPlayer player, UUID village) {
        if (!nearKeeper(player, village)) {
            return 0;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.isEmpty() || held.isDamaged() || !held.getComponentsPatch().isEmpty()) {
            player.displayClientMessage(Component.translatable("mythic_age.village.donate.invalid"), true);
            return 0;
        }
        VillageEconomy eco = VillageEconomy.get(player.server);
        VillageEconomy.State s = eco.state(village);
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        Component name = held.getHoverName();
        int got = s.stock.deposit(id, held.getCount());
        if (got > 0) {
            held.shrink(got);
            s.seenBy.add(player.getUUID());
            eco.setDirty();
            creditDonation(player, village, got);
            // 捐赠恰好补上了某个委托的缺口：任务当刻转「需求已满足」（T21 §5.3）
            io.github.happysnappingturtle.mythicage.village.quest.QuestService.refreshNow(player.server, village);
        }
        player.displayClientMessage(Component.translatable(got > 0 ? "mythic_age.village.donate.done"
                : "mythic_age.village.donate.full", name, got, held.getCount()), true);
        return got;
    }

    /** 捐献记贡献（T24 §3.3）：每件与委托交付同权重；贡献只是记录，不换东西（D7）。 */
    static void creditDonation(ServerPlayer player, UUID village, int count) {
        VillageRegistry.get(player.server).byId(village).flatMap(v -> VillageData.civ(v.civ())).ifPresent(d -> {
            int points = count * d.quests().contribution().item();
            io.github.happysnappingturtle.mythicage.village.quest.VillageQuests.get(player.server).book(village)
                    .contribute(player.getUUID(), "donate", points, MythDays.today(player.server));
        });
    }

    /** 离谷仓管事 {@value #REACH} 格内（T21 顺手修正：以前任意居民身边都能捐）。 */
    private static boolean nearKeeper(ServerPlayer player, UUID village) {
        return io.github.happysnappingturtle.mythicage.village.quest.QuestService.nearKeeper(player, village);
    }

    /** 只供 GameTest（TC-T19-51）：跳过距离检查直接捐。 */
    public static int donateForTest(ServerPlayer player, UUID village, ItemStack stack) {
        VillageEconomy.State s = VillageEconomy.get(player.server).state(village);
        int got = s.stock.deposit(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount());
        stack.shrink(got);
        s.seenBy.add(player.getUUID());
        if (got > 0) {
            creditDonation(player, village, got);
        }
        return got;
    }
}
