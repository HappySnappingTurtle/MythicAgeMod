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
        add("entity.mythic_age.minotaur", "弥诺陶洛斯", "Minotaur", "ミノタウロス");
        add("commands.mythic_age.dev.no_entity", "没有可操作的实体（只支持本模组的动画实体）", "No supported entity selected", "対象となるエンティティがありません");
        add("commands.mythic_age.dev.anim", "已将 %s 个实体的动画设为 %s", "Set animation of %s entit(ies) to %s", "%s 体のアニメーションを %s に設定しました");
        add("commands.mythic_age.dev.charge", "%s 个实体开始冲锋（%s 格）", "%s entit(ies) charging (%s blocks)", "%s 体が突進を開始（%s ブロック）");
        add("commands.mythic_age.dev.variant", "已将 %s 个实体的变体设为「%s」（缩放 %s）", "Set variant of %s entit(ies) to '%s' (scale %s)", "%s 体の変種を「%s」に設定しました（倍率 %s）");
        add("commands.mythic_age.dev.unknown_state", "未知动画状态：%s", "Unknown animation state: %s", "不明なアニメーション状態：%s");
        add("mythic_age.altar.place.no_age", "尚未接触希腊文明，不能建立神坛", "You have not met the Greek pantheon yet; you cannot raise an altar", "まだギリシャの神々と接していないため、祭壇を建てられません");
        add("mythic_age.altar.place.limit", "当前时代最多拥有 %s 座希腊神坛", "Your current age allows at most %s Greek altar(s)", "現在の時代ではギリシャの祭壇は最大 %s 基までです");
        add("mythic_age.altar.place.spacing", "离已有神坛太近：两座神坛至少相隔 %s 格（%s, %s, %s，主人 %s）", "Too close to another altar: altars must be at least %s blocks apart (%s, %s, %s, owner %s)", "既存の祭壇に近すぎます：祭壇同士は %s ブロック以上離してください（%s, %s, %s、所有者 %s）");
        add("mythic_age.altar.protected", "这是 %s 的神坛，只有主人能拆除", "This altar belongs to %s; only its owner can remove it", "これは %s の祭壇です。所有者だけが撤去できます");
        add("mythic_age.altar.title", "奥林匹斯祭坛", "Olympian Altar", "オリュンポスの祭壇");
        add("mythic_age.altar.owner", "主人：%s（%s时代）", "Owner: %s (%s Age)", "所有者：%s（%s時代）");
        add("mythic_age.altar.no_result", "尚未完成扫描", "Survey not finished yet", "調査はまだ終わっていません");
        add("mythic_age.altar.level", "神庙：%s（系数 %s）", "Temple: %s (×%s)", "神殿：%s（係数 %s）");
        add("mythic_age.altar.structural", "结构已达到：%s", "Structure qualifies for: %s", "構造は %s の条件を満たしています");
        add("mythic_age.altar.next", "升级到「%s」需要：", "To become %s:", "「%s」への条件：");
        add("mythic_age.altar.max", "已是最高等级", "Highest tier reached", "最高位に達しています");
        add("mythic_age.altar.row.columns", "希腊柱（高≥3）", "Greek columns (height ≥3)", "ギリシャ柱（高さ3以上）");
        add("mythic_age.altar.row.roof", "柱间屋顶", "Roof over the colonnade", "柱に囲まれた屋根");
        add("mythic_age.altar.row.litBraziers", "点燃的圣火盆", "Lit braziers", "灯った聖火台");
        add("mythic_age.altar.row.statues", "神像", "Cult statues", "神像");
        add("mythic_age.altar.row.pediments", "三角楣石", "Pediments", "ペディメント");
        add("mythic_age.altar.row.marbleFloor", "大理石地面", "Marble floor", "大理石の床");
        add("mythic_age.altar.slots", "祈祷位：%s / %s", "Prayer spots: %s / %s", "祈りの場：%s / %s");
        add("mythic_age.altar.slots.short", "只找到 %s 个可站立且可达的祈祷位", "Only %s standable, reachable prayer spot(s) found", "立てて到達できる祈りの場が %s か所しかありません");
        add("mythic_age.altar.estimate", "满员时预计 %s 恩宠/日", "Full attendance: about %s favor per day", "満員時の見込み：1日あたり約 %s の恩寵");
        add("mythic_age.altar.estimate.note", "估算，不是发放承诺；信徒与结算在后续版本接入", "Estimate only, not a promise; worshippers and payout come in a later version", "見込みであり支給の約束ではありません。信徒と精算は今後の版で実装されます");
        add("mythic_age.altar.missing.title", "缺失项", "Missing", "不足している項目");
        add("mythic_age.altar.missing.row", "%s %s/%s", "%s %s/%s", "%s %s/%s");
        add("mythic_age.altar.missing.none", "下一级条件已全部满足", "All conditions for the next tier are met", "次の段階の条件はすべて満たしています");
        add("mythic_age.altar.age_required", "需要%s时代才能认定为「%s」", "Requires the %s Age to count as %s", "「%s」と認められるには%s時代が必要です");
        add("mythic_age.altar.unverified", "暂不可验证：扫描范围内有区块未加载，显示上一次的结果", "Cannot verify: part of the area is not loaded; showing the last result", "確認できません：範囲内に未読み込みのチャンクがあります。前回の結果を表示しています");
        add("mythic_age.altar.highlight", "高亮缺失格", "Highlight gaps", "不足箇所を表示");
        add("mythic_age.altar.highlight.legend", "白 范围 · 红 无屋顶/祭坛被遮 · 绿 台基上的柱 · 黄 短柱或贴地柱 · 橙 熄灭的火盆 · 蓝 祈祷位", "White area · red no roof / covered altar · green column on platform · yellow short or grounded column · orange unlit brazier · blue prayer spot", "白 範囲・赤 屋根なし／祭壇の遮蔽・緑 基壇上の柱・黄 短い柱／地面の柱・橙 消えた聖火台・青 祈りの場");
        add("mythic_age.altar.row.openAltar", "祭坛露天", "Open-air altar", "祭壇が露天");
        add("mythic_age.altar.row.raisedColumns", "台基上的柱", "Columns on the platform", "基壇上の柱");
        add("mythic_age.altar.row.platformAccess", "台基可走上去", "Platform accessible", "基壇へ上がれる");
        add("mythic_age.altar.missing.openAltar", "祭坛应露天：神坛头顶有遮挡", "The altar must stand in the open: something covers it", "祭壇は露天に置いてください：頭上に遮るものがあります");
        add("mythic_age.altar.missing.platformAccess", "台基走不上去：在正门前加台阶或坡道", "The platform cannot be climbed: add steps or a ramp at the front", "基壇に上がれません：正面に階段かスロープを付けてください");
        add("mythic_age.altar.place.hint", "神坛面向神庙：神庙建在神坛前方，大门朝向神坛", "The altar faces the temple: build the temple in front of it, doors toward the altar", "祭壇は神殿の方を向きます：祭壇の正面に神殿を建て、扉を祭壇に向けてください");
        add("mythic_age.temple.tier.open_altar", "露天祭坛", "Open-air Altar", "露天の祭壇");
        add("mythic_age.temple.tier.naiskos", "小神庙", "Naiskos", "小神殿");
        add("mythic_age.temple.tier.prostylos", "中神庙", "Prostylos", "前柱式神殿");
        add("mythic_age.temple.tier.peripteros", "大神庙", "Peripteros", "周柱式神殿");
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
