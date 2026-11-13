package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestCivRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.core.village.quest.RepairRules;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 修复设施（T21 设计文档 §6）：来源 {@code repair:<建筑盒>}。接了任务的玩家在损坏格放回同一种方块，逐格核验、记到他名下；
 * 他自己打掉的格算修好但不给报酬；到期或没人接的损坏转给村民自修（{@link RepairJob}，D3）。
 */
final class RepairQuests {
    static final String TYPE = "repair";
    static final String PREFIX = "repair:";
    static final String PRICE = "repair";
    /** 接了任务的人能看到多远的损坏格粒子。 */
    static final int MARK_RADIUS = 32;
    static final int MARK_LIMIT = 64;

    private RepairQuests() {
    }

    // ───────────── 候选与发布 ─────────────

    /** 日开始：到期没人修的格转自修；够门槛的建筑发修复任务（§4.2）。 */
    static List<QuestRules.Candidate> candidates(VillageContext ctx, VillageQuests.Book book, VillagesConfig.Quests cfg) {
        sync(ctx, book);
        List<QuestRules.Candidate> out = new ArrayList<>();
        long today = ctx.today();
        for (VillageQuests.Damage d : book.damage.values()) {
            boolean questOpen = QuestRules.openSource(book.quests, PREFIX + d.key);
            List<VillageQuests.Cell> waiting = new ArrayList<>();
            for (VillageQuests.Cell c : d.cells.values()) {
                if (!c.self && !questOpen && RepairRules.selfRepairDue(c.day, today, cfg.repairDays())) {
                    c.self = true;
                }
                if (!c.self) {
                    waiting.add(c);
                }
            }
            boolean key = waiting.stream().anyMatch(c -> keyBlock(ctx, c.state));
            if (!questOpen && RepairRules.shouldPublish(waiting.size(), key, cfg.repairMinCells())) {
                out.add(new QuestRules.Candidate(PREFIX + d.key, 80 + waiting.size()));
            }
        }
        book.damage.values().removeIf(d -> d.cells.isEmpty() && d.lastDay.isEmpty());
        return out;
    }

    /** 门、床、工作站（本文明配方用到的工作站方块）。 */
    static boolean keyBlock(VillageContext ctx, BlockState st) {
        if (st.is(BlockTags.DOORS) || st.is(BlockTags.BEDS)) {
            return true;
        }
        String id = BuildingDamage.id(st);
        for (VillageRecipe r : ctx.data().recipes().values()) {
            if (id.equals(r.station())) {
                return true;
            }
        }
        return false;
    }

    static Optional<Quest> create(VillageContext ctx, VillageQuests.Book book, int number, String source, VillagesConfig.Quests cfg) {
        VillageQuests.Damage d = book.damage.get(source.substring(PREFIX.length()));
        if (d == null) {
            return Optional.empty();
        }
        int waiting = (int) d.cells.values().stream().filter(c -> !c.self).count();
        if (waiting == 0) {
            return Optional.empty();
        }
        QuestCivRules rules = ctx.data().quests();
        Quest q = new Quest(number, TYPE, source, d.key, Quest.Scope.PUBLIC, ctx.today(), waiting, ctx.today() + cfg.repairDays());
        q.prices.put(PRICE, rules.repairPrice());
        return Optional.of(q);
    }

    // ───────────── 刷新 ─────────────

    /** 损坏格已经被别的途径恢复原状（别人放的、村民自修的）：去掉，不记到任何人名下。 */
    static void sync(VillageContext ctx, VillageQuests.Book book) {
        ServerLevel level = ctx.level();
        for (VillageQuests.Damage d : book.damage.values()) {
            d.cells.values().removeIf(c -> {
                BlockPos p = BlockPos.of(c.pos);
                return level.isLoaded(p) && BuildingDamage.id(level.getBlockState(p)).equals(c.block);
            });
        }
    }

    /** 建筑盒还在不在规划里（被整座拆掉、重建时不在了）。 */
    static boolean siteGone(VillageContext ctx, Quest q) {
        return ctx.plan().info().built().stream().noneMatch(b -> VillageQuests.Damage.key(b).equals(q.target));
    }

    static void refresh(VillageContext ctx, VillageQuests.Book book, Quest q, long today) {
        sync(ctx, book);
        VillageQuests.Damage d = book.damage.get(q.target);
        if (siteGone(ctx, q)) {
            q.move(QuestState.INVALID, today, "site");
            return;
        }
        int rem = d == null ? 0 : (int) d.cells.values().stream().filter(c -> !c.self).count();
        q.need = q.done + rem;
        if (rem == 0) {
            q.move(q.filled ? QuestState.COMPLETE : QuestState.INVALID, today, q.filled ? "" : "satisfied");
            return;
        }
        if (QuestRules.expired(q, today)) {
            q.move(QuestState.EXPIRED, today, "expired");
            d.cells.values().forEach(c -> c.self = true);
        }
    }

    // ───────────── 放回核验 ─────────────

    /**
     * 玩家在损坏格放了方块（§6.3）：同一种方块就算修好；接了这座建筑的修复任务的人记到名下（自己打掉的、超出每日上限的不给报酬）。
     * 返回是否是一格修复（不是的话调用方把它记成玩家方块）。
     */
    static boolean onPlaced(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState placed, BuildingDamage.Located loc) {
        VillageQuests all = VillageQuests.get(level.getServer());
        VillageQuests.Book book = all.book(loc.village().id());
        VillageQuests.Damage d = book.damage.get(VillageQuests.Damage.key(loc.box()));
        VillageQuests.Cell cell = d == null ? null : d.cells.get(pos.asLong());
        if (cell == null || !RepairRules.matches(cell.block, BuildingDamage.id(placed))) {
            return false;
        }
        d.cells.remove(pos.asLong());
        io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.invalidate(loc.village().id());
        UUID me = player.getUUID();
        long today = MythDays.today(level.getServer());
        Optional<Quest> q = book.quests.stream().filter(x -> x.type.equals(TYPE) && x.target.equals(d.key)
                && x.state == QuestState.OPEN && x.accepted.contains(me)).findFirst();
        if (q.isPresent()) {
            Quest quest = q.get();
            quest.done++;
            quest.filled = true;
            VillagesConfig.Quests cfg = QuestService.config();
            if (RepairRules.credit(me, cell.cause, book.repairedToday(me, today), cfg.repairDailyCap())) {
                QuestCivRules rules = io.github.happysnappingturtle.mythicage.village.work.VillageData.civ(loc.village().civ())
                        .map(io.github.happysnappingturtle.mythicage.core.village.CivVillageData::quests).orElse(QuestCivRules.DEFAULT);
                quest.credit(me, quest.prices.getOrDefault(PRICE, 0.0), rules.contribution().repairCell());
                book.addRepaired(me, today);
            }
        }
        all.setDirty();
        QuestService.refreshNow(level.getServer(), loc.village().id());
        return true;
    }

    // ───────────── 粒子标记 ─────────────

    /** 接了修复任务的玩家在 32 格内能看到未修的损坏格（只发给他，§9.2）。 */
    static void showMarks(VillageContext ctx, VillageQuests.Book book) {
        for (Quest q : book.quests) {
            if (!q.type.equals(TYPE) || q.state != QuestState.OPEN || q.accepted.isEmpty()) {
                continue;
            }
            VillageQuests.Damage d = book.damage.get(q.target);
            if (d == null) {
                continue;
            }
            for (UUID id : q.accepted) {
                ServerPlayer p = OnlinePlayers.find(ctx.level().getServer(), id);
                if (p == null || p.level() != ctx.level()) {
                    continue;
                }
                int n = 0;
                for (VillageQuests.Cell c : d.cells.values()) {
                    BlockPos b = BlockPos.of(c.pos);
                    if (c.self || b.distSqr(p.blockPosition()) > MARK_RADIUS * MARK_RADIUS) {
                        continue;
                    }
                    ctx.level().sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5,
                            4, 0.3, 0.3, 0.3, 0);
                    if (++n >= MARK_LIMIT) {
                        break;
                    }
                }
            }
        }
    }

    // ───────────── 文本 ─────────────

    /** 标题：施工建成的建筑用它的名字；生成时就有的建筑用位置。 */
    static AffairsView.Line title(VillageContext ctx, Quest q) {
        VillageEconomy.State s = ctx.state();
        for (VillageEconomy.Built b : s.constructed) {
            if (VillageQuests.Damage.key(b.box()).equals(q.target)) {
                var spec = io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings.all().get(b.building());
                if (spec != null) {
                    return AffairsView.Line.of("mythic_age.quest.title.repair.named",
                            io.github.happysnappingturtle.mythicage.village.build.Construction.nameArg(spec, b.god()));
                }
            }
        }
        String[] k = q.target.split(",");
        int x = (Integer.parseInt(k[0]) + Integer.parseInt(k[3])) / 2;
        int z = (Integer.parseInt(k[2]) + Integer.parseInt(k[5])) / 2;
        return AffairsView.Line.of("mythic_age.quest.title.repair.at", String.valueOf(x), String.valueOf(z));
    }
}
