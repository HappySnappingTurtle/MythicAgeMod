package io.github.happysnappingturtle.mythicage.content;

/** 物品模型种类。 */
public enum ItemModelSpec {
    /** 平面图标：{@code item/generated}，贴图 {@code mythic_age:item/<id>}。 */
    GENERATED,
    /** 手持类（工具、武器）：{@code item/handheld}。 */
    HANDHELD,
    /** 方块物品：继承方块模型（墙用 inventory 模型）。 */
    BLOCK,
    /** 模型手写在资源目录中，datagen 不生成。 */
    MANUAL
}
