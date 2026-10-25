package io.github.happysnappingturtle.mythicage.village.economy;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.core.village.time.VillageCalendar;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 村务（T19 设计文档 §4.3，D4）：右键居民先说一句话（{@link ResidentDialogue}）；谷仓管事接着打开村务界面（库存、缺口、记录、捐赠），
 * 有交易表的职业接着打开交易界面（{@link ResidentTrade}）。
 * 捐赠是真实转移：满仓退回、记贡献（{@code seenBy}），<b>不加善恶、不加恩宠</b>（11 号 §8）。
 * 文本行的参数以 {@code @} 开头时，客户端把它当语言键翻译；{@code @@} 开头的把后面的参数都交给它（记录行）。
 */
public final class VillageAffairs {
    public static final double REACH = 8;

    private VillageAffairs() {
    }

    public static void onResidentInteract(ServerPlayer player, ResidentEntity r) {
        player.sendSystemMessage(Component.translatable("mythic_age.village.resident.says",
                r.getCustomName() == null ? Component.empty() : r.getCustomName(),
                Component.translatable(professionKey(r.profession())), ResidentDialogue.line(player.server, r)));
        Optional<UUID> v = r.village();
        if (v.isEmpty() || r.inTransit()) {
            return;
        }
        boolean keeper = VillageEconomy.get(player.server).peek(v.get())
                .map(s -> s.roster.get(r.getUUID())).map(VillageEconomy.Member::keeper).orElse(false);
        if (keeper) {
            open(player, v.get());
        } else if (ResidentTrade.trades(r)) {
            ResidentTrade.open(player, r);
        }
    }

    public static String professionKey(String profession) {
        return "mythic_age.profession." + profession.replace(':', '.');
    }

    public static void open(ServerPlayer player, UUID village) {
        view(player, village).ifPresent(v -> PacketDistributor.sendToPlayer(player, new AffairsPayloads.View(v)));
    }

    public static Optional<AffairsView> view(ServerPlayer player, UUID village) {
        Optional<VillageRegistry.Village> vo = VillageRegistry.get(player.server).byId(village);
        if (vo.isEmpty()) {
            return Optional.empty();
        }
        VillageRegistry.Village v = vo.get();
        VillageEconomy.State s = VillageEconomy.get(player.server).state(village);
        Optional<CivVillageData> data = VillageData.civ(v.civ());
        List<AffairsView.Entry> stock = new ArrayList<>();
        s.stock.items().forEach((k, n) -> stock.add(new AffairsView.Entry(k, n, s.stock.escrowed(k))));
        s.stock.escrows().values().forEach(m -> m.keySet().forEach(k -> {
            if (s.stock.count(k) == 0 && stock.stream().noneMatch(e -> e.item().equals(k))) {
                stock.add(new AffairsView.Entry(k, 0, s.stock.escrowed(k)));
            }
        }));
        List<AffairsView.Line> shortages = new ArrayList<>();
        data.ifPresent(d -> {
            if (d.rations().belowFloor(s.stock)) {
                shortages.add(AffairsView.Line.of("mythic_age.village.short.food",
                        String.valueOf((int) d.rations().total(s.stock)), String.valueOf(d.rations().floor())));
            }
            for (ResourcePoint p : s.points.values()) {
                if (p.remaining() <= 0) {
                    shortages.add(AffairsView.Line.of("mythic_age.village.short.point", "@mythic_age.village.point." + p.kind()));
                }
            }
        });
        if (s.stock.free() == 0) {
            shortages.add(AffairsView.Line.of("mythic_age.village.short.full"));
        }
        if (io.github.happysnappingturtle.mythicage.village.VillagePopulation.needsHousing(village)) {
            shortages.add(AffairsView.Line.of("mythic_age.village.short.housing"));
        }
        shortages.addAll(0, projectLines(s));
        List<AffairsView.Line> log = new ArrayList<>();
        for (int i = s.log.size() - 1; i >= 0 && log.size() < 8; i--) {
            VillageEconomy.LogEntry e = s.log.get(i);
            List<String> args = new ArrayList<>();
            args.add(String.valueOf(e.day() + 1));
            args.add("@@" + e.key());
            args.addAll(e.args());
            log.add(new AffairsView.Line("mythic_age.village.log.line", List.copyOf(args)));
        }
        AffairsView.Line title = v.patron().isEmpty() ? AffairsView.Line.of("mythic_age.village.title.plain")
                : AffairsView.Line.of("mythic_age.village.title", "@mythic_age.god." + v.patron());
        AffairsView.Line date = data.map(d -> {
            VillageCalendar.Date dt = d.calendar().date(MythDays.today(player.server), VillageData.offset(d, s));
            return dt.period().isEmpty() ? AffairsView.Line.of("mythic_age.village.date.plain", String.valueOf(dt.dayOfYear()))
                    : AffairsView.Line.of("mythic_age.village.date", "@mythic_age.village.period." + v.civ() + "." + dt.period(),
                    String.valueOf(dt.dayInPeriod()));
        }).orElse(AffairsView.Line.of("mythic_age.village.date.plain", "?"));
        List<AffairsView.Line> notes = data.map(d -> calendarNotes(player.server, v, d, s)).orElse(List.of());
        return Optional.of(new AffairsView(village, title, date, notes, s.stock.total(), s.stock.capacity(), stock, shortages, log));
    }

    /** 历法提示（§1.10 J8）：英雄起显示下一个节日还有几日（今天就是则说今天）；有神谕所时显示下一个神谕日。 */
    public static List<AffairsView.Line> calendarNotes(net.minecraft.server.MinecraftServer server, VillageRegistry.Village v,
                                                      CivVillageData d, VillageEconomy.State s) {
        List<AffairsView.Line> out = new ArrayList<>();
        long today = MythDays.today(server);
        int offset = VillageData.offset(d, s);
        int best = Integer.MAX_VALUE;
        String next = "";
        for (io.github.happysnappingturtle.mythicage.core.village.life.LifeData.Festival f : d.life().festivals().values()) {
            if (!io.github.happysnappingturtle.mythicage.core.village.life.FestivalRules.open(f.minAge(), v.stage())) {
                continue;
            }
            int n = d.calendar().daysUntil(today, offset, f.id());
            if (n >= 0 && n < best) {
                best = n;
                next = f.id();
            }
        }
        if (!next.isEmpty()) {
            out.add(best == 0 ? AffairsView.Line.of("mythic_age.village.date.festival_today", "@mythic_age.festival." + next)
                    : AffairsView.Line.of("mythic_age.village.date.festival_in", "@mythic_age.festival." + next, String.valueOf(best)));
        }
        io.github.happysnappingturtle.mythicage.village.life.OracleService.status(server, v).ifPresent(st -> out.add(
                st.state() == io.github.happysnappingturtle.mythicage.core.village.life.OracleRules.State.OPEN
                        ? AffairsView.Line.of("mythic_age.village.date.oracle_today")
                        : AffairsView.Line.of("mythic_age.village.date.oracle_next", String.valueOf(st.nextDay()))));
        return out;
    }

    /** 施工项目（T19 §1.4 E7）：进度、缺料、停工坐标。 */
    public static List<AffairsView.Line> projectLines(VillageEconomy.State s) {
        List<AffairsView.Line> out = new ArrayList<>();
        s.activeProject().ifPresent(p -> {
            io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec spec =
                    io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings.all().get(p.building);
            if (spec == null) {
                return;
            }
            String name = io.github.happysnappingturtle.mythicage.village.build.Construction.nameArg(spec, p.god);
            switch (p.state) {
                case STOCKING -> {
                    out.add(AffairsView.Line.of("mythic_age.village.project.stocking", name));
                    p.missing().forEach((k, n) -> out.add(AffairsView.Line.of("mythic_age.village.project.missing",
                            "@" + itemKey(k), String.valueOf(n))));
                }
                case BUILDING -> out.add(AffairsView.Line.of("mythic_age.village.project.building", name,
                        String.valueOf(p.placed.cardinality()), String.valueOf(p.segments)));
                case BLOCKED -> out.add(AffairsView.Line.of("mythic_age.village.project.blocked_line", name,
                        p.blockedAt.x() + ", " + p.blockedAt.y() + ", " + p.blockedAt.z()));
                default -> {
                }
            }
        });
        return out;
    }

    /** 物品（或 {@code #组}）的语言键。 */
    static String itemKey(String id) {
        if (id.startsWith("#")) {
            return "mythic_age.village.cost_group." + id.substring(1);
        }
        return BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)).getDescriptionId();
    }

    /** 捐赠主手物品：容量够多少收多少，其余留在手上。 */
    public static void handleDonate(AffairsPayloads.Donate payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p) {
                donate(p, payload.village());
                open(p, payload.village());
            }
        });
    }

    /** 返回实收数。 */
    public static int donate(ServerPlayer player, UUID village) {
        if (!nearKeeper(player, village)) {
            return 0;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.isEmpty() || held.isDamaged() || !held.getComponentsPatch().isEmpty()) {
            player.displayClientMessage(Component.translatable("mythic_age.village.donate.invalid"), true);
            return 0;
        }
        VillageEconomy eco = VillageEconomy.get(player.server);
        VillageEconomy.State s = eco.state(village);
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        Component name = held.getHoverName();
        int got = s.stock.deposit(id, held.getCount());
        if (got > 0) {
            held.shrink(got);
            s.seenBy.add(player.getUUID());
            eco.setDirty();
        }
        player.displayClientMessage(Component.translatable(got > 0 ? "mythic_age.village.donate.done"
                : "mythic_age.village.donate.full", name, got, held.getCount()), true);
        return got;
    }

    private static boolean nearKeeper(ServerPlayer player, UUID village) {
        for (ResidentEntity r : VillageScheduler.loaded(village)) {
            if (r.distanceToSqr(player) <= REACH * REACH) {
                return true;
            }
        }
        return false;
    }

    /** 只供 GameTest（TC-T19-51）：跳过距离检查直接捐。 */
    public static int donateForTest(ServerPlayer player, UUID village, ItemStack stack) {
        VillageEconomy.State s = VillageEconomy.get(player.server).state(village);
        int got = s.stock.deposit(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount());
        stack.shrink(got);
        s.seenBy.add(player.getUUID());
        return got;
    }
}
