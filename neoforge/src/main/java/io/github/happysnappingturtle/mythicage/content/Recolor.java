package io.github.happysnappingturtle.mythicage.content;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 原版造型换色贴图的名字（T6 设计文档 §3.2）。贴图本身不在本模组里：由 {@code tools/texgen} 的
 * recolor 规格生成调色板，并在 {@code assets/minecraft/atlases/blocks.json} 登记 paletted_permutations，
 * 游戏加载资源时用原版贴图实时换色。名字规则与 {@code tools/texgen/recolor.py} 一致：
 * {@code <原版贴图>_mythic_age_<id>}。
 */
public final class Recolor {
    private static final Set<ResourceLocation> SPRITES = new LinkedHashSet<>();

    private Recolor() {
    }

    /**
     * @param base 原版贴图，例如 {@code minecraft:item/iron_ingot}
     * @param id   recolor 规格中的 id，例如 {@code tin_ingot}
     */
    public static ResourceLocation sprite(String base, String id) {
        ResourceLocation rl = ResourceLocation.parse(base + "_mythic_age_" + id);
        SPRITES.add(rl);
        return rl;
    }

    /** 已声明的全部换色贴图（datagen 用来告诉模型检查器这些贴图在运行时生成）。 */
    public static Set<ResourceLocation> sprites() {
        return Collections.unmodifiableSet(SPRITES);
    }
}
