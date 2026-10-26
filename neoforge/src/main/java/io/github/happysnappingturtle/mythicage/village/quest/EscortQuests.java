package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.village.quest.EscortRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestCivRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 护送在途移民（T21 设计文档 §7）：来源 {@code escort:<移民>}，个人任务（第一个接的人拥有它）。移民照常走进村、不等人（D4）。
 * <ul>
 *   <li>接取起按刻累计：接取者在移民 {@code escortRadius} 格内的刻数 / 总刻数 ≥ {@code escortPresence} 才算一路陪着；</li>
 *   <li>移民走到床位登记 → 陪着的：成功（绿宝石定额 + 贡献）；没陪着的：失效「你没有一路陪着」；没人接的：失效；</li>
 *   <li>移民死了 → 失败；移民 24000 刻没走到被放弃 → 失败；</li>
 *   <li>途中有敌对生物盯上或伤了移民，它对接取者是真实危险（{@link ThreatWatch}），且在移民到达前被打死或丢掉目标 →
 *       成功时另加善 {@code rescueEscortQuest}（结算时发，§7.3）。</li>
 * </ul>
 * 计数存在任务的 {@code vars}：{@code last}（上次采样的刻）、{@code total}、{@code near}、{@code danger}（0 / 1）。
 */
final class EscortQuests {
    static final String TYPE = "escort";
    static final String PREFIX = "escort:";
    static final String PRICE = "escort";
    /** 威胁离开移民多久算「丢掉目标」。 */
    static final long LOST_TICKS = 40;

    private EscortQuests() {
    }

    /** 日开始：有在途移民而没有护送任务的（开关刚打开、移民生成时开放名额满了），补发。 */
    static List<QuestRules.Candidate> candidates(VillageContext ctx, VillageQuests.Book book) {
        UUID migrant = ctx.state().migrant;
        if (migrant == null || migrant(ctx, migrant).isEmpty()) {
            return List.of();
        }
        return List.of(new QuestRules.Candidate(PREFIX + migrant, 100));
    }

    static Optional<Quest> create(VillageContext ctx, int number, String source) {
        UUID id = UUID.fromString(source.substring(PREFIX.length()));
        Optional<ResidentEntity> r = migrant(ctx, id);
        if (r.isEmpty() || !id.equals(ctx.state().migrant)) {
            return Optional.empty();
        }
        QuestCivRules rules = ctx.data().quests();
        Quest q = new Quest(number, TYPE, source, id.toString(), Quest.Scope.PERSONAL, ctx.today(), 1, -1);
        q.label = r.get().getCustomName() == null ? "" : r.get().getCustomName().getString();
        q.prices.put(PRICE, (double) rules.escortReward());
        return Optional.of(q);
    }

    static Optional<ResidentEntity> migrant(VillageContext ctx, UUID id) {
        Entity e = ctx.level().getEntity(id);
        return e instanceof ResidentEntity r && r.isAlive() ? Optional.of(r) : Optional.empty();
    }

    /** 接取或放弃：从这一刻重新计在场（在场比例只看现在的接取者）。 */
    static void restart(Quest q, long now) {
        q.vars.put("start", now);
        q.vars.put("last", now);
        q.vars.put("total", 0L);
        q.vars.put("near", 0L);
        q.vars.put("danger", 0L);
    }

    /** 每 20 刻：累计在场刻数；看途中的威胁有没有被解决。 */
    static void sample(VillageContext ctx, VillageQuests.Book book, VillagesConfig.Quests cfg, long now) {
        for (Quest q : book.quests) {
            if (q.type.equals(TYPE) && q.state == QuestState.OPEN && !q.accepted.isEmpty()) {
                sample(ctx, q, cfg, now);
            }
        }
    }

    static void sample(VillageContext ctx, Quest q, VillagesConfig.Quests cfg, long now) {
        UUID escort = q.accepted.iterator().next();
        UUID id = UUID.fromString(q.target);
        Optional<ResidentEntity> m = migrant(ctx, id);
        long last = q.vars.getOrDefault("last", now);
        long elapsed = now - last;
        if (elapsed > 0) {
            q.vars.merge("total", elapsed, Long::sum);
            ServerPlayer p = OnlinePlayers.find(ctx.level().getServer(), escort);
            if (p != null && m.isPresent() && p.level() == m.get().level()
                    && EscortRules.within(p.distanceToSqr(m.get()), cfg.escortRadius())) {
                q.vars.merge("near", elapsed, Long::sum);
            }
            q.vars.put("last", now);
        }
        if (m.isEmpty()) {
            return;
        }
        ThreatWatch.of(id).ifPresent(w -> {
            for (ThreatWatch.Threat t : w.threats.values()) {
                if (t.escortSeen) {
                    continue;
                }
                Entity e = ctx.level().getEntity(t.id);
                if (e instanceof Mob mob && mob.isAlive() && mob.getTarget() == m.get()) {
                    t.lastSeen = now;
                }
                boolean gone = !(e instanceof Mob mob2) || !mob2.isAlive();
                boolean resolved = t.dead || gone || now - t.lastSeen >= LOST_TICKS;
                // 接取之后才解决的才算这次护送里的危险（接之前就被打死的不算）
                long at = t.dead ? t.deadAt : gone ? now : t.lastSeen + LOST_TICKS;
                if (resolved && at >= q.vars.getOrDefault("start", Long.MIN_VALUE)) {
                    t.escortSeen = true;
                    if (ThreatWatch.danger(w, t).realFor(escort)) {
                        q.vars.put("danger", 1L);
                    }
                }
            }
        });
    }

    /** 刷新：移民登记进名册 → 到达判定；不再是在途移民又没登记（被放弃、消失）→ 失败。 */
    static void refresh(VillageContext ctx, Quest q, long today) {
        UUID id = UUID.fromString(q.target);
        VillageEconomy.State s = ctx.state();
        if (s.roster.containsKey(id)) {
            arrived(ctx, q, ctx.now());
        } else if (!id.equals(s.migrant)) {
            q.move(QuestState.FAILED, today, "lost");
            ThreatWatch.forget(id);
        }
    }

    /** 移民走到了床位（{@code ResidentMigration.Arrive} 登记之后调用，也由刷新兜底）。 */
    static void arrived(VillageContext ctx, Quest q, long now) {
        if (q.state != QuestState.OPEN) {
            return;
        }
        long today = ctx.today();
        if (q.accepted.isEmpty()) {
            q.move(QuestState.INVALID, today, "unescorted");
            ThreatWatch.forget(UUID.fromString(q.target));
            return;
        }
        VillagesConfig.Quests cfg = QuestService.config();
        sample(ctx, q, cfg, now);
        UUID escort = q.accepted.iterator().next();
        ServerPlayer p = OnlinePlayers.find(ctx.level().getServer(), escort);
        if (EscortRules.enough(q.vars.getOrDefault("near", 0L), q.vars.getOrDefault("total", 0L), cfg.escortPresence())) {
            q.creditOnce(escort, q.prices.getOrDefault(PRICE, 0.0), ctx.data().quests().contribution().escort());
            q.filled = true;
            q.done = 1;
            q.move(QuestState.COMPLETE, today, "");
        } else {
            q.move(QuestState.INVALID, today, "absent");
            if (p != null) {
                p.displayClientMessage(Component.translatable("mythic_age.quest.escort.absent", q.label), true);
            }
        }
        ThreatWatch.forget(UUID.fromString(q.target));
    }

    /** 结算时：一路有真实危险的护送另加善。 */
    static boolean dangerous(Quest q) {
        return q.vars.getOrDefault("danger", 0L) == 1L;
    }

    static AffairsView.Line title(Quest q) {
        return AffairsView.Line.of("mythic_age.quest.title.escort", q.label);
    }
}
