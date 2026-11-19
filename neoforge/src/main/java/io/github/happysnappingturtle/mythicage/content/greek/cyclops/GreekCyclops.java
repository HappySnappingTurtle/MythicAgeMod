package io.github.happysnappingturtle.mythicage.content.greek.cyclops;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.entity.cyclops.ShepherdCyclopsEntity;
import io.github.happysnappingturtle.mythicage.lair.EliteLairs;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.structure.RuinVaultBlockEntity;
import io.github.happysnappingturtle.mythicage.structure.StructureInfoPiece;
import io.github.happysnappingturtle.mythicage.structure.StructureRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;
import java.util.List;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 独眼巨人洞穴（ST-GR-08）与精英「独眼巨人·牧人」（EL-GR-02），T25a 阶段 ⑤：
 * <ul>
 *   <li>封洞巨石：不可破坏、不掉落、推不动，只由巢穴作为临时方块放下（《奥德赛》9.240–243）；</li>
 *   <li>结构登记：宝库设表 {@value #VAULT_TABLE}，交给 {@link EliteLairs} 建账（生成牧人与 {@value #FLOCK} 只羊，D16）；</li>
 *   <li>巢穴行为：家 = {@code cyclops} 锚点，傍晚牧人回到睡处后封洞。</li>
 * </ul>
 */
public final class GreekCyclops {
    public static final String CAVE_KIND = "greek:cyclops_cave";
    public static final String VAULT_TABLE = "mythic_age:greek_cyclops_cave";
    /** 每洞的羊（D16）。 */
    public static final int FLOCK = 6;

    public static final DeferredBlock<Block> CAVE_BOULDER = Content.block("cave_boulder", GREEK, Block::new)
            .properties(() -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK))
            .model(BlockModelSpec.cubeAll())
            .loot(LootSpec.none())
            .lang("封洞巨石", "Cave Boulder", "洞をふさぐ大岩")
            .register();

    /** 奶酪架（《奥德赛》9.219「编织的架子上压满了奶酪」）：装饰方块，朝向放置者，模型由 tools/models/greek_cyclops_blocks.py 生成。 */
    public static final DeferredBlock<io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock> CHEESE_RACK =
            Content.block("cheese_rack", GREEK, p -> new io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock(
                            p, false, Block.box(1, 0, 3, 15, 16, 13)))
                    .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F)
                            .sound(SoundType.WOOD).noOcclusion().ignitedByLava())
                    .model(new BlockModelSpec.StateModels(s -> "", true, ""))
                    .tags(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)
                    .lang("奶酪架", "Cheese Rack", "チーズ棚")
                    .register();

    /** 地上的酒碗用的外观（只给 {@link WineBowl} 显示用的物品；常春藤木碗，《奥德赛》9.346）。 */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> WINE_BOWL_ITEM =
            Content.item("wine_bowl", GREEK)
                    .model(io.github.happysnappingturtle.mythicage.content.ItemModelSpec.MANUAL)
                    .lang("一碗烈酒", "Bowl of Wine", "ぶどう酒の椀")
                    .register();

    /** 独眼巨人·牧人：碰撞同召唤版 1.8 × 3.8。 */
    public static final DeferredHolder<EntityType<?>, EntityType<ShepherdCyclopsEntity>> SHEPHERD =
            ModRegistries.ENTITY_TYPES.register("cyclops",
                    () -> EntityType.Builder.of(ShepherdCyclopsEntity::new, MobCategory.MONSTER)
                            .sized(1.8F, 3.8F)
                            .eyeHeight(3.35F)
                            .clientTrackingRange(10)
                            .build("mythic_age:cyclops"));

    // ───────────── 阶段 ⑥：烈酒、酒碗、投掷巨石、掉落物 ─────────────

    /** 马洛涅烈酒（D9：葡萄 ×4 + 蜂蜜瓶，无序合成；D10：像雪球一样扔）。 */
    public static final net.neoforged.neoforge.registries.DeferredItem<MaroneanWineItem> MARONEAN_WINE =
            Content.item("maronean_wine", GREEK, MaroneanWineItem::new)
                    .properties(p -> p.stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON))
                    .lang("马洛涅烈酒", "Maronean Wine", "マロネイアの強いぶどう酒")
                    .register();

    /** 库克罗普斯之火（宙斯雷霆的材料，T25h；《神谱》139–146）。 */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> CYCLOPS_FIRE =
            Content.item("cyclops_fire", GREEK)
                    .properties(p -> p.rarity(net.minecraft.world.item.Rarity.RARE))
                    .lang("库克罗普斯之火", "Cyclopean Fire", "キュクロプスの火")
                    .register();

    /** 英雄锻造模板（希腊、北欧、中华共用；用途在 T25c 开放，D14）。 */
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> HERO_SMITHING_TEMPLATE =
            Content.item("hero_smithing_template", GREEK)
                    .properties(p -> p.rarity(net.minecraft.world.item.Rarity.RARE))
                    .lang("英雄锻造模板", "Hero Smithing Template", "英雄の鍛造テンプレート")
                    .register();

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownWine>> THROWN_WINE =
            ModRegistries.ENTITY_TYPES.register("maronean_wine",
                    () -> EntityType.Builder.<ThrownWine>of(ThrownWine::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("mythic_age:maronean_wine"));

    public static final DeferredHolder<EntityType<?>, EntityType<WineBowl>> WINE_BOWL =
            ModRegistries.ENTITY_TYPES.register("wine_bowl",
                    () -> EntityType.Builder.<WineBowl>of(WineBowl::new, MobCategory.MISC)
                            .sized(0.5F, 0.25F)
                            .clientTrackingRange(6)
                            .build("mythic_age:wine_bowl"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownBoulder>> THROWN_BOULDER =
            ModRegistries.ENTITY_TYPES.register("cyclops_boulder",
                    () -> EntityType.Builder.<ThrownBoulder>of(ThrownBoulder::new, MobCategory.MISC)
                            .sized(0.9F, 0.9F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("mythic_age:cyclops_boulder"));

    private GreekCyclops() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityAttributeCreationEvent e) ->
                e.put(SHEPHERD.get(), ShepherdCyclopsEntity.createShepherdAttributes().build()));
        StructureRegistration.handler(CAVE_KIND, GreekCyclops::registerCave);
        EliteLairs.kind(CAVE_KIND, new CaveKind());
    }

    /** 登记一处洞穴：设宝库表、建巢穴账（生成牧人与羊群）。 */
    public static void registerCave(ServerLevel level, StructureInfoPiece.Info info) {
        for (BlockPos v : info.anchors("vault")) {
            if (level.getBlockEntity(v) instanceof RuinVaultBlockEntity be) {
                be.setTable(VAULT_TABLE);
            }
        }
        List<BlockPos> home = info.anchors("cyclops");
        EliteLairs.create(level, lairKey(level, home.isEmpty() ? info.anchors("entrance").get(0) : home.get(0)), CAVE_KIND,
                info.anchors());
    }

    /** 巢穴键：种类 + 维度 + 家的位置。 */
    public static String lairKey(ServerLevel level, BlockPos home) {
        return CAVE_KIND + "@" + level.dimension().location() + "@" + home.asLong();
    }

    static final class CaveKind implements EliteLairs.Kind {
        @Nullable
        @Override
        public Mob spawnElite(ServerLevel level, EliteLairs.Lair lair) {
            ShepherdCyclopsEntity c = SHEPHERD.get().create(level);
            if (c == null) {
                return null;
            }
            BlockPos home = lair.home(this);
            List<BlockPos> sleep = lair.anchors("sleep");
            c.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
            c.bindLair(lair.key, home, sleep.isEmpty() ? home : sleep.get(0));
            c.finalizeSpawn(level, level.getCurrentDifficultyAt(home), MobSpawnType.STRUCTURE, null);
            c.setHealth(c.getMaxHealth());
            level.addFreshEntity(c);
            return c;
        }

        @Nullable
        @Override
        public Mob spawnFollower(ServerLevel level, EliteLairs.Lair lair) {
            Sheep s = EntityType.SHEEP.create(level);
            if (s == null) {
                return null;
            }
            List<BlockPos> flock = lair.anchors("flock");
            BlockPos at = flock.isEmpty() ? lair.home(this) : flock.get(0);
            s.moveTo(at.getX() + 0.5 + level.getRandom().nextInt(3) - 1, at.getY(), at.getZ() + 0.5 + level.getRandom().nextInt(3) - 1,
                    level.getRandom().nextFloat() * 360, 0);
            s.setPersistenceRequired();
            level.addFreshEntity(s);
            return s;
        }

        @Override
        public int followers() {
            return FLOCK;
        }

        @Override
        public BlockState sealBlock() {
            return CAVE_BOULDER.get().defaultBlockState();
        }

        @Override
        public boolean inside(Mob elite, EliteLairs.Lair lair) {
            return elite instanceof ShepherdCyclopsEntity s && s.inside();
        }

        @Override
        public String homeAnchor() {
            return "cyclops";
        }
    }
}
