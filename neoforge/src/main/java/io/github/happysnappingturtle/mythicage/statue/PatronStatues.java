package io.github.happysnappingturtle.mythicage.statue;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKinds;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 玩家的主神像（T11a 设计文档 §5.2；用户修订 U3、U7、U8）：多方块结构，用道具一键按 NBT 模板放下。
 * <ul>
 *   <li>核心方块渲染整尊神像、右键兑换；部件方块只提供碰撞；模板里其余非装饰方块（底座）计入完整性；</li>
 *   <li>放置时把结构清单记进核心方块实体；结构内方块变化后重新比对，缺任何一块即「受损」，不能兑换；
 *       部件缺了由核心补回；</li>
 *   <li>只有主人能拆；爆炸不破坏；主人拆核心时整座神像的部件消失、道具返还；</li>
 *   <li>登记为神庙种类 {@code <文明>:statue_<主神>}，每位玩家每位主神 1 座。</li>
 * </ul>
 * 模板：{@code data/<ns>/structure/statue/<文明>_<主神>.nbt}，没有时用 {@code statue/default}（T11a 测试模板）。
 */
public final class PatronStatues {
    /** 模板按朝北绘制（核心 facing = north）。 */
    public static final String DEFAULT_TEMPLATE = "statue/default";
    /** 部件、底座离核心的最大水平距离（找核心、建索引用）。 */
    static final int REACH = 4;
    /** 不计入完整性的装饰方块（踏步、花盆……）。 */
    public static final TagKey<Block> DECOR = ModTags.block("statue_decor");

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MythicAgeIds.MOD_ID);
    public static final Supplier<DataComponentType<StatueTarget>> TARGET = COMPONENTS.registerComponentType("statue_target",
            b -> b.persistent(StatueTarget.CODEC).networkSynchronized(StatueTarget.STREAM_CODEC));

    public static final DeferredBlock<StatueCoreBlock> CORE = Content.block("patron_statue_core", GREEK, StatueCoreBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(3.0f, 3_600_000.0f)
                    .sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK))
            .model(new BlockModelSpec.Manual())
            .noItem()
            .loot(LootSpec.none())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("主神像核心", "Patron Statue Core", "主神像の核")
            .register();

    public static final DeferredBlock<StatuePartBlock> PART = Content.block("patron_statue_part", GREEK, StatuePartBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(3.0f, 3_600_000.0f)
                    .sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK))
            .model(new BlockModelSpec.Manual())
            .noItem()
            .loot(LootSpec.none())
            .tags(BlockTags.MINEABLE_WITH_PICKAXE)
            .lang("主神像部件", "Patron Statue Part", "主神像の部品")
            .register();

    public static final DeferredItem<PatronStatueItem> ITEM = Content.item("patron_statue", GREEK, PatronStatueItem::new)
            .properties(p -> p.stacksTo(1))
            .lang("主神像", "Patron Statue", "主神像")
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StatueCoreBlockEntity>> CORE_BE =
            ModRegistries.BLOCK_ENTITY_TYPES.register("patron_statue_core",
                    () -> BlockEntityType.Builder.of(StatueCoreBlockEntity::new, CORE.get()).build(null));

    /** 结构内方块变化后，本刻结束时要重新比对的核心。 */
    private static final Set<GlobalPos> PENDING = new LinkedHashSet<>();

    private PatronStatues() {
    }

    public static void init(IEventBus modBus, IEventBus game) {
        COMPONENTS.register(modBus);
        game.addListener(PatronStatues::onNeighborNotify);
        game.addListener(PatronStatues::onBreak);
        game.addListener(PatronStatues::onExplosion);
        game.addListener(PatronStatues::onServerTickPost);
        game.addListener((ServerStoppedEvent e) -> PENDING.clear());
    }

    // ───────────── 种类 ─────────────

    private static final Map<String, ShrineKind> KINDS = new HashMap<>();

    public static String kindId(Civilization civ, String patron) {
        return civ.id() + ":statue_" + patron;
    }

    /** 每位主神一种神庙种类（数量上限每种 1 座）；第一次用到时登记。 */
    public static synchronized ShrineKind kindFor(Civilization civ, String patron) {
        String id = kindId(civ, patron);
        return KINDS.computeIfAbsent(id, k -> ShrineKinds.byId(k)
                .orElseGet(() -> ShrineKinds.register(new ShrineKind(k, civ, CORE, 1, ShrineKind.Hooks.NONE))));
    }

    static boolean isStatueKind(String kind) {
        return kind.contains(":statue_");
    }

    // ───────────── 查询 ─────────────

    /** @param intact 结构完整（可以兑换） */
    public record Info(UUID owner, Civilization civ, String patron, boolean intact) {
    }

    public static Optional<Info> info(ServerLevel level, BlockPos core) {
        if (!(level.getBlockEntity(core) instanceof StatueCoreBlockEntity be) || be.owner().isEmpty()) {
            return Optional.empty();
        }
        check(level, be);
        return Optional.of(new Info(be.owner().get(), be.civ(), be.patron(), be.intact()));
    }

    /** pos 属于哪座主神像（核心本身、部件或计入完整性的方块）；不属于时为空。 */
    public static Optional<BlockPos> coreOf(ServerLevel level, BlockPos pos) {
        ShrineRegistry reg = ShrineRegistry.get(level.getServer());
        for (ShrineKind kind : ShrineKinds.all()) {
            if (!isStatueKind(kind.id())) {
                continue;
            }
            for (BlockPos core : reg.near(kind.id(), level.dimension(), pos, REACH)) {
                if (core.equals(pos)) {
                    return Optional.of(core);
                }
                if (level.isLoaded(core) && level.getBlockEntity(core) instanceof StatueCoreBlockEntity be
                        && be.parts().stream().anyMatch(p -> core.offset(p.offset()).equals(pos))) {
                    return Optional.of(core);
                }
            }
        }
        return Optional.empty();
    }

    // ───────────── 放置 ─────────────

    public enum PlaceResult {
        OK, NO_TARGET, NO_TEMPLATE, LIMIT, NO_SPACE, OVERLAP;

        public String langKey() {
            return "mythic_age.statue.place." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** 某主神的模板；没有专属模板时用默认模板。 */
    static Optional<StructureTemplate> template(ServerLevel level, StatueTarget t) {
        var manager = level.getStructureManager();
        Optional<StructureTemplate> own = manager.get(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID,
                "statue/" + t.civ().id() + "_" + t.patron()));
        return own.isPresent() ? own : manager.get(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, DEFAULT_TEMPLATE));
    }

    static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * 按模板放下一座主神像：模板底面的中心落在 target，神像正面朝 facing。
     * 空间（全部为空气或可替换方块）、数量上限、与其他神庙不重叠都满足才放下。
     */
    public static PlaceResult place(ServerPlayer player, ServerLevel level, StatueTarget target, BlockPos at, Direction facing) {
        if (target.patron().isEmpty()) {
            return PlaceResult.NO_TARGET;
        }
        Optional<StructureTemplate> tpl = template(level, target);
        if (tpl.isEmpty()) {
            return PlaceResult.NO_TEMPLATE;
        }
        String kind = kindFor(target.civ(), target.patron()).id();
        ShrineRegistry reg = ShrineRegistry.get(level.getServer());
        if (reg.countOwned(player.getUUID(), kind) >= 1) {
            return PlaceResult.LIMIT;
        }
        StructureTemplate t = tpl.get();
        var size = t.getSize();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotationFor(facing))
                .setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2));
        BlockPos origin = at.offset(-size.getX() / 2, 0, -size.getZ() / 2);
        BoundingBox box = t.getBoundingBox(settings, origin);
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (!level.isLoaded(p) || !s.canBeReplaced() || !s.getFluidState().isEmpty() || !level.mayInteract(player, p)) {
                return PlaceResult.NO_SPACE;
            }
            if (reg.get(GlobalPos.of(level.dimension(), p.immutable())).isPresent()) {
                return PlaceResult.OVERLAP;
            }
        }
        t.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_ALL);
        BlockPos core = null;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (level.getBlockState(p).is(CORE.get())) {
                core = p.immutable();
                break;
            }
        }
        if (core == null || !(level.getBlockEntity(core) instanceof StatueCoreBlockEntity be)) {
            // 模板里没有核心：撤销
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            return PlaceResult.NO_TEMPLATE;
        }
        List<StatueCoreBlockEntity.Part> parts = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (!p.equals(core) && !s.isAir() && !s.is(DECOR)) {
                parts.add(new StatueCoreBlockEntity.Part(p.subtract(core), s));
            }
        }
        be.bind(player.getUUID(), player.getGameProfile().getName(), MythData.get(player).civ(target.civ()).age());
        be.setup(target.civ(), target.patron(), parts);
        Direction coreFacing = level.getBlockState(core).getValue(StatueCoreBlock.FACING);
        reg.add(new ShrineRegistry.Entry(GlobalPos.of(level.dimension(), core), player.getUUID(),
                player.getGameProfile().getName(), coreFacing, target.civ(), kind));
        check(level, be);
        return PlaceResult.OK;
    }

    // ───────────── 完整性 ─────────────

    /** 比对结构清单；部件缺了且该格可替换时补回。只在置脏后重算。 */
    public static void check(ServerLevel level, StatueCoreBlockEntity be) {
        if (!be.dirty()) {
            return;
        }
        BlockPos core = be.getBlockPos();
        List<BlockPos> missing = new ArrayList<>();
        for (StatueCoreBlockEntity.Part p : be.parts()) {
            BlockPos at = core.offset(p.offset());
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState actual = level.getBlockState(at);
            if (p.state().is(PART.get())) {
                if (!actual.is(PART.get())) {
                    if (actual.canBeReplaced() && actual.getFluidState().isEmpty()) {
                        level.setBlock(at, PART.get().defaultBlockState(), Block.UPDATE_ALL);
                    } else {
                        missing.add(at);
                    }
                }
            } else if (!actual.is(p.state().getBlock())) {
                missing.add(at);
            }
        }
        be.setResult(missing.isEmpty(), missing);
    }

    /** 用红色微粒标出缺失的格子（只给这名玩家看）。 */
    static void showMissing(ServerPlayer player, StatueCoreBlockEntity be) {
        DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0f, 0.25f, 0.2f), 1.2f);
        for (BlockPos p : be.missing()) {
            player.serverLevel().sendParticles(player, red, true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12,
                    0.3, 0.3, 0.3, 0);
        }
    }

    private static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            coreOf(level, event.getPos()).ifPresent(core -> PENDING.add(GlobalPos.of(level.dimension(), core)));
        }
    }

    private static void onServerTickPost(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        List<GlobalPos> todo = new ArrayList<>(PENDING);
        PENDING.clear();
        for (GlobalPos g : todo) {
            ServerLevel level = server.getLevel(g.dimension());
            if (level != null && level.isLoaded(g.pos()) && level.getBlockEntity(g.pos()) instanceof StatueCoreBlockEntity be) {
                be.markDirty();
                check(level, be);
            }
        }
    }

    /** 只有主人（或创造模式管理员）能拆结构内的方块。 */
    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Optional<BlockPos> core = coreOf(level, event.getPos());
        if (core.isEmpty() || !(level.getBlockEntity(core.get()) instanceof StatueCoreBlockEntity be)) {
            return;
        }
        var player = event.getPlayer();
        boolean owner = be.owner().map(player.getUUID()::equals).orElse(true);
        boolean admin = player.isCreative() && player.hasPermissions(2);
        if (!owner && !admin) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("mythic_age.statue.protected", be.ownerName()), true);
        }
    }

    /** 爆炸不破坏主神像。 */
    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            event.getAffectedBlocks().removeIf(p -> coreOf(level, p).isPresent());
        }
    }

    /** 核心被拆：部件消失、登记注销、道具返还（底座方块保留为普通方块）。 */
    static void onCoreRemoved(ServerLevel level, BlockPos core, StatueCoreBlockEntity be) {
        for (StatueCoreBlockEntity.Part p : be.parts()) {
            BlockPos at = core.offset(p.offset());
            if (level.getBlockState(at).is(PART.get())) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        ShrineService.onRemoved(level, core, be.kind());
        if (be.owner().isPresent()) {
            Block.popResource(level, core, stack(be.civ(), be.patron()));
        }
    }

    public static ItemStack stack(Civilization civ, String patron) {
        ItemStack s = new ItemStack(ITEM.get());
        s.set(TARGET.get(), new StatueTarget(civ, patron));
        return s;
    }

    /** 玩家是否已有该主神的像（放下的或背包里的道具）。 */
    public static boolean owns(ServerPlayer player, Civilization civ, String patron) {
        if (ShrineRegistry.get(player.server).countOwned(player.getUUID(), kindId(civ, patron)) > 0) {
            return true;
        }
        StatueTarget t = new StatueTarget(civ, patron);
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ITEM.get()) && t.equals(s.get(TARGET.get()))) {
                return true;
            }
        }
        return false;
    }
}
