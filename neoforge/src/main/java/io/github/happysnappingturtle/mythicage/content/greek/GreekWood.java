package io.github.happysnappingturtle.mythicage.content.greek;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.wood.ModWoodTypes;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/** 橄榄木族的八种建筑与红石形态（T6b）；基础木材、告示牌与船见 GreekAgriculture。 */
public final class GreekWood {
    // 橄榄木台阶：形状标签与双台阶掉落由 Content 自动补齐。
    public static final DeferredBlock<SlabBlock> OLIVE_SLAB = Content.block("olive_slab", GREEK, SlabBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB))
            .model(BlockModelSpec.slab(GreekAgriculture.OLIVE_PLANKS))
            .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.WOODEN_SLABS)
            .itemTags(ItemTags.WOODEN_SLABS)
            .lang("橄榄木台阶", "Olive Slab", "オリーブのハーフブロック")
            .register();

    // 橄榄木楼梯：复用橄榄木板的贴图与基础方块状态。
    public static final DeferredBlock<StairBlock> OLIVE_STAIRS = Content.block("olive_stairs", GREEK,
                    p -> new StairBlock(GreekAgriculture.OLIVE_PLANKS.get().defaultBlockState(), p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS))
            .model(BlockModelSpec.stairs(GreekAgriculture.OLIVE_PLANKS))
            .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.WOODEN_STAIRS)
            .itemTags(ItemTags.WOODEN_STAIRS)
            .lang("橄榄木楼梯", "Olive Stairs", "オリーブの階段")
            .register();

    // 橄榄木栅栏：使用原版木栅栏连接规则。
    public static final DeferredBlock<FenceBlock> OLIVE_FENCE = Content.block("olive_fence", GREEK, FenceBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE))
            .model(new BlockModelSpec.Fence(GreekAgriculture.OLIVE_PLANKS))
            .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.WOODEN_FENCES)
            .itemTags(ItemTags.WOODEN_FENCES)
            .lang("橄榄木栅栏", "Olive Fence", "オリーブのフェンス")
            .register();

    // 橄榄木栅栏门：开关声音取自橄榄木类型。
    public static final DeferredBlock<FenceGateBlock> OLIVE_FENCE_GATE = Content.block("olive_fence_gate", GREEK,
                    p -> new FenceGateBlock(ModWoodTypes.OLIVE, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE))
            .model(new BlockModelSpec.FenceGate(GreekAgriculture.OLIVE_PLANKS))
            .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.FENCE_GATES)
            .itemTags(ItemTags.FENCE_GATES)
            .lang("橄榄木栅栏门", "Olive Fence Gate", "オリーブのフェンスゲート")
            .register();

    // 橄榄木门：双格放置，只有下半扇掉落门物品；使用已有的手绘图标。
    public static final DeferredBlock<DoorBlock> OLIVE_DOOR = Content.block("olive_door", GREEK,
                    p -> new DoorBlock(ModWoodTypes.OLIVE_SET, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR))
            .model(new BlockModelSpec.Door(mod("block/olive_door_bottom"), mod("block/olive_door_top")))
            .loot(LootSpec.door())
            .item((b, p) -> new DoubleHighBlockItem(b, p))
            .itemTexture(mod("item/olive_door"))
            .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.WOODEN_DOORS)
            .itemTags(ItemTags.WOODEN_DOORS)
            .lang("橄榄木门", "Olive Door", "オリーブのドア")
            .register();

    // 橄榄木活板门：可徒手开关，使用已有的手绘贴图。
    public static final DeferredBlock<TrapDoorBlock> OLIVE_TRAPDOOR = Content.block("olive_trapdoor", GREEK,
                    p -> new TrapDoorBlock(ModWoodTypes.OLIVE_SET, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR))
            .model(new BlockModelSpec.Trapdoor(mod("block/olive_trapdoor")))
            .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.WOODEN_TRAPDOORS)
            .itemTags(ItemTags.WOODEN_TRAPDOORS)
            .lang("橄榄木活板门", "Olive Trapdoor", "オリーブのトラップドア")
            .register();

    // 橄榄木按钮：按下保持 30 tick，与原版木按钮一致。
    public static final DeferredBlock<ButtonBlock> OLIVE_BUTTON = Content.block("olive_button", GREEK,
                    p -> new ButtonBlock(ModWoodTypes.OLIVE_SET, 30, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON))
            .model(new BlockModelSpec.Button(GreekAgriculture.OLIVE_PLANKS))
            .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.WOODEN_BUTTONS)
            .itemTags(ItemTags.WOODEN_BUTTONS)
            .lang("橄榄木按钮", "Olive Button", "オリーブのボタン")
            .register();

    // 橄榄木压力板：实体检测规则取自橄榄木方块集类型。
    public static final DeferredBlock<PressurePlateBlock> OLIVE_PRESSURE_PLATE = Content.block(
                    "olive_pressure_plate", GREEK, p -> new PressurePlateBlock(ModWoodTypes.OLIVE_SET, p))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE))
            .model(new BlockModelSpec.PressurePlate(GreekAgriculture.OLIVE_PLANKS))
            .tags(BlockTags.MINEABLE_WITH_AXE, BlockTags.WOODEN_PRESSURE_PLATES)
            .itemTags(ItemTags.WOODEN_PRESSURE_PLATES)
            .lang("橄榄木压力板", "Olive Pressure Plate", "オリーブの感圧板")
            .register();

    private GreekWood() {
    }

    private static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }

    public static void init() {
    }
}
