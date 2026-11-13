package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.faith.GraceSources;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKinds;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.function.Supplier;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊神坛与神庙构件（T8a 设计文档 §2）：奥林匹斯祭坛、基础圣火盆、神像、三角楣石、大理石柱，
 * 以及神庙判定使用的方块标签。
 */
public final class GreekTemple {
    /** 柱：大理石柱、原版石英柱，T8b 的柱身也加入；数据包可扩展。 */
    public static final TagKey<Block> GREEK_COLUMN = ModTags.block("greek_column");
    public static final TagKey<Block> BRAZIERS = ModTags.block("braziers");
    public static final TagKey<Block> TEMPLE_STATUES = ModTags.block("temple_statues");
    public static final TagKey<Block> TEMPLE_PEDIMENTS = ModTags.block("temple_pediments");
    /** 大理石地面：大理石族的整块、台阶、楼梯。 */
    public static final TagKey<Block> TEMPLE_FLOOR = ModTags.block("temple_floor");
    /** 不算屋顶遮盖的方块：玻璃、树叶。 */
    public static final TagKey<Block> ROOF_EXCLUDED = ModTags.block("temple_roof_excluded");

    public static final DeferredBlock<GreekAltarBlock> GREEK_ALTAR = Content.block("greek_altar", GREEK, GreekAltarBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(GreekMaterials.MARBLE.get()).strength(3.0f, 1200.0f)
                    .noOcclusion().pushReaction(PushReaction.BLOCK))
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("旧祭坛", "Old Altar", "古い祭壇")
            .register();

    /** 希腊神坛这一种神庙（T10b；GA-13 起只用于识别旧存档的玩家祭坛并迁移）：同种间距的垂直范围 = 扫描体高度；扫描体相交、提示与扫描由 TempleService 接入。 */
    public static final ShrineKind ALTAR_KIND = ShrineKinds.register(new ShrineKind("greek:altar", GREEK, GREEK_ALTAR,
            TempleService.VERTICAL_SPACING, TempleService.HOOKS));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GreekAltarBlockEntity>> GREEK_ALTAR_BE =
            ModRegistries.BLOCK_ENTITY_TYPES.register("greek_altar",
                    () -> BlockEntityType.Builder.of(GreekAltarBlockEntity::new, GREEK_ALTAR.get()).build(null));

    public static final DeferredBlock<BrazierBlock> GREEK_BRAZIER = Content.block("greek_brazier", GREEK, BrazierBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()
                    .lightLevel(s -> s.getValue(BrazierBlock.LIT) ? 15 : 0))
            .model(new BlockModelSpec.StateModels(s -> s.getValue(BrazierBlock.LIT) ? "_lit" : "", false, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, BRAZIERS)
            .lang("基础圣火盆", "Sacred Brazier", "聖火台")
            .register();

    public static final DeferredBlock<StatueBlock> GREEK_STATUE = Content.block("greek_statue", GREEK, StatueBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(GreekMaterials.MARBLE.get()).noOcclusion()
                    .pushReaction(PushReaction.BLOCK))
            .model(new BlockModelSpec.StateModels(
                    s -> s.getValue(StatueBlock.HALF) == DoubleBlockHalf.LOWER ? "_lower" : "_upper", true, "_item"))
            .loot(LootSpec.door())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, TEMPLE_STATUES)
            .lang("神像", "Cult Statue", "神像")
            .register();

    public static final DeferredBlock<PedimentBlock> GREEK_PEDIMENT = Content.block("greek_pediment", GREEK,
                    PedimentBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(GreekMaterials.MARBLE.get()).noOcclusion())
            .model(new BlockModelSpec.StateModels(s -> "_" + s.getValue(PedimentBlock.SHAPE).getSerializedName()
                    + (s.getValue(PedimentBlock.BOTTOM) ? "_bottom" : ""), true, "_single_bottom"))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, TEMPLE_PEDIMENTS)
            .lang("三角楣石", "Pediment", "ペディメント")
            .register();

    public static final DeferredBlock<RotatedPillarBlock> MARBLE_PILLAR = Content.block("marble_pillar", GREEK,
                    RotatedPillarBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(GreekMaterials.MARBLE.get()))
            .model(BlockModelSpec.pillar())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GREEK_COLUMN, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("大理石柱", "Marble Pillar", "大理石の柱")
            .register();

    private GreekTemple() {
    }

    public static void init() {
        GraceSources.register(GreekTempleSource.INSTANCE);
        io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.register("greek", GreekVillageTemple.INSTANCE);
        io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.registerAltarUse("greek", VillageAltarUse::useItemOn);
        io.github.happysnappingturtle.mythicage.rite.PromotionRequirements.register(GreekTempleRequirement.INSTANCE);
        // 登录时按登记补发神庙等级的进度事件（进度上线前就建好神庙的存档）
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
                    if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) {
                        LegacyAltars.onLogin(p);
                        int level = GreekTempleRequirement.bestLevel(p, TempleService.rules());
                        if (level > 0) {
                            io.github.happysnappingturtle.mythicage.advancement.MythEvents.fire(p,
                                    io.github.happysnappingturtle.mythicage.advancement.MythEvents.TEMPLE_LEVEL,
                                    io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK, level);
                        }
                    }
                });
    }

    /** 不由 Content 条目声明的标签成员（原版方块、其他标签、大理石族），由 datagen 写出。 */
    public record ExtraTag(TagKey<Block> tag, List<Supplier<? extends Block>> blocks, List<TagKey<Block>> tags) {
    }

    public static List<ExtraTag> extraTags() {
        return List.of(
                new ExtraTag(GREEK_COLUMN, List.of(() -> Blocks.QUARTZ_PILLAR), List.of()),
                new ExtraTag(TEMPLE_FLOOR, List.of(GreekMaterials.MARBLE, GreekMaterials.MARBLE_SLAB,
                        GreekMaterials.MARBLE_STAIRS, GreekMaterials.SMOOTH_MARBLE, GreekMaterials.SMOOTH_MARBLE_SLAB,
                        GreekMaterials.MARBLE_ASHLAR, GreekMaterials.MARBLE_ASHLAR_SLAB, GreekMaterials.MARBLE_ASHLAR_STAIRS,
                        GreekMaterials.POLYGONAL_MARBLE, GreekMaterials.POLISHED_MARBLE, GreekMaterials.POLISHED_MARBLE_SLAB,
                        GreekMaterials.POLISHED_MARBLE_STAIRS, GreekMaterials.BLUE_MEANDER_MARBLE,
                        GreekMaterials.RED_MEANDER_MARBLE), List.of()),
                new ExtraTag(ROOF_EXCLUDED, List.of(), List.of(BlockTags.IMPERMEABLE, BlockTags.LEAVES)));
    }
}
