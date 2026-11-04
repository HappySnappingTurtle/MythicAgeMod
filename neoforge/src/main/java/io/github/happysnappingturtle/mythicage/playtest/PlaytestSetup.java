package io.github.happysnappingturtle.mythicage.playtest;

import io.github.happysnappingturtle.mythicage.bestiary.Bestiary;
import io.github.happysnappingturtle.mythicage.boss.TrialRewards;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.playtest.PlaytestStations;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.handbook.Handbook;
import io.github.happysnappingturtle.mythicage.player.ModAttachments;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.PlayerDataNbt;
import io.github.happysnappingturtle.mythicage.rite.RiteCommand;
import io.github.happysnappingturtle.mythicage.rite.RiteTables;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.summon.SummonRegistry;
import io.github.happysnappingturtle.mythicage.summon.SummonService;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.quest.QuestService;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 进站的清场与准备（GA-3 设计文档 §4.2、§4.4）。神庙由 {@link PlaytestTemple} 另搭，位置由 {@link PlaytestJobs} 安排。
 */
public final class PlaytestSetup {
    static final Civilization CIV = Civilization.GREEK;
    static final String HANDBOOK_RECEIPT = Handbook.RECEIPT;
    static final String KNOSSOS = "greek:knossos";
    static final String MINOTAUR_ENTRY = "greek/minotaur";

    private PlaytestSetup() {
    }

    /**
     * 清场：第一次时存快照；神话数据换新（保留手札回执，免得重新登录再发一本）；撤销本模组进度；
     * 拆掉名下的祭坛、遣散召唤物；生存模式、满血、清效果。不清背包。
     *
     * @return 这次是否第一次进站（新存了快照）
     */
    public static boolean reset(ServerPlayer p, PlaytestState state) {
        boolean first = state.snapshotIfAbsent(p.getUUID(), PlayerDataNbt.write(MythData.get(p)));
        boolean hadBook = MythData.get(p).receipts().contains(ReceiptBook.Scope.UNIQUE, HANDBOOK_RECEIPT);
        PlayerMythData fresh = new PlayerMythData();
        // 版本号接着往上走：客户端丢弃版本号不比已有的新的同步包
        fresh.setDataVersion(MythData.get(p).dataVersion() + 1);
        if (hadBook) {
            fresh.receipts().record(ReceiptBook.Scope.UNIQUE, HANDBOOK_RECEIPT);
        }
        p.setData(ModAttachments.PLAYER_DATA, fresh);
        MythData.changed(p);
        revokeAdvancements(p);
        removeShrines(p);
        SummonRegistry reg = SummonRegistry.get(p.server);
        for (SummonRegistry.Unit u : List.copyOf(reg.byOwner(p.getUUID()))) {
            SummonService.dismiss(p.server, u.unitId);
        }
        p.setGameMode(GameType.SURVIVAL);
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(20);
        p.clearFire();
        return first;
    }

    static void revokeAdvancements(ServerPlayer p) {
        for (AdvancementHolder h : p.server.getAdvancements().getAllAdvancements()) {
            if (!h.id().getNamespace().equals(MythicAgeIds.MOD_ID)) {
                continue;
            }
            AdvancementProgress prog = p.getAdvancements().getOrStartProgress(h);
            List<String> done = new ArrayList<>();
            prog.getCompletedCriteria().forEach(done::add);
            for (String c : done) {
                p.getAdvancements().revoke(h, c);
            }
        }
    }

    /** 名下的祭坛：已载入的连方块拆掉（走正常的移除钩子），没载入的只删登记。 */
    static void removeShrines(ServerPlayer p) {
        ShrineRegistry reg = ShrineRegistry.get(p.server);
        for (ShrineRegistry.Entry e : List.copyOf(reg.all())) {
            if (!e.owner().equals(p.getUUID())) {
                continue;
            }
            ServerLevel level = p.server.getLevel(e.pos().dimension());
            if (level != null && level.isLoaded(e.pos().pos())) {
                level.removeBlock(e.pos().pos(), false);
            }
            reg.remove(e.pos());
        }
    }

    /** 只供 GameTest 清场。 */
    public static void removeShrinesForTest(ServerPlayer p) {
        removeShrines(p);
    }

    /** 恢复快照；没有快照时返回 false。 */
    public static boolean restore(ServerPlayer p, PlaytestState state) {
        return state.takeSnapshot(p.getUUID()).map(tag -> {
            revokeAdvancements(p);
            PlayerMythData restored = PlayerDataNbt.read(tag);
            restored.setDataVersion(Math.max(restored.dataVersion(), MythData.get(p).dataVersion()) + 1);
            p.setData(ModAttachments.PLAYER_DATA, restored);
            MythData.changed(p);
            return true;
        }).orElse(false);
    }

    /**
     * 按站点规格准备神话状态与物品（神庙除外）。晋升要在村庄里做（同时推进该村），所以调用方先把玩家传到村里。
     *
     * @return 找不到的物品 id（正常为空）
     */
    public static List<String> apply(ServerPlayer p, PlaytestStations.Spec spec) {
        List<String> missing = new ArrayList<>();
        if (spec.age().atLeast(Age.ARCHAIC)) {
            TradeService.grantBlessing(p, CIV, spec.altarGift());
        }
        while (MythData.get(p).civ(CIV).age().ordinal() < spec.age().ordinal()) {
            RiteCommand.promoteDirect(p, CIV, MythData.get(p).civ(CIV).age() == Age.ARCHAIC ? spec.god() : "");
        }
        if (spec.firstKill()) {
            TrialRewards.grant(p, KNOSSOS, "trial:playtest:" + UUID.randomUUID());
            Bestiary.record(p, MINOTAUR_ENTRY, BestiaryRules.Tier.DEFEATED);
        }
        if (spec.favor() > 0) {
            PlayerMythData.CivState s = MythData.get(p).civ(CIV);
            s.setFavor(Math.min(spec.favor(), MythData.cap(p, CIV)));
            MythData.changed(p);
        }
        List<PlaytestStations.Give> items = new ArrayList<>(spec.items());
        items.addAll(PlaytestStations.offerings(spec.offerings(), RiteTables.promotion(CIV)));
        for (PlaytestStations.Give g : items) {
            ItemStack st = TradeService.stack(g.item(), g.count());
            if (st.isEmpty()) {
                missing.add(g.item());
                continue;
            }
            // 超过一组的分几组给
            for (int left = g.count(); left > 0; ) {
                int n = Math.min(left, st.getMaxStackSize());
                TradeService.give(p, st.copyWithCount(n));
                left -= n;
            }
        }
        if (spec.handbook()) {
            Handbook.giveOnce(p);
        }
        return missing;
    }

    /**
     * 让村里有一件可接的委托：已有就不动；否则先按村庄自己的规则开一项工程再扫；还没有就把口粮取空再扫（缺粮委托）。
     *
     * @return 开放中的委托数
     */
    public static int ensureQuest(ServerLevel level, UUID village) {
        if (open(level, village) > 0) {
            return open(level, village);
        }
        VillageScheduler.contextOf(level, village).ifPresent(ctx -> {
            if (ctx.state().activeProject().isEmpty()) {
                Construction.daily(ctx);
            }
        });
        QuestService.scanNow(level.getServer(), village);
        if (open(level, village) > 0) {
            return open(level, village);
        }
        VillageEconomy.State st = VillageEconomy.get(level.getServer()).state(village);
        VillageScheduler.contextOf(level, village).map(ctx -> ctx.data()).ifPresent(data ->
                data.rations().portions().keySet().forEach(item -> st.stock.take(item, Integer.MAX_VALUE, 0)));
        QuestService.scanNow(level.getServer(), village);
        return open(level, village);
    }

    static int open(ServerLevel level, UUID village) {
        return (int) QuestService.quests(level.getServer(), village).stream().filter(q -> q.state == QuestState.OPEN).count();
    }
}
