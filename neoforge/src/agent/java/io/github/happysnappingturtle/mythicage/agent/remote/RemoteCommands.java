package io.github.happysnappingturtle.mythicage.agent.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.agent.FrameCapture;
import io.github.happysnappingturtle.mythicage.devtools.remote.RemoteChannel;
import io.github.happysnappingturtle.mythicage.devtools.remote.RemoteProtocol;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/** 指令名 → 执行体（T2b 设计文档 §3）。移植自 MCMagicBuild 的 U6 遥控口，去掉了该项目特有的指令。 */
final class RemoteCommands {
    private RemoteCommands() {
    }

    static RemoteExecution build(RemoteContext ctx, RemoteProtocol.Command cmd) {
        JsonObject a = cmd.args();
        return switch (cmd.cmd()) {
            case "state" -> simple(mc -> Snapshots.state(mc, ctx.sessionId(), ctx.tick(), ChatTap.recent(10)));
            case "screen" -> simple(mc -> {
                if (mc.screen == null) {
                    throw new CommandFailure("当前没有打开界面");
                }
                return Snapshots.screen(mc);
            });
            case "capture" -> new CaptureExecution(ctx, optString(a, "name", "frame"));
            case "key" -> keyCmd(a);
            case "hotbar" -> hotbarCmd(a);
            case "look" -> simple(mc -> look(mc, a));
            case "mouse" -> simple(mc -> mouse(mc, a));
            case "click" -> clickCmd(a);
            case "drag" -> dragCmd(a);
            case "text" -> simple(mc -> text(mc, a));
            case "chat" -> new ChatExecution(requireString(a, "text"), optInt(a, "waitTicks", 3));
            case "tp" -> new TpExecution(a);
            case "walk" -> walkCmd(a);
            case "wait" -> new WaitExecution(optInt(a, "ticks", 0));
            case "creative_tab" -> simple(mc -> {
                // 参数名用 tab：若叫 id 会与协议信封的 id 撞名（参考项目实测问题）
                String tab = requireString(a, "tab");
                String err = CreativeTabs.open(tab);
                if (err != null) {
                    throw new CommandFailure(err);
                }
                return field("tab", tab);
            });
            case "close" -> simple(mc -> {
                closeScreen(mc);
                return JsonNull.INSTANCE;
            });
            case "scroll" -> simple(mc -> {
                int delta = optInt(a, "delta", 1);
                scroll(mc, delta);
                return field("delta", Integer.toString(delta));
            });
            // 视角：first 第一人称、back 第三人称背后、front 第三人称正面（看自己身上的效果用）
            case "camera" -> simple(mc -> {
                String mode = optString(a, "mode", "first");
                mc.options.setCameraType(switch (mode) {
                    case "back" -> net.minecraft.client.CameraType.THIRD_PERSON_BACK;
                    case "front" -> net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
                    default -> net.minecraft.client.CameraType.FIRST_PERSON;
                });
                return field("camera", mode);
            });
            case "hud" -> simple(mc -> {
                mc.options.hideGui = "hide".equals(optString(a, "action", "show"));
                return field("hud", mc.options.hideGui ? "hide" : "show");
            });
            default -> simple(mc -> {
                throw new CommandFailure("未知指令：" + cmd.cmd());
            });
        };
    }

    // ───────────── 单 tick 指令 ─────────────

    private static RemoteExecution keyCmd(JsonObject a) {
        String name = requireString(a, "name");
        String action = requireString(a, "action");
        if ("escape".equals(name)) {
            return simple(mc -> {
                closeScreen(mc);
                return keyResult(name, action);
            });
        }
        KeyMapping mapping = resolveKey(name);
        if (mapping == null) {
            return failure("未知按键：" + name);
        }
        // 原版开背包、切快捷栏这类按键读的是点击计数；只设按下状态不会触发（参考项目实测）
        return switch (action) {
            case "down" -> simple(mc -> {
                pressDown(mapping);
                return keyResult(name, action);
            });
            case "up" -> simple(mc -> {
                mapping.setDown(false);
                return keyResult(name, action);
            });
            case "press" -> new PressExecution(mapping, keyResult(name, action));
            default -> failure("未知 action：" + action);
        };
    }

    private static void pressDown(KeyMapping mapping) {
        KeyMapping.click(mapping.getKey());
        mapping.setDown(true);
    }

    private static JsonElement keyResult(String name, String action) {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("action", action);
        return o;
    }

    private static KeyMapping resolveKey(String name) {
        var options = Minecraft.getInstance().options;
        switch (name) {
            case "use": return options.keyUse;
            case "attack": return options.keyAttack;
            case "forward": return options.keyUp;
            case "back": return options.keyDown;
            case "left": return options.keyLeft;
            case "right": return options.keyRight;
            case "jump": return options.keyJump;
            case "sneak": return options.keyShift;
            case "sprint": return options.keySprint;
            case "inventory": return options.keyInventory;
            case "drop": return options.keyDrop;
            case "swap": return options.keySwapOffhand;
            default:
                break;
        }
        if (name.startsWith("hotbar") && name.length() == 7) {
            int i = name.charAt(6) - '1';
            if (i >= 0 && i < options.keyHotbarSlots.length) {
                return options.keyHotbarSlots[i];
            }
        }
        // 其余按注册名查找，包括本 mod 的按键（key.mythic_age.<name>）
        for (KeyMapping km : options.keyMappings) {
            if (km.getName().equals(name) || km.getName().equals("key.mythic_age." + name)
                    || km.getName().equals("key." + name)) {
                return km;
            }
        }
        return null;
    }

    private static RemoteExecution hotbarCmd(JsonObject a) {
        int slot = optInt(a, "slot", -1);
        if (slot < 0 || slot > 8) {
            return failure("hotbar slot 须在 0-8 之间：" + slot);
        }
        return simple(mc -> {
            mc.player.getInventory().selected = slot;
            JsonObject o = new JsonObject();
            o.addProperty("slot", slot);
            o.add("item", Snapshots.itemJson(mc.player.getInventory().getItem(slot)));
            return o;
        });
    }

    private static JsonElement look(Minecraft mc, JsonObject a) {
        float yaw;
        float pitch;
        if (a.has("at")) {
            JsonArray at = a.getAsJsonArray("at");
            var eye = mc.player.getEyePosition();
            double dx = at.get(0).getAsDouble() - eye.x;
            double dy = at.get(1).getAsDouble() - eye.y;
            double dz = at.get(2).getAsDouble() - eye.z;
            yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            pitch = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
        } else {
            yaw = a.get("yaw").getAsFloat();
            pitch = a.get("pitch").getAsFloat();
        }
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        mc.player.yHeadRot = yaw;
        mc.player.yBodyRot = yaw;
        JsonObject o = new JsonObject();
        o.addProperty("yaw", yaw);
        o.addProperty("pitch", pitch);
        return o;
    }

    /**
     * 把鼠标移到界面某点，让悬停提示能渲染。窗口在后台没有焦点时移动系统光标不会回调，
     * 因此直接写入 MouseHandler 记录的坐标；高分屏下按窗口逻辑尺寸换算（参考项目实测）。
     */
    private static JsonElement mouse(Minecraft mc, JsonObject a) {
        double x = a.get("x").getAsDouble();
        double y = a.get("y").getAsDouble();
        var window = mc.getWindow();
        double sx = (double) window.getScreenWidth() / window.getGuiScaledWidth();
        double sy = (double) window.getScreenHeight() / window.getGuiScaledHeight();
        GLFW.glfwSetCursorPos(window.getWindow(), x * sx, y * sy);
        try {
            Field fx = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
            Field fy = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
            fx.setAccessible(true);
            fy.setAccessible(true);
            fx.setDouble(mc.mouseHandler, x * sx);
            fy.setDouble(mc.mouseHandler, y * sy);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("写入鼠标坐标失败", e);
        }
        if (mc.screen != null) {
            mc.screen.mouseMoved(x, y);
        }
        JsonObject o = new JsonObject();
        o.addProperty("x", x);
        o.addProperty("y", y);
        return o;
    }

    /** 模拟滚轮：走原版 MouseHandler.onScroll，与真实滚轮同一路径。 */
    private static void scroll(Minecraft mc, int delta) {
        try {
            Method m = net.minecraft.client.MouseHandler.class.getDeclaredMethod(
                    "onScroll", long.class, double.class, double.class);
            m.setAccessible(true);
            m.invoke(mc.mouseHandler, mc.getWindow().getWindow(), 0.0, (double) delta);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("模拟滚轮失败", e);
        }
    }

    /**
     * 关闭界面。容器界面要走 closeContainer（会通知服务端），只 setScreen(null) 会导致服务端仍认为容器开着，
     * 之后的点击因容器编号对不上被丢弃（参考项目实测）。
     */
    static void closeScreen(Minecraft mc) {
        if (mc.player != null && mc.screen instanceof AbstractContainerScreen<?>) {
            mc.player.closeContainer();
        } else {
            mc.setScreen(null);
        }
    }

    private static JsonElement text(Minecraft mc, JsonObject a) {
        if (mc.screen == null) {
            throw new CommandFailure("当前没有打开界面");
        }
        int index = a.get("widget").getAsInt();
        var children = mc.screen.children();
        if (index < 0 || index >= children.size() || !(children.get(index) instanceof EditBox editBox)) {
            throw new CommandFailure("widget 不是文本框：" + index);
        }
        editBox.setValue(requireString(a, "value"));
        if (a.has("enter") && a.get("enter").getAsBoolean()) {
            editBox.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
        }
        JsonObject o = new JsonObject();
        o.addProperty("widget", index);
        o.addProperty("value", editBox.getValue());
        return o;
    }

    private static RemoteExecution clickCmd(JsonObject a) {
        if (a.has("slot")) {
            int slotIndex = a.get("slot").getAsInt();
            int button = optInt(a, "button", 0);
            ClickType type = switch (optString(a, "type", "pickup")) {
                case "quick_move" -> ClickType.QUICK_MOVE;
                case "swap" -> ClickType.SWAP;
                case "throw" -> ClickType.THROW;
                case "clone" -> ClickType.CLONE;
                default -> ClickType.PICKUP;
            };
            return simple(mc -> {
                if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) {
                    throw new CommandFailure("当前没有打开容器界面");
                }
                AbstractContainerMenu menu = screen.getMenu();
                if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
                    throw new CommandFailure("slot 越界：" + slotIndex);
                }
                if (screen instanceof CreativeModeInventoryScreen creative) {
                    // 创造栏的物品区不是真实容器格，必须走它重写的 slotClicked，
                    // 否则客户端看着拿起来了，服务端并不知道（参考项目实测）
                    clickCreative(creative, menu.slots.get(slotIndex), slotIndex, button, type);
                } else {
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, slotIndex, button, type, mc.player);
                }
                JsonObject o = new JsonObject();
                o.addProperty("slot", slotIndex);
                o.add("carried", Snapshots.itemJson(menu.getCarried()));
                return o;
            });
        }
        Integer widget = a.has("widget") ? a.get("widget").getAsInt() : null;
        Double x = a.has("x") ? a.get("x").getAsDouble() : null;
        Double y = a.has("y") ? a.get("y").getAsDouble() : null;
        if (widget == null && (x == null || y == null)) {
            return failure("click 需要 slot、widget，或 x 与 y");
        }
        return new ClickWidgetExecution(widget, x, y, optInt(a, "button", 0));
    }

    private static void clickCreative(CreativeModeInventoryScreen creative, Slot slot, int slotId, int button,
                                      ClickType type) {
        try {
            Method m = CreativeModeInventoryScreen.class.getDeclaredMethod(
                    "slotClicked", Slot.class, int.class, int.class, ClickType.class);
            m.setAccessible(true);
            m.invoke(creative, slot, slotId, button, type);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("调用创造栏 slotClicked 失败", e);
        }
    }

    private static RemoteExecution dragCmd(JsonObject a) {
        JsonArray from = a.getAsJsonArray("from");
        JsonArray to = a.getAsJsonArray("to");
        return new DragExecution(from.get(0).getAsDouble(), from.get(1).getAsDouble(),
                to.get(0).getAsDouble(), to.get(1).getAsDouble(), optInt(a, "button", 0));
    }

    private static RemoteExecution walkCmd(JsonObject a) {
        JsonArray to = a.getAsJsonArray("to");
        WalkStep step = new WalkStep(to.get(0).getAsDouble(), to.get(2).getAsDouble(), optInt(a, "timeoutTicks", 200));
        return new MultiTick() {
            @Override
            boolean step(Minecraft mc) {
                if (!step.tick(mc)) {
                    return false;
                }
                JsonObject o = new JsonObject();
                o.addProperty("reached", step.reached());
                JsonArray pos = new JsonArray();
                for (double v : step.finalPos()) {
                    pos.add(v);
                }
                o.add("pos", pos);
                o.addProperty("ticks", step.usedTicks());
                data = o;
                return true;
            }
        };
    }

    // ───────────── 工具 ─────────────

    /** 指令执行中的可预期失败：写成 ok=false 的结果，而不是异常堆栈。 */
    static final class CommandFailure extends RuntimeException {
        CommandFailure(String message) {
            super(message);
        }
    }

    @FunctionalInterface
    private interface DataFn {
        JsonElement apply(Minecraft mc);
    }

    private static JsonElement field(String key, String value) {
        JsonObject o = new JsonObject();
        o.addProperty(key, value);
        return o;
    }

    private static String optString(JsonObject o, String key, String def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
    }

    private static int optInt(JsonObject o, String key, int def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : def;
    }

    private static String requireString(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            throw new CommandFailure("缺少参数：" + key);
        }
        return o.get(key).getAsString();
    }

    private static RemoteExecution failure(String message) {
        return simple(mc -> {
            throw new CommandFailure(message);
        });
    }

    /** 单 tick 完成的指令。 */
    private static RemoteExecution simple(DataFn fn) {
        return new MultiTick() {
            @Override
            boolean step(Minecraft mc) {
                data = fn.apply(mc);
                return true;
            }
        };
    }

    /** 执行体基类：子类实现 step；可预期失败抛 CommandFailure，其他异常由调度器统一处理。 */
    private abstract static class MultiTick implements RemoteExecution {
        protected boolean ok = true;
        protected String error;
        protected JsonElement data = JsonNull.INSTANCE;

        abstract boolean step(Minecraft mc);

        @Override
        public final boolean tick(Minecraft mc) {
            try {
                return step(mc);
            } catch (CommandFailure e) {
                ok = false;
                error = e.getMessage();
                return true;
            }
        }

        @Override
        public boolean ok() {
            return ok;
        }

        @Override
        public String error() {
            return error;
        }

        @Override
        public JsonElement data() {
            return data;
        }
    }

    /** key press：本 tick 按下，下一 tick 松开。 */
    private static final class PressExecution extends MultiTick {
        private final KeyMapping mapping;
        private int phase;

        PressExecution(KeyMapping mapping, JsonElement result) {
            this.mapping = mapping;
            this.data = result;
        }

        @Override
        boolean step(Minecraft mc) {
            if (phase++ == 0) {
                pressDown(mapping);
                return false;
            }
            mapping.setDown(false);
            return true;
        }
    }

    /** capture：请求下一帧，渲染后落盘为 frames/&lt;序号&gt;_&lt;名字&gt;.png。 */
    private static final class CaptureExecution extends MultiTick {
        private final RemoteContext ctx;
        private final String name;
        private String fileName;
        private int waited;

        CaptureExecution(RemoteContext ctx, String name) {
            this.ctx = ctx;
            this.name = name;
        }

        @Override
        boolean step(Minecraft mc) {
            if (fileName == null) {
                fileName = RemoteChannel.frameFileName(ctx.nextFrameSeq(), name);
                FrameCapture.request(ctx.channel().framesDir().resolve(fileName));
                return false;
            }
            if (FrameCapture.error() != null) {
                throw new CommandFailure(FrameCapture.error());
            }
            FrameCapture.Result result = FrameCapture.result();
            if (result == null) {
                if (++waited > 100) {
                    throw new CommandFailure("截图超时（5 秒内没有渲染帧，窗口可能被最小化）");
                }
                return false;
            }
            JsonObject o = new JsonObject();
            o.addProperty("path", "frames/" + fileName);
            o.addProperty("absolutePath", ctx.channel().framesDir().resolve(fileName).toAbsolutePath().toString());
            o.addProperty("width", result.width());
            o.addProperty("height", result.height());
            o.addProperty("guiScale", (int) mc.getWindow().getGuiScale());
            data = o;
            return true;
        }
    }

    /** click（控件或坐标）：本 tick mouseClicked，下一 tick mouseReleased。 */
    private static final class ClickWidgetExecution extends MultiTick {
        private final Integer widget;
        private final Double x;
        private final Double y;
        private final int button;
        private int phase;
        private double cx;
        private double cy;

        ClickWidgetExecution(Integer widget, Double x, Double y, int button) {
            this.widget = widget;
            this.x = x;
            this.y = y;
            this.button = button;
        }

        @Override
        boolean step(Minecraft mc) {
            if (phase == 0) {
                if (mc.screen == null) {
                    throw new CommandFailure("当前没有打开界面");
                }
                if (widget != null) {
                    var children = mc.screen.children();
                    if (widget < 0 || widget >= children.size() || !(children.get(widget) instanceof AbstractWidget w)) {
                        throw new CommandFailure("未知控件下标：" + widget);
                    }
                    cx = w.getX() + w.getWidth() / 2.0;
                    cy = w.getY() + w.getHeight() / 2.0;
                } else {
                    cx = x;
                    cy = y;
                }
                mc.screen.mouseClicked(cx, cy, button);
                phase = 1;
                return false;
            }
            // 点击后界面可能已关闭（完成、返回这类按钮），此时不需要松开
            if (mc.screen != null) {
                mc.screen.mouseReleased(cx, cy, button);
            }
            JsonObject o = new JsonObject();
            if (widget != null) {
                o.addProperty("widget", widget);
            }
            o.addProperty("x", cx);
            o.addProperty("y", cy);
            data = o;
            return true;
        }
    }

    /** drag：按下 → 拖动 → 松开，分 3 tick。 */
    private static final class DragExecution extends MultiTick {
        private final double fromX;
        private final double fromY;
        private final double toX;
        private final double toY;
        private final int button;
        private int phase;

        DragExecution(double fromX, double fromY, double toX, double toY, int button) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.button = button;
        }

        @Override
        boolean step(Minecraft mc) {
            if (mc.screen == null) {
                throw new CommandFailure("当前没有打开界面");
            }
            switch (phase++) {
                case 0 -> mc.screen.mouseClicked(fromX, fromY, button);
                case 1 -> mc.screen.mouseDragged(toX, toY, button, toX - fromX, toY - fromY);
                default -> {
                    mc.screen.mouseReleased(toX, toY, button);
                    JsonObject o = new JsonObject();
                    JsonArray from = new JsonArray();
                    from.add(fromX);
                    from.add(fromY);
                    JsonArray to = new JsonArray();
                    to.add(toX);
                    to.add(toY);
                    o.add("from", from);
                    o.add("to", to);
                    data = o;
                    return true;
                }
            }
            return false;
        }
    }

    /** chat：以 / 开头的作为命令发送，否则作为聊天；等待 waitTicks 收回执。 */
    private static final class ChatExecution extends MultiTick {
        private final String text;
        private final int waitTicks;
        private int elapsed = -1;

        ChatExecution(String text, int waitTicks) {
            this.text = text;
            this.waitTicks = waitTicks;
        }

        @Override
        boolean step(Minecraft mc) {
            if (elapsed < 0) {
                if (text.startsWith("/")) {
                    mc.player.connection.sendCommand(text.substring(1));
                } else {
                    mc.player.connection.sendChat(text);
                }
                elapsed = 0;
                return false;
            }
            return ++elapsed >= waitTicks;
        }
    }

    /**
     * tp：先开飞行再 /tp（否则传送后会先掉一截，参考项目实测）。目标区块可能需要先生成，
     * 因此等到玩家确实到达目标附近再返回（最多 5 秒），而不是固定等几 tick。
     */
    private static final class TpExecution extends MultiTick {
        private static final int TIMEOUT_TICKS = 100;
        private final double x;
        private final double y;
        private final double z;
        private final boolean fly;
        private int elapsed = -1;

        TpExecution(JsonObject a) {
            this.x = a.get("x").getAsDouble();
            this.y = a.get("y").getAsDouble();
            this.z = a.get("z").getAsDouble();
            this.fly = a.has("fly") && a.get("fly").getAsBoolean();
        }

        @Override
        boolean step(Minecraft mc) {
            if (elapsed < 0) {
                if (fly) {
                    forceFlying(mc);
                }
                mc.player.connection.sendCommand(String.format(Locale.ROOT, "tp @s %s %s %s", x, y, z));
                elapsed = 0;
                return false;
            }
            boolean arrived = mc.player.distanceToSqr(x, y, z) < 1.0;
            if (!arrived && ++elapsed <= TIMEOUT_TICKS) {
                return false;
            }
            if (fly) {
                forceFlying(mc);
            }
            if (!arrived) {
                throw new CommandFailure("传送超时：5 秒内没有到达目标（命令是否被拒绝？看 chat 字段）");
            }
            JsonObject pos = new JsonObject();
            pos.addProperty("x", mc.player.getX());
            pos.addProperty("y", mc.player.getY());
            pos.addProperty("z", mc.player.getZ());
            JsonObject o = new JsonObject();
            o.add("pos", pos);
            o.addProperty("ticks", elapsed);
            data = o;
            return true;
        }

        private static void forceFlying(Minecraft mc) {
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        }
    }

    private static final class WaitExecution extends MultiTick {
        private int remaining;

        WaitExecution(int ticks) {
            this.remaining = ticks;
        }

        @Override
        boolean step(Minecraft mc) {
            return remaining-- <= 1;
        }
    }
}
