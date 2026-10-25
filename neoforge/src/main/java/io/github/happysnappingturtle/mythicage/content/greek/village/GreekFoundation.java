package io.github.happysnappingturtle.mythicage.content.greek.village;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.village.found.FoundationStoneBlock;
import io.github.happysnappingturtle.mythicage.village.found.FoundationStoneBlockEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 城邦奠基石（T19 设计文档 §1.13 K1，17 号 §3）：市政厅里赫斯提亚的公共灶——圆形石灶上一团常燃的圣火。
 * 殖民者从母邦市政厅的灶里取火种去点燃新城的灶（Wikipedia「Prytaneion」「Hestia」）。逻辑在通用的 {@link FoundationStoneBlock}。
 */
public final class GreekFoundation {
    public static final DeferredBlock<FoundationStoneBlock> HEARTH_STONE = Content.block("greek_hearth_stone", GREEK,
                    p -> new FoundationStoneBlock(p, GREEK, () -> GreekFoundation.HEARTH_STONE_BE.get()))
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(3.0f, 1200.0f)
                    .sound(SoundType.STONE).lightLevel(s -> 13).noOcclusion().pushReaction(PushReaction.BLOCK))
            .model(new BlockModelSpec.StateModels(s -> "", false, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("城邦奠基石", "Polis Hearthstone", "ポリスの礎石（かまど）")
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundationStoneBlockEntity>> HEARTH_STONE_BE =
            ModRegistries.BLOCK_ENTITY_TYPES.register("greek_hearth_stone",
                    () -> BlockEntityType.Builder.of((pos, state) -> new FoundationStoneBlockEntity(GreekFoundation.HEARTH_STONE_BE.get(), pos, state),
                            HEARTH_STONE.get()).build(null));

    private GreekFoundation() {
    }

    public static void init() {
    }
}
