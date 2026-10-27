package io.github.happysnappingturtle.mythicage.conduct;

import io.github.happysnappingturtle.mythicage.core.conduct.ConductDisplay;
import io.github.happysnappingturtle.mythicage.core.config.conduct.ConductConfig;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.village.quest.ContributionLog;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.quest.VillageQuests;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 「善恶与贡献」界面（T24 设计文档 §3）：左栏善恶值（分值、显示档、今日正分、最近事件），右栏本人有贡献的村庄与最近来源。
 * 只组装请求者本人的数据（D6）；贡献只是记录，不能兑换（D7）。
 */
public final class ConductViewService {
    /** 每个村庄显示几条最近来源。 */
    public static final int VILLAGE_RECENT = 3;
    public static final long DAY_TICKS = 24000;

    private ConductViewService() {
    }

    public static ConductView build(ServerPlayer player) {
        ConductConfig cfg = ConductService.config();
        PlayerMythData data = MythData.get(player);
        int score = data.conductScore();
        long now = player.level().getGameTime();
        List<ConductView.Event> recent = new ArrayList<>();
        for (PlayerMythData.ConductEntry e : ConductDisplay.newestFirst(data.recentConduct(), ConductDisplay.RECENT)) {
            recent.add(new ConductView.Event(e.reason(), e.delta(), Math.max(0, (now - e.gameTime()) / DAY_TICKS)));
        }
        long today = MythDays.today(player.server);
        UUID me = player.getUUID();
        record Row(ConductView.Village v, int points) {
        }
        List<Row> rows = new ArrayList<>();
        VillageRegistry reg = VillageRegistry.get(player.server);
        for (VillageQuests.Book b : VillageQuests.get(player.server).all()) {
            int points = b.contribution(me);
            if (points <= 0) {
                continue;
            }
            Optional<VillageRegistry.Village> v = reg.byId(b.village);
            if (v.isEmpty()) {
                continue;
            }
            List<ConductView.Event> events = new ArrayList<>();
            for (ContributionLog.Event e : b.contributionLog.recent(me, VILLAGE_RECENT)) {
                events.add(new ConductView.Event(e.reason(), e.points(), Math.max(0, today - e.day())));
            }
            rows.add(new Row(new ConductView.Village(VillageAffairs.titleLine(v.get()), points, List.copyOf(events)), points));
        }
        rows.sort(Comparator.comparingInt(Row::points).reversed());
        return new ConductView(cfg.enabled(), score, cfg.range().min(), cfg.range().max(),
                ConductDisplay.tier(score, cfg.range().min(), cfg.range().max()).id(),
                data.conduct().positiveGranted(today), cfg.positiveDailyCap(), List.copyOf(recent),
                rows.stream().map(Row::v).toList());
    }

    public static void open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ConductPayloads.View(build(player)));
    }

    public static void handleRequest(ConductPayloads.Request payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                open(p);
            }
        });
    }
}
