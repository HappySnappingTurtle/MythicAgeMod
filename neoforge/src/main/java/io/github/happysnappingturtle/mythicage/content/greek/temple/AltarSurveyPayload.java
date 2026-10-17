package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleSurvey;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleVerdict;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：神坛检视（T8a 设计文档 §4）。只携带展示所需的数字与坐标，客户端不做判定。
 *
 * @param rows     下一级（结构等级 + 1）的每一项要求：当前值 / 需要值；已是最高级时为当前各项统计（need = 0）
 * @param estimate 预计日产出（估算，不是发放承诺；T10 起按近 7 日信徒计）
 * @param prayer   信徒祈祷的实时统计（T10 设计文档 §8）
 */
public record AltarSurveyPayload(BlockPos pos, String ownerName, boolean viewerIsOwner, Age ownerAge, boolean hasResult,
                                 boolean unverified, int structuralLevel, int grantedLevel, List<String> tierNames,
                                 double coefficient, int slotsCap, int slotsFound, List<Row> rows, String ageRequired,
                                 double estimate, BlockPos scanMin, BlockPos scanMax, List<BlockPos> uncoveredRoof,
                                 List<BlockPos> countedColumns, List<BlockPos> shortColumns, List<BlockPos> unlitBraziers,
                                 List<BlockPos> prayerSlots, List<BlockPos> altarCover, Prayer prayer)
        implements CustomPacketPayload {

    /** 与 {@link PrayerService.Stats} 一一对应（预计日产在外层的 estimate）。 */
    public record Prayer(int praying, int believers, double today, boolean rainHalved, int tooClose,
                         boolean unreachable, boolean favorFull) {
        static Prayer of(PrayerService.Stats s) {
            return new Prayer(s.praying(), s.believers(), s.today(), s.rainHalved(), s.tooClose(), s.unreachable(),
                    s.favorFull());
        }
    }

    /** 一项条件：have / need；布尔条件（露天祭坛、台基可达）用 0/1 表示。 */
    public record Row(String key, int have, int need) {
        public static final java.util.Set<String> BOOLEAN = java.util.Set.of("openAltar", "platformAccess");

        public boolean isBoolean() {
            return BOOLEAN.contains(key);
        }
    }

    public static final Type<AltarSurveyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "altar_survey"));


    public static AltarSurveyPayload of(GreekAltarBlockEntity be, ServerPlayer viewer, TempleRules rules) {
        TempleSurvey s = be.survey();
        TempleVerdict v = be.verdict(rules);
        TempleRules.Tier granted = rules.tier(v.grantedLevel());
        int found = be.prayerSlots(rules).size();
        List<Row> rows = new ArrayList<>();
        if (v.structuralLevel() < rules.maxLevel()) {
            TempleRules.Requirement r = rules.tier(v.structuralLevel() + 1).require();
            row(rows, "columns", s.columns(), r.columns());
            if (r.openAltar()) {
                rows.add(new Row("openAltar", s.openAltar() ? 1 : 0, 1));
            }
            if (r.roof()) {
                rows.add(new Row("roof", s.roofCovered(), rules.roof().need(s.roofArea())));
            }
            if (r.crepidoma()) {
                rows.add(new Row("raisedColumns", s.raisedColumns(), r.columns()));
                rows.add(new Row("platformAccess", s.platformReachable() ? 1 : 0, 1));
            }
            row(rows, "litBraziers", s.litBraziers(), r.litBraziers());
            row(rows, "statues", s.statues(), r.statues());
            row(rows, "pediments", s.pediments(), r.pediments());
            row(rows, "marbleFloor", s.marbleFloor(), r.marbleFloor());
        } else {
            rows.add(new Row("columns", s.columns(), 0));
            rows.add(new Row("raisedColumns", s.raisedColumns(), 0));
            rows.add(new Row("roof", s.roofCovered(), 0));
            rows.add(new Row("litBraziers", s.litBraziers(), 0));
            rows.add(new Row("statues", s.statues(), 0));
            rows.add(new Row("pediments", s.pediments(), 0));
            rows.add(new Row("marbleFloor", s.marbleFloor(), 0));
        }
        List<String> names = rules.tiers().stream().map(TempleRules.Tier::name).toList();
        PrayerService.Stats stats = PrayerService.stats(be, rules);
        BlockPos p = be.getBlockPos();
        BlockPos[] bounds = TempleScan.bounds(p, be.getBlockState().getValue(GreekAltarBlock.FACING), rules.scan());
        BlockPos min = bounds[0];
        BlockPos max = bounds[1];
        TempleScan.Highlights h = be.highlights();
        boolean isOwner = be.owner().map(id -> id.equals(viewer.getUUID())).orElse(false);
        return new AltarSurveyPayload(p, be.ownerName(), isOwner, be.ownerAge(), be.hasResult(), be.unverified(),
                v.structuralLevel(), v.grantedLevel(), names, granted.coefficient(), granted.prayerSlots(), found, rows,
                v.ageRequired().map(Age::id).orElse(""), stats.estimate(), min, max,
                h == null ? List.of() : h.uncoveredRoof(), h == null ? List.of() : h.countedColumns(),
                h == null ? List.of() : h.shortColumns(), h == null ? List.of() : h.unlitBraziers(),
                be.prayerSlots(rules), h == null ? List.of() : h.altarCover(), Prayer.of(stats));
    }

    private static void row(List<Row> rows, String key, int have, int need) {
        if (need > 0) {
            rows.add(new Row(key, have, need));
        }
    }

    private static void writePositions(FriendlyByteBuf buf, List<BlockPos> list) {
        buf.writeVarInt(list.size());
        list.forEach(buf::writeBlockPos);
    }

    private static List<BlockPos> readPositions(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n > TempleScan.HIGHLIGHT_LIMIT * 2) {
            throw new IllegalArgumentException("高亮坐标过多：" + n);
        }
        List<BlockPos> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(buf.readBlockPos());
        }
        return out;
    }

    public static final StreamCodec<FriendlyByteBuf, AltarSurveyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos());
                buf.writeUtf(p.ownerName());
                buf.writeBoolean(p.viewerIsOwner());
                buf.writeEnum(p.ownerAge());
                buf.writeBoolean(p.hasResult());
                buf.writeBoolean(p.unverified());
                buf.writeVarInt(p.structuralLevel());
                buf.writeVarInt(p.grantedLevel());
                buf.writeVarInt(p.tierNames().size());
                p.tierNames().forEach(buf::writeUtf);
                buf.writeDouble(p.coefficient());
                buf.writeVarInt(p.slotsCap());
                buf.writeVarInt(p.slotsFound());
                buf.writeVarInt(p.rows().size());
                for (Row r : p.rows()) {
                    buf.writeUtf(r.key());
                    buf.writeVarInt(r.have());
                    buf.writeVarInt(r.need());
                }
                buf.writeUtf(p.ageRequired());
                buf.writeDouble(p.estimate());
                buf.writeBlockPos(p.scanMin());
                buf.writeBlockPos(p.scanMax());
                writePositions(buf, p.uncoveredRoof());
                writePositions(buf, p.countedColumns());
                writePositions(buf, p.shortColumns());
                writePositions(buf, p.unlitBraziers());
                writePositions(buf, p.prayerSlots());
                writePositions(buf, p.altarCover());
                Prayer pr = p.prayer();
                buf.writeVarInt(pr.praying());
                buf.writeVarInt(pr.believers());
                buf.writeDouble(pr.today());
                buf.writeBoolean(pr.rainHalved());
                buf.writeVarInt(pr.tooClose());
                buf.writeBoolean(pr.unreachable());
                buf.writeBoolean(pr.favorFull());
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                String owner = buf.readUtf();
                boolean isOwner = buf.readBoolean();
                Age age = buf.readEnum(Age.class);
                boolean hasResult = buf.readBoolean();
                boolean unverified = buf.readBoolean();
                int structural = buf.readVarInt();
                int granted = buf.readVarInt();
                int nNames = buf.readVarInt();
                List<String> names = new ArrayList<>(nNames);
                for (int i = 0; i < nNames; i++) {
                    names.add(buf.readUtf());
                }
                double coefficient = buf.readDouble();
                int cap = buf.readVarInt();
                int found = buf.readVarInt();
                int nRows = buf.readVarInt();
                List<Row> rows = new ArrayList<>(nRows);
                for (int i = 0; i < nRows; i++) {
                    rows.add(new Row(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
                }
                String ageRequired = buf.readUtf();
                double estimate = buf.readDouble();
                BlockPos min = buf.readBlockPos();
                BlockPos max = buf.readBlockPos();
                List<BlockPos> uncovered = readPositions(buf);
                List<BlockPos> counted = readPositions(buf);
                List<BlockPos> shortCols = readPositions(buf);
                List<BlockPos> unlit = readPositions(buf);
                List<BlockPos> slots = readPositions(buf);
                List<BlockPos> cover = readPositions(buf);
                Prayer prayer = new Prayer(buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readBoolean(),
                        buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
                return new AltarSurveyPayload(pos, owner, isOwner, age, hasResult, unverified, structural, granted, names,
                        coefficient, cap, found, rows, ageRequired, estimate, min, max, uncovered, counted, shortCols,
                        unlit, slots, cover, prayer);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
