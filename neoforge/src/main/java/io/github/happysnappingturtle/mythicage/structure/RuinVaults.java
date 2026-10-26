package io.github.happysnappingturtle.mythicage.structure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.structure.VaultRules;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 遗迹宝库（T22 §3.4、§4.4）：方块注册、宝库表（{@code data/<ns>/mythic_age/ruin_vault/<id>.json}）与开启发放。
 *
 * <p>宝库表：{@code relics}（圣物池，优先给没拿过的）、{@code loot}（战利品表 id，每人一份）、{@code blueprint}
 * （图纸；阶段 A 留空，锻造系统在阶段 B）。发放顺序：写这座宝库的收据（重复就拒绝）→ 圣物（写圣物收据）→ 战利品，
 * 都直接进背包，满了掉在脚下（不吞，红线 6）。
 */
public final class RuinVaults extends SimpleJsonResourceReloadListener {
    /** 一张宝库表。 */
    /**
     * 一张宝库表。{@code requiresFirstKill} 非空时，只有该试炼 Boss 有首杀记录的玩家能开（克诺索斯：阿喀琉斯之甲，T23 §7.2）。
     */
    public record Table(List<String> relics, String loot, String blueprint, String requiresFirstKill) {
        public Table {
            relics = List.copyOf(relics);
        }

        public Table(List<String> relics, String loot, String blueprint) {
            this(relics, loot, blueprint, "");
        }
    }

    public static final DeferredBlock<RuinVaultBlock> VAULT = Content.block("ruin_vault", Civilization.GREEK, RuinVaultBlock::new)
            .properties(() -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0f, 3600000.0f)
                    .sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK))
            .model(new BlockModelSpec.StateModels(s -> "", true, ""))
            .loot(new LootSpec.None())
            .tags(BlockTags.DRAGON_IMMUNE, BlockTags.WITHER_IMMUNE)
            .lang("遗迹宝库", "Ruin Vault", "遺跡の宝庫")
            .register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RuinVaultBlockEntity>> BLOCK_ENTITY =
            ModRegistries.BLOCK_ENTITY_TYPES.register("ruin_vault",
                    () -> BlockEntityType.Builder.of(RuinVaultBlockEntity::new, VAULT.get()).build(null));

    private static final Map<String, Table> TABLES = new ConcurrentHashMap<>();
    private static final Map<String, Table> TEST = new ConcurrentHashMap<>();

    public RuinVaults() {
        super(new com.google.gson.Gson(), "mythic_age/ruin_vault");
    }

    public static void init() {
        // 触发静态注册
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new RuinVaults());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Table> out = new ConcurrentHashMap<>();
        files.forEach((id, json) -> {
            try {
                out.put(id.getNamespace() + ":" + id.getPath(), parse(json.getAsJsonObject()));
            } catch (RuntimeException e) {
                MythicAge.LOGGER.error("宝库表 {} 读取失败：{}", id, e.getMessage());
            }
        });
        TABLES.clear();
        TABLES.putAll(out);
    }

    static Table parse(JsonObject o) {
        List<String> relics = new ArrayList<>();
        if (o.has("relics")) {
            o.getAsJsonArray("relics").forEach(e -> relics.add(e.getAsString()));
        }
        return new Table(relics, o.has("loot") ? o.get("loot").getAsString() : "",
                o.has("blueprint") && !o.get("blueprint").isJsonNull() ? o.get("blueprint").getAsString() : "",
                o.has("requires_first_kill") ? o.get("requires_first_kill").getAsString() : "");
    }

    public static Optional<Table> table(String id) {
        Table t = TEST.get(id);
        return Optional.ofNullable(t != null ? t : TABLES.get(id));
    }

    /** 只供 GameTest：临时放一张宝库表（null 移除）。 */
    public static void putForTest(String id, @Nullable Table table) {
        if (table == null) {
            TEST.remove(id);
        } else {
            TEST.put(id, table);
        }
    }

    // ───────────── 开启 ─────────────

    /** 开启结果（GameTest 看）。 */
    public enum Outcome { GIVEN, ALREADY, EMPTY, LOCKED }

    public static Outcome open(ServerPlayer player, RuinVaultBlockEntity be) {
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockPos pos = be.getBlockPos();
        Optional<Table> table = table(be.table());
        if (level == null || table.isEmpty()) {
            player.displayClientMessage(Component.translatable("mythic_age.vault.empty"), true);
            return Outcome.EMPTY;
        }
        String req = table.get().requiresFirstKill();
        if (!req.isEmpty() && !MythData.get(player).firstKills().contains(req)) {
            // 没打赢过：不写收据，以后打赢了还能开
            player.displayClientMessage(Component.translatable("mythic_age.vault.trial_locked"), true);
            return Outcome.LOCKED;
        }
        String receipt = VaultRules.receipt(level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
        if (MythData.get(player).receipts().contains(ReceiptBook.Scope.UNIQUE, receipt)
                || MythData.transact(player, Transactions.Request.unique(receipt)).status() != Transactions.Status.COMMITTED) {
            be.markOpened(player.getUUID());
            player.displayClientMessage(Component.translatable("mythic_age.vault.already"), true);
            return Outcome.ALREADY;
        }
        be.markOpened(player.getUUID());
        List<ItemStack> given = new ArrayList<>();
        VaultRules.pickRelic(table.get().relics(), item -> MythData.get(player).receipts()
                        .contains(ReceiptBook.Scope.UNIQUE, VaultRules.relicReceipt(item)), new java.util.Random(level.getRandom().nextLong()))
                .flatMap(RuinVaults::stack).ifPresent(relic -> {
                    MythData.transact(player, Transactions.Request.unique(VaultRules.relicReceipt(
                            BuiltInRegistries.ITEM.getKey(relic.getItem()).toString())));
                    given.add(relic);
                });
        if (!table.get().loot().isEmpty()) {
            LootTable loot = level.getServer().reloadableRegistries().getLootTable(
                    ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(table.get().loot())));
            given.addAll(loot.getRandomItems(new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.CHEST)));
        }
        given.forEach(st -> TradeService.give(player, st));
        level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("mythic_age.vault.opened"), true);
        return Outcome.GIVEN;
    }

    private static Optional<ItemStack> stack(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).map(ItemStack::new);
    }
}
