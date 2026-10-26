package io.github.happysnappingturtle.mythicage.content.greek.ruin;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.ColumnShaftBlock;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.StatueBlock;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.structure.RuinVaultBlockEntity;
import io.github.happysnappingturtle.mythicage.structure.StructureInfoPiece;
import io.github.happysnappingturtle.mythicage.structure.StructureRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊遗迹（T22 阶段 ②④）：废弃神殿与克诺索斯的方块、守卫与登记处理。
 * <ul>
 *   <li>大理石碎块：倒塌建筑的碎石堆（矮，可走上去）；挖下来是大理石原石；</li>
 *   <li>残缺神像：同神像，但头盔、右臂与矛已经断了；<b>不算神庙神像</b>（不进 temple_statues 标签，免得废墟里立神坛就白捡神庙等级）；</li>
 *   <li>米诺斯构件（克诺索斯）：红柱、双斧门楣、圣角，模型与贴图由 {@code tools/models/greek_minoan.py} 生成；</li>
 *   <li>敌对哨兵石像 {@link RuinSentinelEntity}；</li>
 *   <li>登记：{@code guardian} 锚点各放一尊哨兵；{@code vault} 锚点的宝库用宝库表 {@value #VAULT_TABLE}；{@code loot} 锚点的箱子用
 *       战利品表 {@value #CHEST_LOOT}。</li>
 * </ul>
 */
public final class GreekRuins {
    public static final String TEMPLE_KIND = "greek:abandoned_temple";
    public static final String VAULT_TABLE = "mythic_age:greek_abandoned_temple";
    public static final String CHEST_LOOT = "mythic_age:chests/greek/abandoned_temple";
    public static final String KNOSSOS_KIND = "greek:knossos";
    public static final String KNOSSOS_LOOT = "mythic_age:chests/greek/knossos_storage";

    public static final DeferredBlock<FacingShapedBlock> MARBLE_RUBBLE = Content.block("marble_rubble", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(0, 0, 0, 16, 5, 16)))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(GreekMaterials.MARBLE.get()).noOcclusion())
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .loot(new LootSpec.Other(GreekMaterials.MARBLE))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE, GreekMaterials.MARBLE_FAMILY)
            .itemTags(GreekMaterials.MARBLE_FAMILY_ITEM)
            .lang("大理石碎块", "Marble Rubble", "大理石の瓦礫")
            .register();

    public static final DeferredBlock<StatueBlock> BROKEN_STATUE = Content.block("broken_greek_statue", GREEK, StatueBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(GreekMaterials.MARBLE.get()).noOcclusion()
                    .pushReaction(PushReaction.BLOCK))
            .model(new BlockModelSpec.StateModels(
                    s -> s.getValue(StatueBlock.HALF) == DoubleBlockHalf.LOWER ? "_lower" : "_upper", true, "_lower"))
            .loot(LootSpec.door())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("残缺神像", "Broken Statue", "壊れた神像")
            .register();

    /** 米诺斯红柱（T22 阶段 ④）：上粗下细的赭红柏木柱，按上下相邻自动分段，顶段带黑色圆垫柱头。 */
    public static final DeferredBlock<ColumnShaftBlock> MINOAN_COLUMN = Content.block("minoan_column", GREEK,
                    ColumnShaftBlock::new)
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_LOG).mapColor(MapColor.COLOR_RED)
                    .noOcclusion())
            .model(new BlockModelSpec.StateModels(s -> "_" + s.getValue(ColumnShaftBlock.PART).getSerializedName(), false,
                    "_single"))
            .tags(BlockTags.MINEABLE_WITH_AXE)
            .lang("米诺斯红柱", "Minoan Column", "ミノア式の赤柱")
            .register();

    /** 双斧门楣：白灰门楣上的青铜双斧浮雕（正面朝放置者）。 */
    public static final DeferredBlock<FacingShapedBlock> LABRYS_LINTEL = Content.block("labrys_lintel", GREEK,
                    p -> new FacingShapedBlock(p, false, Shapes.block()))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(GreekVillageBlocks.PLASTER.get()))
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("双斧门楣", "Labrys Lintel", "双斧のまぐさ石")
            .register();

    /** 圣角：矮座上两只向上外翻的角，立在墙头与门楣上。 */
    public static final DeferredBlock<FacingShapedBlock> HORNS_OF_CONSECRATION = Content.block("horns_of_consecration", GREEK,
                    p -> new FacingShapedBlock(p, false, Block.box(1, 0, 1, 15, 14, 15)))
            .properties(() -> BlockBehaviour.Properties.ofFullCopy(GreekMaterials.SMOOTH_MARBLE.get()).noOcclusion())
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("圣角", "Horns of Consecration", "聖別の角")
            .register();

    public static final DeferredHolder<EntityType<?>, EntityType<RuinSentinelEntity>> RUIN_SENTINEL =
            ModRegistries.ENTITY_TYPES.register("ruin_sentinel",
                    () -> EntityType.Builder.of(RuinSentinelEntity::new, MobCategory.MISC)
                            .sized(0.8F, 2.0F)
                            .eyeHeight(1.7F)
                            .fireImmune()
                            .clientTrackingRange(10)
                            .build("mythic_age:ruin_sentinel"));

    private GreekRuins() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityAttributeCreationEvent e) ->
                e.put(RUIN_SENTINEL.get(), RuinSentinelEntity.createRuinAttributes().build()));
        StructureRegistration.handler(TEMPLE_KIND, GreekRuins::registerTemple);
        StructureRegistration.handler(KNOSSOS_KIND, GreekRuins::registerKnossos);
    }

    /**
     * 登记一座克诺索斯迷宫：线团祭坛与竞技场实例（T23，{@code GreekKnossos.register}）；储物间与宝库房的箱子设战利品表。
     */
    static void registerKnossos(ServerLevel level, StructureInfoPiece.Info info) {
        io.github.happysnappingturtle.mythicage.content.greek.knossos.GreekKnossos.register(level, info);
        for (BlockPos l : info.anchors("loot")) {
            if (level.getBlockEntity(l) instanceof RandomizableContainerBlockEntity chest) {
                chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(KNOSSOS_LOOT)),
                        level.getRandom().nextLong());
            }
        }
    }

    /** 登记一座废弃神殿：放哨兵、设宝库表与箱子战利品表。 */
    static void registerTemple(ServerLevel level, StructureInfoPiece.Info info) {
        for (BlockPos g : info.anchors("guardian")) {
            RuinSentinelEntity s = RUIN_SENTINEL.get().create(level);
            if (s == null) {
                continue;
            }
            s.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
            s.standing();
            level.addFreshEntity(s);
        }
        for (BlockPos v : info.anchors("vault")) {
            if (level.getBlockEntity(v) instanceof RuinVaultBlockEntity be) {
                be.setTable(VAULT_TABLE);
            }
        }
        for (BlockPos l : info.anchors("loot")) {
            if (level.getBlockEntity(l) instanceof RandomizableContainerBlockEntity chest) {
                chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(CHEST_LOOT)),
                        level.getRandom().nextLong());
            }
        }
    }
}
