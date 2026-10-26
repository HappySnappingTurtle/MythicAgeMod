package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestCivRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestRules;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import io.github.happysnappingturtle.mythicage.core.village.quest.RescueRules;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 敌袭救援（T21 设计文档 §8）：事件任务，来源 {@code rescue:<警戒场次>:<居民>}，不用接、不占名额。
 * <ul>
 *   <li>警戒中，{@link ThreatWatch} 记着有敌对生物盯上或伤了本村居民（含在途移民，不含在岗士兵）→ 发任务，提示
 *       {@code rescueRadius} 格内的玩家「X 遇险」；</li>
 *   <li>威胁死了 → 按致死一击与过半伤害判谁救下（{@link RescueRules}），对他是真实危险的才记一份；</li>
 *   <li>警戒结束、居民活着 → 有人救下：完成（结算时善 {@code rescueResident}，同一场警戒同一居民一张收据）；没人救下：失效；</li>
 *   <li>居民死了 → 失败。</li>
 * </ul>
 */
final class RescueQuests {
    static final String TYPE = "rescue";
    static final String PREFIX = "rescue:";
    static final String PRICE = "rescue";

    private RescueQuests() {
    }

    /** 每 20 刻：发新的救援任务、按死掉的威胁判谁救下、警戒结束时收尾；清掉不用的威胁记录。 */
    static void step(VillageContext ctx, VillageQuests.Book book, VillagesConfig.Quests cfg, long now) {
        boolean alarmed = ctx.rt().alarmed(now);
        long serial = ctx.rt().alertSerial();
        Set<UUID> inUse = new HashSet<>();
        if (cfg.enabled() && alarmed) {
            for (ThreatWatch.Watch w : ThreatWatch.inVillage(ctx.village().id())) {
                Optional<ResidentEntity> ward = ward(ctx, w.ward);
                if (ward.isEmpty() || ward.get().defending() || !threatened(ctx, w, ward.get(), now)) {
                    continue;
                }
                String source = PREFIX + serial + ":" + w.ward;
                if (!QuestRules.openSource(book.quests, source) && book.quests.stream().noneMatch(q -> q.source.equals(source))) {
                    publish(ctx, book, source, ward.get(), serial, cfg);
                }
            }
        }
        for (Quest q : book.quests) {
            if (!q.type.equals(TYPE) || q.state != QuestState.OPEN) {
                continue;
            }
            UUID id = UUID.fromString(q.target);
            inUse.add(id);
            ThreatWatch.of(id).ifPresent(w -> judge(ctx, q, w));
            if (!alarmed || serial != q.vars.getOrDefault("serial", serial)) {
                finish(ctx, q);
            }
        }
        ThreatWatch.prune(ctx.level(), ctx.village().id(), inUse, now);
    }

    /** 还有活着的、仍盯着这名居民（或刚伤过他）的威胁。 */
    private static boolean threatened(VillageContext ctx, ThreatWatch.Watch w, ResidentEntity ward, long now) {
        for (ThreatWatch.Threat t : w.threats.values()) {
            Entity e = ctx.level().getEntity(t.id);
            if (t.dead || !(e instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            if (mob.getTarget() == ward) {
                t.lastSeen = now;
            }
            if (now - t.lastSeen <= ThreatWatch.WINDOW) {
                return true;
            }
        }
        return false;
    }

    static void publish(VillageContext ctx, VillageQuests.Book book, String source, ResidentEntity ward, long serial,
                        VillagesConfig.Quests cfg) {
        QuestCivRules rules = ctx.data().quests();
        Quest q = new Quest(book.next++, TYPE, source, ward.getUUID().toString(), Quest.Scope.EVENT, ctx.today(), 1, -1);
        q.label = ward.getCustomName() == null ? "" : ward.getCustomName().getString();
        q.prices.put(PRICE, (double) rules.rescueReward());
        q.vars.put("serial", serial);
        book.quests.add(q);
        double r2 = (double) cfg.rescueRadius() * cfg.rescueRadius();
        for (ServerPlayer p : io.github.happysnappingturtle.mythicage.player.OnlinePlayers.in(ctx.level())) {
            if (p.distanceToSqr(ward) <= r2) {
                p.displayClientMessage(Component.translatable("mythic_age.quest.hint.rescue", q.label), true);
            }
        }
    }

    /** 死掉的威胁：判谁救下，对他是真实危险的记一份（同一任务同一人只记一次）。 */
    static void judge(VillageContext ctx, Quest q, ThreatWatch.Watch w) {
        QuestCivRules rules = ctx.data().quests();
        for (ThreatWatch.Threat t : w.threats.values()) {
            if (!t.dead || t.judged) {
                continue;
            }
            t.judged = true;
            RescueRules.Danger danger = ThreatWatch.danger(w, t);
            for (UUID p : RescueRules.rescuers(t.byPlayer, t.total, t.killer)) {
                if (danger.realFor(p)) {
                    q.creditOnce(p, q.prices.getOrDefault(PRICE, 0.0), rules.contribution().rescue());
                }
            }
        }
    }

    /** 警戒结束：居民活着，有人救下就完成，没有就失效（士兵打死的、威胁自己走掉的，居民没事）。 */
    static void finish(VillageContext ctx, Quest q) {
        UUID id = UUID.fromString(q.target);
        if (!q.contrib.isEmpty()) {
            q.filled = true;
            q.done = 1;
            q.move(QuestState.COMPLETE, ctx.today(), "");
        } else {
            q.move(QuestState.INVALID, ctx.today(), "no_rescuer");
        }
        ThreatWatch.forget(id);
    }

    static Optional<ResidentEntity> ward(VillageContext ctx, UUID id) {
        Entity e = ctx.level().getEntity(id);
        return e instanceof ResidentEntity r && r.isAlive() ? Optional.of(r) : Optional.empty();
    }

    /** 善恶收据（§3.3）：同一场警戒救同一人只算一次。 */
    static String conductReceipt(UUID village, Quest q) {
        return "rescue:" + village + ":" + q.source.substring(PREFIX.length());
    }

    static AffairsView.Line title(Quest q) {
        return AffairsView.Line.of("mythic_age.quest.title.rescue", q.label);
    }
}
