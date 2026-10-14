package io.github.happysnappingturtle.mythicage.content;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/** 本模组与通用（{@code c:}）标签。 */
public final class ModTags {
    private ModTags() {
    }

    /** 通用方块标签，例如 {@code c:ores/tin}。 */
    public static TagKey<Block> cBlock(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    /** 通用物品标签，例如 {@code c:ingots/tin}。 */
    public static TagKey<Item> cItem(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    public static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    public static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    /** 文明地域主群系（总纲 §4.1），例如 {@code mythic_age:region/greek}。 */
    public static TagKey<Biome> region(Civilization civ) {
        return TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "region/" + civ.id()));
    }

    /** 文明地域副群系，例如 {@code mythic_age:region/greek_secondary}。 */
    public static TagKey<Biome> regionSecondary(Civilization civ) {
        return TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "region/" + civ.id() + "_secondary"));
    }
}
