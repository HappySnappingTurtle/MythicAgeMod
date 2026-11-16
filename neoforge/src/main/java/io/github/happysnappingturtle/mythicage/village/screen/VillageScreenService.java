package io.github.happysnappingturtle.mythicage.village.screen;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.rite.VillageStageTable;
import io.github.happysnappingturtle.mythicage.core.village.VillageBlockers;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.core.village.map.MapDelta;
import io.github.happysnappingturtle.mythicage.core.village.map.VillageMapData;
import io.github.happysnappingturtle.mythicage.core.rite.MigrationRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.relic.TempleRelics;
import io.github.happysnappingturtle.mythicage.rite.RiteTables;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.village.VillagePopulation;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import io.github.happysnappingturtle.mythicage.village.VillageSpace;
import io.github.happysnappingturtle.mythicage.village.build.BuildSite;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.build.ProjectPayloads;
import io.github.happysnappingturtle.mythicage.village.build.SiteMarkers;
import io.github.happysnappingturtle.mythicage.village.build.VillageProjects;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.economy.VillageOverview;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.map.VillageMaps;
import io.github.happysnappingturtle.mythicage.village.quest.QuestPayloads;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.quest.VillageQuests;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import io.github.happysnappingturtle.mythicage.village.work.ResidentMigration;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 村庄界面的服务端（GA-12）：打开时发状态栏与各分页的全量，界面开着时每 {@value #PERIOD} 刻比一次地图与状态栏、有变化才发；
 * 关界面、下线或走出能看的范围就停。五文明通用：信仰分页里文明特有的部分（今日信徒与恩宠、退出供奉）由各文明登记
 * {@link FaithHooks}。
 * <p>
 * 谁能看：站在村界内或钟 {@value VillageService#LEAVE_RANGE} 格内、谷仓管事身边、本村祭司的会话里，或权限 2。
 * 交付委托、捐主手物品仍要在谷仓管事身边（T21，没变）；「捐出背包里的」也可以在工地旁（§10 I3）。
 */
public final class VillageScreenService {
    public static final int PERIOD = 20;
    /** 在工地旁捐项目材料：离工地包围盒多远以内。 */
    public static final int SITE_REACH = 8;

    /** 文明特有的信仰数据（GA-13 的祈祷台账在各文明内容里）。 */
    public interface FaithHooks {
        /** 今天在这里为 viewer 祈祷的信徒数、得到的恩宠、今天献过没有、献供加成百分比。 */
        Today today(MinecraftServer server, VillageTemple t, UUID viewer);

        /** 不再供奉此村；不是供奉者时返回 false。 */
        boolean leave(ServerPlayer player, UUID village);
    }

    public record Today(int believers, double favor, boolean offered, int offerBonus) {
        public static final Today NONE = new Today(0, 0, false, 0);
    }

    private static final Map<String, FaithHooks> FAITH = new ConcurrentHashMap<>();

    public static void registerFaith(String civ, FaithHooks hooks) {
        FAITH.put(civ, hooks);
    }

    /** 一名玩家开着的界面。 */
    static final class Session {
        final UUID village;
        VillageMapData map = VillageMapData.EMPTY;
        VillageScreenPayloads.Header header;

        Session(UUID village) {
            this.village = village;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    /** 只供 GameTest：发给每名（假）玩家的包（假玩家收不到网络包）。 */
    private static final Map<UUID, List<net.minecraft.network.protocol.common.custom.CustomPacketPayload>> SENT_FOR_TEST =
            new ConcurrentHashMap<>();
    private static volatile boolean recordForTest;

    private VillageScreenService() {
    }

    // ───────────── 打开 ─────────────

    /** 打开村庄界面到 tab；看不了返回 false（动作栏提示）。 */
    public static boolean open(ServerPlayer player, UUID village, int tab) {
        Optional<VillageContext> ctx = context(player.server, village);
        if (ctx.isEmpty()) {
            return false;
        }
        if (!canView(player, village)) {
            player.displayClientMessage(Component.translatable("mythic_age.village.screen.far"), true);
            return false;
        }
        int t = Math.max(0, Math.min(VillageScreenPayloads.TABS - 1, tab));
        Session s = new Session(village);
        Session old = SESSIONS.put(player.getUUID(), s);
        s.header = header(player, ctx.get());
        send(player, new VillageScreenPayloads.Opened(village, t, s.header));
        sendAll(player, ctx.get(), s, old != null && old.village.equals(village) ? old.map : VillageMapData.EMPTY);
        return true;
    }

    /** 各分页的数据；地图在已有数据上发增量（切分页时不必重发全量）。 */
    static void sendAll(ServerPlayer player, VillageContext ctx, Session s, VillageMapData known) {
        UUID village = ctx.village().id();
        VillageAffairs.view(player, village).ifPresent(v -> send(player, new AffairsPayloads.View(v)));
        QuestService.view(player, village).ifPresent(v -> send(player, new QuestPayloads.View(v)));
        ProjectPayloads.view(player, ctx).ifPresent(v -> send(player, v));
        send(player, people(player, ctx));
        send(player, faith(player, ctx));
        s.map = known;
        sendMap(player, s, VillageMaps.build(ctx));
    }

    static void sendMap(ServerPlayer player, Session s, VillageMapData now) {
        Optional<MapDelta> d = s.map == VillageMapData.EMPTY ? Optional.empty() : MapDelta.diff(s.map, now);
        VillageScreenPayloads.MapPacket packet;
        if (d.isPresent()) {
            if (d.get().isEmpty()) {
                return;
            }
            packet = new VillageScreenPayloads.MapPacket(s.village, false, now.centerX(), now.centerZ(),
                    d.get().encode(now.centerX(), now.centerZ()));
        } else {
            packet = new VillageScreenPayloads.MapPacket(s.village, true, now.centerX(), now.centerZ(), now.encode());
        }
        s.map = now;
        send(player, packet);
    }

    /** 能不能看这个村子的界面。 */
    public static boolean canView(ServerPlayer player, UUID village) {
        if (player.hasPermissions(2)) {
            return true;
        }
        Optional<VillageRegistry.Village> v = VillageRegistry.get(player.server).byId(village);
        if (v.isEmpty()) {
            return false;
        }
        if (VillageSpace.villageAt(player.serverLevel(), player.blockPosition()).map(x -> x.id().equals(village)).orElse(false)) {
            return true;
        }
        if (v.get().bell().dimension() == player.level().dimension()
                && player.blockPosition().closerThan(v.get().bell().pos(), VillageService.LEAVE_RANGE)) {
            return true;
        }
        return priestVillage(player).filter(village::equals).isPresent() || QuestService.nearKeeper(player, village);
    }

    /** 正在和哪个村的祭司说话。 */
    static Optional<UUID> priestVillage(ServerPlayer player) {
        return TradeService.session(player).filter(s -> TradeService.valid(player, s)).map(TradeService.Session::villageId);
    }

    static Optional<VillageContext> context(MinecraftServer server, UUID village) {
        Optional<VillageRegistry.Village> v = VillageRegistry.get(server).byId(village);
        if (v.isEmpty()) {
            return Optional.empty();
        }
        ServerLevel level = server.getLevel(v.get().bell().dimension());
        return level == null ? Optional.empty() : VillageScheduler.contextOf(level, village);
    }

    public static boolean isOpen(UUID player) {
        return SESSIONS.containsKey(player);
    }

    public static Optional<UUID> openVillage(UUID player) {
        return Optional.ofNullable(SESSIONS.get(player)).map(s -> s.village);
    }

    public static void close(UUID player) {
        SESSIONS.remove(player);
    }

    // ───────────── 每 20 刻 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (SESSIONS.isEmpty() || server.getTickCount() % PERIOD != 0) {
            return;
        }
        for (Map.Entry<UUID, Session> e : List.copyOf(SESSIONS.entrySet())) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                p = FAKE.get(e.getKey());
            }
            Session s = e.getValue();
            if (p == null || p.isRemoved() || !canView(p, s.village)) {
                SESSIONS.remove(e.getKey(), s);
                continue;
            }
            tick(p, s);
        }
    }

    static void tick(ServerPlayer p, Session s) {
        Optional<VillageContext> ctx = context(p.server, s.village);
        if (ctx.isEmpty()) {
            SESSIONS.remove(p.getUUID(), s);
            return;
        }
        sendMap(p, s, VillageMaps.build(ctx.get()));
        VillageScreenPayloads.Header h = header(p, ctx.get());
        if (!h.equals(s.header)) {
            s.header = h;
            send(p, new VillageScreenPayloads.HeaderUpdate(s.village, h));
        }
    }

    // ───────────── 状态栏 ─────────────

    static VillageScreenPayloads.Header header(ServerPlayer player, VillageContext ctx) {
        MinecraftServer server = player.server;
        VillageRegistry.Village v = ctx.village();
        VillageEconomy.State s = ctx.state();
        Optional<VillageTemple> temple = VillageTemples.of(server, v);
        List<AffairsView.Line> alerts = VillageOverview.alerts(server, v.id(), s);
        AffairsView.Line project = AffairsView.Line.of("");
        int progress = -1;
        Optional<BuildProject> active = s.activeProject();
        if (active.isPresent()) {
            BuildingSpec spec = VillageBuildings.all().get(active.get().building);
            if (spec != null) {
                project = AffairsView.Line.of("mythic_age.village.project.name", Construction.nameArg(spec, active.get().god));
                progress = VillageMaps.progress(ctx, active.get());
            }
        }
        int food = (int) ctx.data().rations().total(s.stock);
        int floor = ctx.data().rations().floor();
        int quests = 0;
        Optional<VillageQuests.Book> book = VillageQuests.get(server).peek(v.id());
        if (book.isPresent()) {
            for (Quest q : book.get().quests) {
                if (!q.state.ended()) {
                    quests++;
                }
            }
        }
        int tabs = 0;
        List<AffairsView.Blocker> blockers = VillageAffairs.blockers(v.id(), s, Optional.of(ctx.data()));
        for (AffairsView.Blocker b : blockers) {
            if (!b.cls().equals(VillageBlockers.Class.SELF_RESOLVING.id()) || b.line().key().equals("mythic_age.village.project.blocked_line")) {
                tabs |= 1 << VillageScreenPayloads.TAB_BUILD;
            }
        }
        if (VillagePopulation.needsHousing(v.id())) {
            tabs |= 1 << VillageScreenPayloads.TAB_PEOPLE;
        }
        if (s.stock.free() == 0 || ctx.data().rations().belowFloor(s.stock)) {
            tabs |= 1 << VillageScreenPayloads.TAB_STOCK;
        }
        if (temple.isPresent() && temple.get().level() > 0
                && temple.get().integrity() < io.github.happysnappingturtle.mythicage.core.village.temple.TempleStanding.PROMOTION_INTEGRITY) {
            tabs |= 1 << VillageScreenPayloads.TAB_FAITH;
        }
        return new VillageScreenPayloads.Header(v.civ(), VillageAffairs.titleLine(v), v.stage().id(), v.patron(),
                temple.map(VillageTemple::level).orElse(0),
                temple.map(t -> "mythic_age.temple.tier." + t.tierName()).orElse(""),
                alerts.isEmpty() ? AffairsView.Line.of("") : alerts.get(0), project, progress, food, floor, quests, tabs,
                QuestService.nearKeeper(player, v.id()));
    }

    // ───────────── 村民 ─────────────

    static VillageScreenPayloads.People people(ServerPlayer player, VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        VillageRegistry.Village v = ctx.village();
        Map<String, Integer> have = new LinkedHashMap<>();
        s.roster.values().forEach(m -> have.merge(m.profession(), 1, Integer::sum));
        Map<String, Integer> slots = ResidentMigration.jobs(ctx);
        List<VillageScreenPayloads.Job> jobs = new ArrayList<>();
        Set<String> all = new java.util.LinkedHashSet<>(slots.keySet());
        all.addAll(have.keySet());
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        VillageMapData map = Optional.ofNullable(SESSIONS.get(player.getUUID())).filter(x -> x.village.equals(v.id()))
                .map(x -> x.map).filter(m -> m != VillageMapData.EMPTY).orElseGet(() -> VillageMaps.build(ctx));
        for (String prof : all) {
            List<String> where = map.buildings().stream().filter(b -> {
                BuildingSpec spec = specs.get(b.building());
                return spec != null && spec.jobs().containsKey(prof);
            }).map(VillageMapData.Building::key).toList();
            jobs.add(new VillageScreenPayloads.Job(VillageAffairs.professionKey(prof), have.getOrDefault(prof, 0),
                    slots.getOrDefault(prof, 0), where));
        }
        jobs.sort(java.util.Comparator.comparingInt(VillageScreenPayloads.Job::slots).reversed()
                .thenComparing(VillageScreenPayloads.Job::key));
        List<AffairsView.Count> collaborators = VillageQuests.get(player.server).peek(v.id())
                .map(b -> VillageOverview.collaborators(player.server, b.contribution)).orElse(List.of());
        return new VillageScreenPayloads.People(v.id(), s.roster.size(), Residents.homes(ctx.plan()).size(), s.soldiers.size(),
                VillagePopulation.guardCap(ctx.level(), v, v.stage()), List.copyOf(jobs), migration(ctx), collaborators);
    }

    /** 移民进度一行：同 {@link ResidentMigration#step} 的判定，但不生成人、不改状态。 */
    static AffairsView.Line migration(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        if (s.migrant != null) {
            return AffairsView.Line.of("mythic_age.village.people.migrant_coming");
        }
        Optional<Civilization> civ = Civilization.byId(ctx.village().civ());
        if (civ.isEmpty()) {
            return AffairsView.Line.of("mythic_age.village.people.migration_inactive");
        }
        VillageRegistry.Village v = VillageRegistry.get(ctx.level().getServer()).byId(ctx.village().id()).orElse(ctx.village());
        VillageStageTable table = RiteTables.stages(civ.get());
        int members = s.roster.size();
        int target = table.populationTarget(v.baseline() >= 0 ? v.baseline() : members, v.stage());
        int interval = table.migrationInterval(v.stage());
        int free = Residents.freeHomes(ctx.plan(), s).size();
        MigrationRules.Decision d = MigrationRules.decide(new MigrationRules.Input(v.stage(), members, target, free, ctx.today(),
                v.lastMigrationDay(), interval, true));
        return switch (d) {
            case MIGRATE -> AffairsView.Line.of("mythic_age.village.people.migration_soon");
            case FULL -> AffairsView.Line.of("mythic_age.village.people.migration_full", String.valueOf(target));
            case NO_BED -> AffairsView.Line.of("mythic_age.village.people.migration_no_bed");
            case WAITING -> AffairsView.Line.of("mythic_age.village.people.migration_in",
                    String.valueOf(Math.max(1, v.lastMigrationDay() + interval - ctx.today())));
            default -> AffairsView.Line.of("mythic_age.village.people.migration_inactive");
        };
    }

    // ───────────── 信仰 ─────────────

    static VillageScreenPayloads.Faith faith(ServerPlayer player, VillageContext ctx) {
        VillageRegistry.Village v = ctx.village();
        MinecraftServer server = player.server;
        List<VillageScreenPayloads.Faith.Shrine> shrines = new ArrayList<>();
        Set<String> built = new java.util.LinkedHashSet<>();
        ctx.state().constructed.forEach(b -> {
            if (!b.god().isEmpty()) {
                built.add(b.god());
            }
        });
        new java.util.TreeMap<>(v.shrines()).forEach((age, gods) -> gods.forEach(g -> {
            if (shrines.stream().noneMatch(x -> x.god().equals(g))) {
                shrines.add(new VillageScreenPayloads.Faith.Shrine(g, built.contains(g)));
            }
        }));
        Optional<VillageTemple> t = VillageTemples.of(server, v);
        if (t.isEmpty()) {
            return new VillageScreenPayloads.Faith(v.id(), false, 0, "", 0, 0, v.patron(), shrines, false, 0, 0, 0, false, 0,
                    List.of(), 0);
        }
        VillageTemple temple = t.get();
        var book = VillageTemples.book(server, v.id());
        Today today = Optional.ofNullable(FAITH.get(v.civ())).map(h -> h.today(server, temple, player.getUUID())).orElse(Today.NONE);
        var shelf = TempleRelics.shelf(player, temple);
        return new VillageScreenPayloads.Faith(v.id(), true, temple.level(), "mythic_age.temple.tier." + temple.tierName(),
                (int) Math.round(temple.integrity() * 100), temple.coefficient(), v.patron(), shrines,
                book.isDevotee(player.getUUID()), book.devotees().size(), today.believers(), today.favor(), today.offered(),
                today.offerBonus(), shelf.relics(), shelf.cap());
    }

    // ───────────── 建筑详情 ─────────────

    static Optional<VillageScreenPayloads.Detail> detail(ServerPlayer player, VillageContext ctx, String key) {
        UUID village = ctx.village().id();
        VillageMapData map = Optional.ofNullable(SESSIONS.get(player.getUUID())).filter(s -> s.village.equals(village))
                .map(s -> s.map).filter(m -> m != VillageMapData.EMPTY).orElseGet(() -> VillageMaps.build(ctx));
        List<VillageProjects.View> candidates = VillageProjects.candidates(ctx);
        List<AffairsView.Line> lines = new ArrayList<>();
        if (key.startsWith("plot:")) {
            String id = key.substring(5);
            Optional<VillagePlan.Plot> plot = ctx.plan().info().plots().stream().filter(p -> p.id().equals(id)).findFirst();
            if (plot.isEmpty()) {
                return Optional.empty();
            }
            lines.add(AffairsView.Line.of("mythic_age.village.map.plot", "@mythic_age.village.plot." + plot.get().kind()));
            lines.add(AffairsView.Line.of("mythic_age.village.map.plot_stage", "@mythic_age.age." + plot.get().stage()));
            List<AffairsView.Line> options = new ArrayList<>();
            for (VillageProjects.View c : candidates) {
                if (c.candidate().kind() != ProjectPicker.Kind.UPGRADE && c.spec().plotKinds().contains(plot.get().kind())) {
                    options.add(AffairsView.Line.of("mythic_age.village.project.name",
                            Construction.nameArg(c.spec(), c.candidate().pick().god())));
                }
            }
            return Optional.of(new VillageScreenPayloads.Detail(village, key, lines, "", "", options));
        }
        Optional<VillageMapData.Building> b = map.building(key);
        if (b.isEmpty()) {
            return Optional.empty();
        }
        VillageMapData.Building bd = b.get();
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        BuildingSpec spec = specs.get(bd.building());
        if (bd.tier() > 0) {
            lines.add(AffairsView.Line.of("mythic_age.village.map.tier", String.valueOf(bd.tier())));
        }
        if (bd.underConstruction()) {
            BuildingSpec target = specs.get(bd.target());
            lines.add(target == null ? AffairsView.Line.of("mythic_age.village.map.building_progress", String.valueOf(bd.progress()))
                    : AffairsView.Line.of("mythic_age.village.map.upgrading", Construction.nameArg(target, ""),
                    String.valueOf(bd.progress())));
        }
        int beds = 0;
        int used = 0;
        Set<Long> homes = new java.util.HashSet<>();
        ctx.state().roster.values().forEach(m -> homes.add(m.home().asLong()));
        for (Pos p : Residents.homes(ctx.plan())) {
            if (bd.box().contains(p.x(), p.z())) {
                beds++;
                if (homes.contains(BlockPos.asLong(p.x(), p.y(), p.z()))) {
                    used++;
                }
            }
        }
        if (beds > 0) {
            lines.add(AffairsView.Line.of("mythic_age.village.map.beds", String.valueOf(beds), String.valueOf(used)));
        }
        if (spec != null) {
            new java.util.TreeMap<>(spec.jobs()).forEach((prof, n) -> lines.add(AffairsView.Line.of("mythic_age.village.map.job",
                    "@" + VillageAffairs.professionKey(prof), String.valueOf(n))));
        }
        String upgradeKey = "";
        String reason = "";
        if (!bd.building().isEmpty() && !bd.underConstruction()) {
            for (VillageProjects.View c : candidates) {
                if (c.candidate().kind() == ProjectPicker.Kind.UPGRADE
                        && c.spec().replace().stream().anyMatch(bd.building()::startsWith)) {
                    upgradeKey = c.candidate().key();
                    reason = c.candidate().reason();
                    lines.add(AffairsView.Line.of("mythic_age.village.map.can_upgrade",
                            Construction.nameArg(c.spec(), c.candidate().pick().god())));
                    break;
                }
            }
        }
        return Optional.of(new VillageScreenPayloads.Detail(village, key, lines, upgradeKey, reason, List.of()));
    }

    // ───────────── 操作 ─────────────

    /** 「捐出背包里的」（§3.2）：只捐当前项目正好还缺的（村库里有的不算缺），多的不动。返回捐了几件。 */
    public static int donateMissing(ServerPlayer player, VillageContext ctx, boolean checkReach) {
        VillageEconomy.State s = ctx.state();
        Optional<BuildProject> active = s.activeProject();
        if (active.isEmpty()) {
            return 0;
        }
        if (checkReach && !QuestService.nearKeeper(player, ctx.village().id()) && !nearSite(player, ctx, active.get())) {
            player.displayClientMessage(Component.translatable("mythic_age.village.screen.donate_far"), true);
            return 0;
        }
        var costs = ctx.data().buildCosts();
        int total = 0;
        for (Map.Entry<String, Integer> e : active.get().missing().entrySet()) {
            List<String> items = costs.items(e.getKey());
            int shortfall = e.getValue();
            for (String item : items) {
                shortfall -= s.stock.count(item);
            }
            for (String item : items) {
                if (shortfall <= 0) {
                    break;
                }
                var inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize() && shortfall > 0; i++) {
                    ItemStack st = inv.getItem(i);
                    if (st.isEmpty() || st.isDamaged() || !st.getComponentsPatch().isEmpty()
                            || !BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals(item)) {
                        continue;
                    }
                    int got = s.stock.deposit(item, Math.min(shortfall, st.getCount()));
                    if (got <= 0) {
                        break;
                    }
                    st.shrink(got);
                    shortfall -= got;
                    total += got;
                }
            }
        }
        if (total > 0) {
            s.seenBy.add(player.getUUID());
            VillageEconomy.get(player.server).setDirty();
            VillageAffairs.creditDonation(player, ctx.village().id(), total);
            QuestService.refreshNow(player.server, ctx.village().id());
        }
        player.displayClientMessage(total > 0 ? Component.translatable("mythic_age.village.screen.donated", total)
                : Component.translatable("mythic_age.village.screen.donate_none"), true);
        return total;
    }

    static boolean nearSite(ServerPlayer player, VillageContext ctx, BuildProject p) {
        Optional<BuildSite> site = Construction.site(ctx, p);
        if (site.isEmpty()) {
            return false;
        }
        var b = site.get().box();
        BlockPos at = player.blockPosition();
        return at.getX() >= b.minX() - SITE_REACH && at.getX() <= b.maxX() + SITE_REACH
                && at.getZ() >= b.minZ() - SITE_REACH && at.getZ() <= b.maxZ() + SITE_REACH;
    }

    public static void act(ServerPlayer player, VillageScreenPayloads.Act a) {
        if (!canView(player, a.village())) {
            return;
        }
        Optional<VillageContext> ctx = context(player.server, a.village());
        if (ctx.isEmpty()) {
            return;
        }
        Optional<VillageTemple> t = VillageTemples.of(player.server, ctx.get().village());
        switch (a.action()) {
            case VillageScreenPayloads.Act.DONATE_MISSING -> {
                donateMissing(player, ctx.get(), true);
                VillageAffairs.view(player, a.village()).ifPresent(v -> send(player, new AffairsPayloads.View(v)));
                ProjectPayloads.view(player, ctx.get()).ifPresent(v -> send(player, v));
            }
            case VillageScreenPayloads.Act.RELIC_OFFER -> t.ifPresent(temple -> {
                ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (!TempleRelics.offer(player, ctx.get().level(), temple, held)) {
                    player.displayClientMessage(Component.translatable("mythic_age.village.screen.relic_hold"), true);
                }
            });
            case VillageScreenPayloads.Act.RELIC_RETRIEVE -> t.ifPresent(temple -> TempleRelics.retrieve(player, temple, a.arg()));
            case VillageScreenPayloads.Act.LEAVE -> Optional.ofNullable(FAITH.get(ctx.get().village().civ()))
                    .ifPresent(h -> h.leave(player, a.village()));
            case VillageScreenPayloads.Act.SHOW_BLOCKED -> ctx.get().state().activeProject()
                    .ifPresent(p -> SiteMarkers.sendTo(player, ctx.get(), p));
            default -> {
                return;
            }
        }
        if (a.action().startsWith("relic") || a.action().equals(VillageScreenPayloads.Act.LEAVE)) {
            send(player, faith(player, ctx.get()));
        }
    }

    // ───────────── 网络 ─────────────

    public static void handleOpen(VillageScreenPayloads.Open payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                openRequested(p, payload.village(), payload.tab());
            }
        });
    }

    /** 客户端要打开：village 为 {@link VillageScreenPayloads#FROM_PRIEST} 时用祭司会话所在的村（没有会话就不开）。 */
    public static boolean openRequested(ServerPlayer p, UUID requested, int tab) {
        UUID village = requested.equals(VillageScreenPayloads.FROM_PRIEST) ? priestVillage(p).orElse(null) : requested;
        return village != null && open(p, village, tab);
    }

    public static void handleClose(VillageScreenPayloads.Close payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                closed(p, payload.village());
            }
        });
    }

    /** 客户端关了这个村的界面。 */
    public static void closed(ServerPlayer p, UUID village) {
        Session s = SESSIONS.get(p.getUUID());
        if (s != null && s.village.equals(village)) {
            SESSIONS.remove(p.getUUID(), s);
        }
    }

    public static void handleDetail(VillageScreenPayloads.DetailRequest payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p && canView(p, payload.village())) {
                context(p.server, payload.village()).flatMap(ctx -> detail(p, ctx, payload.key())).ifPresent(d -> send(p, d));
            }
        });
    }

    public static void handleAct(VillageScreenPayloads.Act payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                act(p, payload);
            }
        });
    }

    static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (recordForTest && FAKE.containsKey(player.getUUID())) {
            SENT_FOR_TEST.computeIfAbsent(player.getUUID(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(payload);
        }
        if (player.connection != null && !(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    // ───────────── 只供 GameTest ─────────────

    /** 假玩家不在玩家列表里：GameTest 登记后，每 20 刻的检查也照样跑。 */
    private static final Map<UUID, ServerPlayer> FAKE = new ConcurrentHashMap<>();

    public static void trackForTest(ServerPlayer fake) {
        FAKE.put(fake.getUUID(), fake);
        recordForTest = true;
    }

    public static void untrackForTest(ServerPlayer fake) {
        FAKE.remove(fake.getUUID());
        SESSIONS.remove(fake.getUUID());
        SENT_FOR_TEST.remove(fake.getUUID());
    }

    public static List<net.minecraft.network.protocol.common.custom.CustomPacketPayload> sentForTest(UUID player) {
        return List.copyOf(SENT_FOR_TEST.getOrDefault(player, List.of()));
    }

    public static void clearSentForTest(UUID player) {
        SENT_FOR_TEST.remove(player);
    }

    public static Optional<VillageScreenPayloads.Detail> detailForTest(ServerPlayer player, UUID village, String key) {
        return context(player.server, village).flatMap(ctx -> detail(player, ctx, key));
    }

    public static Optional<VillageMapData> sessionMapForTest(UUID player) {
        return Optional.ofNullable(SESSIONS.get(player)).map(s -> s.map);
    }

    public static void onServerStopped() {
        SESSIONS.clear();
        FAKE.clear();
        SENT_FOR_TEST.clear();
    }
}
