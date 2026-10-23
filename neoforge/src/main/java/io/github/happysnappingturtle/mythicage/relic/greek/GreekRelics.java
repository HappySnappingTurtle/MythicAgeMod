package io.github.happysnappingturtle.mythicage.relic.greek;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.relic.RelicAnchor;
import io.github.happysnappingturtle.mythicage.relic.RelicItem;
import io.github.happysnappingturtle.mythicage.relic.RelicItems;
import net.neoforged.neoforge.registries.DeferredItem;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊圣物（T16：06 号 RE-GR-01～03）与希腊圣物锚点。效果数值在圣物卡 {@code relic/greek/*.json}。
 * 生存中的来源（废弃神殿、克诺索斯迷宫宝库）在对应结构的任务里接入；T16 先放进创造模式物品栏。
 */
public final class GreekRelics {
    /** RE-GR-01：佩戴 希腊恩宠上限 +50；供奉 祈祷产出 R +15%。 */
    public static final DeferredItem<RelicItem> RHEIA_CROWN = Content.item("rheia_crown", GREEK, RelicItem::new)
            .tags(RelicItems.CURIOS_TAG)
            .lang("瑞亚之冠", "Rheia's Crown", "レアの冠")
            .register();
    /** RE-GR-02：佩戴 受近战伤害 −8%；供奉 范围内友方受近战伤害 −15%。 */
    public static final DeferredItem<RelicItem> ARMOR_OF_ACHILLES = Content.item("armor_of_achilles", GREEK, RelicItem::new)
            .tags(RelicItems.CURIOS_TAG)
            .lang("阿喀琉斯之甲", "Armor of Achilles", "アキレウスの鎧")
            .register();
    /** RE-GR-03：佩戴 生命上限 +4；供奉 范围内信徒村民生命上限 +40%，且不会被僵尸转化。 */
    public static final DeferredItem<RelicItem> PYGMALION_STATUE = Content.item("pygmalion_statue", GREEK, RelicItem::new)
            .tags(RelicItems.CURIOS_TAG)
            .lang("皮格马利翁雕像", "Pygmalion's Statue", "ピュグマリオンの像")
            .register();

    private GreekRelics() {
    }

    public static void init() {
        RelicAnchor.Registry.register(new GreekRelicAnchor());
    }
}
