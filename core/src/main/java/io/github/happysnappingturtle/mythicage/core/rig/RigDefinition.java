package io.github.happysnappingturtle.mythicage.core.rig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 实体的服务端骨架数据（T7 设计文档 §6.2）：零姿势下的挂点、碰撞尺寸与变体。
 * GeckoLib 的骨骼只存在于客户端，服务端需要的位置（出手点、鼻息、放大后的碰撞）从这里读取。
 *
 * <p>坐标沿用设计卡约定：模型空间，单位 u（16u = 1 格），+X 为角色左、+Y 为上、+Z 为角色前，原点在脚底中心。
 */
public record RigDefinition(String model, double width, double height, Map<String, Vec3> locators,
                            Map<String, Variant> variants) {
    public static final double UNITS_PER_BLOCK = 16.0;

    public RigDefinition {
        locators = Collections.unmodifiableMap(new LinkedHashMap<>(locators));
        variants = Collections.unmodifiableMap(new LinkedHashMap<>(variants));
    }

    public record Vec3(double x, double y, double z) {
    }

    /** 变体：缩放与贴图（贴图为资源路径，只给客户端用）。 */
    public record Variant(double scale, String texture) {
    }

    public Optional<Variant> variant(String name) {
        return Optional.ofNullable(variants.get(name));
    }

    /** 变体的缩放；{@code null} 或未知变体为 1。 */
    public double scaleOf(String variantName) {
        if (variantName == null) {
            return 1.0;
        }
        Variant v = variants.get(variantName);
        return v == null ? 1.0 : v.scale();
    }

    /**
     * 把挂点从模型空间换算到世界坐标。
     *
     * @param yawDeg Minecraft 偏航角：0° 面朝 +Z（南），90° 面朝 -X（西）
     * @return 世界坐标 [x, y, z]
     * @throws IllegalArgumentException 挂点不存在
     */
    public double[] locatorWorld(String name, double entityX, double entityY, double entityZ, double yawDeg, double scale) {
        Vec3 p = locators.get(name);
        if (p == null) {
            throw new IllegalArgumentException("骨架 " + model + " 没有挂点 " + name);
        }
        double yaw = Math.toRadians(yawDeg);
        // 前方 f = (-sin, 0, cos)；角色左 l = (cos, 0, sin)。偏航 0° 时 l = +X、f = +Z，与卡片坐标一致。
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double lx = Math.cos(yaw);
        double lz = Math.sin(yaw);
        double k = scale / UNITS_PER_BLOCK;
        return new double[]{
                entityX + (p.x() * lx + p.z() * fx) * k,
                entityY + p.y() * k,
                entityZ + (p.x() * lz + p.z() * fz) * k,
        };
    }

    // ───────────── 解析 ─────────────

    /** 解析错误，消息以字段路径开头（如 {@code collision.width: 必须为正数}）。 */
    public static final class RigParseException extends RuntimeException {
        public RigParseException(String message) {
            super(message);
        }
    }

    public static RigDefinition parse(String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            throw new RigParseException("(根): JSON 语法错误：" + e.getMessage());
        }
        return parse(root);
    }

    public static RigDefinition parse(JsonElement root) {
        JsonObject o = object(root, "(根)");
        String model = string(o, "model", "model");
        JsonObject collision = object(o.get("collision"), "collision");
        double width = positive(collision.get("width"), "collision.width");
        double height = positive(collision.get("height"), "collision.height");

        Map<String, Vec3> locators = new LinkedHashMap<>();
        if (o.has("locators")) {
            JsonObject locs = object(o.get("locators"), "locators");
            for (Map.Entry<String, JsonElement> e : locs.entrySet()) {
                locators.put(e.getKey(), vec3(e.getValue(), "locators." + e.getKey()));
            }
        }

        Map<String, Variant> variants = new LinkedHashMap<>();
        if (o.has("variants")) {
            JsonObject vars = object(o.get("variants"), "variants");
            for (Map.Entry<String, JsonElement> e : vars.entrySet()) {
                String path = "variants." + e.getKey();
                JsonObject v = object(e.getValue(), path);
                double scale = positive(v.get("scale"), path + ".scale");
                String texture = v.has("texture") ? string(v, "texture", path + ".texture") : null;
                variants.put(e.getKey(), new Variant(scale, texture));
            }
        }
        return new RigDefinition(model, width, height, locators, variants);
    }

    private static JsonObject object(JsonElement e, String path) {
        if (e == null || !e.isJsonObject()) {
            throw new RigParseException(path + ": 缺失或不是对象");
        }
        return e.getAsJsonObject();
    }

    private static String string(JsonObject o, String key, String path) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString() || e.getAsString().isEmpty()) {
            throw new RigParseException(path + ": 缺失或不是非空字符串");
        }
        return e.getAsString();
    }

    private static double number(JsonElement e, String path) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            throw new RigParseException(path + ": 缺失或不是数字");
        }
        double d = e.getAsDouble();
        if (!Double.isFinite(d)) {
            throw new RigParseException(path + ": 不是有限数");
        }
        return d;
    }

    private static double positive(JsonElement e, String path) {
        double d = number(e, path);
        if (d <= 0) {
            throw new RigParseException(path + ": 必须为正数，实际 " + d);
        }
        return d;
    }

    private static Vec3 vec3(JsonElement e, String path) {
        if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() != 3) {
            throw new RigParseException(path + ": 必须是 3 个数的数组");
        }
        JsonArray a = e.getAsJsonArray();
        return new Vec3(number(a.get(0), path + "[0]"), number(a.get(1), path + "[1]"), number(a.get(2), path + "[2]"));
    }
}
