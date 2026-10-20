package io.github.happysnappingturtle.mythicage.core.village.gen;

/** 世界生成时的群系查询：{@code id} 为群系 id（{@code minecraft:plains}）或群系标签（{@code #minecraft:is_river}）。 */
@FunctionalInterface
public interface BiomeView {
    boolean matches(String id, int x, int z);
}
