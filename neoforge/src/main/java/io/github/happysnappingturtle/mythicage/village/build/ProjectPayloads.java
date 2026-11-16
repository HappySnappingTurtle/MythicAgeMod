package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker;
import io.github.happysnappingturtle.mythicage.village.VillageSpace;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 项目选择的网络负载（GA-11 §2.3）：请求候选列表、点名、清空。界面在 GA-12 的「建设」分页；本任务先给负载与服务端处理。
 * 只有站在这个村子村界内的玩家（或权限 2）能看、能点；点名另要贡献达门槛（{@link VillageProjects}）。
 */
public final class ProjectPayloads {
    private ProjectPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    /** 客户端 → 服务端：要这个村子的候选列表。 */
    public record Request(UUID village) implements CustomPacketPayload {
        public static final Type<Request> TYPE = payloadType("village_projects_request");
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> buf.writeUUID(r.village), buf -> new Request(buf.readUUID()));

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    /** 客户端 → 服务端：点名候选键为 key 的一项；key 为空串表示清空「下一个」位。 */
    public record Nominate(UUID village, String key) implements CustomPacketPayload {
        public static final Type<Nominate> TYPE = payloadType("village_projects_nominate");
        public static final StreamCodec<FriendlyByteBuf, Nominate> STREAM_CODEC = StreamCodec.of(
                (buf, n) -> {
                    buf.writeUUID(n.village);
                    buf.writeUtf(n.key);
                }, buf -> new Nominate(buf.readUUID(), buf.readUtf()));

        @Override
        public Type<Nominate> type() {
            return TYPE;
        }
    }

    /**
     * 一项候选。
     *
     * @param name      建筑名（文本行）
     * @param kind      new / upgrade / shrine
     * @param reason    不能开工的原因（语言键后缀）；能开工为空串
     * @param rank      村子自己选时排第几（从 0 起）；不能开工为 −1
     * @param materials 材料：物品 id（或 {@code #组}）→ 件数
     */
    public record Entry(String key, AffairsView.Line name, String kind, String reason, int rank, int targets,
                        Map<String, Integer> materials) {
        static final StreamCodec<FriendlyByteBuf, Entry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.key);
                    AffairsView.LINE.encode(buf, e.name);
                    buf.writeUtf(e.kind);
                    buf.writeUtf(e.reason);
                    buf.writeVarInt(e.rank + 1);
                    buf.writeVarInt(e.targets);
                    buf.writeMap(e.materials, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
                }, buf -> new Entry(buf.readUtf(), AffairsView.LINE.decode(buf), buf.readUtf(), buf.readUtf(), buf.readVarInt() - 1,
                        buf.readVarInt(), Map.copyOf(buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt))));
    }

    /**
     * 服务端 → 客户端：候选列表。
     *
     * @param current 当前项目的几行（同村务界面）；没有为空
     * @param nextKey 「下一个」位的候选键；空着为空串
     * @param contribution 这名玩家的贡献；{@code threshold} 是他的点名门槛
     */
    public record View(UUID village, List<AffairsView.Line> current, String nextKey, String nextPlayer, List<Entry> entries,
                       int contribution, int threshold, Map<String, Integer> missing, int blocked, int workers, int placed,
                       int total) implements CustomPacketPayload {
        public static final Type<View> TYPE = payloadType("village_projects_view");
        public static final StreamCodec<FriendlyByteBuf, View> STREAM_CODEC = StreamCodec.of(
                (buf, v) -> {
                    buf.writeUUID(v.village);
                    buf.writeCollection(v.current, AffairsView.LINE);
                    buf.writeUtf(v.nextKey);
                    buf.writeUtf(v.nextPlayer);
                    buf.writeCollection(v.entries, Entry.CODEC);
                    buf.writeVarInt(v.contribution);
                    buf.writeVarInt(v.threshold);
                    buf.writeMap(v.missing, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
                    buf.writeVarInt(v.blocked);
                    buf.writeVarInt(v.workers);
                    buf.writeVarInt(v.placed);
                    buf.writeVarInt(v.total);
                }, buf -> new View(buf.readUUID(), buf.readList(AffairsView.LINE), buf.readUtf(), buf.readUtf(),
                        buf.readList(Entry.CODEC), buf.readVarInt(), buf.readVarInt(),
                        Map.copyOf(buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt)), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<View> type() {
            return TYPE;
        }
    }

    /** 这名玩家能不能看、能不能点这个村子的项目：站在村界内，或权限 2。 */
    static Optional<VillageContext> context(ServerPlayer player, UUID village) {
        boolean inside = VillageSpace.villageAt(player.serverLevel(), player.blockPosition())
                .map(v -> v.id().equals(village)).orElse(false);
        if (!inside && !player.hasPermissions(2)) {
            return Optional.empty();
        }
        return VillageScheduler.contextOf(player.serverLevel(), village);
    }

    public static Optional<View> view(ServerPlayer player, VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        List<Entry> entries = new ArrayList<>();
        for (VillageProjects.View v : VillageProjects.candidates(ctx)) {
            ProjectPicker.Candidate c = v.candidate();
            entries.add(new Entry(c.key(), AffairsView.Line.of("mythic_age.village.project.name",
                    Construction.nameArg(v.spec(), c.pick().god())), c.kind().name().toLowerCase(java.util.Locale.ROOT), c.reason(),
                    c.rank(), c.targets(), v.materials()));
        }
        List<AffairsView.Line> current = VillageAffairs.projectLines(s).stream()
                .filter(l -> !l.key().equals("mythic_age.village.project.next")).toList();
        Map<String, Integer> missing = Map.of();
        int blocked = 0;
        int workers = 0;
        int placed = 0;
        int total = 0;
        Optional<io.github.happysnappingturtle.mythicage.core.village.build.BuildProject> active = s.activeProject();
        if (active.isPresent()) {
            var p = active.get();
            missing = Map.copyOf(p.missing());
            blocked = io.github.happysnappingturtle.mythicage.core.village.build.BuildProject.count(p.blockedCells);
            placed = io.github.happysnappingturtle.mythicage.core.village.build.BuildProject.count(p.placedCells);
            total = Construction.site(ctx, p).map(site -> site.segments.stream().mapToInt(seg -> seg.cells().size()).sum()).orElse(0);
            workers = (int) VillageScheduler.loaded(ctx.village().id()).stream()
                    .filter(r -> r.job().filter(j -> j.type().equals(BuildJob.ID)).isPresent()).count();
        }
        return Optional.of(new View(ctx.village().id(), current, s.nextProject == null ? "" : s.nextProject.key(),
                s.nextProject == null ? "" : s.nextProject.playerName(), entries, VillageProjects.contribution(player, ctx),
                VillageProjects.threshold(player, ctx), missing, blocked, workers, placed, total));
    }

    public static void handleRequest(Request payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                send(p, payload.village());
            }
        });
    }

    public static void handleNominate(Nominate payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                nominate(p, payload);
            }
        });
    }

    /** 村庄界面的「点名」「清空」（GA-12 建设页）：与命令 {@code village nominate} 走同一条路。看不了这个村子时为空。 */
    public static Optional<VillageProjects.Outcome> nominate(ServerPlayer p, Nominate payload) {
        Optional<VillageContext> ctx = context(p, payload.village());
        if (ctx.isEmpty()) {
            return Optional.empty();
        }
        VillageProjects.Outcome o = payload.key().isEmpty() ? VillageProjects.clear(p, ctx.get())
                : VillageProjects.nominate(p, ctx.get(), payload.key());
        p.displayClientMessage(message(o), true);
        send(p, payload.village());
        return Optional.of(o);
    }

    static void send(ServerPlayer p, UUID village) {
        if (p.connection != null && !(p instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            context(p, village).flatMap(ctx -> view(p, ctx)).ifPresent(v -> PacketDistributor.sendToPlayer(p, v));
        }
    }

    /** 点名结果的提示（动作栏）。 */
    public static Component message(VillageProjects.Outcome o) {
        return switch (o.result()) {
            case OK -> Component.translatable("commands.mythic_age.village.nominate.ok");
            case CLEARED -> Component.translatable("commands.mythic_age.village.nominate.cleared");
            case LOW_CONTRIBUTION -> Component.translatable("mythic_age.village.project.nominate.low", o.have(), o.need(),
                    o.need() - o.have());
            case CURRENT -> Component.translatable("mythic_age.village.project.nominate.current");
            case UNAVAILABLE -> Component.translatable("mythic_age.village.project.nominate.unavailable",
                    Component.translatable("mythic_age.village.project.reason." + o.reason()));
        };
    }
}
