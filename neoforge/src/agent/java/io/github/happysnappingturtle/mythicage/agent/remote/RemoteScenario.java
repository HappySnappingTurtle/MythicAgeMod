package io.github.happysnappingturtle.mythicage.agent.remote;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.agent.AgentEntrypoint;
import io.github.happysnappingturtle.mythicage.agent.AgentMode;
import io.github.happysnappingturtle.mythicage.agent.AutoWorldBootstrap;
import io.github.happysnappingturtle.mythicage.agent.ScenarioStep;
import io.github.happysnappingturtle.mythicage.agent.Steps;
import io.github.happysnappingturtle.mythicage.devtools.remote.RemoteChannel;
import io.github.happysnappingturtle.mythicage.devtools.remote.RemoteProtocol;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.tutorial.TutorialSteps;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 遥控口调度（T2b 设计文档 §3 执行模型）：世界就绪 → 会话轮换 → 环境准备 → 写 ready.json →
 * 每 tick 驱动当前指令，完成后写结果再读下一条。只在渲染线程（ClientTickEvent.Post）上运行。
 */
public final class RemoteScenario {
    private static final DateTimeFormatter SESSION_ID = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static boolean rotated;
    private static boolean initialized;
    private static RemoteContext ctx;
    private static long tick;
    private static int idleTicks;
    private static int idleLimitTicks;
    private static List<ScenarioStep> setupSteps;
    private static int setupIndex;
    private static RemoteProtocol.Command currentCommand;
    private static RemoteExecution currentExecution;
    private static long currentStartTick;
    private static Field mouseGrabbedField;

    private RemoteScenario() {
    }

    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!AutoWorldBootstrap.isReady()) {
            if (AutoWorldBootstrap.isFailed()) {
                AgentEntrypoint.LOG.error("[agent] 世界未能就绪，退出客户端");
                mc.stop();
            }
            return;
        }
        if (!initialized) {
            initialize(mc);
            return;
        }
        tick++;
        ctx.setTick(tick);
        ChatTap.setCurrentTick(tick);
        keepInputActive(mc);

        if (currentExecution != null) {
            driveCurrent(mc);
            return;
        }
        String line;
        try {
            line = ctx.channel().nextCompleteLine();
        } catch (IOException e) {
            AgentEntrypoint.LOG.error("[agent] 读取 commands.jsonl 失败", e);
            return;
        }
        if (line == null) {
            if (++idleTicks > idleLimitTicks) {
                writeResult(-1, "_idle_quit", true, null, List.of(), JsonNull.INSTANCE);
                AgentEntrypoint.LOG.info("[agent] 空闲超过 {} 分钟，退出", AgentMode.idleMinutes());
                mc.stop();
            }
            return;
        }
        idleTicks = 0;
        RemoteProtocol.Command cmd;
        try {
            cmd = RemoteProtocol.parse(line);
        } catch (RemoteProtocol.RemoteProtocolException e) {
            writeRaw(RemoteProtocol.result(-1, "?", false, e.getMessage(), tick, List.of(), null));
            return;
        }
        if ("quit".equals(cmd.cmd())) {
            writeResult(cmd.id(), cmd.cmd(), true, null, List.of(), JsonNull.INSTANCE);
            AgentEntrypoint.LOG.info("[agent] 收到 quit，退出客户端");
            mc.stop();
            return;
        }
        currentCommand = cmd;
        currentStartTick = tick;
        try {
            currentExecution = RemoteCommands.build(ctx, cmd);
        } catch (RuntimeException e) {
            writeResult(cmd.id(), cmd.cmd(), false, describe(e), List.of(), JsonNull.INSTANCE);
            currentCommand = null;
            return;
        }
        driveCurrent(mc);
    }

    private static void driveCurrent(Minecraft mc) {
        boolean done;
        String crash = null;
        try {
            done = currentExecution.tick(mc);
        } catch (RuntimeException e) {
            done = true;
            crash = describe(e);
            AgentEntrypoint.LOG.warn("[agent] 指令 #{} {} 执行异常", currentCommand.id(), currentCommand.cmd(), e);
        }
        if (!done) {
            return;
        }
        boolean ok = crash == null && currentExecution.ok();
        if (!ok) {
            releaseAllKeys(mc); // 任何失败都松开全部按键，避免卡住
        }
        JsonElement data = crash == null ? currentExecution.data() : JsonNull.INSTANCE;
        writeResult(currentCommand.id(), currentCommand.cmd(), ok, crash != null ? crash : currentExecution.error(),
                ChatTap.since(currentStartTick), data);
        currentCommand = null;
        currentExecution = null;
    }

    private static String describe(Exception e) {
        return e.getClass().getSimpleName() + "：" + e.getMessage();
    }

    private static void writeResult(long id, String cmd, boolean ok, String error, List<String> chat,
                                    JsonElement data) {
        writeRaw(RemoteProtocol.result(id, cmd, ok, error, tick, chat, data));
    }

    private static void writeRaw(String line) {
        try {
            ctx.channel().writeResult(line);
        } catch (IOException e) {
            AgentEntrypoint.LOG.error("[agent] 写 results.jsonl 失败", e);
        }
    }

    private static void initialize(Minecraft mc) {
        if (!rotated) {
            // 窗口在后台没有焦点时，原版会因失焦自动弹出暂停界面，盖掉遥控口打开或关闭的界面
            mc.options.pauseOnLostFocus = false;
            RemoteChannel channel = new RemoteChannel(mc.gameDirectory.toPath().resolve("agent_out").resolve("remote"));
            String now = LocalDateTime.now().format(SESSION_ID);
            try {
                channel.rotateSession(channel.previousSessionIdOr(now));
            } catch (IOException e) {
                AgentEntrypoint.LOG.error("[agent] 会话轮换失败", e);
            }
            ctx = new RemoteContext(now, channel);
            idleLimitTicks = AgentMode.idleMinutes() * 20 * 60;
            setupSteps = environmentSteps(mc);
            setupIndex = 0;
            rotated = true;
            return;
        }
        if (setupIndex < setupSteps.size()) {
            if (setupSteps.get(setupIndex).tick(mc)) {
                setupIndex++;
            }
            return;
        }
        writeReady(mc);
        initialized = true;
        AgentEntrypoint.LOG.info("[agent] 遥控口就绪 sessionId={} 目录={}", ctx.sessionId(), ctx.channel().root());
    }

    /** 让画面好读：白天、晴天、不刷怪、关闭教程提示、切换语言（-PremoteRealtime=true 时保留真实昼夜与天气）。 */
    private static List<ScenarioStep> environmentSteps(Minecraft mc) {
        List<ScenarioStep> steps = new ArrayList<>();
        steps.add(Steps.action(() -> {
            mc.options.tutorialStep = TutorialSteps.NONE;
            mc.getTutorial().setStep(TutorialSteps.NONE);
            mc.getToasts().clear();
        }));
        if (!AgentMode.realtime() && AgentMode.server().isEmpty()) {
            for (String cmd : new String[]{"gamerule doDaylightCycle false", "time set noon", "weather clear",
                    "gamerule doWeatherCycle false", "gamerule doMobSpawning false"}) {
                steps.add(Steps.runCommand(cmd));
            }
        }
        String lang = AgentMode.language();
        if (!lang.equals(mc.options.languageCode)) {
            List<CompletableFuture<Void>> reload = new ArrayList<>(1);
            steps.add(Steps.action(() -> {
                mc.getLanguageManager().setSelected(lang);
                mc.options.languageCode = lang;
                mc.options.save();
                reload.add(mc.reloadResourcePacks());
                AgentEntrypoint.LOG.info("[agent] 切换语言到 {}，等待资源重载", lang);
            }));
            steps.add(Steps.until(() -> !reload.isEmpty() && reload.get(0).isDone(), 20 * 60,
                    () -> AgentEntrypoint.LOG.warn("[agent] 语言切换超时，继续")));
        }
        steps.add(Steps.action(() -> mc.getToasts().clear()));
        steps.add(Steps.waitTicks(5));
        return steps;
    }

    private static void writeReady(Minecraft mc) {
        JsonObject ready = new JsonObject();
        ready.addProperty("sessionId", ctx.sessionId());
        ready.addProperty("world", AutoWorldBootstrap.worldName());
        ready.addProperty("gamemode", mc.gameMode != null ? mc.gameMode.getPlayerMode().getName() : "unknown");
        ready.addProperty("guiScale", (int) mc.getWindow().getGuiScale());
        ready.addProperty("width", mc.getWindow().getWidth());
        ready.addProperty("height", mc.getWindow().getHeight());
        ready.addProperty("language", mc.options.languageCode);
        try {
            ctx.channel().writeReady(ready);
        } catch (IOException e) {
            AgentEntrypoint.LOG.error("[agent] 写 ready.json 失败", e);
        }
    }

    /**
     * 窗口在后台时，原版「按住左键持续挖掘或攻击」要求鼠标处于捕获状态；这里只打开捕获标志，
     * 不激活窗口，避免用户鼠标经过时带动视角（参考项目 R5）。
     */
    private static void keepInputActive(Minecraft mc) {
        if (mc.screen != null || mc.mouseHandler.isMouseGrabbed()) {
            return;
        }
        if (mc.isWindowActive()) {
            mc.mouseHandler.grabMouse();
            return;
        }
        try {
            if (mouseGrabbedField == null) {
                mouseGrabbedField = net.minecraft.client.MouseHandler.class.getDeclaredField("mouseGrabbed");
                mouseGrabbedField.setAccessible(true);
            }
            mouseGrabbedField.setBoolean(mc.mouseHandler, true);
        } catch (ReflectiveOperationException e) {
            AgentEntrypoint.LOG.warn("[agent] 无法在后台打开鼠标捕获，左键操作需要窗口在前台", e);
        }
    }

    private static void releaseAllKeys(Minecraft mc) {
        for (KeyMapping key : mc.options.keyMappings) {
            key.setDown(false);
        }
    }
}
