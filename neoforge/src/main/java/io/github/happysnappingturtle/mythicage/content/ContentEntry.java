package io.github.happysnappingturtle.mythicage.content;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.Optional;

/**
 * 一个已声明的内容条目（方块或物品）。datagen、创造栏与一致性测试都从这里读取。
 *
 * @param block     方块条目才有
 * @param item      物品；方块条目为其方块物品，没有物品的方块（墙上告示牌、盆栽等）为空
 * @param model     方块条目的模型种类
 * @param itemModel 物品模型种类
 * @param loot      方块条目的战利品种类
 * @param texture   贴图覆盖：方块为六面贴图，物品为图标；为空时用 {@code mythic_age:block|item/<id>}
 * @param itemTexture 方块物品改用平面图标时的贴图（门、树苗等）
 */
public record ContentEntry(String id, Civilization civ, Optional<DeferredBlock<? extends Block>> block,
                           Optional<DeferredItem<? extends Item>> item, Optional<BlockModelSpec> model,
                           ItemModelSpec itemModel, Optional<LootSpec> loot, List<TagKey<Block>> blockTags,
                           List<TagKey<Item>> itemTags, Lang3 name,
                           Optional<ResourceLocation> texture, Optional<ResourceLocation> itemTexture) {
    public boolean isBlock() {
        return block.isPresent();
    }
}
