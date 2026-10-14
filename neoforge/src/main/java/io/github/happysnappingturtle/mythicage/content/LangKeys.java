package io.github.happysnappingturtle.mythicage.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 非内容条目的语言键（命令、HUD、文明与时代名称、物品栏标题等）的三语文本（T6 设计文档 §2.4）。
 * 内容条目的名称写在 {@link Content} 的声明里；两处都由 datagen 输出到 zh_cn、en_us、ja_jp。
 */
public final class LangKeys {
    private static final Map<String, Lang3> KEYS = new LinkedHashMap<>();

    static {
        add("commands.mythic_age.config.result", "%s：%s（版本 %s，%s 个问题）%s", "%s: %s (version %s, %s issue(s)) %s", "%s：%s（バージョン %s、問題 %s 件）%s");
        add("commands.mythic_age.config.show", "%s（版本 %s）：\n%s", "%s (version %s):\n%s", "%s（バージョン %s）：\n%s");
        add("commands.mythic_age.config.unknown", "未知配置「%s」。可选：all、runtime、world_rules、conduct", "Unknown config '%s'. Valid: all, runtime, world_rules, conduct", "不明な設定「%s」。選択肢：all、runtime、world_rules、conduct");
        add("mythic_age.civ.greek", "希腊", "Greek", "ギリシャ");
        add("mythic_age.civ.egyptian", "埃及", "Egyptian", "エジプト");
        add("mythic_age.civ.norse", "北欧", "Norse", "北欧");
        add("mythic_age.civ.chinese", "中华", "Chinese", "中華");
        add("mythic_age.civ.atlantean", "亚特兰蒂斯", "Atlantean", "アトランティス");
        add("mythic_age.age.none", "未接触", "Not contacted", "未接触");
        add("mythic_age.age.archaic", "远古", "Archaic", "アルカイック");
        add("mythic_age.age.classical", "古典", "Classical", "古典");
        add("mythic_age.age.heroic", "英雄", "Heroic", "英雄");
        add("mythic_age.age.mythic", "神话", "Mythic", "神話");
        add("mythic_age.hud.favor", "%s恩宠 %s/%s", "%s Favor %s/%s", "%sの恩寵 %s/%s");
        add("commands.mythic_age.unknown_civ", "未知文明：%s", "Unknown civilization: %s", "不明な文明：%s");
        add("commands.mythic_age.unknown_age", "未知时代：%s", "Unknown age: %s", "不明な時代：%s");
        add("commands.mythic_age.favor.get", "%s %s：%s/%s（%s）", "%s %s: %s/%s (%s)", "%s %s：%s/%s（%s）");
        add("commands.mythic_age.favor.add", "%s %s +%s → %s（%s）", "%s %s +%s → %s (%s)", "%s %s +%s → %s（%s）");
        add("commands.mythic_age.favor.set", "%s %s = %s", "%s %s = %s", "%s %s = %s");
        add("commands.mythic_age.age.set", "%s %s → %s", "%s %s → %s", "%s %s → %s");
        add("commands.mythic_age.runtime.unknown", "未知台账：%s", "Unknown ledger: %s", "不明な台帳：%s");
        add("commands.mythic_age.runtime.added", "已创建台账 %s", "Ledger %s created", "台帳 %s を作成しました");
        add("commands.mythic_age.runtime.count", "共 %s 本台账，当前 tick=%s", "%s ledger(s), now=%s", "台帳 %s 件、現在 tick=%s");
        add("commands.mythic_age.runtime.real", "%s 真实运行 = %s", "%s real running = %s", "%s 実稼働 = %s");
        add("commands.mythic_age.runtime.invalidated", "%s 已失效", "%s invalidated", "%s を無効化しました");
        add("commands.mythic_age.runtime.removed", "%s 已删除", "%s removed", "%s を削除しました");
        add("commands.mythic_age.runtime.settled", "已结算 %s 本台账", "Settled %s ledger(s)", "台帳 %s 件を精算しました");
        add("commands.mythic_age.runtime.status", "%s：活跃=%s，未操作 %s/%s tick，当前 tick=%s，台账 %s 本", "%s: active=%s, idle %s/%s ticks, now=%s, ledgers=%s", "%s：アクティブ=%s、無操作 %s/%s tick、現在 tick=%s、台帳 %s 件");
        add("itemGroup.mythic_age.greek", "神话时代·希腊", "Mythic Age: Greek", "ミシックエイジ・ギリシャ");
        add("mythic_age.recipe.age_locked", "%s 需要%s%s时代", "%s requires %s %s Age", "%sには%s・%s時代が必要です");
    }

    private LangKeys() {
    }

    private static void add(String key, String zh, String en, String ja) {
        if (KEYS.put(key, new Lang3(zh, en, ja)) != null) {
            throw new IllegalStateException("重复的语言键：" + key);
        }
    }

    public static Map<String, Lang3> all() {
        return Collections.unmodifiableMap(KEYS);
    }
}
