package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/** 希腊的四种食物（08 号与 T6b 设计文档 §6）：无状态效果，容器由原版食用逻辑返还。 */
public final class GreekFoods {
    // 橄榄薄饼：营养 6，饱和度系数 0.6。
    public static final DeferredItem<Item> OLIVE_FLATBREAD = Content.item("olive_flatbread", GREEK)
            .properties(p -> p.food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build()))
            .tags(ModTags.cItem("foods"))
            .lang("橄榄薄饼", "Olive Flatbread", "オリーブのフラットブレッド")
            .register();

    // 蜜酪：最多堆叠 16 份，吃完返还碗。
    public static final DeferredItem<Item> HONEY_CURD = Content.item("honey_curd", GREEK)
            .properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f)
                    .usingConvertsTo(Items.BOWL).build()))
            .tags(ModTags.cItem("foods"))
            .lang("蜜酪", "Honey Curd", "蜂蜜がけチーズ")
            .register();

    // 葡萄果盘：最多堆叠 16 份，吃完返还木盘。
    public static final DeferredItem<Item> GRAPE_PLATTER = Content.item("grape_platter", GREEK)
            .properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.4f)
                    .usingConvertsTo(GreekMaterials.WOODEN_PLATTER.get()).build()))
            .tags(ModTags.cItem("foods"))
            .lang("葡萄果盘", "Grape Platter", "ブドウの盛り皿")
            .register();

    // 希腊行旅餐：营养 8，饱和度系数 0.7。
    public static final DeferredItem<Item> GREEK_TRAVEL_MEAL = Content.item("greek_travel_meal", GREEK)
            .properties(p -> p.food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.7f).build()))
            .tags(ModTags.cItem("foods"))
            .lang("希腊行旅餐", "Greek Travel Meal", "ギリシャの旅の食事")
            .register();

    private GreekFoods() {
    }

    public static void init() {
    }
}
