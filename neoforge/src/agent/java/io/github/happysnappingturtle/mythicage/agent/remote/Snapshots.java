package io.github.happysnappingturtle.mythicage.agent.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.agent.AgentEntrypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** state 与 screen 指令的快照生成（只读）。 */
final class Snapshots {
    private static final int NEARBY_RADIUS = 16;
    private static final int NEARBY_LIMIT = 20;
    /** aim 射线长度：远于原版交互距离，用于判断视线指向的远处目标。 */
    private static final double AIM_RANGE = 48.0;

    private Snapshots() {
    }

    static JsonObject state(Minecraft mc, String sessionId, long tick, List<String> recentChat) {
        JsonObject o = new JsonObject();
        o.addProperty("sessionId", sessionId);
        o.addProperty("tick", tick);
        o.addProperty("gameTime", mc.level != null ? mc.level.getGameTime() : 0L);
        o.addProperty("dayTime", mc.level != null ? mc.level.getDayTime() % 24000 : 0L);
        if (mc.player != null) {
            o.add("pos", vec(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            JsonObject rot = new JsonObject();
            rot.addProperty("yaw", mc.player.getYRot());
            rot.addProperty("pitch", mc.player.getXRot());
            o.add("rot", rot);
            o.addProperty("dimension", mc.player.level().dimension().location().toString());
            o.addProperty("onGround", mc.player.onGround());
            o.addProperty("flying", mc.player.getAbilities().flying);
            o.addProperty("health", mc.player.getHealth());
            o.addProperty("food", mc.player.getFoodData().getFoodLevel());
            o.addProperty("isUsingItem", mc.player.isUsingItem());
            o.addProperty("selectedSlot", mc.player.getInventory().selected);
            // 只列有东西的格子，避免空格子淹没信息
            JsonArray hotbar = new JsonArray();
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    JsonObject slot = itemJson(stack);
                    slot.addProperty("slot", i);
                    hotbar.add(slot);
                }
            }
            o.add("hotbar", hotbar);
            o.add("mainHand", itemJson(mc.player.getMainHandItem()));
            o.add("offHand", itemJson(mc.player.getOffhandItem()));
        }
        o.addProperty("gamemode", mc.gameMode != null ? mc.gameMode.getPlayerMode().getName() : "unknown");
        o.addProperty("hudHidden", mc.options.hideGui);
        if (mc.screen != null) {
            JsonObject screen = new JsonObject();
            screen.addProperty("class", mc.screen.getClass().getSimpleName());
            screen.addProperty("title", mc.screen.getTitle().getString());
            o.add("screen", screen);
        } else {
            o.add("screen", JsonNull.INSTANCE);
        }
        o.add("crosshair", crosshair(mc));
        o.add("aim", aim(mc));
        o.add("nearby", nearby(mc));
        o.add("mythic", StateContributors.collect(mc));
        JsonArray chat = new JsonArray();
        recentChat.forEach(chat::add);
        o.add("recentChat", chat);
        return o;
    }

    static JsonObject screen(Minecraft mc) {
        Screen screen = mc.screen;
        JsonObject o = new JsonObject();
        o.addProperty("class", screen.getClass().getSimpleName());
        o.addProperty("title", screen.getTitle().getString());
        o.addProperty("guiScale", (int) mc.getWindow().getGuiScale());
        o.addProperty("guiWidth", mc.getWindow().getGuiScaledWidth());
        o.addProperty("guiHeight", mc.getWindow().getGuiScaledHeight());
        JsonArray widgets = new JsonArray();
        List<? extends GuiEventListener> children = screen.children();
        for (int i = 0; i < children.size(); i++) {
            if (!(children.get(i) instanceof AbstractWidget widget)) {
                continue;
            }
            JsonObject w = new JsonObject();
            w.addProperty("index", i);
            w.addProperty("class", widget.getClass().getSimpleName());
            w.addProperty("message", widget.getMessage().getString());
            w.addProperty("x", widget.getX());
            w.addProperty("y", widget.getY());
            w.addProperty("w", widget.getWidth());
            w.addProperty("h", widget.getHeight());
            w.addProperty("active", widget.active);
            w.addProperty("visible", widget.visible);
            w.addProperty("focused", widget.isFocused());
            if (widget instanceof EditBox editBox) {
                w.addProperty("value", editBox.getValue());
            }
            widgets.add(w);
        }
        o.add("widgets", widgets);
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            o.add("container", containerJson(containerScreen));
        }
        JsonObject custom = describeForRemote(screen);
        if (custom != null) {
            o.add("custom", custom);
        }
        return o;
    }

    private static JsonObject containerJson(AbstractContainerScreen<?> screen) {
        JsonObject c = new JsonObject();
        c.addProperty("guiLeft", screen.getGuiLeft());
        c.addProperty("guiTop", screen.getGuiTop());
        AbstractContainerMenu menu = screen.getMenu();
        c.addProperty("containerId", menu.containerId);
        JsonArray slots = new JsonArray();
        for (Slot slot : menu.slots) {
            JsonObject s = itemJson(slot.getItem());
            s.addProperty("index", slot.index);
            s.addProperty("x", screen.getGuiLeft() + slot.x);
            s.addProperty("y", screen.getGuiTop() + slot.y);
            slots.add(s);
        }
        c.add("slots", slots);
        c.add("carried", itemJson(menu.getCarried()));
        Slot hovered = screen.getSlotUnderMouse();
        c.addProperty("hoveredSlot", hovered == null ? -1 : hovered.index);
        return c;
    }

    /**
     * 界面若有 {@code public JsonObject describeForRemote()}，用反射读取其自定义描述。
     * 用反射而不是接口，界面类就不需要 import agent 包。
     */
    private static JsonObject describeForRemote(Screen screen) {
        try {
            Method m = screen.getClass().getMethod("describeForRemote");
            return m.getReturnType() == JsonObject.class ? (JsonObject) m.invoke(screen) : null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            AgentEntrypoint.LOG.error("[agent] describeForRemote 调用失败", e);
            return null;
        }
    }

    private static JsonObject crosshair(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() != HitResult.Type.MISS) {
            return blockJson(mc, blockHit.getBlockPos(), -1);
        }
        if (hit instanceof EntityHitResult entityHit) {
            return entityJson(entityHit.getEntity(), -1);
        }
        JsonObject o = new JsonObject();
        o.addProperty("type", "miss");
        return o;
    }

    /** 沿视线打一条 48 格射线：先找方块，再在方块之前找实体。 */
    static JsonObject aim(Minecraft mc) {
        JsonObject miss = new JsonObject();
        miss.addProperty("type", "miss");
        if (mc.player == null || mc.level == null) {
            return miss;
        }
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getLookAngle().scale(AIM_RANGE));
        BlockHitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, mc.player));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(mc.player, eye, limit,
                mc.player.getBoundingBox().expandTowards(limit.subtract(eye)).inflate(1.0),
                e -> !e.isSpectator() && e.isPickable(), eye.distanceToSqr(limit));
        if (entity != null) {
            return entityJson(entity.getEntity(), eye.distanceTo(entity.getLocation()));
        }
        if (block.getType() != HitResult.Type.MISS) {
            return blockJson(mc, block.getBlockPos(), eye.distanceTo(block.getLocation()));
        }
        return miss;
    }

    private static JsonObject blockJson(Minecraft mc, BlockPos pos, double distance) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "block");
        JsonArray arr = new JsonArray();
        arr.add(pos.getX());
        arr.add(pos.getY());
        arr.add(pos.getZ());
        o.add("pos", arr);
        if (mc.level != null) {
            o.addProperty("id", BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).toString());
        }
        if (distance >= 0) {
            o.addProperty("distance", Math.round(distance * 10) / 10.0);
        }
        return o;
    }

    private static JsonObject entityJson(Entity entity, double distance) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "entity");
        o.addProperty("entityType", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        o.addProperty("name", entity.getName().getString());
        if (entity instanceof LivingEntity living) {
            o.addProperty("health", living.getHealth());
        }
        if (distance >= 0) {
            o.addProperty("distance", Math.round(distance * 10) / 10.0);
        }
        return o;
    }

    private static JsonArray nearby(Minecraft mc) {
        JsonArray arr = new JsonArray();
        if (mc.level == null || mc.player == null) {
            return arr;
        }
        List<Entity> found = new ArrayList<>(mc.level.getEntities(mc.player,
                mc.player.getBoundingBox().inflate(NEARBY_RADIUS), e -> true));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)));
        for (Entity entity : found.subList(0, Math.min(NEARBY_LIMIT, found.size()))) {
            JsonObject e = entityJson(entity, Math.sqrt(entity.distanceToSqr(mc.player)));
            e.remove("type");
            e.add("pos", vec(entity.getX(), entity.getY(), entity.getZ()));
            arr.add(e);
        }
        return arr;
    }

    static JsonObject itemJson(ItemStack stack) {
        JsonObject o = new JsonObject();
        if (stack == null || stack.isEmpty()) {
            o.addProperty("id", "minecraft:air");
            return o;
        }
        o.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        o.addProperty("count", stack.getCount());
        o.addProperty("name", stack.getHoverName().getString());
        return o;
    }

    private static JsonObject vec(double x, double y, double z) {
        JsonObject o = new JsonObject();
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("z", z);
        return o;
    }
}
