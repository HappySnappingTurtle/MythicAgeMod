package com.example.mythic_age.client.visual;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.validation.ValidationCommand;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local-only visual probe for MCP driven creature screenshots.
 *
 * <p>The probe is disabled unless the JVM is launched with
 * {@code -Dmythic.visualProbe=true}. It binds to loopback only and is meant for
 * design-gate screenshots in a local creative test world.</p>
 */
public final class VisualProbeServer {
    private static final Queue<CaptureTask> CAPTURE_TASKS = new ConcurrentLinkedQueue<>();
    private static final int DEFAULT_GUI_CLEAR_TICKS = 6;
    private static HttpServer server;
    private static boolean tickRegistered;
    private static boolean autoWorldAttempted;
    private static boolean captureSessionStateCaptured;
    private static boolean restorePauseOnLostFocus;

    private VisualProbeServer() {
    }

    public static synchronized void startIfEnabled() {
        if (!Boolean.getBoolean("mythic.visualProbe") || server != null) {
            return;
        }
        registerTickHandler();
        int port = Integer.getInteger("mythic.visualProbePort", 47620);
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
            server.createContext("/health", VisualProbeServer::handleHealth);
            server.createContext("/open_world", VisualProbeServer::handleOpenWorld);
            server.createContext("/capture", VisualProbeServer::handleCapture);
            server.createContext("/capture_sequence", VisualProbeServer::handleCaptureSequence);
            server.createContext("/command", VisualProbeServer::handleCommand);
            server.createContext("/validate_nbt", VisualProbeServer::handleValidateNbt);
            server.createContext("/validate_nbt_restart", VisualProbeServer::handleValidateNbtRestart);
            server.createContext("/validate_nbt_restart_prepare", VisualProbeServer::handleValidateNbtRestartPrepare);
            server.createContext("/validate_nbt_restart_verify", VisualProbeServer::handleValidateNbtRestartVerify);
            server.createContext("/validate_god_power_playtest", VisualProbeServer::handleValidateGodPowerPlaytest);
            server.createContext("/shutdown", VisualProbeServer::handleShutdown);
            server.createContext("/quit_client", VisualProbeServer::handleQuitClient);
            server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "MythicAgeVisualProbe");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
            MythicAgeMod.LOGGER.info("Mythic Age visual probe listening on 127.0.0.1:{}", port);
        } catch (IOException e) {
            MythicAgeMod.LOGGER.error("Failed to start Mythic Age visual probe", e);
        }
    }

    private static synchronized void registerTickHandler() {
        if (tickRegistered) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(VisualProbeServer::tick);
        tickRegistered = true;
    }

    private static void tick(MinecraftClient client) {
        tryOpenConfiguredWorld(client);
        keepProbeSessionUnpaused(client);
        CaptureTask task = CAPTURE_TASKS.peek();
        if (task == null) {
            return;
        }
        if (client.world == null || client.player == null) {
            restoreVisualState(client, task);
            finishTask(task, "{\"ok\":false,\"error\":\"client_not_in_world\"}", true);
            return;
        }
        if (!task.commandsSent) {
            task.commandsSent = true;
            task.ticksRemaining = Math.max(1, task.delayTicks);
            task.guiClearTicksRemaining = Math.max(1, task.requiredGuiClearTicks);
            beginCleanCaptureState(client, task);
            closeCurrentScreen(client, task);
            issueVisualSetupCommands(client, task);
            return;
        }
        if (closeCurrentScreen(client, task)) {
            task.guiClearTicksRemaining = Math.max(1, task.requiredGuiClearTicks);
            return;
        }
        if (task.guiClearTicksRemaining > 0) {
            task.guiClearTicksRemaining--;
            return;
        }
        task.ticksRemaining--;
        if (task.ticksRemaining > 0) {
            return;
        }
        if (closeCurrentScreen(client, task)) {
            task.guiClearTicksRemaining = Math.max(1, task.requiredGuiClearTicks);
            return;
        }
        takeScreenshot(client, task);
    }

    private static boolean closeCurrentScreen(MinecraftClient client, CaptureTask task) {
        if (client.currentScreen == null) {
            return false;
        }
        task.screenWasOpen = true;
        if (task.closedScreenClass.isBlank()) {
            task.closedScreenClass = client.currentScreen.getClass().getName();
        }
        client.setScreen(null);
        return true;
    }

    private static void issueVisualSetupCommands(MinecraftClient client, CaptureTask task) {
        executeCommand(client, "time set day");
        executeCommand(client, "weather clear");
        executeCommand(client, "gamemode creative @p");
        executeCommand(client, "effect clear @p");
        executeCommand(client, "fill -18 120 -18 18 150 18 air replace");
        executeCommand(client, "fill -12 119 -12 12 119 12 smooth_quartz replace");
        executeCommand(client, "fill -14 119 -14 -13 119 14 polished_blackstone replace");
        executeCommand(client, "fill 13 119 -14 14 119 14 polished_blackstone replace");
        executeCommand(client, "fill -14 119 -14 14 119 -13 polished_blackstone replace");
        executeCommand(client, "fill -14 119 13 14 119 14 polished_blackstone replace");
        executeCommand(client, "kill @e[type=!player,x=0,y=120,z=0,distance=..96]");
        executeCommand(client, "kill @e[type=minecraft:item,x=0,y=120,z=0,distance=..96]");
        executeCommand(client, "kill @e[type=minecraft:experience_orb,x=0,y=120,z=0,distance=..96]");
        executeCommand(client, cameraCommand(task));
        executeCommand(client, "summon " + task.entityId + " 0 120 0 " + task.summonNbt);
    }

    private static String cameraCommand(CaptureTask task) {
        float distance = cameraDistance(task.entityId);
        return switch (task.view) {
            case "side" -> String.format(java.util.Locale.ROOT, "tp @p %.1f 122 0 90 12", distance);
            case "three_quarter" -> {
                float diagonal = distance * 0.72F;
                yield String.format(java.util.Locale.ROOT, "tp @p %.1f 122 %.1f 135 12", diagonal, diagonal);
            }
            default -> String.format(java.util.Locale.ROOT, "tp @p 0 122 %.1f 180 12", distance);
        };
    }

    private static float cameraDistance(String entityId) {
        if (entityId.endsWith(":hydra")) {
            return 6.0F;
        }
        if (entityId.endsWith(":pegasus")) {
            return 6.2F;
        }
        if (entityId.endsWith(":cyclops")) {
            return 7.0F;
        }
        if (entityId.endsWith(":colossus")) {
            return 9.0F;
        }
        return 7.0F;
    }

    private static void sendCommand(MinecraftClient client, String command) {
        Objects.requireNonNull(client.player).networkHandler.sendChatCommand(command);
    }

    private static int executeCommand(MinecraftClient client, String command) {
        MinecraftServer integratedServer = client.getServer();
        if (integratedServer != null) {
            String prefixedCommand = command.startsWith("/") ? command : "/" + command;
            CompletableFuture<Integer> future = new CompletableFuture<>();
            integratedServer.execute(() -> {
                try {
                    future.complete(integratedServer.getCommandManager().executeWithPrefix(
                            integratedServer.getCommandSource().withLevel(4),
                            prefixedCommand));
                } catch (Exception e) {
                    MythicAgeMod.LOGGER.error("Visual probe command failed: {}", prefixedCommand, e);
                    future.complete(0);
                }
            });
            try {
                return future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                MythicAgeMod.LOGGER.error("Visual probe command timed out: {}", prefixedCommand, e);
                return 0;
            }
        }
        sendCommand(client, command.startsWith("/") ? command.substring(1) : command);
        return 0;
    }

    private static void takeScreenshot(MinecraftClient client, CaptureTask task) {
        int frameIndex = task.screenshots.size();
        String frameSuffix = task.frameCount > 1
                ? "_frame_" + String.format(java.util.Locale.ROOT, "%02d", frameIndex)
                : "";
        String fileName = "visual_probe_" + sanitize(task.creatureId) + "_" + sanitize(task.view)
                + frameSuffix + "_"
                + System.currentTimeMillis() + ".png";
        File screenshotDir = new File(client.runDirectory, "screenshots");
        File screenshot = new File(screenshotDir, fileName);
        try {
            ScreenshotRecorder.saveScreenshot(client.runDirectory, fileName, client.getFramebuffer(), text -> {
            });
            task.screenshots.add(screenshot.getAbsolutePath());
            if (task.screenshots.size() < task.frameCount) {
                task.ticksRemaining = Math.max(1, task.frameIntervalTicks);
                return;
            }
            restoreVisualState(client, task);
            finishTask(task, captureResultPayload(task, screenshot), true);
        } catch (Exception e) {
            restoreVisualState(client, task);
            finishTask(task, "{\"ok\":false,\"error\":\"" + jsonEscape(e.getMessage()) + "\"}", true);
        }
    }

    private static String captureResultPayload(CaptureTask task, File screenshot) {
        return "{"
                    + "\"ok\":true,"
                    + "\"entity_id\":\"" + jsonEscape(task.entityId) + "\","
                    + "\"creature_id\":\"" + jsonEscape(task.creatureId) + "\","
                    + "\"view\":\"" + jsonEscape(task.view) + "\","
                    + "\"motion_mode\":\"" + jsonEscape(task.motionMode) + "\","
                    + "\"screen_clear\":true,"
                    + "\"hud_hidden_during_capture\":true,"
                    + "\"debug_hidden_during_capture\":true,"
                    + "\"screen_was_open\":" + task.screenWasOpen + ","
                    + "\"closed_screen\":\"" + jsonEscape(task.closedScreenClass) + "\","
                    + "\"required_gui_clear_ticks\":" + task.requiredGuiClearTicks + ","
                    + "\"frame_count\":" + task.frameCount + ","
                    + "\"frame_interval_ticks\":" + task.frameIntervalTicks + ","
                    + "\"screenshots\":" + jsonArray(task.screenshots) + ","
                    + "\"screenshot\":\"" + jsonEscape(screenshot.getAbsolutePath()) + "\""
                    + "}";
    }

    private static void beginCleanCaptureState(MinecraftClient client, CaptureTask task) {
        if (task.visualStateChanged) {
            return;
        }
        task.restoreHudHidden = client.options.hudHidden;
        task.restoreDebugEnabled = client.options.debugEnabled;
        client.options.hudHidden = true;
        client.options.debugEnabled = false;
        task.visualStateChanged = true;
    }

    private static void restoreVisualState(MinecraftClient client, CaptureTask task) {
        if (!task.visualStateChanged) {
            return;
        }
        client.options.hudHidden = task.restoreHudHidden;
        client.options.debugEnabled = task.restoreDebugEnabled;
        task.visualStateChanged = false;
    }

    private static void keepProbeSessionUnpaused(MinecraftClient client) {
        if (!captureSessionStateCaptured) {
            restorePauseOnLostFocus = client.options.pauseOnLostFocus;
            captureSessionStateCaptured = true;
        }
        client.options.pauseOnLostFocus = false;
    }

    private static void finishTask(CaptureTask task, String payload, boolean removeFromQueue) {
        if (removeFromQueue) {
            CAPTURE_TASKS.remove(task);
        }
        task.future.complete(payload);
    }

    private static void tryOpenConfiguredWorld(MinecraftClient client) {
        if (autoWorldAttempted || client.world != null || client.player != null) {
            return;
        }
        String worldName = configuredAutoWorldName();
        if (worldName.isBlank() || client.currentScreen == null) {
            return;
        }
        autoWorldAttempted = true;
        try {
            if (!client.getLevelStorage().levelExists(worldName)) {
                MythicAgeMod.LOGGER.error("Visual probe auto-world does not exist: {}", worldName);
                return;
            }
            MythicAgeMod.LOGGER.info("Visual probe opening auto-world: {}", worldName);
            client.createIntegratedServerLoader().start(client.currentScreen, worldName);
        } catch (Exception e) {
            MythicAgeMod.LOGGER.error("Failed to open visual probe auto-world {}", worldName, e);
        }
    }

    private static String configuredAutoWorldName() {
        String encoded = System.getProperty("mythic.visualProbeWorldBase64", "").trim();
        if (!encoded.isBlank()) {
            try {
                return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8).trim();
            } catch (IllegalArgumentException e) {
                MythicAgeMod.LOGGER.error("Invalid visual probe base64 world name", e);
            }
        }
        return System.getProperty("mythic.visualProbeWorld", "").trim();
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        String payload = "{"
                + "\"ok\":true,"
                + "\"enabled\":true,"
                + "\"client_ready\":" + (client != null) + ","
                + "\"in_world\":" + (client != null && client.world != null) + ","
                + "\"player_present\":" + (client != null && client.player != null) + ","
                + "\"queued_captures\":" + CAPTURE_TASKS.size() + ","
                + "\"timestamp\":\"" + jsonEscape(Instant.now().toString()) + "\""
                + "}";
        writeJson(exchange, 200, payload);
    }

    private static void handleCapture(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        String body = readBody(exchange);
        String entityId = extractJsonString(body, "entity_id", "");
        String creatureId = extractJsonString(body, "creature_id", entityId.replace("mythic_age:", ""));
        String view = normalizeView(extractJsonString(body, "view", "front"));
        int delayTicks = extractJsonInt(body, "delay_ticks", 50);
        int guiClearTicks = extractJsonInt(body, "gui_clear_ticks", DEFAULT_GUI_CLEAR_TICKS);
        if (!entityId.startsWith("mythic_age:")) {
            writeJson(exchange, 400, "{\"ok\":false,\"error\":\"entity_id_must_be_namespaced\"}");
            return;
        }
        CaptureTask task = new CaptureTask(entityId, creatureId, view, delayTicks, guiClearTicks);
        CAPTURE_TASKS.add(task);
        try {
            writeJson(exchange, 200, task.future.get(20, TimeUnit.SECONDS));
        } catch (Exception e) {
            CAPTURE_TASKS.remove(task);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> restoreVisualState(client, task));
            }
            writeJson(exchange, 504, "{\"ok\":false,\"error\":\"capture_timeout\"}");
        }
    }

    private static void handleCaptureSequence(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        String body = readBody(exchange);
        String entityId = extractJsonString(body, "entity_id", "");
        String creatureId = extractJsonString(body, "creature_id", entityId.replace("mythic_age:", ""));
        String view = normalizeView(extractJsonString(body, "view", "front"));
        int delayTicks = extractJsonInt(body, "delay_ticks", 50);
        int guiClearTicks = extractJsonInt(body, "gui_clear_ticks", DEFAULT_GUI_CLEAR_TICKS);
        int frameCount = Math.max(2, Math.min(24, extractJsonInt(body, "frame_count", 8)));
        int frameIntervalTicks = Math.max(1, Math.min(200, extractJsonInt(body, "frame_interval_ticks", 8)));
        String motionMode = extractJsonString(body, "motion_mode", "idle");
        String summonNbt = extractJsonString(body, "summon_nbt", "");
        if (!entityId.startsWith("mythic_age:")) {
            writeJson(exchange, 400, "{\"ok\":false,\"error\":\"entity_id_must_be_namespaced\"}");
            return;
        }
        if (summonNbt.isBlank()) {
            summonNbt = "free_ai".equals(motionMode) ? "{Silent:1b}" : "{NoAI:1b,Silent:1b}";
        }
        CaptureTask task = new CaptureTask(
                entityId,
                creatureId,
                view,
                delayTicks,
                guiClearTicks,
                frameCount,
                frameIntervalTicks,
                motionMode,
                summonNbt);
        CAPTURE_TASKS.add(task);
        long timeoutSeconds = Math.max(20L, ((long) delayTicks + (long) frameCount * frameIntervalTicks) / 20L + 12L);
        try {
            writeJson(exchange, 200, task.future.get(timeoutSeconds, TimeUnit.SECONDS));
        } catch (Exception e) {
            CAPTURE_TASKS.remove(task);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> restoreVisualState(client, task));
            }
            writeJson(exchange, 504, "{\"ok\":false,\"error\":\"capture_sequence_timeout\"}");
        }
    }

    private static void handleOpenWorld(HttpExchange exchange) throws IOException {
        try {
            handleOpenWorldUnsafe(exchange);
        } catch (Exception e) {
            MythicAgeMod.LOGGER.error("Visual probe open_world request failed", e);
            writeJson(exchange, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(e.getMessage()) + "\"}");
        }
    }

    private static void handleOpenWorldUnsafe(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"client_not_ready\"}");
            return;
        }
        String body = readBody(exchange);
        String worldName = extractJsonString(body, "world_name", "");
        if (worldName.isBlank()) {
            writeJson(exchange, 400, "{\"ok\":false,\"error\":\"world_name_required\"}");
            return;
        }
        if (client.world != null && client.player != null) {
            writeJson(exchange, 200, "{\"ok\":true,\"already_in_world\":true}");
            return;
        }
        if (!client.getLevelStorage().levelExists(worldName)) {
            writeJson(exchange, 404, "{\"ok\":false,\"error\":\"world_not_found\",\"world_name\":\""
                    + jsonEscape(worldName) + "\"}");
            return;
        }
        client.execute(() -> {
            try {
                client.createIntegratedServerLoader().start(client.currentScreen, worldName);
            } catch (Exception e) {
                MythicAgeMod.LOGGER.error("Failed to open visual probe world {}", worldName, e);
            }
        });
        writeJson(exchange, 202, "{\"ok\":true,\"world_name\":\"" + jsonEscape(worldName)
                + "\",\"opening\":true}");
    }

    private static void handleCommand(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"client_not_in_world\"}");
            return;
        }
        String body = readBody(exchange);
        String command = extractJsonString(body, "command", "");
        if (command.isBlank()) {
            writeJson(exchange, 400, "{\"ok\":false,\"error\":\"command_required\"}");
            return;
        }
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        String finalCommand = command;
        CompletableFuture<String> future = new CompletableFuture<>();
        client.execute(() -> {
            try {
                int result = executeCommand(client, finalCommand);
                future.complete("{\"ok\":true,\"command\":\"" + jsonEscape(finalCommand)
                        + "\",\"result\":" + result + "}");
            } catch (Exception e) {
                future.complete("{\"ok\":false,\"error\":\"" + jsonEscape(e.getMessage())
                        + "\",\"command\":\"" + jsonEscape(finalCommand) + "\"}");
            }
        });
        try {
            writeJson(exchange, 200, future.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            writeJson(exchange, 504, "{\"ok\":false,\"error\":\"command_timeout\",\"command\":\""
                    + jsonEscape(command) + "\"}");
        }
    }

    private static void handleValidateNbt(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"client_not_in_world\"}");
            return;
        }
        MinecraftServer integratedServer = client.getServer();
        if (integratedServer == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"integrated_server_required\"}");
            return;
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        integratedServer.execute(() -> {
            try {
                ValidationCommand.NbtValidationReport report = ValidationCommand.writeNbtReport(integratedServer);
                future.complete("{"
                        + "\"ok\":" + (report.failed() == 0) + ","
                        + "\"total\":" + report.total() + ","
                        + "\"passed\":" + report.passed() + ","
                        + "\"failed\":" + report.failed() + ","
                        + "\"report\":\"" + jsonEscape(report.output().getAbsolutePath()) + "\""
                        + "}");
            } catch (Exception e) {
                future.complete("{\"ok\":false,\"error\":\"" + jsonEscape(e.getMessage()) + "\"}");
            }
        });
        try {
            writeJson(exchange, 200, future.get(30, TimeUnit.SECONDS));
        } catch (Exception e) {
            writeJson(exchange, 504, "{\"ok\":false,\"error\":\"validate_nbt_timeout\"}");
        }
    }

    private static void handleValidateNbtRestart(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"client_not_in_world\"}");
            return;
        }
        MinecraftServer integratedServer = client.getServer();
        if (integratedServer == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"integrated_server_required\"}");
            return;
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        integratedServer.execute(() -> {
            try {
                ValidationCommand.RestartNbtValidationReport report = ValidationCommand.writeRestartNbtReport(integratedServer);
                future.complete("{"
                        + "\"ok\":" + (report.failed() == 0) + ","
                        + "\"total\":" + report.total() + ","
                        + "\"passed\":" + report.passed() + ","
                        + "\"failed\":" + report.failed() + ","
                        + "\"report\":\"" + jsonEscape(report.output().getAbsolutePath()) + "\""
                        + "}");
            } catch (Exception e) {
                future.complete("{\"ok\":false,\"error\":\"" + jsonEscape(e.getMessage()) + "\"}");
            }
        });
        try {
            writeJson(exchange, 200, future.get(30, TimeUnit.SECONDS));
        } catch (Exception e) {
            writeJson(exchange, 504, "{\"ok\":false,\"error\":\"validate_nbt_restart_timeout\"}");
        }
    }

    private static void handleValidateNbtRestartPrepare(HttpExchange exchange) throws IOException {
        handleRestartCyclePhase(exchange, "prepare");
    }

    private static void handleValidateNbtRestartVerify(HttpExchange exchange) throws IOException {
        handleRestartCyclePhase(exchange, "verify");
    }

    private static void handleValidateGodPowerPlaytest(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"client_not_in_world\"}");
            return;
        }
        MinecraftServer integratedServer = client.getServer();
        if (integratedServer == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"integrated_server_required\"}");
            return;
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        integratedServer.execute(() -> {
            try {
                ValidationCommand.GodPowerPlaytestReport report = ValidationCommand.writeGodPowerPlaytestReport(integratedServer);
                future.complete("{"
                        + "\"ok\":" + (report.failed() == 0) + ","
                        + "\"total\":" + report.total() + ","
                        + "\"passed\":" + report.passed() + ","
                        + "\"failed\":" + report.failed() + ","
                        + "\"report\":\"" + jsonEscape(report.output().getAbsolutePath()) + "\""
                        + "}");
            } catch (Exception e) {
                future.complete("{\"ok\":false,\"error\":\"" + jsonEscape(e.getMessage()) + "\"}");
            }
        });
        try {
            writeJson(exchange, 200, future.get(120, TimeUnit.SECONDS));
        } catch (Exception e) {
            writeJson(exchange, 504, "{\"ok\":false,\"error\":\"validate_god_power_playtest_timeout\"}");
        }
    }

    private static void handleRestartCyclePhase(HttpExchange exchange, String phase) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"client_not_in_world\"}");
            return;
        }
        MinecraftServer integratedServer = client.getServer();
        if (integratedServer == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"integrated_server_required\"}");
            return;
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        integratedServer.execute(() -> {
            try {
                ValidationCommand.RestartNbtValidationReport report = "prepare".equals(phase)
                        ? ValidationCommand.writeRestartCyclePrepareReport(integratedServer)
                        : ValidationCommand.writeRestartCycleVerifyReport(integratedServer);
                future.complete("{"
                        + "\"ok\":" + (report.failed() == 0) + ","
                        + "\"phase\":\"" + jsonEscape(phase) + "\","
                        + "\"total\":" + report.total() + ","
                        + "\"passed\":" + report.passed() + ","
                        + "\"failed\":" + report.failed() + ","
                        + "\"report\":\"" + jsonEscape(report.output().getAbsolutePath()) + "\""
                        + "}");
            } catch (Exception e) {
                future.complete("{\"ok\":false,\"phase\":\"" + jsonEscape(phase)
                        + "\",\"error\":\"" + jsonEscape(e.getMessage()) + "\"}");
            }
        });
        try {
            writeJson(exchange, 200, future.get(30, TimeUnit.SECONDS));
        } catch (Exception e) {
            writeJson(exchange, 504, "{\"ok\":false,\"error\":\"validate_nbt_restart_"
                    + jsonEscape(phase) + "_timeout\"}");
        }
    }

    private static void handleShutdown(HttpExchange exchange) throws IOException {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && captureSessionStateCaptured) {
            client.execute(() -> client.options.pauseOnLostFocus = restorePauseOnLostFocus);
        }
        writeJson(exchange, 200, "{\"ok\":true,\"message\":\"visual_probe_shutdown\"}");
        HttpServer current = server;
        server = null;
        if (current != null) {
            Thread stopper = new Thread(() -> current.stop(1), "MythicAgeVisualProbeStopper");
            stopper.setDaemon(true);
            stopper.start();
        }
    }

    private static void handleQuitClient(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"error\":\"method_not_allowed\"}");
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            writeJson(exchange, 409, "{\"ok\":false,\"error\":\"client_not_ready\"}");
            return;
        }
        if (captureSessionStateCaptured) {
            client.execute(() -> client.options.pauseOnLostFocus = restorePauseOnLostFocus);
        }
        long processId = ProcessHandle.current().pid();
        writeJson(exchange, 202, "{\"ok\":true,\"message\":\"visual_probe_client_quit_requested\",\"process_id\":"
                + processId + "}");
        client.execute(client::scheduleStop);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream stream = exchange.getRequestBody()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractJsonString(String json, String key, String fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return matcher.find() ? jsonUnescape(matcher.group(1)) : fallback;
    }

    private static int extractJsonInt(String json, String key, int fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    private static String normalizeView(String view) {
        return switch (view) {
            case "side", "three_quarter" -> view;
            default -> "front";
        };
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9_\\-]+", "_");
    }

    private static String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String jsonArray(List<String> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"').append(jsonEscape(values.get(index))).append('"');
        }
        return builder.append(']').toString();
    }

    private static String jsonUnescape(String value) {
        if (value == null || value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current != '\\' || index + 1 >= value.length()) {
                builder.append(current);
                continue;
            }
            char escaped = value.charAt(++index);
            switch (escaped) {
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case '\\' -> builder.append('\\');
                case '"' -> builder.append('"');
                case 'u' -> {
                    if (index + 4 < value.length()) {
                        try {
                            builder.append((char) Integer.parseInt(value.substring(index + 1, index + 5), 16));
                            index += 4;
                        } catch (NumberFormatException e) {
                            builder.append("\\u");
                        }
                    } else {
                        builder.append("\\u");
                    }
                }
                default -> builder.append(escaped);
            }
        }
        return builder.toString();
    }

    private static void writeJson(HttpExchange exchange, int statusCode, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }

    private static final class CaptureTask {
        final String entityId;
        final String creatureId;
        final String view;
        final int delayTicks;
        final int requiredGuiClearTicks;
        final int frameCount;
        final int frameIntervalTicks;
        final String motionMode;
        final String summonNbt;
        final List<String> screenshots = new ArrayList<>();
        final CompletableFuture<String> future = new CompletableFuture<>();
        boolean commandsSent;
        boolean visualStateChanged;
        boolean restoreHudHidden;
        boolean restoreDebugEnabled;
        boolean screenWasOpen;
        String closedScreenClass = "";
        int ticksRemaining;
        int guiClearTicksRemaining;

        CaptureTask(String entityId, String creatureId, String view, int delayTicks, int requiredGuiClearTicks) {
            this(entityId, creatureId, view, delayTicks, requiredGuiClearTicks, 1, 1, "single", "{NoAI:1b,Silent:1b}");
        }

        CaptureTask(String entityId, String creatureId, String view, int delayTicks, int requiredGuiClearTicks,
                    int frameCount, int frameIntervalTicks, String motionMode, String summonNbt) {
            this.entityId = entityId;
            this.creatureId = creatureId;
            this.view = view;
            this.delayTicks = delayTicks;
            this.requiredGuiClearTicks = Math.max(1, requiredGuiClearTicks);
            this.frameCount = Math.max(1, frameCount);
            this.frameIntervalTicks = Math.max(1, frameIntervalTicks);
            this.motionMode = motionMode == null || motionMode.isBlank() ? "single" : motionMode;
            this.summonNbt = summonNbt == null || summonNbt.isBlank() ? "{NoAI:1b,Silent:1b}" : summonNbt;
        }
    }
}
