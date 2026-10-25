package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.village.VillageRules;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 村庄登记服务（T10a 设计文档 §2、§6）：
 * <ul>
 *   <li>每 100 刻，登记在线玩家 48 格内的新聚会钟；不在原版村庄结构里的钟只判定一次（缓存）；</li>
 *   <li>同时检查玩家附近已登记村庄的钟是否还在，连续 7 个 MC 日不在即注销；</li>
 *   <li>每 20 刻检查玩家是否进入村庄范围，进入时显示「宙斯之村 / 希腊」。</li>
 * </ul>
 */
public final class VillageService {
    public static final int REGISTER_INTERVAL = 100;
    public static final int TITLE_INTERVAL = 20;
    /** T18 之前的临时村庄范围（钟周围水平距离）。 */
    public static final int RANGE = 48;
    public static final int LEAVE_RANGE = 64;

    private static final Set<GlobalPos> NON_VILLAGE = new HashSet<>();
    private static final Map<UUID, UUID> INSIDE = new HashMap<>();

    private VillageService() {
    }

    static VillagesConfig config() {
        return ModConfigs.villages().current();
    }

    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        NON_VILLAGE.clear();
        INSIDE.clear();
    }

    /** 忘掉某个钟的「非村庄钟」缓存（reset 命令、测试用）。 */
    public static void forget(GlobalPos bell) {
        NON_VILLAGE.remove(bell);
    }

    /** 只供 GameTest：清空缓存与进村状态。 */
    public static void clearCachesForTest() {
        NON_VILLAGE.clear();
        INSIDE.clear();
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int t = server.getTickCount();
        if (t % REGISTER_INTERVAL == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!p.isSpectator()) {
                    registerNear(p.serverLevel(), p.blockPosition());
                    checkBells(p.serverLevel(), p.blockPosition());
                }
            }
        }
        if (t % TITLE_INTERVAL == 0 && config().titleOnEnter()) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                updateTitle(p);
            }
        }
    }

    /** 登记 pos 周围 48 格内的新聚会钟，返回本次新登记的村庄。 */
    public static List<VillageRegistry.Village> registerNear(ServerLevel level, BlockPos pos) {
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        return level.getPoiManager()
                .findAll(h -> h.is(PoiTypes.MEETING), b -> true, pos, RANGE, PoiManager.Occupancy.ANY)
                .map(BlockPos::immutable)
                .filter(b -> reg.byBell(GlobalPos.of(level.dimension(), b)).isEmpty())
                .map(b -> register(level, b))
                .flatMap(Optional::stream)
                .toList();
    }

    /** 判定并登记一个钟；不在原版村庄结构里时缓存为「非村庄钟」并返回空。 */
    public static Optional<VillageRegistry.Village> register(ServerLevel level, BlockPos bell) {
        GlobalPos key = GlobalPos.of(level.dimension(), bell);
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        Optional<VillageRegistry.Village> existing = reg.byBell(key);
        if (existing.isPresent() || NON_VILLAGE.contains(key)) {
            return existing;
        }
        Optional<String> type = VillageResolver.vanillaType(level, bell);
        if (type.isEmpty()) {
            // 文明村庄的钟：按结构片段立即登记（V0 §4.2 兜底；正常情况下起点区块载入时已登记）
            Optional<VillageRegistry.Village> generated =
                    io.github.happysnappingturtle.mythicage.village.gen.VillageRegistration.registerAt(level, bell);
            if (generated.isPresent()) {
                return generated;
            }
            NON_VILLAGE.add(key);
            return Optional.empty();
        }
        VillagesConfig cfg = config();
        String civ = "";
        String patron = "";
        if (cfg.vanillaVillages() == VillagesConfig.Mode.AFFILIATE) {
            Map<String, Integer> civWeights = cfg.vanillaTypeToCiv().getOrDefault(type.get(), Map.of());
            civ = VillageRules.pick(civWeights, VillageRules.unit(bell.getX(), bell.getY(), bell.getZ(), VillageRules.SALT_CIV))
                    .orElse("");
        }
        if (!civ.isEmpty()) {
            patron = pickPatron(level, bell, civ, cfg);
        }
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), key, civ, patron,
                VillageRegistry.Origin.VANILLA, null, type.get(), -1);
        reg.add(v);
        return Optional.of(v);
    }

    /**
     * 按坐标哈希抽主神（T10a 设计文档 §2；T10b：群系加成按文明配置）。该文明还没有主神权重时为空。
     * 同一位置结果固定，所以补抽与「当初就有权重」时的结果相同。
     */
    public static String pickPatron(ServerLevel level, BlockPos bell, String civ, VillagesConfig cfg) {
        Map<String, Integer> pw = cfg.patronWeights().get(civ);
        if (pw == null || pw.isEmpty()) {
            return "";
        }
        pw = VillageRules.biomeBonus(pw, VillageResolver.matchedBonuses(level, bell, cfg.biomeBonus(civ)));
        String patron = VillageRules.pick(pw, VillageRules.unit(bell.getX(), bell.getY(), bell.getZ(),
                VillageRules.SALT_PATRON)).orElse("");
        Optional<Civilization> c = Civilization.byId(civ);
        if (!patron.isEmpty() && c.isPresent() && !PantheonLoader.get(c.get()).patrons().isEmpty()
                && !PantheonLoader.get(c.get()).hasPatron(patron)) {
            MythicAge.LOGGER.warn("villages.json 中 {} 的主神 {} 不在主神表里", civ, patron);
        }
        return patron;
    }

    /** 主神为空、但该文明现在有了主神权重的村庄：补抽一次（19 号 M2）。返回补上的主神，没有补抽时为空。 */
    public static Optional<String> backfillPatron(ServerLevel level, VillageRegistry.Village v) {
        if (v.neutral() || !v.patron().isEmpty()) {
            return Optional.empty();
        }
        String patron = pickPatron(level, v.bell().pos(), v.civ(), config());
        if (patron.isEmpty()) {
            return Optional.empty();
        }
        VillageRegistry.get(level.getServer()).setPatron(v, patron);
        MythicAge.LOGGER.info("村庄 {} 补抽主神：{}（{}）", v.bell().pos(), patron, v.civ());
        return Optional.of(patron);
    }

    /** 检查玩家 64 格内已登记村庄的钟：不在则记下起始日，满 7 日注销；回来了就清除标记；主神为空的补抽。 */
    public static void checkBells(ServerLevel level, BlockPos pos) {
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        long today = MythDays.today(level.getServer());
        VillagePlans plans = VillagePlans.get(level.getServer());
        for (VillageRegistry.Village v : reg.in(level.dimension())) {
            BlockPos b = v.bell().pos();
            if (b.distManhattan(pos) > LEAVE_RANGE * 2 || !level.isLoaded(b)) {
                continue;
            }
            // 奠基村庄（T19 §1.13 K5）：看奠基石还在不在；拆了进入 3 日宽限，过期注销（立过的原版村庄回到中立）
            if (v.origin() == VillageRegistry.Origin.FOUNDED) {
                if (FoundationService.present(level, v)) {
                    reg.setMissingSince(v, -1);
                } else if (v.missingSinceDay() < 0) {
                    reg.setMissingSince(v, today);
                } else if (io.github.happysnappingturtle.mythicage.core.village.found.FoundationRules.grace(v.missingSinceDay(), today)
                        == io.github.happysnappingturtle.mythicage.core.village.found.FoundationRules.Grace.EXPIRED) {
                    FoundationService.expire(level, v);
                }
                continue;
            }
            // 按锚点种类判断村庄还在不在（T18 §1）：生成村庄看规划数据，不要求中心有钟
            boolean present = v.anchor().kind() == VillageAnchor.Kind.CENTER && plans.has(v.id())
                    || level.getPoiManager().existsAtPosition(PoiTypes.MEETING, b);
            if (present) {
                reg.setMissingSince(v, -1);
                backfillPatron(level, reg.byBell(v.bell()).orElse(v));
            } else if (v.missingSinceDay() < 0) {
                reg.setMissingSince(v, today);
            } else if (today - v.missingSinceDay() >= VillageRegistry.EXPIRE_DAYS) {
                reg.remove(v.bell());
            }
        }
    }

    /**
     * pos 周围 range 格内最近的已登记村庄（按钟的水平距离）。判断「在不在村里」请用 {@link VillageSpace#villageAt}：
     * 有规划数据的村庄按已建范围判断（T13b）。
     */
    public static Optional<VillageRegistry.Village> villageAt(ServerLevel level, BlockPos pos, int range) {
        return villageAt(level, pos, range, v -> true);
    }

    static Optional<VillageRegistry.Village> villageAt(ServerLevel level, BlockPos pos, int range,
                                                       java.util.function.Predicate<VillageRegistry.Village> filter) {
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        VillageRegistry.Village best = null;
        double bestD = Double.MAX_VALUE;
        for (VillageRegistry.Village v : reg.in(level.dimension())) {
            if (!filter.test(v)) {
                continue;
            }
            BlockPos b = v.bell().pos();
            double dx = b.getX() - pos.getX();
            double dz = b.getZ() - pos.getZ();
            double d = dx * dx + dz * dz;
            if (d <= (double) range * range && d < bestD) {
                bestD = d;
                best = v;
            }
        }
        return Optional.ofNullable(best);
    }

    static void updateTitle(ServerPlayer p) {
        if (p instanceof FakePlayer || p.connection == null) {
            return;
        }
        UUID inside = INSIDE.get(p.getUUID());
        if (inside != null) {
            Optional<VillageRegistry.Village> cur = VillageRegistry.get(p.server).byId(inside);
            if (cur.isPresent() && cur.get().bell().dimension().equals(p.level().dimension())
                    && VillageSpace.stillInside(p.serverLevel(), cur.get(), p.blockPosition())) {
                return;
            }
            INSIDE.remove(p.getUUID());
        }
        VillageSpace.villageAt(p.serverLevel(), p.blockPosition()).ifPresent(v -> {
            INSIDE.put(p.getUUID(), v.id());
            VillageVisits.mark(p, v);
            io.github.happysnappingturtle.mythicage.village.build.BuildNotices.onEnter(p, v);
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(stagedTitle(v)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle(v)));
            io.github.happysnappingturtle.mythicage.core.player.Civilization.byId(v.civ()).ifPresent(c ->
                    io.github.happysnappingturtle.mythicage.advancement.MythEvents.fire(p, io.github.happysnappingturtle.mythicage.advancement.MythEvents.VILLAGE_ENTERED, c));
        });
    }

    /** 只供 GameTest：玩家此刻若会看到进村标题，返回该村庄。 */
    public static Optional<VillageRegistry.Village> titleFor(ServerPlayer p) {
        UUID inside = INSIDE.get(p.getUUID());
        if (inside != null) {
            Optional<VillageRegistry.Village> cur = VillageRegistry.get(p.server).byId(inside);
            if (cur.isPresent() && VillageSpace.stillInside(p.serverLevel(), cur.get(), p.blockPosition())) {
                return Optional.empty();
            }
            INSIDE.remove(p.getUUID());
        }
        Optional<VillageRegistry.Village> v = VillageSpace.villageAt(p.serverLevel(), p.blockPosition());
        v.ifPresent(x -> {
            INSIDE.put(p.getUUID(), x.id());
            VillageVisits.mark(p, x);
        });
        return v;
    }

    private static double horizontal(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static Component title(VillageRegistry.Village v) {
        return v.patron().isEmpty() ? Component.translatable("mythic_age.village.title.plain")
                : Component.translatable("mythic_age.village.title", Component.translatable("mythic_age.god." + v.patron()));
    }

    /** 进村标题：有文明的村庄一律带阶段后缀，「宙斯之村 · 远古」（U10）；中立村庄不带。 */
    public static Component stagedTitle(VillageRegistry.Village v) {
        if (v.civ().isEmpty() || v.stage() == io.github.happysnappingturtle.mythicage.core.player.Age.NONE) {
            return title(v);
        }
        return Component.translatable("mythic_age.village.title.staged", title(v),
                Component.translatable("mythic_age.age." + v.stage().id()));
    }

    public static Component subtitle(VillageRegistry.Village v) {
        return v.civ().isEmpty() ? Component.translatable("mythic_age.village.neutral")
                : Component.translatable("mythic_age.civ." + v.civ());
    }
}
