package io.github.happysnappingturtle.mythicage.entity;

/**
 * 实体按自身状态临时显示渲染器默认隐藏的骨骼（客户端 MythGeoModel 的 hiddenBones）。
 */
public interface BoneVisibility {
    /** 这块默认隐藏的骨骼现在要不要显示。 */
    boolean showBone(String bone);
}
