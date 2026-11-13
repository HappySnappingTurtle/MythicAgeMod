package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildCosts;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectQueue;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家点名下一个项目（GA-11 §2.3，GA-9 D1–D3）：贡献达门槛（{@code village_civ} 的 {@code projectPick}）的玩家
 * 从候选里点一项进「下一个」位，或清空它；当前项目开工后不能换。点名、开工、作废都提示村界内的玩家并写进发展记录。
 * 五文明通用：门槛与建筑都来自文明数据。
 */
public final class VillageProjects {
    public enum Result { OK, CLEARED, LOW_CONTRIBUTION, UNAVAILABLE, CURRENT }

    /** @param need 门槛（贡献不够时有意义） */
    public record Outcome(Result result, int need, int have, String reason) {
        static Outcome of(Result r) {
            return new Outcome(r, 0, 0, "");
        }
    }

    /** 候选的显示信息：材料表（新建：模板全量；升级：差额，已按折算表合并成村库物品）。 */
    public record View(ProjectPicker.Candidate candidate, BuildingSpec spec, Map<String, Integer> materials) {
    }

    /** 材料表缓存：建筑 id（升级再加旧建筑 id）→ 账单。模板与折算不随位置变。 */
    private static final Map<String, Map<String, Integer>> MATERIALS = new ConcurrentHashMap<>();

    private VillageProjects() {
    }

    public static void clearCache() {
        MATERIALS.clear();
        LAST_ANNOUNCE.clear();
    }

    public static List<View> candidates(VillageContext ctx) {
        Optional<ProjectPicker.Input> in = Construction.input(ctx);
        if (in.isEmpty()) {
            return List.of();
        }
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        List<View> out = new ArrayList<>();
        for (ProjectPicker.Candidate c : ProjectPicker.candidates(in.get())) {
            BuildingSpec base = specs.get(c.pick().building());
            if (base == null) {
                continue;
            }
            BuildingSpec spec = BuildingSpec.forPatron(specs, base.civ(), base.shortBase(), ctx.village().patron());
            out.add(new View(c, spec, materials(ctx, spec, c.pick().replaces().isEmpty() ? null : specs.get(c.pick().replaces()))));
        }
        return out;
    }

    static Map<String, Integer> materials(VillageContext ctx, BuildingSpec spec, BuildingSpec old) {
        String key = spec.id() + (old == null ? "" : ">" + old.id());
        Map<String, Integer> hit = MATERIALS.get(key);
        if (hit != null) {
            return hit;
        }
        ServerLevel level = ctx.level();
        BuildCosts civ = ctx.data().buildCosts();
        Optional<BuildSite> site;
        if (old == null) {
            site = BuildSite.create(level, civ, spec, place(spec, new Pos(0, 0, 0)));
        } else {
            BuildPlacement.Placed oldAt = place(old, new Pos(0, 0, 0));
            site = BuildSite.replace(level, civ, spec, BuildPlacement.replacing(spec, old, oldAt), old, oldAt);
        }
        Map<String, Integer> bill = site.map(s -> Map.copyOf(BuildCosts.ceil(s.totalCost()))).orElse(Map.of());
        MATERIALS.put(key, bill);
        return bill;
    }

    private static BuildPlacement.Placed place(BuildingSpec s, Pos origin) {
        return new BuildPlacement.Placed(s.id(), origin, Turn.R0,
                TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), Turn.R0));
    }

    /** 这名玩家在这个村子的贡献。 */
    public static int contribution(ServerPlayer player, VillageContext ctx) {
        return QuestService.contribution(player.server, ctx.village().id(), player.getUUID());
    }

    /** 供奉者（门槛减半）：这个村子神庙的供奉者（GA-13 §2.2）。 */
    public static boolean patron(ServerPlayer player, VillageContext ctx) {
        return ctx.state().temple.isDevotee(player.getUUID());
    }

    public static int threshold(ServerPlayer player, VillageContext ctx) {
        return ctx.data().projectPick().threshold(patron(player, ctx));
    }

    /** 点名：把候选键为 key 的一项放进「下一个」位（有人点过就换掉）。村子正闲着就马上开工。 */
    public static Outcome nominate(ServerPlayer player, VillageContext ctx, String key) {
        int have = contribution(player, ctx);
        int need = threshold(player, ctx);
        if (have < need) {
            return new Outcome(Result.LOW_CONTRIBUTION, need, have, "");
        }
        Optional<ProjectPicker.Input> in = Construction.input(ctx);
        Optional<ProjectPicker.Candidate> c = in.flatMap(i -> ProjectPicker.candidates(i).stream()
                .filter(x -> x.key().equals(key)).findFirst());
        // 正在盖的唯一建筑、正在升级的最后一座不再列出：就是「开工后不能换」的那一项（住居这类还有别的可升级时照常能点）
        if (c.isEmpty() && in.isPresent() && in.get().inProgress().contains(key)) {
            return Outcome.of(Result.CURRENT);
        }
        if (c.isEmpty() || !c.get().available()) {
            return new Outcome(Result.UNAVAILABLE, need, have, c.map(ProjectPicker.Candidate::reason).orElse(ProjectQueue.GONE));
        }
        VillageEconomy.State s = ctx.state();
        ProjectQueue.Next next = new ProjectQueue.Next(key, c.get().pick().building(), c.get().pick().god(), player.getUUID(),
                player.getGameProfile().getName(), ctx.today());
        s.nextProject = next;
        String name = nameArg(next);
        s.log(ctx.today(), "mythic_age.village.log.project_nominated", next.playerName(), name);
        announce(ctx, Component.translatable("mythic_age.village.project.nominated", next.playerName(), name(next)));
        VillageEconomy.get(player.server).setDirty();
        if (s.activeProject().isEmpty()) {
            Construction.advance(ctx, false);
        }
        return Outcome.of(Result.OK);
    }

    /** 清空「下一个」位。 */
    public static Outcome clear(ServerPlayer player, VillageContext ctx) {
        int have = contribution(player, ctx);
        int need = threshold(player, ctx);
        if (have < need) {
            return new Outcome(Result.LOW_CONTRIBUTION, need, have, "");
        }
        VillageEconomy.State s = ctx.state();
        if (s.nextProject != null) {
            String name = nameArg(s.nextProject);
            s.nextProject = null;
            s.log(ctx.today(), "mythic_age.village.log.project_next_cleared", player.getGameProfile().getName(), name);
            VillageEconomy.get(player.server).setDirty();
        }
        return Outcome.of(Result.CLEARED);
    }

    /** 点名的项目开工了。 */
    static void started(VillageContext ctx, ProjectQueue.Next next) {
        announce(ctx, Component.translatable("mythic_age.village.project.nominated_started", next.playerName(), name(next)));
    }

    /** 点名的项目开工前变得不能开工：已清空，记一行原因。 */
    static void dropped(VillageContext ctx, ProjectQueue.Next next, String reason) {
        ctx.state().log(ctx.today(), "mythic_age.village.log.project_next_dropped", nameArg(next),
                "@mythic_age.village.project.reason." + reason);
        VillageEconomy.get(ctx.level().getServer()).setDirty();
    }

    static void announce(VillageContext ctx, Component msg) {
        java.util.List<java.util.UUID> to = new ArrayList<>();
        for (ServerPlayer p : io.github.happysnappingturtle.mythicage.player.OnlinePlayers.in(ctx.level())) {
            if (BuildNotices.near(ctx, p)) {
                p.sendSystemMessage(msg);
                to.add(p.getUUID());
            }
        }
        LAST_ANNOUNCE.put(ctx.village().id(), new Announce(msg, List.copyOf(to)));
    }

    /** 只供 GameTest：最近一次提示的内容与收到的玩家（假玩家收不到聊天消息）。 */
    public record Announce(Component message, List<java.util.UUID> to) {
    }

    private static final Map<java.util.UUID, Announce> LAST_ANNOUNCE = new ConcurrentHashMap<>();

    public static Optional<Announce> lastAnnounceForTest(java.util.UUID village) {
        return Optional.ofNullable(LAST_ANNOUNCE.get(village));
    }

    /** 「下一个」位的建筑名（发展记录参数）。 */
    public static String nameArg(ProjectQueue.Next next) {
        BuildingSpec spec = VillageBuildings.all().get(next.building());
        return spec == null ? next.building() : Construction.nameArg(spec, next.god());
    }

    public static Component name(ProjectQueue.Next next) {
        BuildingSpec spec = VillageBuildings.all().get(next.building());
        return spec == null ? Component.literal(next.building()) : Construction.name(spec, next.god());
    }
}
