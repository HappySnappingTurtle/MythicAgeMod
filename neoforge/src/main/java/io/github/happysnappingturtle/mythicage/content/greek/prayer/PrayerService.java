package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import io.github.happysnappingturtle.mythicage.runtime.ProducerProbe;
import io.github.happysnappingturtle.mythicage.runtime.ProducerProbes;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleFunction;

/**
 * 祈祷的公共部分（T10；GA-13 起入账、献供在 {@link TemplePrayer}）：祈祷位预留表、圣物加成 R 的来源、
 * 下雨判定、服务器倍率；定期清掉已经不是村庄神庙祭坛的预留。
 * <p>
 * 旧的玩家祭坛台账类型 {@value #PROBE_TYPE} 还登记着一个探针：开服时这些台账会被注销（{@code TempleService}），
 * 在那之前探针一律报「没在运转」、不产出。
 */
public final class PrayerService {
    /** 旧玩家祭坛的台账类型（GA-13 之前），只用来识别、注销。 */
    public static final String PROBE_TYPE = "greek_temple";
    public static final int SAMPLE_INTERVAL = 20;
    /** 主人在祭坛这个半径内算「在照看」（T5 照看期）。 */
    public static final int OWNER_NEARBY = 64;
    /** 「祈祷位走不到」提示保留的时长。 */
    static final int UNREACHABLE_MEMORY = 1200;

    static final PrayerSlots SLOTS = new PrayerSlots();
    /** 圣物加成 R（T16 接入）。 */
    private static volatile ToDoubleFunction<UUID> relicBonus = owner -> 0;
    /** 「此处在下雨」的判定；GameTest 结构上方有遮挡，测试时替换。 */
    private static volatile BiPredicate<Level, BlockPos> raining = Level::isRainingAt;

    private PrayerService() {
    }

    public static void init() {
        ProducerProbes.register(PROBE_TYPE, (server, ledger, owner, now, interval) -> ProducerProbe.Observation.NOT_RUNNING);
    }

    /** T16 圣物系统设置 R 的来源。 */
    public static void setRelicBonus(ToDoubleFunction<UUID> provider) {
        relicBonus = provider;
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SLOTS.clear();
    }

    static double multiplier() {
        return io.github.happysnappingturtle.mythicage.config.ModConfigs.worldRules().current().greek().prayer().rateMultiplier();
    }

    public enum OfferResult { OK, NOT_OFFERING, NOT_ENOUGH, NOT_OWNER, ALREADY, NOT_DEVOTEE }

    /** 圣物加成 R（供奉者）。 */
    static double relicR(UUID player) {
        return relicBonus.applyAsDouble(player);
    }

    /** 此处在下雨（GameTest 可替换）。 */
    static boolean raining(Level level, BlockPos pos) {
        return raining.test(level, pos);
    }

    // ───────────── 预留清理 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (WorldClock.now(server) % SAMPLE_INTERVAL == 0) {
            sample(server);
        }
    }

    /** 不再是村庄神庙祭坛（神庙扩建后祭坛挪了、村子没了）或区块没加载的，放掉预留。 */
    private static void sample(MinecraftServer server) {
        long now = WorldClock.now(server);
        for (GlobalPos key : SLOTS.altars(now)) {
            ServerLevel level = server.getLevel(key.dimension());
            if (level == null || !level.isLoaded(key.pos()) || VillageTemples.byAltar(server, key).isEmpty()) {
                SLOTS.releaseAll(key);
            }
        }
    }

    // ───────────── 测试与调试 ─────────────

    /** 只供 GameTest：直接放一条已到达的预留（不经过村民 AI）。 */
    public static void putClaimForTest(GlobalPos altar, UUID villager, BlockPos slot, boolean counted, long now) {
        SLOTS.put(altar, new PrayerSlots.Claim(villager, slot, now + 100_000, true, counted, !counted));
    }

    /** 只供 GameTest：替换下雨判定；传 null 恢复。 */
    public static void overrideRainForTest(BiPredicate<Level, BlockPos> f) {
        raining = f == null ? Level::isRainingAt : f;
    }

    /** 放掉某座祭坛的全部预留（祭坛被拆、测试清场）。 */
    public static void resetForTest(GlobalPos altar) {
        SLOTS.releaseAll(altar);
    }

    /** 被祈祷位预留了的村民（测试、调试命令）。 */
    public static List<UUID> claimants(GlobalPos altar, MinecraftServer server) {
        return SLOTS.live(altar, WorldClock.now(server)).stream().map(PrayerSlots.Claim::villager).toList();
    }
}
