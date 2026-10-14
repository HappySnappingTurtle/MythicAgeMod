package io.github.happysnappingturtle.mythicage.agent.remote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.agent.AgentEntrypoint;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.ClientMythData;
import io.github.happysnappingturtle.mythicage.player.PlayerSyncPayload;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * {@code state.mythic} 的填充点（T2b 设计文档 §4）。各系统的状态提供者写在 agent 源集中，
 * 读取玩法代码的公开接口；玩法代码不 import 任何 agent 类。
 */
public final class StateContributors {
    private static final Map<String, Function<Minecraft, JsonElement>> CONTRIBUTORS = new LinkedHashMap<>();

    static {
        // T4：五个文明的恩宠、上限与时代（读取客户端同步缓存）
        register("favor", mc -> {
            JsonObject favor = new JsonObject();
            for (Civilization civ : Civilization.values()) {
                PlayerSyncPayload.CivEntry e = ClientMythData.civ(civ);
                if (e == null) {
                    continue;
                }
                JsonObject o = new JsonObject();
                o.addProperty("favor", e.favor());
                o.addProperty("cap", e.cap());
                o.addProperty("age", e.age().id());
                favor.add(civ.id(), o);
            }
            return favor;
        });
        register("dataVersion", mc -> new com.google.gson.JsonPrimitive(ClientMythData.dataVersion()));
    }

    private StateContributors() {
    }

    static void register(String key, Function<Minecraft, JsonElement> contributor) {
        CONTRIBUTORS.put(key, contributor);
    }

    static JsonObject collect(Minecraft mc) {
        JsonObject o = new JsonObject();
        CONTRIBUTORS.forEach((key, fn) -> {
            try {
                o.add(key, fn.apply(mc));
            } catch (RuntimeException e) {
                AgentEntrypoint.LOG.warn("[agent] 状态提供者 {} 出错", key, e);
                o.addProperty(key, "error: " + e);
            }
        });
        return o;
    }
}
