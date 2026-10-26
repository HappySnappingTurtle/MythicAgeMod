package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.structure.VaultRules;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics;
import io.github.happysnappingturtle.mythicage.structure.RuinVaultBlockEntity;
import io.github.happysnappingturtle.mythicage.structure.RuinVaults;
import io.github.happysnappingturtle.mythicage.structure.StructureInfoPiece;
import io.github.happysnappingturtle.mythicage.structure.StructureRegistration;
import io.github.happysnappingturtle.mythicage.structure.StructureSites;
import io.github.happysnappingturtle.mythicage.structure.TerrainTemplatePiece;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildingPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T22 阶段 ①：通用结构框架（单元测试用例文档 TC-T22-30、32～37）。宝库用测试表 {@code mythic_age:test_vault}
 * （圣物池 = 瑞亚之冠、皮格马利翁雕像，带一张原版战利品表），每个用例自己放宝库方块。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureFrameworkGameTests {
    private static final String BATCH = "structure_framework";
    private static final String T = GameTestTemplates.EMPTY_11X11;
    private static final String TABLE = "mythic_age:test_vault";
    private static final String CROWN = "mythic_age:rheia_crown";
    private static final String STATUE = "mythic_age:pygmalion_statue";

    private StructureFrameworkGameTests() {
    }

    static RuinVaultBlockEntity vault(GameTestHelper h) {
        RuinVaults.putForTest(TABLE, new RuinVaults.Table(List.of(CROWN, STATUE), "minecraft:chests/simple_dungeon", ""));
        BlockPos rel = new BlockPos(5, 1, 5);
        h.setBlock(rel, RuinVaults.VAULT.get());
        RuinVaultBlockEntity be = (RuinVaultBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
        be.setTable(TABLE);
        return be;
    }

    static FakePlayer player(GameTestHelper h, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5, 1, 3))));
        p.getInventory().clearContent();
        OnlinePlayers.forTest(p, true);
        return p;
    }

    static int relics(FakePlayer p) {
        return p.getInventory().countItem(GreekRelics.RHEIA_CROWN.get()) + p.getInventory().countItem(GreekRelics.PYGMALION_STATUE.get());
    }

    /** TC-T22-32、33：A 开一次得到圣物与战利品、记下收据；再开一次拒绝，背包不变。 */
    @GameTest(template = T, batch = BATCH)
    public static void vault_once_per_player(GameTestHelper h) {
        RuinVaultBlockEntity be = vault(h);
        FakePlayer a = player(h, "v32");
        h.assertTrue(RuinVaults.open(a, be) == RuinVaults.Outcome.GIVEN, "第一次开到");
        h.assertTrue(relics(a) == 1, "圣物 1 件：" + relics(a));
        BlockPos pos = be.getBlockPos();
        String receipt = VaultRules.receipt(h.getLevel().dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
        h.assertTrue(MythData.get(a).receipts().contains(ReceiptBook.Scope.UNIQUE, receipt), "宝库收据");
        int items = a.getInventory().items.stream().mapToInt(ItemStack::getCount).sum();
        h.assertTrue(RuinVaults.open(a, be) == RuinVaults.Outcome.ALREADY, "第二次拒绝");
        h.assertTrue(a.getInventory().items.stream().mapToInt(ItemStack::getCount).sum() == items, "背包不变");
        OnlinePlayers.forTest(a, false);
        RuinVaults.putForTest(TABLE, null);
        h.succeed();
    }

    /** TC-T22-34：B 开同一座也得一份（合同「两名玩家先后开宝库各得一份」）；同一人第二座给另一件圣物。 */
    @GameTest(template = T, batch = BATCH)
    public static void vault_each_player_and_fresh_relic(GameTestHelper h) {
        RuinVaultBlockEntity be = vault(h);
        FakePlayer a = player(h, "v34a");
        FakePlayer b = player(h, "v34b");
        RuinVaults.open(a, be);
        h.assertTrue(RuinVaults.open(b, be) == RuinVaults.Outcome.GIVEN && relics(b) == 1, "B 也得一份");
        String first = a.getInventory().countItem(GreekRelics.RHEIA_CROWN.get()) > 0 ? CROWN : STATUE;
        // 第二座宝库（另一个位置）：A 拿到的应是另一件
        BlockPos rel2 = new BlockPos(7, 1, 5);
        h.setBlock(rel2, RuinVaults.VAULT.get());
        RuinVaultBlockEntity be2 = (RuinVaultBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel2));
        be2.setTable(TABLE);
        RuinVaults.open(a, be2);
        h.assertTrue(a.getInventory().countItem(GreekRelics.RHEIA_CROWN.get()) == 1
                && a.getInventory().countItem(GreekRelics.PYGMALION_STATUE.get()) == 1, "两件各一（先拿 " + first + "）");
        OnlinePlayers.forTest(a, false);
        OnlinePlayers.forTest(b, false);
        RuinVaults.putForTest(TABLE, null);
        h.succeed();
    }

    /** TC-T22-35：背包满了，东西掉在脚下，不吞。 */
    @GameTest(template = T, batch = BATCH)
    public static void vault_full_inventory_drops(GameTestHelper h) {
        RuinVaultBlockEntity be = vault(h);
        FakePlayer a = player(h, "v35");
        for (int i = 0; i < a.getInventory().items.size(); i++) {
            a.getInventory().items.set(i, new ItemStack(Items.BEDROCK, 64));
        }
        RuinVaults.open(a, be);
        List<ItemEntity> dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(a.blockPosition()).inflate(3));
        h.assertTrue(dropped.stream().anyMatch(e -> e.getItem().is(GreekRelics.RHEIA_CROWN.get())
                || e.getItem().is(GreekRelics.PYGMALION_STATUE.get())), "圣物掉在脚下：" + dropped.size());
        dropped.forEach(ItemEntity::discard);
        OnlinePlayers.forTest(a, false);
        RuinVaults.putForTest(TABLE, null);
        h.succeed();
    }

    /** TC-T22-36：挖不动、炸不掉。 */
    @GameTest(template = T, batch = BATCH)
    public static void vault_indestructible(GameTestHelper h) {
        RuinVaultBlockEntity be = vault(h);
        BlockPos pos = be.getBlockPos();
        h.assertTrue(h.getLevel().getBlockState(pos).getDestroySpeed(h.getLevel(), pos) < 0, "硬度同基岩");
        h.getLevel().explode(null, pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5, 4.0F, Level.ExplosionInteraction.TNT);
        h.assertTrue(h.getLevel().getBlockState(pos).is(RuinVaults.VAULT.get()), "炸不掉");
        RuinVaults.putForTest(TABLE, null);
        h.succeed();
    }

    /** TC-T22-37：同一处结构登记两次只处理一次；锚点所在区块没加载时等待。 */
    @GameTest(template = T, batch = BATCH)
    public static void registration_once(GameTestHelper h) {
        AtomicInteger calls = new AtomicInteger();
        StructureRegistration.handler("test:site", (level, info) -> calls.incrementAndGet());
        Structure s = h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "greek_village"));
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        StructureInfoPiece piece = new StructureInfoPiece(new StructureInfoPiece.Info("test:site",
                Map.of("guardian", List.of(at))), at);
        ChunkPos cp = new ChunkPos(at);
        StructureStart start = new StructureStart(s, cp, 0, new PiecesContainer(List.of(piece)));
        String key = StructureRegistration.key(h.getLevel(), start);
        StructureSites.get(h.getLevel().getServer()).forgetForTest(key);
        h.assertTrue(StructureRegistration.register(h.getLevel(), start) == StructureRegistration.Result.REGISTERED, "第一次登记");
        h.assertTrue(StructureRegistration.register(h.getLevel(), start) == StructureRegistration.Result.ALREADY, "第二次不再处理");
        h.assertTrue(calls.get() == 1, "处理一次：" + calls.get());
        // 锚点在没加载的远处：等待
        BlockPos far = new BlockPos(29_000_000, 64, 29_000_000);
        StructureStart farStart = new StructureStart(s, new ChunkPos(far), 0, new PiecesContainer(List.of(
                new StructureInfoPiece(new StructureInfoPiece.Info("test:site", Map.of("guardian", List.of(far))), far))));
        h.assertTrue(StructureRegistration.register(h.getLevel(), farStart) == StructureRegistration.Result.WAITING, "等锚点区块");
        h.assertTrue(calls.get() == 1, "等待时不处理");
        StructureSites.get(h.getLevel().getServer()).forgetForTest(key);
        h.succeed();
    }

    /** TC-T22-30：村庄建筑件抽出公共整地件后，T22 之前的存档（只有 Shape 字段）仍按原来的方式读出。 */
    @GameTest(template = T, batch = BATCH)
    public static void village_piece_reads_old_save(GameTestHelper h) {
        var manager = h.getLevel().getStructureManager();
        VillageBuildingPiece bridge = new VillageBuildingPiece(manager, "mythic_age:village/greek/house_small",
                h.absolutePos(BlockPos.ZERO), Rotation.NONE, 0, false);
        h.assertTrue(bridge.terrain() == TerrainTemplatePiece.Terrain.PILLARS, "桥、码头只补桥墩");
        CompoundTag tag = bridge.createTag(StructurePieceSerializationContext.fromLevel(h.getLevel()));
        tag.remove("Terrain");
        tag.putBoolean("Shape", false);
        h.assertTrue(new VillageBuildingPiece(manager, tag).terrain() == TerrainTemplatePiece.Terrain.PILLARS, "旧存档 Shape=false");
        tag.putBoolean("Shape", true);
        h.assertTrue(new VillageBuildingPiece(manager, tag).terrain() == TerrainTemplatePiece.Terrain.FULL, "旧存档 Shape=true");
        tag.remove("Shape");
        h.assertTrue(new VillageBuildingPiece(manager, tag).terrain() == TerrainTemplatePiece.Terrain.FULL, "更早的存档（没有 Shape）");
        h.succeed();
    }
}
