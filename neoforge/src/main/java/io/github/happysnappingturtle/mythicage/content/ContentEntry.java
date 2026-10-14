package io.github.happysnappingturtle.mythicage.content;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
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
 * @param item      方块条目为其方块物品
 * @param model     方块条目的模型种类
 * @param itemModel 物品模型种类
 * @param loot      方块条目的战利品种类
 */
public record ContentEntry(String id, Civilization civ, Optional<DeferredBlock<? extends Block>> block,
                           DeferredItem<? extends Item> item, Optional<BlockModelSpec> model,
                           ItemModelSpec itemModel, Optional<LootSpec> loot, List<TagKey<Block>> blockTags,
                           List<TagKey<Item>> itemTags, Lang3 name) {
    public boolean isBlock() {
        return block.isPresent();
    }
}
