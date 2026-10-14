package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊基础材料（08 号 §2；T6 设计文档 §6）。声明顺序即创造模式物品栏顺序。
 * 新增条目照下面的样板写：一个条目一段，名称三语齐全，标签用 {@link ModTags} 或原版/NeoForge 常量。
 */
public final class GreekMaterials {
    private static final ResourceLocation STONE_TEXTURE = ResourceLocation.withDefaultNamespace("block/stone");

    // ---- 锡（远古）：锡石矿 → 粗锡 → 锡锭 ----

    public static final DeferredBlock<Block> TIN_ORE = Content.block("tin_ore", GREEK)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE))
            .model(BlockModelSpec.oreOverlay(STONE_TEXTURE))
            .loot(LootSpec.ore(() -> GreekMaterials.RAW_TIN.get()))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, Tags.Blocks.ORES,
                    ModTags.cBlock("ores/tin"), Tags.Blocks.ORES_IN_GROUND_STONE, Tags.Blocks.ORE_RATES_SINGULAR)
            .itemTags(Tags.Items.ORES, ModTags.cItem("ores/tin"), Tags.Items.ORES_IN_GROUND_STONE,
                    Tags.Items.ORE_RATES_SINGULAR)
            .lang("锡石矿", "Tin Ore", "錫鉱石")
            .register();

    public static final DeferredItem<Item> RAW_TIN = Content.item("raw_tin", GREEK)
            .tags(Tags.Items.RAW_MATERIALS, ModTags.cItem("raw_materials/tin"))
            .lang("粗锡", "Raw Tin", "錫の原石")
            .register();

    public static final DeferredItem<Item> TIN_INGOT = Content.item("tin_ingot", GREEK)
            .tags(Tags.Items.INGOTS, ModTags.cItem("ingots/tin"))
            .lang("锡锭", "Tin Ingot", "錫インゴット")
            .register();

    // ---- 青铜（古典解锁）：铜锭 3 + 锡锭 1 → 青铜混合物 4 → 熔炼为青铜锭 ----

    public static final DeferredItem<Item> BRONZE_MIX = Content.item("bronze_mix", GREEK)
            .lang("青铜混合物", "Bronze Mix", "青銅の混合物")
            .register();

    public static final DeferredItem<Item> BRONZE_INGOT = Content.item("bronze_ingot", GREEK)
            .tags(Tags.Items.INGOTS, ModTags.cItem("ingots/bronze"))
            .lang("青铜锭", "Bronze Ingot", "青銅インゴット")
            .register();

    // ---- 希腊大理石（远古）：地表斑块 ----

    public static final DeferredBlock<Block> MARBLE = Content.block("marble", GREEK)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.QUARTZ))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, ModTags.block("marble"))
            .itemTags(ModTags.item("marble"))
            .lang("希腊大理石", "Greek Marble", "ギリシャ大理石")
            .register();

    private GreekMaterials() {
    }

    /** 触发类加载，使上面的条目在注册事件之前完成声明。 */
    public static void init() {
    }
}
