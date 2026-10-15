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
        // 守护者与兑换（T9）
        add("entity.mythic_age.greek_guardian", "神庙祭司", "Temple Priest", "神殿の神官");
        add("mythic_age.guardian.talk.first", "远方来的旅人，奥林匹斯的众神注视着你。接受祝福，带走这座祭坛，在你的居所为众神立起香火。", "Traveller from afar, the gods of Olympus are watching you. Accept their blessing and take this altar; kindle an offering to the gods where you dwell.", "遠方からの旅人よ、オリュンポスの神々があなたを見守っている。祝福を受け、この祭壇を持ち帰り、住まいで神々に灯をともしなさい。");
        add("mythic_age.guardian.talk.reissue", "你的祭坛遗失了？众神仍记得你。我可以再给你一座，但每天只能一次。", "Your altar is lost? The gods still remember you. I can give you another, but only once a day.", "祭壇をなくしたのか？神々はまだあなたを覚えている。もう一基渡そう。ただし一日に一度だけだ。");
        add("mythic_age.guardian.talk.greet", "愿众神庇佑你。需要什么，就到兑换页看看。", "May the gods protect you. See the exchange page for what I can offer.", "神々のご加護を。必要なものは交換のページを見てほしい。");
        add("mythic_age.guardian.accept_blessing", "接受祝福", "Accept the blessing", "祝福を受ける");
        add("mythic_age.guardian.reissue", "补领祭坛", "Ask for another altar", "祭壇を再び受け取る");
        add("mythic_age.guardian.to_trade", "去兑换", "Exchange", "交換へ");
        add("mythic_age.trade.tab.talk", "对话", "Talk", "会話");
        add("mythic_age.trade.tab.trade", "兑换", "Exchange", "交換");
        add("mythic_age.trade.altar_title", "奥林匹斯祭坛 · 兑换", "Olympian Altar · Exchange", "オリュンポスの祭壇・交換");
        add("mythic_age.trade.favor", "希腊恩宠 %s/%s · %s时代", "Greek Favor %s/%s · %s Age", "ギリシャの恩寵 %s/%s・%s時代");
        add("mythic_age.trade.favor.none", "尚未得到希腊众神的祝福", "Not yet blessed by the Greek gods", "まだギリシャの神々の祝福を受けていません");
        add("mythic_age.trade.buy", "兑换", "Buy", "交換");
        add("mythic_age.trade.pending", "祭司正在准备……", "The priest is preparing…", "神官が準備しています……");
        add("mythic_age.trade.empty", "此处暂时没有可兑换的物品。", "Nothing to exchange here yet.", "ここで交換できるものはまだありません。");
        add("mythic_age.trade.price.favor", "%s 恩宠", "%s Favor", "%s 恩寵");
        add("mythic_age.trade.category.god_power", "神力", "God Power", "神力");
        add("mythic_age.trade.category.summon", "召唤", "Summon", "召喚");
        add("mythic_age.trade.category.blueprint", "图纸", "Blueprint", "設計図");
        add("mythic_age.trade.category.supplies", "补给", "Supplies", "補給");
        add("mythic_age.trade.state.locked", "%s时代解锁", "Unlocks in the %s Age", "%s時代で解放");
        add("mythic_age.trade.state.sold_out", "今日已售罄", "Sold out today", "本日は売り切れ");
        add("mythic_age.trade.state.no_favor", "恩宠不足", "Not enough Favor", "恩寵が足りません");
        add("mythic_age.trade.state.no_items", "物品不足", "Missing items", "品物が足りません");
        add("mythic_age.trade.state.remaining", "今日剩余 %s", "%s left today", "本日残り %s");
        add("mythic_age.trade.result.ok", "获得 %s ×%s", "Received %s ×%s", "%s ×%s を受け取りました");
        add("mythic_age.trade.result.duplicate", "这次请求已经处理过了。", "This request has already been handled.", "この依頼はすでに処理されています。");
        add("mythic_age.trade.result.npc_gone", "祭司已离开。", "The priest has left.", "神官は立ち去りました。");
        add("mythic_age.trade.result.unknown_offer", "这件商品已经下架。", "This item is no longer offered.", "この品物はもう扱っていません。");
        add("mythic_age.trade.result.wrong_venue", "这件商品只能在祭司处兑换。", "This item can only be exchanged with the priest.", "この品物は神官のもとでしか交換できません。");
        add("mythic_age.trade.result.age_locked", "你的信仰时代还不够。", "Your Age of faith is not high enough.", "信仰の時代がまだ足りません。");
        add("mythic_age.trade.result.price_changed", "价格已变为 %s，请确认后再兑换。", "The price is now %s; please confirm and try again.", "価格が %s に変わりました。確認してからもう一度交換してください。");
        add("mythic_age.trade.result.daily_limit", "今天已经达到限购次数，明天再来。", "Daily limit reached; come back tomorrow.", "本日の購入上限に達しました。また明日来てください。");
        add("mythic_age.trade.result.not_enough_favor", "恩宠不足，还差 %s。", "Not enough Favor: %s short.", "恩寵が足りません（あと %s）。");
        add("mythic_age.trade.result.not_enough_items", "背包里的物品不够支付。", "You do not carry enough items to pay.", "支払う品物が足りません。");
        add("mythic_age.trade.result.inventory_full", "背包已满，腾出空间再来。", "Your inventory is full; make room first.", "持ち物がいっぱいです。空きを作ってから来てください。");
        add("mythic_age.trade.result.already_blessed", "你已经得到过众神的祝福。", "You have already received the gods' blessing.", "すでに神々の祝福を受けています。");
        add("mythic_age.trade.result.not_blessed", "请先接受祝福。", "Accept the blessing first.", "まず祝福を受けてください。");
        add("mythic_age.trade.result.has_altar", "你名下已有祭坛，或背包里还有一座。", "You already own an altar or carry one.", "すでに祭壇を持っているか、持ち物に一基あります。");
        add("mythic_age.trade.result.reissued_today", "今天已经补领过了，明天再来。", "You already received one today; come back tomorrow.", "本日はすでに受け取りました。また明日来てください。");
        add("commands.mythic_age.guardian.spawned", "已在 %s %s %s 生成神庙祭司", "Temple priest spawned at %s %s %s", "%s %s %s に神殿の神官を生成しました");
        add("commands.mythic_age.guardian.count", "共 %s 位神庙祭司", "%s temple priest(s)", "神殿の神官 %s 人");
        add("commands.mythic_age.guardian.alive", "· %s %s (%s, %s, %s) 在世", "· %s %s (%s, %s, %s) alive", "· %s %s (%s, %s, %s) 健在");
        add("commands.mythic_age.guardian.dead", "· %s %s (%s, %s, %s) 已故，死于第 %s 日", "· %s %s (%s, %s, %s) dead since day %s", "· %s %s (%s, %s, %s) 死亡（%s 日目）");
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
