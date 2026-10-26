package io.github.happysnappingturtle.mythicage.boss;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.MythicAge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 试炼竞技场的运行时数据（T23 §4.1）：{@code data/<ns>/mythic_age/arena/<名>.json} 的 {@code runtime} 段，按 {@code kind}（结构种类）索引。
 * 竞技场的格子坐标来自结构锚点（T22），这里只放参数：Boss 实体、试炼 id、钥匙物品、基础血量、大厅半径与穹顶高度、
 * 柱（高度、断桩段数、碎块方块）、机关墙（方块、高度）、门洞与宝库门的尺寸、拴绳半径。
 */
public record ArenaSpec(String kind, ResourceLocation boss, String trial, ResourceLocation keyItem, float baseHealth,
                        int halfSize, int roof, int pillarHeight, int pillarStump, ResourceLocation rubble,
                        ResourceLocation wallBlock, int wallHeight, int gateWidth, int gateHeight, int vaultWidth, int vaultHeight,
                        int vaultDepth, double leash, int bossOffsetZ, Rewards rewards) {

    /** 每场胜利发给每位合法参战者的奖励（§7.1）：物品、经验；首杀另解锁一张图纸（只写记录）。 */
    public record Rewards(java.util.List<Reward> items, int xp, String firstKillBlueprint) {
        public static final Rewards NONE = new Rewards(java.util.List.of(), 0, "");
    }

    public record Reward(ResourceLocation item, int count) {
    }

    static Rewards rewards(JsonObject r) {
        if (!r.has("rewards")) {
            return Rewards.NONE;
        }
        JsonObject o = r.getAsJsonObject("rewards");
        java.util.List<Reward> items = new java.util.ArrayList<>();
        if (o.has("items")) {
            o.getAsJsonArray("items").forEach(e -> items.add(new Reward(ResourceLocation.parse(
                    GsonHelper.getAsString(e.getAsJsonObject(), "item")), GsonHelper.getAsInt(e.getAsJsonObject(), "count", 1))));
        }
        return new Rewards(java.util.List.copyOf(items), GsonHelper.getAsInt(o, "xp", 0),
                GsonHelper.getAsString(o, "first_kill_blueprint", ""));
    }

    public static ArenaSpec parse(JsonObject root) {
        JsonObject r = GsonHelper.getAsJsonObject(root, "runtime");
        JsonObject pillar = GsonHelper.getAsJsonObject(r, "pillar");
        JsonObject wall = GsonHelper.getAsJsonObject(r, "wall");
        JsonObject gate = GsonHelper.getAsJsonObject(r, "gate");
        JsonObject vault = GsonHelper.getAsJsonObject(r, "vault_door");
        int bossZ = root.has("boss_spawn") ? root.getAsJsonArray("boss_spawn").get(1).getAsInt() : 0;
        return new ArenaSpec(GsonHelper.getAsString(r, "kind"),
                ResourceLocation.parse(GsonHelper.getAsString(r, "boss")),
                GsonHelper.getAsString(r, "trial"),
                ResourceLocation.parse(GsonHelper.getAsString(r, "key_item")),
                GsonHelper.getAsFloat(r, "base_health"),
                GsonHelper.getAsInt(r, "half_size"),
                GsonHelper.getAsInt(r, "roof"),
                GsonHelper.getAsInt(pillar, "height"),
                GsonHelper.getAsInt(pillar, "stump"),
                ResourceLocation.parse(GsonHelper.getAsString(pillar, "rubble")),
                ResourceLocation.parse(GsonHelper.getAsString(wall, "block")),
                GsonHelper.getAsInt(wall, "height"),
                GsonHelper.getAsInt(gate, "width"),
                GsonHelper.getAsInt(gate, "height"),
                GsonHelper.getAsInt(vault, "width"),
                GsonHelper.getAsInt(vault, "height"),
                GsonHelper.getAsInt(vault, "depth"),
                GsonHelper.getAsDouble(r, "leash"),
                bossZ,
                rewards(r));
    }

    /** 读取全部竞技场数据。 */
    public static final class Loader extends SimpleJsonResourceReloadListener {
        private static volatile Map<String, ArenaSpec> specs = Map.of();

        private Loader() {
            super(new com.google.gson.Gson(), "mythic_age/arena");
        }

        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(new Loader());
        }

        public static Optional<ArenaSpec> get(String kind) {
            return Optional.ofNullable(specs.get(kind));
        }

        @Override
        protected void apply(Map<ResourceLocation, com.google.gson.JsonElement> files, ResourceManager manager,
                             ProfilerFiller profiler) {
            Map<String, ArenaSpec> next = new HashMap<>();
            files.forEach((id, json) -> {
                try {
                    if (json.getAsJsonObject().has("runtime")) {
                        ArenaSpec s = parse(json.getAsJsonObject());
                        next.put(s.kind(), s);
                    }
                } catch (RuntimeException e) {
                    MythicAge.LOGGER.error("竞技场数据 {} 解析失败，已忽略：{}", id, e.getMessage());
                }
            });
            specs = Map.copyOf(next);
        }
    }
}
