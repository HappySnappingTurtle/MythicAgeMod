package io.github.happysnappingturtle.mythicage.agent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 自动创建或打开测试世界（T2b 设计文档 §5）。
 * <p>
 * 只在 {@code ClientTickEvent.Post} 中轮询是否处于主菜单，<b>不注册 ScreenEvent 监听器</b>：
 * {@code Minecraft.setScreen} 在 Opening 事件返回后才赋值，在事件处理器里同步开新界面会被覆盖（参考项目记录过的重入问题）。
 * 世界创建走原版公开的 {@code WorldOpenFlows.createFreshLevel}，与主菜单「演示世界」按钮同一路径，不用反射。
 */
public final class AutoWorldBootstrap {
    /** 固定种子：保证每次新建的地形一致，截图可以前后对比。 */
    private static final long FIXED_SEED = 2026_10_13L;
    /** 建世界或进世界最多等待 120 秒。 */
    private static final int LOADING_TIMEOUT_TICKS = 20 * 120;

    private enum State { IDLE, LOADING, READY, FAILED }

    private static volatile String worldName = "ma_autotest";
    private static volatile boolean deleteBeforeCreate;
    private static volatile boolean generateStructures;
    private static volatile State state = State.IDLE;
    private static int loadingTicks;

    private AutoWorldBootstrap() {
    }

    /** 必须在第一次 tick 之前调用。 */
    public static void configure(String name, boolean fresh, boolean structures) {
        worldName = name;
        deleteBeforeCreate = fresh;
        generateStructures = structures;
    }

    public static boolean isReady() {
        return state == State.READY;
    }

    public static boolean isFailed() {
        return state == State.FAILED;
    }

    public static String worldName() {
        return AgentMode.server().isEmpty() ? worldName : "server:" + AgentMode.server();
    }

    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        switch (state) {
            case IDLE -> tryStart(mc);
            case LOADING -> checkLoading(mc);
            default -> {
            }
        }
    }

    private static void tryStart(Minecraft mc) {
        // 新的运行目录首次启动时，原版先显示无障碍引导界面而不是主菜单：关闭引导并进入主菜单
        if (mc.screen instanceof AccessibilityOnboardingScreen) {
            AgentEntrypoint.LOG.info("[agent] 跳过首次启动的无障碍引导界面");
            mc.options.onboardAccessibility = false;
            mc.options.save();
            mc.setScreen(new TitleScreen());
            return;
        }
        if (!(mc.screen instanceof TitleScreen)) {
            return;
        }
        state = State.LOADING;
        loadingTicks = 0;
        if (!AgentMode.server().isEmpty()) {
            // 专服实测（T24 §10.2）：不开本地世界，连进指定的专服
            String addr = AgentMode.server();
            AgentEntrypoint.LOG.info("[agent] 连接专服 {}", addr);
            net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen, mc,
                    net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(addr),
                    new net.minecraft.client.multiplayer.ServerData("ma_server", addr, net.minecraft.client.multiplayer.ServerData.Type.OTHER),
                    false, null);
            return;
        }
        if (deleteBeforeCreate) {
            deleteBeforeCreate = false; // 只删一次
            deleteExistingSave(mc);
        }
        if (mc.getLevelSource().levelExists(worldName)) {
            AgentEntrypoint.LOG.info("[agent] 打开已有世界 {}", worldName);
            mc.createWorldOpenFlows().openWorld(worldName, () -> {
                AgentEntrypoint.LOG.error("[agent] 打开世界失败：{}", worldName);
                state = State.IDLE;
            });
        } else {
            AgentEntrypoint.LOG.info("[agent] 新建世界 {}（固定种子 {}，结构 {}）", worldName, FIXED_SEED, generateStructures);
            LevelSettings settings = new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                    true, new GameRules(), WorldDataConfiguration.DEFAULT);
            WorldOptions options = new WorldOptions(FIXED_SEED, generateStructures, false);
            // createFreshLevel 失败时会自己回到原界面、不调用任何回调；靠下面的超时兜底
            mc.createWorldOpenFlows().createFreshLevel(worldName, settings, options,
                    WorldPresets::createNormalWorldDimensions, mc.screen);
        }
    }

    private static void deleteExistingSave(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(worldName);
        if (!Files.exists(dir)) {
            return;
        }
        AgentEntrypoint.LOG.info("[agent] 按 -PremoteFresh 删除旧存档 {}", dir);
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    AgentEntrypoint.LOG.warn("[agent] 删除失败：{}", p, e);
                }
            });
        } catch (IOException e) {
            AgentEntrypoint.LOG.error("[agent] 删除旧存档失败：{}", dir, e);
        }
    }

    private static void checkLoading(Minecraft mc) {
        if (mc.player != null && mc.level != null) {
            state = State.READY;
            AgentEntrypoint.LOG.info("[agent] 世界就绪：{}", worldName);
            return;
        }
        if (++loadingTicks > LOADING_TIMEOUT_TICKS) {
            state = State.FAILED;
            AgentEntrypoint.LOG.error("[agent] 建立或进入世界超时（{} tick）", loadingTicks);
        }
    }
}
