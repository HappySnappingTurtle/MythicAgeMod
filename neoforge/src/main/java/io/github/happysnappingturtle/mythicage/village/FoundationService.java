package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.found.FoundationRules;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianRegistry;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.found.FoundationPayloads;
import io.github.happysnappingturtle.mythicage.village.found.FoundationStoneBlock;
import io.github.happysnappingturtle.mythicage.village.found.FoundationStoneBlockEntity;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.life.VillageLife;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 奠基石（T19 设计文档 §1.13，17 号 §3）：五文明通用，文明数据 {@code village_civ/<civ>.json} 的 {@code foundation} 给方块与主神。
 * <ul>
 *   <li>放置检查：限额（数登记表里本人立的该文明村庄，宽限中的也算）、32 格内没有别的村庄（中立原版村庄除外：那是要立的村）；</li>
 *   <li>选主神后立村：空地立新村（钟坐标 = 奠基石）；32 格内有中立原版村庄的钟就把它立为该文明（钟不变，原版村民照旧是本村的人）；</li>
 *   <li>只有建立者能拆；拆后 3 个 MC 日内放回原处恢复，过期注销（立过的原版村庄回到中立）。</li>
 * </ul>
 */
public final class FoundationService {
    /** 规划数据里奠基石的锚点；露宿位。 */
    public static final String ANCHOR = "foundation", CAMP = "camp";
    /** 奠基石周围算作已建范围的半径（7 × 7）。 */
    public static final int CORE = 3;
    /** 立原版村庄时，按床与钟算已建范围的搜索半径。 */
    public static final int VANILLA_SCAN = 48;

    private FoundationService() {
    }

    static Optional<CivVillageData.Foundation> foundation(Civilization civ) {
        return VillageData.civ(civ.id()).map(CivVillageData::foundation).filter(CivVillageData.Foundation::enabled);
    }

    // ───────────── 放置 ─────────────

    /** 本人立的该文明村庄（含拆除宽限中的）。 */
    public static List<VillageRegistry.Village> foundedBy(ServerLevel level, UUID player, Civilization civ) {
        return VillageRegistry.get(level.getServer()).all().stream()
                .filter(v -> v.origin() == VillageRegistry.Origin.FOUNDED && player.equals(v.founder()) && v.civ().equals(civ.id()))
                .toList();
    }

    /** 宽限中、原处在 2 格内的本人村庄（放回去就恢复）。 */
    static Optional<VillageRegistry.Village> restorable(ServerLevel level, UUID player, Civilization civ, BlockPos pos) {
        long today = MythDays.today(level.getServer());
        for (VillageRegistry.Village v : foundedBy(level, player, civ)) {
            if (FoundationRules.grace(v.missingSinceDay(), today) != FoundationRules.Grace.RESTORABLE) {
                continue;
            }
            Optional<BlockPos> at = stonePos(level, v);
            if (at.isPresent() && v.bell().dimension().equals(level.dimension())
                    && at.get().closerThan(pos, FoundationRules.RESTORE_RADIUS + 0.5)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    /** 32 格内的中立原版村庄（按钟的水平距离取最近）。 */
    static Optional<VillageRegistry.Village> neutralNear(ServerLevel level, BlockPos pos) {
        VillageRegistry.Village best = null;
        double bestD = Double.MAX_VALUE;
        for (VillageRegistry.Village v : VillageRegistry.get(level.getServer()).in(level.dimension())) {
            if (!v.neutral()) {
                continue;
            }
            double d = Math.hypot(v.bell().pos().getX() - pos.getX(), v.bell().pos().getZ() - pos.getZ());
            if (d <= FoundationRules.MIN_GAP && d < bestD) {
                bestD = d;
                best = v;
            }
        }
        return Optional.ofNullable(best);
    }

    /** 别的村庄有没有在 32 格内（有规划数据的看已建范围，没有的看钟）。{@code except} 是要立的中立原版村庄。 */
    static boolean tooClose(ServerLevel level, BlockPos pos, Optional<UUID> except) {
        VillagePlans plans = VillagePlans.get(level.getServer());
        for (VillageRegistry.Village v : VillageRegistry.get(level.getServer()).in(level.dimension())) {
            if (except.isPresent() && except.get().equals(v.id())) {
                continue;
            }
            Optional<VillagePlans.Plan> p = plans.byVillage(v.id());
            if (p.isPresent()) {
                if (FoundationRules.tooClose(p.get().info().built(), pos.getX(), pos.getZ(), FoundationRules.MIN_GAP)) {
                    return true;
                }
            } else if (Math.hypot(v.bell().pos().getX() - pos.getX(), v.bell().pos().getZ() - pos.getZ())
                    <= FoundationRules.MIN_GAP) {
                return true;
            }
        }
        return false;
    }

    /** 放下前的检查；能放返回 empty。 */
    public static Optional<Component> placementError(ServerPlayer player, ServerLevel level, BlockPos pos, Civilization civ) {
        if (foundation(civ).isEmpty()) {
            return Optional.of(Component.translatable("mythic_age.foundation.error.unavailable"));
        }
        if (restorable(level, player.getUUID(), civ, pos).isPresent()) {
            return Optional.empty();
        }
        List<VillageRegistry.Village> mine = foundedBy(level, player.getUUID(), civ);
        long grace = mine.stream().filter(v -> v.missingSinceDay() >= 0).count();
        if (!FoundationRules.canFound(mine.size() - (int) grace, (int) grace, FoundationRules.PER_PLAYER)) {
            return Optional.of(Component.translatable("mythic_age.foundation.error.limit", FoundationRules.PER_PLAYER));
        }
        Optional<VillageRegistry.Village> neutral = neutralNear(level, pos);
        if (tooClose(level, pos, neutral.map(VillageRegistry.Village::id))) {
            return Optional.of(Component.translatable("mythic_age.foundation.error.too_close", FoundationRules.MIN_GAP));
        }
        return Optional.empty();
    }

    public static void onPlaced(ServerLevel level, BlockPos pos, ServerPlayer player, FoundationStoneBlockEntity be, Civilization civ) {
        be.setOwner(player.getUUID());
        Optional<VillageRegistry.Village> back = restorable(level, player.getUUID(), civ, pos);
        if (back.isPresent()) {
            restore(level, back.get(), pos, be);
            player.displayClientMessage(Component.translatable("mythic_age.foundation.restored"), false);
            return;
        }
        openChoice(player, pos, civ);
    }

    public static void use(ServerLevel level, ServerPlayer player, BlockPos pos, FoundationStoneBlockEntity be, Civilization civ) {
        if (be.pending()) {
            if (player.getUUID().equals(be.owner())) {
                openChoice(player, pos, civ);
            }
            return;
        }
        VillageRegistry.get(level.getServer()).byId(be.village()).ifPresent(v -> player.displayClientMessage(
                Component.translatable("mythic_age.foundation.info", Component.translatable("mythic_age.god." + v.patron())), true));
    }

    static void openChoice(ServerPlayer player, BlockPos pos, Civilization civ) {
        foundation(civ).ifPresent(f -> {
            List<FoundationPayloads.Option> opts = new ArrayList<>();
            var pantheon = PantheonLoader.get(civ);
            for (String god : f.patrons()) {
                List<String> minors = pantheon.minorGods().stream().filter(g -> g.patrons().contains(god)).map(g -> g.id()).toList();
                opts.add(new FoundationPayloads.Option(god, minors));
            }
            PacketDistributor.sendToPlayer(player, new FoundationPayloads.Choose(pos, civ.id(), opts));
        });
    }

    /** 客户端选好主神。 */
    public static void handleFound(FoundationPayloads.Found payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer p && p.level() instanceof ServerLevel level
                    && p.distanceToSqr(payload.pos().getCenter()) <= 64) {
                Optional<Component> err = found(level, p, payload.pos(), payload.patron());
                err.ifPresent(e -> p.displayClientMessage(e, true));
            }
        });
    }

    /** 立村（GameTest 也直接调用）。不成返回原因。 */
    public static Optional<Component> found(ServerLevel level, ServerPlayer player, BlockPos pos, String patron) {
        if (!(level.getBlockEntity(pos) instanceof FoundationStoneBlockEntity be) || !be.pending()
                || !(level.getBlockState(pos).getBlock() instanceof FoundationStoneBlock block)
                || !player.getUUID().equals(be.owner())) {
            return Optional.of(Component.translatable("mythic_age.foundation.error.not_pending"));
        }
        Civilization civ = block.civ();
        Optional<CivVillageData.Foundation> f = foundation(civ);
        if (f.isEmpty() || !f.get().patrons().contains(patron)) {
            return Optional.of(Component.translatable("mythic_age.foundation.error.unavailable"));
        }
        Optional<Component> err = placementError(player, level, pos, civ);
        if (err.isPresent()) {
            return err;
        }
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        Optional<VillageRegistry.Village> neutral = neutralNear(level, pos);
        List<Box> built = new ArrayList<>();
        built.add(new Box(pos.getX() - CORE, pos.getY() - 1, pos.getZ() - CORE, pos.getX() + CORE, pos.getY() + 6, pos.getZ() + CORE));
        VillageRegistry.Village v;
        if (neutral.isPresent()) {
            v = neutral.get().withFounding(civ.id(), patron, player.getUUID());
            reg.update(v);
            vanillaArea(level, v.bell().pos()).ifPresent(built::add);
        } else {
            v = new VillageRegistry.Village(UUID.randomUUID(), GlobalPos.of(level.dimension(), pos.immutable()), civ.id(), patron,
                    VillageRegistry.Origin.FOUNDED, player.getUUID(), "", -1).withFounding(civ.id(), patron, player.getUUID());
            reg.add(v);
        }
        Map<String, List<Pos>> anchors = new LinkedHashMap<>();
        anchors.put("center", List.of(VillageContextPos.of(pos)));
        anchors.put(ANCHOR, List.of(VillageContextPos.of(pos)));
        anchors.put(CAMP, camps(level, pos, f.get().camp()));
        VillageInfoPiece.Info info = new VillageInfoPiece.Info(civ.id(), patron, "founded", 1, "", VillageContextPos.of(pos), anchors,
                built, List.of());
        VillagePlans.get(level.getServer()).put(new VillagePlans.Plan(v.id(), level.dimension(), "founded", pos.asLong(), info, true));
        be.setVillage(v.id());
        TradeService.learnPatron(player, civ, patron);
        if (GuardianSpawner.hasGuardian(civ)) {
            GuardianSpawner.standableNear(level, pos, 4).ifPresent(at -> GuardianSpawner.spawnNew(level, at, Optional.of(v.bell().pos()), civ));
        }
        VillageEconomy.State s = VillageEconomy.get(level.getServer()).state(v.id());
        s.log(MythDays.today(level.getServer()), "mythic_age.village.log.founded_by", player.getGameProfile().getName(),
                "@mythic_age.god." + patron);
        VillageEconomy.get(level.getServer()).setDirty();
        player.displayClientMessage(Component.translatable("mythic_age.foundation.founded", Component.translatable("mythic_age.god." + patron)),
                false);
        return Optional.empty();
    }

    /** 原版村庄的范围：钟与 48 格内的床的外接盒。 */
    static Optional<Box> vanillaArea(ServerLevel level, BlockPos bell) {
        List<BlockPos> pts = new ArrayList<>();
        pts.add(bell);
        level.getPoiManager().findAll(t -> t.is(PoiTypes.HOME), p -> true, bell, VANILLA_SCAN, PoiManager.Occupancy.ANY)
                .forEach(pts::add);
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (BlockPos p : pts) {
            x0 = Math.min(x0, p.getX() - 3);
            z0 = Math.min(z0, p.getZ() - 3);
            x1 = Math.max(x1, p.getX() + 3);
            z1 = Math.max(z1, p.getZ() + 3);
        }
        return Optional.of(new Box(x0, bell.getY() - 8, z0, x1, bell.getY() + 12, z1));
    }

    /** 露宿位：石灶外 2–3 格的三处落脚点。 */
    static List<Pos> camps(ServerLevel level, BlockPos pos, int n) {
        List<Pos> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double a = i * 2 * Math.PI / Math.max(1, n) + 0.4;
            BlockPos at = VillageLife.ground(level, (int) Math.floor(pos.getX() + 0.5 + Math.cos(a) * 2.6),
                    (int) Math.floor(pos.getZ() + 0.5 + Math.sin(a) * 2.6), pos.getY());
            out.add(VillageContextPos.of(at));
        }
        return out;
    }

    // ───────────── 拆除、宽限、恢复 ─────────────

    /** 规划数据里的奠基石位置。 */
    public static Optional<BlockPos> stonePos(ServerLevel level, VillageRegistry.Village v) {
        return VillagePlans.get(level.getServer()).byVillage(v.id())
                .flatMap(p -> p.anchors(ANCHOR).stream().findFirst()).map(VillageInfoPiece::block);
    }

    /** FOUNDED 村庄还在不在：奠基石还在原处。 */
    public static boolean present(ServerLevel level, VillageRegistry.Village v) {
        Optional<BlockPos> at = stonePos(level, v);
        return at.isPresent() && level.getBlockState(at.get()).getBlock() instanceof FoundationStoneBlock
                && level.getBlockEntity(at.get()) instanceof FoundationStoneBlockEntity be && v.id().equals(be.village());
    }

    /** 只有建立者能拆（别人挖不动）。 */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof FoundationStoneBlock
                && event.getLevel().getBlockEntity(event.getPos()) instanceof FoundationStoneBlockEntity be
                && be.owner() != null && !be.owner().equals(event.getPlayer().getUUID())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("mythic_age.foundation.error.not_owner"), true);
        }
    }

    /** 方块没了：立过的村庄进入宽限。 */
    public static void onRemoved(ServerLevel level, BlockPos pos, FoundationStoneBlockEntity be) {
        if (be.village() == null) {
            return;
        }
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        reg.byId(be.village()).ifPresent(v -> {
            if (v.missingSinceDay() < 0) {
                reg.markMissing(v, MythDays.today(level.getServer()));
            }
        });
    }

    /** 宽限内放回：清除宽限；位置挪了就把锚点（新立的村庄连同钟坐标）挪过去。 */
    static void restore(ServerLevel level, VillageRegistry.Village v, BlockPos pos, FoundationStoneBlockEntity be) {
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        reg.markMissing(v, -1);
        be.setVillage(v.id());
        VillagePlans plans = VillagePlans.get(level.getServer());
        plans.byVillage(v.id()).ifPresent(p -> {
            VillageInfoPiece.Info i = p.info();
            Map<String, List<Pos>> a = new LinkedHashMap<>(i.anchors());
            a.put(ANCHOR, List.of(VillageContextPos.of(pos)));
            if (v.vanillaType().isEmpty()) {
                a.put("center", List.of(VillageContextPos.of(pos)));
            }
            plans.updateInfo(v.id(), new VillageInfoPiece.Info(i.civ(), i.patron(), i.layout(), i.layoutVersion(), i.templateSet(),
                    v.vanillaType().isEmpty() ? VillageContextPos.of(pos) : i.center(), a, i.built(), i.plots()));
        });
        VillageRegistry.Village now = reg.byId(v.id()).orElse(v);
        if (now.vanillaType().isEmpty() && !now.bell().pos().equals(pos)) {
            reg.remove(now.bell());
            reg.add(new VillageRegistry.Village(now.id(), GlobalPos.of(level.dimension(), pos.immutable()), now.civ(), now.patron(),
                    now.origin(), now.founder(), now.vanillaType(), -1, now.stage(), now.shrines(), now.baseline(),
                    now.lastMigrationDay()));
        }
    }

    /**
     * 宽限过期：新立的村庄整个删掉（登记、规划、村庄经济、本模组居民、祭司）；立过的原版村庄回到中立（原版村民留下）。
     */
    public static void expire(ServerLevel level, VillageRegistry.Village v) {
        var server = level.getServer();
        for (ResidentEntity r : VillageScheduler.loaded(v.id())) {
            r.discard();
        }
        GuardianRegistry gr = GuardianRegistry.get(server);
        gr.byBell(level.dimension(), v.bell().pos()).ifPresent(e -> {
            if (level.getEntity(e.entityUuid()) instanceof GuardianEntity g) {
                g.discard();
            }
            gr.remove(e.guardianId());
        });
        VillagePlans.get(server).remove(v.id());
        VillageEconomy.get(server).remove(v.id());
        VillageScheduler.clearForTest(v.id());
        VillageRegistry reg = VillageRegistry.get(server);
        if (!v.vanillaType().isEmpty()) {
            reg.update(v.asNeutralVanilla());
        } else {
            reg.remove(v.bell());
        }
    }

    /** 管理命令：发一块某文明的奠基石。 */
    public static boolean give(ServerPlayer player, Civilization civ) {
        return foundation(civ).map(f -> BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(f.block())))
                .filter(i -> i != net.minecraft.world.item.Items.AIR)
                .map(i -> player.getInventory().add(new net.minecraft.world.item.ItemStack(i))).orElse(false);
    }

    /** {@code BlockPos → Pos}。 */
    static final class VillageContextPos {
        static Pos of(BlockPos b) {
            return new Pos(b.getX(), b.getY(), b.getZ());
        }
    }
}
