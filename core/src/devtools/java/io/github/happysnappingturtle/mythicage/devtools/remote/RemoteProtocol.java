package io.github.happysnappingturtle.mythicage.devtools.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.List;

/**
 * 遥控口协议（T2b 设计文档 §3）：一行文本 ↔ 一条指令 / 一行结果 JSON。纯 Java，可单元测试。
 * 移植自 MCMagicBuild 的 U6 遥控口。
 */
public final class RemoteProtocol {
    // error/data 为 null 时字段仍要出现：默认 Gson 会丢弃 null 成员，必须开 serializeNulls
    private static final Gson GSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

    /** 一条已解析的指令。{@code raw} 只用于日志排障。 */
    public record Command(long id, String cmd, JsonObject args, String raw) {
    }

    public static final class RemoteProtocolException extends Exception {
        public RemoteProtocolException(String message) {
            super(message);
        }
    }

    private RemoteProtocol() {
    }

    /**
     * @throws RemoteProtocolException 不是合法 JSON、顶层不是对象、缺 id/cmd，或 id 不是整数（原因为中文）
     */
    public static Command parse(String line) throws RemoteProtocolException {
        JsonElement root;
        try {
            root = JsonParser.parseString(line);
        } catch (JsonParseException e) {
            throw new RemoteProtocolException("不是合法 JSON：" + e.getMessage());
        }
        if (root == null || !root.isJsonObject()) {
            throw new RemoteProtocolException("不是合法 JSON：顶层不是对象");
        }
        JsonObject obj = root.getAsJsonObject();
        if (!obj.has("id") || obj.get("id").isJsonNull()) {
            throw new RemoteProtocolException("缺少 id 字段");
        }
        if (!obj.has("cmd") || obj.get("cmd").isJsonNull()) {
            throw new RemoteProtocolException("缺少 cmd 字段");
        }
        JsonElement idEl = obj.get("id");
        if (!idEl.isJsonPrimitive() || !idEl.getAsJsonPrimitive().isNumber()) {
            throw new RemoteProtocolException("id 必须是整数");
        }
        double rawId = idEl.getAsDouble();
        long id = (long) rawId;
        if (id != rawId) {
            throw new RemoteProtocolException("id 必须是整数");
        }
        JsonElement cmdEl = obj.get("cmd");
        if (!cmdEl.isJsonPrimitive() || !cmdEl.getAsJsonPrimitive().isString()) {
            throw new RemoteProtocolException("cmd 必须是字符串");
        }
        return new Command(id, cmdEl.getAsString(), obj, line);
    }

    /** 组装一行结果 JSON（不带换行）。 */
    public static String result(long id, String cmd, boolean ok, String error, long tick, List<String> chat,
                                JsonElement data) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("cmd", cmd);
        obj.addProperty("ok", ok);
        obj.add("error", error == null ? JsonNull.INSTANCE : new JsonPrimitive(error));
        obj.addProperty("tick", tick);
        JsonArray chatArr = new JsonArray();
        for (String line : chat) {
            chatArr.add(line);
        }
        obj.add("chat", chatArr);
        obj.add("data", data == null ? JsonNull.INSTANCE : data);
        return GSON.toJson(obj);
    }

    public static String toJson(JsonElement element) {
        return GSON.toJson(element);
    }
}
