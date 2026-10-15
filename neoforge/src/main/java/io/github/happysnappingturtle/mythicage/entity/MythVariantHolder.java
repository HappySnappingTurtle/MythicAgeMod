package io.github.happysnappingturtle.mythicage.entity;

/** 带外观变体的实体（召唤版、Boss 版共用模型）。变体名与缩放由服务端写入并同步；渲染器还据此决定头部是否跟随视线。 */
public interface MythVariantHolder {
    /** 变体名；空字符串为基础外观。 */
    String variant();

    /** 变体缩放（模型与碰撞同步放大）。 */
    float variantScale();

    /** 正在播放机制动作（如冲锋）时为 true：头部不再叠加视线方向，保持动画里的姿势。 */
    default boolean headLookLocked() {
        return false;
    }
}
