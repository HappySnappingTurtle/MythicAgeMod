package io.github.happysnappingturtle.mythicage.core.summon;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * 神话单位卡（T14 设计文档 §1.1，数据包 {@code summon_unit/<civ>/<id>.json}）。
 *
 * @param respawnDelayDays 0 表示死亡后不重生；&gt; 0 表示在主人神坛旁按 MC 日延迟重生（B1：只给非战斗侦察单位）
 * @param combat           false 的单位不接受攻击指令，「自由攻击」视同跟随
 */
public record SummonUnitCard(String civ, String id, String entity, int cost, int respawnDelayDays, boolean combat,
                             String token) {
    public SummonUnitCard {
        Objects.requireNonNull(civ);
        Objects.requireNonNull(id);
        Objects.requireNonNull(entity);
        Objects.requireNonNull(token);
        if (cost < 1 || cost > 2) {
            throw new IllegalArgumentException("占用必须是 1 或 2：" + cost);
        }
        if (respawnDelayDays < 0) {
            throw new IllegalArgumentException("重生延迟不能为负：" + respawnDelayDays);
        }
        if (respawnDelayDays > 0 && combat) {
            throw new IllegalArgumentException("只有非战斗单位可以重生（总纲 B1）：" + civ + ":" + id);
        }
    }

    public String key() {
        return civ + ":" + id;
    }

    public boolean respawns() {
        return respawnDelayDays > 0;
    }

    /** 解析一张卡；字段缺失或不合法时抛出 {@link IllegalArgumentException}（调用方记日志并跳过）。 */
    public static SummonUnitCard parse(String civ, String id, JsonObject o) {
        try {
            int delay = 0;
            if (o.has("respawn") && o.get("respawn").isJsonObject()) {
                JsonObject r = o.getAsJsonObject("respawn");
                if (!"shrine".equals(r.get("kind").getAsString())) {
                    throw new IllegalArgumentException("未知的重生方式：" + r.get("kind"));
                }
                delay = r.get("delayDays").getAsInt();
                if (delay < 1) {
                    throw new IllegalArgumentException("重生延迟至少 1 日");
                }
            } else if (o.has("respawn") && !"none".equals(o.get("respawn").getAsString())) {
                throw new IllegalArgumentException("respawn 只能是 \"none\" 或对象");
            }
            return new SummonUnitCard(civ, id, o.get("entity").getAsString(), o.get("cost").getAsInt(), delay,
                    !o.has("combat") || o.get("combat").getAsBoolean(), o.get("token").getAsString());
        } catch (NullPointerException | UnsupportedOperationException | IllegalStateException e) {
            throw new IllegalArgumentException("单位卡 " + civ + ":" + id + " 缺少字段或类型不对", e);
        }
    }
}
