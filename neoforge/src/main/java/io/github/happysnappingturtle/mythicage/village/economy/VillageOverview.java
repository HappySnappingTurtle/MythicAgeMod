package io.github.happysnappingturtle.mythicage.village.economy;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.village.VillagePopulation;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.quest.VillageQuests;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 村庄总览分页（T24 设计文档 §4.2，12 号 §4.2）：人口与岗位、警报、口粮、在建、协作者、委托摘要。五文明通用。
 */
public final class VillageOverview {
    /** 协作者最多列几名（D7）。 */
    public static final int COLLABORATORS = 8;
    /** 警报看最近几个 MC 日的记录。 */
    public static final int ALERT_DAYS = 2;
    /** 算作警报的记录键（饥荒、移民失踪、停工）。 */
    static final Set<String> ALERT_LOGS = Set.of("mythic_age.village.log.hungry", "mythic_age.village.log.migrant_lost",
            "mythic_age.village.log.project_blocked");

    private VillageOverview() {
    }

    static AffairsView.Overview build(ServerPlayer player, VillageRegistry.Village v, VillageEconomy.State s,
                                      Optional<CivVillageData> data, List<AffairsView.Blocker> blockers) {
        MinecraftServer server = player.server;
        ServerLevel level = server.getLevel(v.bell().dimension());
        if (level == null) {
            return AffairsView.Overview.EMPTY;
        }
        Map<String, Integer> jobs = new LinkedHashMap<>();
        s.roster.values().forEach(m -> jobs.merge(m.profession(), 1, Integer::sum));
        List<AffairsView.Count> jobCounts = new ArrayList<>();
        jobs.forEach((k, n) -> jobCounts.add(new AffairsView.Count(VillageAffairs.professionKey(k), false, n)));
        jobCounts.sort(Comparator.comparingInt(AffairsView.Count::n).reversed().thenComparing(AffairsView.Count::key));
        AffairsView.Line food = data.map(d -> AffairsView.Line.of("mythic_age.village.overview.food",
                String.valueOf((int) d.rations().total(s.stock)), String.valueOf(d.rations().floor())))
                .orElse(AffairsView.Line.of(""));
        List<AffairsView.Line> project = VillageAffairs.projectLines(s).stream()
                .filter(l -> !l.key().equals("mythic_age.village.project.missing")).toList();
        int quests = 0;
        int publicQuests = 0;
        List<AffairsView.Count> collaborators = List.of();
        Optional<VillageQuests.Book> book = VillageQuests.get(server).peek(v.id());
        if (book.isPresent()) {
            for (Quest q : book.get().quests) {
                if (!q.state.ended()) {
                    quests++;
                    if (q.scope == Quest.Scope.PUBLIC) {
                        publicQuests++;
                    }
                }
            }
            collaborators = collaborators(server, book.get().contribution);
        }
        return new AffairsView.Overview(v.stage().id(), s.roster.size(), VillagePopulation.freeBeds(level, v), s.soldiers.size(),
                VillagePopulation.guardCap(level, v, v.stage()), List.copyOf(jobCounts), alerts(server, v.id(), s), food,
                List.copyOf(project), collaborators, quests, publicQuests);
    }

    /** 贡献 &gt; 0 的前 {@value #COLLABORATORS} 名，按贡献降序；名字取服务器的玩家资料缓存，查不到就不列。 */
    public static List<AffairsView.Count> collaborators(MinecraftServer server, Map<UUID, Integer> contribution) {
        return collaborators(contribution, id -> name(server, id));
    }

    /** 名字查找可替换（GameTest 服务器没有玩家资料缓存）。 */
    public static List<AffairsView.Count> collaborators(Map<UUID, Integer> contribution,
                                                        java.util.function.Function<UUID, Optional<String>> names) {
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(contribution.entrySet());
        sorted.removeIf(e -> e.getValue() <= 0);
        sorted.sort(Map.Entry.<UUID, Integer>comparingByValue().reversed());
        List<AffairsView.Count> out = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : sorted) {
            if (out.size() >= COLLABORATORS) {
                break;
            }
            names.apply(e.getKey()).ifPresent(n -> out.add(new AffairsView.Count(n, true, e.getValue())));
        }
        return List.copyOf(out);
    }

    static Optional<String> name(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return Optional.of(online.getGameProfile().getName());
        }
        var cache = server.getProfileCache();
        return cache == null ? Optional.empty() : cache.get(id).map(com.mojang.authlib.GameProfile::getName);
    }

    public static List<AffairsView.Line> alerts(MinecraftServer server, UUID village, VillageEconomy.State s) {
        List<AffairsView.Line> out = new ArrayList<>();
        if (VillageScheduler.runtime(village).alarmed(WorldClock.now(server))) {
            out.add(AffairsView.Line.of("mythic_age.village.overview.alarm"));
        }
        long today = MythDays.today(server);
        for (int i = s.log.size() - 1; i >= 0 && out.size() < 4; i--) {
            VillageEconomy.LogEntry e = s.log.get(i);
            if (today - e.day() >= ALERT_DAYS) {
                break;
            }
            if (ALERT_LOGS.contains(e.key())) {
                List<String> args = new ArrayList<>();
                args.add("@@" + e.key());
                args.addAll(e.args());
                out.add(new AffairsView.Line("mythic_age.village.overview.alert", List.copyOf(args)));
            }
        }
        return List.copyOf(out);
    }
}
