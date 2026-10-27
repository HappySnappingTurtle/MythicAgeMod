package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleSurvey;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleVerdict;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.runtime.SettlementEngine;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.shrine.inspect.ShrineSurvey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 希腊神庙填通用神坛检视（T8a 设计文档 §4；T24 §5.1）：下一级逐项条件、祈祷位与信徒、预计日产、离开时的记账、高亮分组。
 */
public final class GreekShrineSurvey {
    /** 布尔条件：露天祭坛、台基可达。 */
    static final java.util.Set<String> BOOLEAN = java.util.Set.of("openAltar", "platformAccess");

    private GreekShrineSurvey() {
    }

    public static ShrineSurvey of(GreekAltarBlockEntity be, ServerPlayer viewer, TempleRules rules) {
        TempleSurvey s = be.survey();
        TempleVerdict v = be.verdict(rules);
        TempleRules.Tier granted = rules.tier(v.grantedLevel());
        int found = be.prayerSlots(rules).size();
        List<ShrineSurvey.Row> rows = new ArrayList<>();
        if (v.structuralLevel() < rules.maxLevel()) {
            TempleRules.Requirement r = rules.tier(v.structuralLevel() + 1).require();
            row(rows, "columns", s.columns(), r.columns());
            if (r.openAltar()) {
                rows.add(new ShrineSurvey.Row("openAltar", s.openAltar() ? 1 : 0, 1, true));
            }
            if (r.roof()) {
                rows.add(new ShrineSurvey.Row("roof", s.roofCovered(), rules.roof().need(s.roofArea()), false));
            }
            if (r.crepidoma()) {
                rows.add(new ShrineSurvey.Row("raisedColumns", s.raisedColumns(), r.columns(), false));
                rows.add(new ShrineSurvey.Row("platformAccess", s.platformReachable() ? 1 : 0, 1, true));
            }
            row(rows, "litBraziers", s.litBraziers(), r.litBraziers());
            row(rows, "statues", s.statues(), r.statues());
            row(rows, "pediments", s.pediments(), r.pediments());
            row(rows, "marbleFloor", s.marbleFloor(), r.marbleFloor());
        } else {
            for (String k : List.of("columns", "raisedColumns", "roof", "litBraziers", "statues", "pediments", "marbleFloor")) {
                int have = switch (k) {
                    case "columns" -> s.columns();
                    case "raisedColumns" -> s.raisedColumns();
                    case "roof" -> s.roofCovered();
                    case "litBraziers" -> s.litBraziers();
                    case "statues" -> s.statues();
                    case "pediments" -> s.pediments();
                    default -> s.marbleFloor();
                };
                rows.add(new ShrineSurvey.Row(k, have, 0, false));
            }
        }
        List<String> tiers = rules.tiers().stream().map(t -> "mythic_age.temple.tier." + t.name()).toList();
        PrayerService.Stats stats = PrayerService.stats(be, rules);
        List<ShrineSurvey.Note> notes = new ArrayList<>();
        if (stats.rainHalved()) {
            notes.add(ShrineSurvey.Note.of("mythic_age.altar.rain", true));
        }
        if (stats.tooClose() > 0) {
            notes.add(ShrineSurvey.Note.of("mythic_age.altar.too_close", true, String.valueOf(stats.tooClose())));
        }
        if (stats.unreachable()) {
            notes.add(ShrineSurvey.Note.of("mythic_age.altar.unreachable", true));
        }
        if (stats.favorFull()) {
            notes.add(ShrineSurvey.Note.of("mythic_age.altar.favor_full", false));
        }
        BlockPos p = be.getBlockPos();
        BlockPos[] bounds = TempleScan.bounds(p, be.getBlockState().getValue(GreekAltarBlock.FACING), rules.scan());
        TempleScan.Highlights h = be.highlights();
        List<ShrineSurvey.Highlight> hs = new ArrayList<>();
        if (h != null) {
            highlight(hs, "roof", ShrineSurvey.Shape.COLUMN, 0xFF3333, h.uncoveredRoof());
            highlight(hs, "cover", ShrineSurvey.Shape.CROSS, 0xFF3333, h.altarCover());
            highlight(hs, "short", ShrineSurvey.Shape.DASHED, 0xFFE619, h.shortColumns());
            highlight(hs, "unlit", ShrineSurvey.Shape.CROSS, 0xFF8C19, h.unlitBraziers());
            highlight(hs, "counted", ShrineSurvey.Shape.CELL, 0x33FF4D, h.countedColumns());
        }
        highlight(hs, "slot", ShrineSurvey.Shape.FLAT, 0x4D99FF, be.prayerSlots(rules));
        boolean isOwner = be.owner().map(id -> id.equals(viewer.getUUID())).orElse(false);
        return new ShrineSurvey(Civilization.GREEK, p, be.ownerName(), isOwner, be.ownerAge(), be.hasResult(), be.unverified(),
                v.structuralLevel(), v.grantedLevel(), tiers, granted.coefficient(), granted.prayerSlots(), found, List.copyOf(rows),
                v.ageRequired().map(Age::id).orElse(""), stats.estimate(),
                new ShrineSurvey.Prayer(stats.praying(), stats.believers(), stats.today(), List.copyOf(notes)),
                away(viewer, be), bounds[0], bounds[1], List.copyOf(hs));
    }

    /** 离开时的记账：本神坛的私人产出台账（T5）。 */
    static ShrineSurvey.Away away(ServerPlayer viewer, GreekAltarBlockEntity be) {
        GlobalPos key = GlobalPos.of(be.getLevel().dimension(), be.getBlockPos());
        RuntimeConfig.Favor cfg = ModConfigs.runtime().current().favor();
        long now = WorldClock.now(viewer.server);
        return RuntimeLedgers.get(viewer.server).get(PrayerService.ledgerId(key))
                .map(l -> new ShrineSurvey.Away(true, SettlementEngine.snapshotDailyRate(l, cfg.snapshot()), l.verified(),
                        SettlementEngine.snapshotUsable(l, now, cfg.snapshot()), l.offlineBuffer()))
                .orElse(ShrineSurvey.Away.NONE);
    }

    private static void row(List<ShrineSurvey.Row> rows, String key, int have, int need) {
        if (need > 0) {
            rows.add(new ShrineSurvey.Row(key, have, need, BOOLEAN.contains(key)));
        }
    }

    private static void highlight(List<ShrineSurvey.Highlight> out, String kind, ShrineSurvey.Shape shape, int rgb,
                                  List<BlockPos> cells) {
        if (!cells.isEmpty()) {
            out.add(new ShrineSurvey.Highlight(kind, shape, 0xFF000000 | rgb,
                    cells.size() > TempleScan.HIGHLIGHT_LIMIT ? cells.subList(0, TempleScan.HIGHLIGHT_LIMIT) : cells));
        }
    }
}
