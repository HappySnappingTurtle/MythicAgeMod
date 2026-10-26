package io.github.happysnappingturtle.mythicage.content.greek.knossos;

import io.github.happysnappingturtle.mythicage.boss.ArenaSpec;
import io.github.happysnappingturtle.mythicage.boss.TrialArena;
import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.structure.StructureInfoPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.Optional;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 克诺索斯迷宫的玩法（T23）：牛首封门、线团祭坛、阿里阿德涅线团、弥诺陶洛斯 Boss，以及迷宫登记时建立竞技场实例。
 */
public final class GreekKnossos {
    public static final String KIND = "greek:knossos";
    /** 宝库表：阿喀琉斯之甲，只给有首杀记录的玩家（§7.2）。 */
    public static final String VAULT_TABLE = "mythic_age:greek_knossos";

    public static final DeferredBlock<BullGateBlock> BULL_GATE = Content.block("bull_gate", GREEK, BullGateBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK))
            .model(new BlockModelSpec.StateModels(s -> s.getValue(BullGateBlock.HEAD) ? "_head" : "", false, ""))
            .loot(LootSpec.none())
            .noItem()
            .lang("牛首封门", "Bull-Head Gate", "牡牛の封門")
            .register();

    public static final DeferredBlock<ThreadAltarBlock> THREAD_ALTAR = Content.block("thread_altar", GREEK, ThreadAltarBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK))
            .model(new BlockModelSpec.StateModels(s -> "", false, ""))
            .loot(LootSpec.none())
            .noItem()
            .lang("线团祭坛", "Thread Altar", "糸玉の祭壇")
            .register();

    public static final DeferredItem<Item> ARIADNE_THREAD = Content.item("ariadne_thread", GREEK, Item::new)
            .properties(p -> p.stacksTo(1))
            .lang("阿里阿德涅线团", "Ariadne's Thread", "アリアドネの糸玉")
            .register();

    /** 弥诺陶洛斯之角：神之信物（总纲 §3.4：只是纪念与触发器，记录在击杀时已写入）。 */
    public static final DeferredItem<TrialTokenItem> MINOTAUR_HORN = Content.item("minotaur_horn", GREEK,
                    p -> new TrialTokenItem(p, "mythic_age:minotaur"))
            .properties(p -> p.stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON))
            .lang("弥诺陶洛斯之角", "Horn of the Minotaur", "ミノタウロスの角")
            .register();

    /** 牛角：英雄装备部件（阶段 B 的英雄锻造用）。 */
    public static final DeferredItem<Item> BULL_HORN = Content.item("bull_horn", GREEK, Item::new)
            .lang("牛角", "Bull Horn", "牛の角")
            .register();

    public static final DeferredHolder<EntityType<?>, EntityType<MinotaurBossEntity>> MINOTAUR_BOSS =
            ModRegistries.ENTITY_TYPES.register("minotaur_boss",
                    () -> EntityType.Builder.of(MinotaurBossEntity::new, MobCategory.MONSTER)
                            .sized(1.1F, 2.5F)
                            .fireImmune()
                            .clientTrackingRange(10)
                            .build("mythic_age:minotaur_boss"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRubble>> THROWN_RUBBLE =
            ModRegistries.ENTITY_TYPES.register("thrown_rubble",
                    () -> EntityType.Builder.<ThrownRubble>of(ThrownRubble::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(6)
                            .updateInterval(10)
                            .build("mythic_age:thrown_rubble"));

    private GreekKnossos() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityAttributeCreationEvent e) ->
                e.put(MINOTAUR_BOSS.get(), MinotaurBossEntity.createBossAttributes().build()));
        NeoForge.EVENT_BUS.addListener(ThreadTrails::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ThreadTrails::onLogout);
    }

    /**
     * 迷宫登记（由 {@code GreekRuins.registerKnossos} 调用）：入口格放线团祭坛，建立竞技场实例。
     */
    public static void register(ServerLevel level, StructureInfoPiece.Info info) {
        for (BlockPos t : info.anchors("thread")) {
            level.setBlock(t, THREAD_ALTAR.get().defaultBlockState(), 2);
        }
        for (BlockPos v : info.anchors("vault")) {
            if (level.getBlockEntity(v) instanceof io.github.happysnappingturtle.mythicage.structure.RuinVaultBlockEntity be) {
                be.setTable(VAULT_TABLE);
            }
        }
        Optional<ArenaSpec> spec = ArenaSpec.Loader.get(KIND);
        List<BlockPos> origin = info.anchors("boss_origin");
        if (spec.isEmpty() || origin.isEmpty()) {
            return;
        }
        BlockPos o = origin.get(0);
        TrialArenas.get(level.getServer()).add(TrialArena.create(TrialArenas.idFor(level, o), KIND, level.dimension(), o,
                first(info, "gate"), first(info, "vault_door"), info.anchors("pillar"), info.anchors("wall_a"),
                info.anchors("wall_b"), region(info, o), spec.get()));
    }

    private static BlockPos first(StructureInfoPiece.Info info, String name) {
        List<BlockPos> l = info.anchors(name);
        return l.isEmpty() ? null : l.get(0);
    }

    /** 迷宫范围（回程线）：64 × 64 的迷宫层加上向南到地表入口的台阶与残迹。 */
    static BoundingBox region(StructureInfoPiece.Info info, BlockPos origin) {
        int cx = origin.getX();
        int cz = origin.getZ() + 2;
        int floor = origin.getY();
        BlockPos entrance = first(info, "entrance");
        int south = entrance != null ? entrance.getZ() + 14 : cz + 40;
        int top = entrance != null ? entrance.getY() + 12 : floor + 40;
        return new BoundingBox(cx - 36, floor - 4, cz - 36, cx + 36, top, Math.max(cz + 36, south));
    }
}
