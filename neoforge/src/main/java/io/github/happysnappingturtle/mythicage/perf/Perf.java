package io.github.happysnappingturtle.mythicage.perf;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.perf.PerfStats;
import io.github.happysnappingturtle.mythicage.core.perf.ChunkTimings;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

/**
 * 性能探针（T24 设计文档 §9.1）：模组的每个每刻处理器都经 {@link #tick} 注册，开着采样时按类名分类计时；
 * {@code /mythicage dev perf start [秒]} 开始一段采样，{@code report} 输出各分类每刻耗时（平均 / p95 / 最大）、
 * 模组合计、服务器 MSPT 与本模组实体数，并写到 {@code logs/mythic_age_perf.txt}。没在采样时只多一次布尔判断。
 */
public final class Perf {
    /** 窗口：最多 6000 刻（5 分钟）。 */
    public static final int WINDOW = 6000;
    private static final PerfStats STATS = new PerfStats(WINDOW);
    private static volatile boolean running;
    private static volatile ChunkTimings chunks = new ChunkTimings();
    private static final Map<Entity, Long> ENTITY_START = new IdentityHashMap<>();
    private static int scanPeak;
    private static long scanCells;
    private static int scanTicks;
    private static int remaining;

    private Perf() {
    }

    /** 注册一个每刻处理器，并以 category 计时。 */
    public static void tick(IEventBus game, String category, Consumer<ServerTickEvent.Post> handler) {
        game.addListener((ServerTickEvent.Post e) -> {
            if (!running) {
                handler.accept(e);
                return;
            }
            long t = System.nanoTime();
            try {
                handler.accept(e);
            } finally {
                STATS.add(category, System.nanoTime() - t);
            }
        });
    }

    public static void register(IEventBus game) {
        game.addListener((ChunkEvent.Load e) -> {
            if (running && e.isNewChunk() && e.getLevel() instanceof ServerLevel level) {
                chunks.generated(chunkKey(level.getChunkSource().getGenerator(), e.getChunk().getPos()));
            }
        });
        game.addListener(EventPriority.LOWEST, Perf::beforeEntity);
        game.addListener(EventPriority.HIGHEST, Perf::afterEntity);
        game.addListener((ServerStoppedEvent e) -> {
            running = false;
            ENTITY_START.clear();
            STATS.reset();
            chunks = new ChunkTimings();
        });
        game.addListener(EventPriority.LOWEST, (ServerTickEvent.Post e) -> {
            if (running) {
                int cells = TempleService.cellsLastTick();
                scanPeak = Math.max(scanPeak, cells);
                scanCells += cells;
                if (cells > 0) {
                    scanTicks++;
                }
                STATS.endTick();
                if (--remaining <= 0) {
                    running = false;
                    MythicAge.LOGGER.info("[perf] 采样结束：{} 刻", STATS.ticks());
                }
            }
            ENTITY_START.clear(); // 其他模组取消 Pre 时没有 Post，不能留下实体引用
        });
        game.addListener(Perf::onRegisterCommands);
    }

    public static boolean running() {
        return running;
    }

    /** 开始采样（GameTest 也用）。 */
    public static void start(int ticks) {
        running = false;
        STATS.reset();
        ENTITY_START.clear();
        chunks = new ChunkTimings();
        scanPeak = 0;
        scanCells = 0;
        scanTicks = 0;
        remaining = Math.max(1, Math.min(WINDOW, ticks));
        running = true;
    }

    public static PerfStats stats() {
        return STATS;
    }

    private static void beforeEntity(EntityTickEvent.Pre e) {
        if (!running || e.getEntity().level().isClientSide()) {
            return;
        }
        Entity entity = e.getEntity();
        if (entity instanceof Villager || BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(MythicAgeIds.MOD_ID)) {
            ENTITY_START.put(entity, System.nanoTime());
        }
    }

    private static void afterEntity(EntityTickEvent.Post e) {
        if (!running || e.getEntity().level().isClientSide()) {
            return;
        }
        Long start = ENTITY_START.remove(e.getEntity());
        if (start != null) {
            STATS.add("entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(e.getEntity().getType()), System.nanoTime() - start);
        }
    }

    /** 同一生成器实例区分维度；区块规划和落块合并。异步旧采样不能污染新窗口。 */
    public record ChunkSample(ChunkTimings window, String key, long start) {
        public void finish() {
            if (running && window == chunks) {
                window.add(key, System.nanoTime() - start);
            }
        }
    }

    public static ChunkSample beginChunk(ChunkGenerator generator, ChunkPos pos) {
        return running ? new ChunkSample(chunks, chunkKey(generator, pos), System.nanoTime()) : null;
    }

    private static String chunkKey(ChunkGenerator generator, ChunkPos pos) {
        return System.identityHashCode(generator) + ":" + pos.toLong();
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mythicage").then(Commands.literal("dev").requires(s -> s.hasPermission(2))
                .then(Commands.literal("perf")
                        .then(Commands.literal("start").executes(c -> start(c, 60))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, WINDOW / 20))
                                        .executes(c -> start(c, IntegerArgumentType.getInteger(c, "seconds")))))
                        .then(Commands.literal("report").executes(Perf::report)))));
    }

    private static int start(CommandContext<CommandSourceStack> c, int seconds) {
        start(seconds * 20);
        c.getSource().sendSuccess(() -> Component.literal("[perf] 开始采样 " + seconds + " 秒"), true);
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> c) {
        List<String> lines = lines(c.getSource().getServer());
        lines.forEach(l -> c.getSource().sendSuccess(() -> Component.literal(l), false));
        Path out = FMLPaths.GAMEDIR.get().resolve("logs/mythic_age_perf.txt");
        try {
            Files.createDirectories(out.getParent());
            Files.write(out, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            MythicAge.LOGGER.warn("[perf] 写报告失败：{}", e.getMessage());
        }
        return 1;
    }

    /** 报告各行（中文，方便贴进文档）。 */
    public static List<String> lines(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        PerfStats.Summary total = STATS.summary(PerfStats.TOTAL);
        out.add(String.format(Locale.ROOT, "[perf] %d 刻%s；服务器 MSPT %.2f ms", total.samples(), running ? "（采样中）" : "",
                server.getAverageTickTimeNanos() / 1.0e6));
        out.add(fmt("已测每刻合计", total));
        out.add("口径：模组每刻处理器 + 模组实体/原版村民完整 tick（含原版开销）；不含其他事件、渲染、网络与区块生成。不是整个模组的独占 CPU。MSPT 为服务器自身近期平均。");
        for (String cat : STATS.categories()) {
            out.add(fmt("  " + cat, STATS.summary(cat)));
        }
        Map<String, Integer> counts = new TreeMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                var id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                if (id.getNamespace().equals(MythicAgeIds.MOD_ID)) {
                    counts.merge(id.getPath(), 1, Integer::sum);
                }
            }
        }
        out.add("本模组实体：" + (counts.isEmpty() ? "无" : counts.toString()));
        long villages = VillageRegistry.get(server).all().stream().filter(v -> {
            ServerLevel level = server.getLevel(v.bell().dimension());
            return level != null && level.isLoaded(v.bell().pos());
        }).count();
        long arenas = TrialArenas.get(server).all().stream().filter(a -> a.phase().fighting()).count();
        out.add("已加载村庄：" + villages + "；进行中竞技场：" + arenas
                + "；已加载预算内临时实体：" + io.github.happysnappingturtle.mythicage.power.TransientEntities.loadedCount());
        out.add("神庙扫描：" + scanCells + " 格 / " + scanTicks + " 个工作刻；单刻峰值 " + scanPeak + " 格");
        PerfStats.Summary generation = chunks.summary();
        out.add("结构生成：" + generation.samples() + " 个有规划/落块调用的区块；" + fmt("每区块", generation)
                + "；容量丢弃 " + chunks.dropped() + "。包含失败的规划尝试，不含原版地形/寻址；0 样本不代表达标。");
        out.add(String.format(Locale.ROOT, "新生成并载入区块：%d；窗口结构总耗时折算平均 %.3f ms/区块。"
                        + "包括为相邻未载入区块做的规划成本；与上行的有工作区块分母不同，0 区块不代表达标。",
                chunks.generatedCount(), chunks.amortizedMicros() / 1000.0));
        return out;
    }

    private static String fmt(String name, PerfStats.Summary s) {
        return String.format(Locale.ROOT, "%s：平均 %.3f ms · p95 %.3f ms · 最大 %.3f ms", name, s.avgMicros() / 1000,
                s.p95Micros() / 1000, s.maxMicros() / 1000);
    }
}
