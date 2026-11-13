package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.OfferingLoader;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.runtime.SettlementEngine;
import io.github.happysnappingturtle.mythicage.core.village.temple.TempleStanding;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.shrine.inspect.ShrineSurvey;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 村庄神庙的检视（GA-13 §2.8）：沿用通用神坛检视的外壳，填村庄神庙的等级、系数、完好度、你在这里的信徒与恩宠、
 * 献供状态、供奉者。等级靠村子扩建卫城提升，不再逐项列结构要求；缺失项只列完好度（晋升要 ≥ 80%）。
 */
public final class VillageTempleSurvey {
    private VillageTempleSurvey() {
    }

    public static void send(ServerPlayer viewer, VillageTemple t) {
        if (viewer.connection != null && !(viewer instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            PacketDistributor.sendToPlayer(viewer, of(viewer, t));
        }
    }

    public static ShrineSurvey of(ServerPlayer viewer, VillageTemple t) {
        TempleRules rules = TempleService.rules();
        TemplePrayer.Stats st = TemplePrayer.stats(viewer.server, t, viewer.getUUID());
        List<String> tiers = rules.tiers().stream().map(x -> "mythic_age.temple.tier." + x.name()).toList();
        int integrity = (int) Math.round(t.integrity() * 100);
        List<ShrineSurvey.Row> rows = new ArrayList<>();
        rows.add(new ShrineSurvey.Row("integrity", integrity, (int) Math.round(TempleStanding.PROMOTION_INTEGRITY * 100), false));
        List<ShrineSurvey.Note> notes = new ArrayList<>();
        notes.add(ShrineSurvey.Note.of("mythic_age.altar.today_believers", false, String.valueOf(st.todayBelievers())));
        String percent = "+" + Math.round(OfferingLoader.greek().bonus() * 100) + "%";
        if (!st.devotee()) {
            notes.add(ShrineSurvey.Note.of("mythic_age.altar.not_devotee", true));
        } else {
            notes.add(st.offered() ? ShrineSurvey.Note.of("mythic_age.altar.offered", false, percent)
                    : ShrineSurvey.Note.of("mythic_age.altar.offer_hint", false, percent));
        }
        if (st.rainHalved()) {
            notes.add(ShrineSurvey.Note.of("mythic_age.altar.rain", true));
        }
        if (st.tooClose() > 0) {
            notes.add(ShrineSurvey.Note.of("mythic_age.altar.too_close", true, String.valueOf(st.tooClose())));
        }
        if (st.unreachable()) {
            notes.add(ShrineSurvey.Note.of("mythic_age.altar.unreachable", true));
        }
        if (st.favorFull()) {
            notes.add(ShrineSurvey.Note.of("mythic_age.altar.favor_full", false));
        }
        List<ShrineSurvey.Highlight> hs = new ArrayList<>();
        if (!t.prayerSlots().isEmpty()) {
            hs.add(new ShrineSurvey.Highlight("slot", ShrineSurvey.Shape.FLAT, 0xFF4D99FF, t.prayerSlots()));
        }
        BlockPos a = t.altar().pos();
        String patron = VillageRegistry.get(viewer.server).byId(t.village()).map(VillageRegistry.Village::patron).orElse("");
        return new ShrineSurvey(Civilization.GREEK, a, "", st.devotee(), MythData.get(viewer).civ(Civilization.GREEK).age(),
                true, false, t.level(), t.level(), tiers, t.coefficient(), t.slotLimit(), t.prayerSlots().size(), List.copyOf(rows),
                "", st.estimate(), new ShrineSurvey.Prayer(st.praying(), st.believers(), st.today(), List.copyOf(notes)),
                away(viewer, t), a, a, List.copyOf(hs),
                new ShrineSurvey.Temple(true, patron, integrity, st.devotee(), st.devotees()));
    }

    /** 离开时的记账：这位玩家在这个村子的台账（T5）。 */
    static ShrineSurvey.Away away(ServerPlayer viewer, VillageTemple t) {
        RuntimeConfig.Favor cfg = ModConfigs.runtime().current().favor();
        long now = WorldClock.now(viewer.server);
        return RuntimeLedgers.get(viewer.server).get(TemplePrayer.ledgerId(t.village(), viewer.getUUID()))
                .map(l -> new ShrineSurvey.Away(true, SettlementEngine.snapshotDailyRate(l, cfg.snapshot()), l.verified(),
                        SettlementEngine.snapshotUsable(l, now, cfg.snapshot()), l.offlineBuffer()))
                .orElse(ShrineSurvey.Away.NONE);
    }
}
