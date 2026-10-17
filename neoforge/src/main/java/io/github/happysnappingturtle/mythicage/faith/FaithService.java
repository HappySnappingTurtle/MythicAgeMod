package io.github.happysnappingturtle.mythicage.faith;

import io.github.happysnappingturtle.mythicage.core.faith.FaithProfile;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.rite.AgeCaps;
import io.github.happysnappingturtle.mythicage.core.rite.PromotionTable;
import io.github.happysnappingturtle.mythicage.core.rite.RequirementScope;
import io.github.happysnappingturtle.mythicage.core.rite.RiteRules;
import io.github.happysnappingturtle.mythicage.core.trade.InventoryPlan;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.rite.PromotionRequirement;
import io.github.happysnappingturtle.mythicage.rite.PromotionRequirements;
import io.github.happysnappingturtle.mythicage.rite.RiteTables;
import io.github.happysnappingturtle.mythicage.rite.UnlockService;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.village.PantheonLoader;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 信仰面板（T11a 设计文档 §7.3；18 号 §2.2）：按文明组装「通用区 + 恩宠来源区 + 特色区」。
 * 通用区的规则对五个文明一样；恩宠来源与特色由各文明通过 {@link GraceSource}、{@link CivFeatureView} 提供。
 */
public final class FaithService {
    private static final List<Age> MINOR_AGES = List.of(Age.CLASSICAL, Age.HEROIC, Age.MYTHIC);

    private FaithService() {
    }

    public static FaithView build(ServerPlayer player) {
        PlayerMythData data = MythData.get(player);
        Age highest = AgeCaps.highest(data);
        List<FaithView.Page> pages = new ArrayList<>();
        for (Civilization civ : Civilization.values()) {
            pages.add(page(player, civ));
        }
        return new FaithView(AgeCaps.mythicUnits(highest), AgeCaps.relicSlots(highest), pages);
    }

    static FaithView.Page page(ServerPlayer player, Civilization civ) {
        Optional<FaithProfile> profile = FaithProfiles.get(civ);
        PlayerMythData.CivState s = MythData.get(player).civ(civ);
        if (profile.isEmpty()) {
            return empty(civ, FaithView.Status.UNOPENED, "");
        }
        if (s.age() == Age.NONE) {
            return empty(civ, FaithView.Status.UNCONTACTED, profile.get().hint());
        }
        // 只列已到达的时代（改回低时代的存档里可能还留着更高时代的记录）
        List<String> gods = MINOR_AGES.stream().map(a -> s.age().atLeast(a) ? s.minorGods().getOrDefault(a, "") : "").toList();
        List<FaithView.Approval> approvals = PantheonLoader.get(civ).patrons().stream()
                .map(p -> new FaithView.Approval(p, s.approval(p))).toList();
        Age nextAge = RiteRules.next(s.age()).orElse(Age.NONE);
        List<FaithView.Line> next = nextLines(player, civ, s, nextAge);
        if (next.isEmpty()) {
            nextAge = Age.NONE;
        }
        List<FaithView.Row> sources = new ArrayList<>();
        Set<UUID> sourceVillages = new LinkedHashSet<>();
        for (GraceSource src : GraceSources.of(civ)) {
            for (GraceSource.Row r : src.rows(player)) {
                sources.add(new FaithView.Row(r.name(), true, r.pos().pos(), r.status(),
                        Component.translatable("mythic_age.faith.per_day", String.format(Locale.ROOT, "%.1f", r.perDay()))));
                r.village().ifPresent(sourceVillages::add);
            }
        }
        List<Component> features = new ArrayList<>();
        CivFeatureView.Registry.of(civ).forEach(f -> features.addAll(f.rows(player)));
        return new FaithView.Page(civ, FaithView.Status.CONTACTED, profile.get().hint(), profile.get().favor(), s.favor(),
                MythData.cap(player, civ), s.age(), gods, approvals, nextAge, next, villages(player, s, sourceVillages),
                sources, features);
    }

    private static FaithView.Page empty(Civilization civ, FaithView.Status status, String hint) {
        return new FaithView.Page(civ, status, hint, "", 0, 0, Age.NONE, List.of(), List.of(), Age.NONE, List.of(),
                List.of(), List.of(), List.of());
    }

    /** 下一次晋升的条件：恩宠、供品、Boss 凭证、玩家范围的建筑条件；村庄范围的只提示在举行地检查。 */
    private static List<FaithView.Line> nextLines(ServerPlayer player, Civilization civ, PlayerMythData.CivState s, Age target) {
        Optional<PromotionTable.AgeEntry> entry = target == Age.NONE ? Optional.empty()
                : RiteTables.promotion(civ).entry(target).filter(e -> !e.broken());
        if (entry.isEmpty()) {
            return List.of();
        }
        PromotionTable.AgeEntry e = entry.get();
        List<FaithView.Line> out = new ArrayList<>();
        out.add(new FaithView.Line(ItemStack.EMPTY, Component.translatable("mythic_age.rite.line.favor"),
                s.favor() >= e.favor(), Component.literal(s.favor() + " / " + e.favor())));
        InventoryPlan inv = TradeService.plan(player);
        for (TradeOffer.ItemAmount a : e.offerings()) {
            ItemStack icon = TradeService.stack(a.item(), 1);
            int have = inv.count(a.item());
            out.add(new FaithView.Line(icon, icon.getHoverName(), have >= a.count(), Component.literal(have + " / " + a.count())));
        }
        if (e.trial() != Age.NONE) {
            int have = s.vouchers(e.trial());
            out.add(new FaithView.Line(ItemStack.EMPTY, Component.translatable("mythic_age.rite.line.voucher",
                    Component.translatable("mythic_age.trial." + e.trial().id())), have >= 1, Component.literal(have + " / 1")));
        }
        boolean villageRules = false;
        for (PromotionTable.RequirementSpec spec : e.requirements()) {
            Optional<PromotionRequirement> req = PromotionRequirements.get(spec.type());
            if (req.isEmpty()) {
                continue;
            }
            if (req.get().scope() == RequirementScope.VILLAGE) {
                villageRules = true;
                continue;
            }
            PromotionRequirement.Line l = req.get().check(player, civ, target, null, spec.params());
            out.add(new FaithView.Line(ItemStack.EMPTY, l.label(), l.met(), l.progress()));
        }
        if (villageRules) {
            out.add(new FaithView.Line(ItemStack.EMPTY, Component.translatable("mythic_age.faith.village_rules"), true,
                    Component.empty()));
        }
        return out;
    }

    /** 你的村庄：举行过仪式的，加上各恩宠来源报来的关联村庄（如自己神坛所在的村庄）；去重。 */
    private static List<FaithView.Row> villages(ServerPlayer player, PlayerMythData.CivState s, Set<UUID> fromSources) {
        VillageRegistry reg = VillageRegistry.get(player.server);
        List<FaithView.Row> out = new ArrayList<>();
        Set<UUID> seen = new LinkedHashSet<>();
        s.riteVillages().forEach((id, mark) -> {
            seen.add(id);
            Optional<VillageRegistry.Village> v = reg.byId(id);
            if (v.isEmpty()) {
                out.add(new FaithView.Row(Component.translatable("mythic_age.faith.village_gone"), false, BlockPos.ZERO,
                        Component.empty(), Component.empty()));
                return;
            }
            out.add(row(v.get(), Component.translatable(mark.promotedHere() ? "mythic_age.faith.village.promoted"
                    : "mythic_age.faith.village.rite")));
        });
        for (UUID id : fromSources) {
            if (seen.add(id)) {
                reg.byId(id).ifPresent(v -> out.add(row(v, Component.translatable("mythic_age.faith.village.shrine"))));
            }
        }
        return out;
    }

    private static FaithView.Row row(VillageRegistry.Village v, Component note) {
        return new FaithView.Row(VillageService.title(v), true, v.bell().pos(), note,
                Component.translatable("mythic_age.age." + v.stage().id()));
    }

    // ───────────── 网络 ─────────────

    public static void handleRequest(FaithPayloads.Request p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            TradeService.send(player, new FaithPayloads.View(build(player)));
        }
    }

    /** 重看某个已到达时代的成功界面。 */
    public static void handleReplay(FaithPayloads.Replay p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player && p.age().atLeast(Age.CLASSICAL)
                && MythData.get(player).civ(p.civ()).age().atLeast(p.age())) {
            UnlockService.replay(player, p.civ(), p.age());
        }
    }
}
