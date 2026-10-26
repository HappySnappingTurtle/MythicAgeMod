package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.conduct.ConductService;
import io.github.happysnappingturtle.mythicage.content.greek.ruin.GreekRuins;
import io.github.happysnappingturtle.mythicage.content.greek.ruin.RuinSentinelEntity;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.structure.RuinStructure;
import io.github.happysnappingturtle.mythicage.structure.RuinVaultBlockEntity;
import io.github.happysnappingturtle.mythicage.structure.StructureInfoPiece;
import io.github.happysnappingturtle.mythicage.structure.StructureRegistration;
import io.github.happysnappingturtle.mythicage.structure.StructureSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T22 阶段 ②：废弃神殿（单元测试用例文档 TC-T22-45～55）。神殿摆在远处的槽位（x = 40000 + n·128，地面高 120 的石台上），
 * 不走选址（选址规则在 core 的 SiteRules 测）；哨兵的射击用场地内的哨兵与被驯服的狼（假玩家不在世界的实体表里，哨兵找不到它）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AbandonedTempleGameTests {
    private static final String BATCH = "abandoned_temple";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final AtomicInteger SLOT = new AtomicInteger();
    private static final int FLOOR = 120;

    private AbandonedTempleGameTests() {
    }

    // ───────────── 搭建 ─────────────

    static Structure structure(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(
                ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "abandoned_temple")));
    }

    /** 在新槽位铺一块石台（地面上第一层 = FLOOR），摆一座神殿并放进各区块；返回结构起点。 */
    static StructureStart place(GameTestHelper h, Turn turn, boolean pit) {
        ServerLevel level = h.getLevel();
        int minX = 40000 + SLOT.getAndIncrement() * 128;
        int minZ = 40000;
        for (int cx = (minX - 8) >> 4; cx <= (minX + 40) >> 4; cx++) {
            for (int cz = (minZ - 8) >> 4; cz <= (minZ + 40) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(minX - 6, FLOOR - 1, minZ - 6, minX + 38, FLOOR - 1, minZ + 38)) {
            level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
        }
        for (BlockPos p : BlockPos.betweenClosed(minX - 6, FLOOR, minZ - 6, minX + 38, FLOOR + 16, minZ + 38)) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        if (pit) {
            // 占地中间挖一个 3 格深的坑：整地应补到实地，台基下不悬空
            for (BlockPos p : BlockPos.betweenClosed(minX + 8, FLOOR - 4, minZ + 8, minX + 12, FLOOR - 1, minZ + 12)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        StructureStart start = RuinStructure.startForTest(level, structure(level), turn, minX, FLOOR, minZ);
        BoundingBox bb = start.getBoundingBox();
        ChunkPos a = new ChunkPos(SectionPos.blockToSectionCoord(bb.minX()), SectionPos.blockToSectionCoord(bb.minZ()));
        ChunkPos b = new ChunkPos(SectionPos.blockToSectionCoord(bb.maxX()), SectionPos.blockToSectionCoord(bb.maxZ()));
        ChunkPos.rangeClosed(a, b).forEach(c -> start.placeInChunk(level, level.structureManager(),
                level.getChunkSource().getGenerator(), level.getRandom(), new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(),
                        c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxBuildHeight(), c.getMaxBlockZ()), c));
        return start;
    }

    static StructureInfoPiece.Info info(StructureStart start) {
        return StructureRegistration.info(start).orElseThrow(() -> new GameTestAssertException("缺元数据件"));
    }

    static List<RuinSentinelEntity> sentinels(ServerLevel level, StructureStart start) {
        BoundingBox bb = start.getBoundingBox();
        return level.getEntitiesOfClass(RuinSentinelEntity.class, new AABB(bb.minX(), bb.minY(), bb.minZ(), bb.maxX() + 1,
                bb.maxY() + 1, bb.maxZ() + 1));
    }

    static void cleanup(ServerLevel level, StructureStart start) {
        sentinels(level, start).forEach(e -> e.discard());
        StructureSites.get(level.getServer()).forgetForTest(StructureRegistration.key(level, start));
    }

    /** 能站人：脚、头两格没有碰撞，脚下有。 */
    static boolean standable(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty();
    }

    /** 从 from 走（一次最多上下 1 格）能不能走到 to 旁边（水平相邻或就是 to）。 */
    static boolean reachable(ServerLevel level, BlockPos from, BlockPos to) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty() && seen.size() < 20000) {
            BlockPos p = queue.poll();
            if (Math.abs(p.getY() - to.getY()) <= 1 && Math.abs(p.getX() - to.getX()) + Math.abs(p.getZ() - to.getZ()) <= 1) {
                return true;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos n = p.relative(d).above(dy);
                    if (dy == 1 && !level.getBlockState(p.above(2)).getCollisionShape(level, p.above(2)).isEmpty()) {
                        continue;
                    }
                    if (seen.add(n) && standable(level, n)) {
                        queue.add(n);
                    }
                }
            }
        }
        return false;
    }

    // ───────────── 用例 ─────────────

    /** TC-T22-45、47：摆好一座：模板、宝库与箱子、残缺神像都在；登记后 3 尊哨兵在基座上、宝库与箱子设好表。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void temple_places_and_registers(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        StructureStart start = place(h, Turn.R0, false);
        StructureInfoPiece.Info info = info(start);
        h.assertTrue(info.kind().equals(GreekRuins.TEMPLE_KIND), "种类：" + info.kind());
        h.assertTrue(info.anchors("guardian").size() == 3 && info.anchors("loot").size() == 2 && info.anchors("vault").size() == 1,
                "锚点：" + info.anchors().keySet());
        BlockPos vault = info.anchors("vault").get(0);
        h.assertTrue(level.getBlockEntity(vault) instanceof RuinVaultBlockEntity, "宝库方块在锚点上：" + level.getBlockState(vault));
        h.assertTrue(level.getBlockState(info.anchors("statue").get(0)).is(GreekRuins.BROKEN_STATUE.get()), "残缺神像");
        h.assertTrue(StructureRegistration.register(level, start) == StructureRegistration.Result.REGISTERED, "登记");
        List<RuinSentinelEntity> s = sentinels(level, start);
        h.assertTrue(s.size() == 3, "3 尊哨兵：" + s.size());
        for (BlockPos g : info.anchors("guardian")) {
            h.assertTrue(s.stream().anyMatch(e -> e.blockPosition().equals(g)), "哨兵在基座上：" + g);
        }
        h.assertTrue(((RuinVaultBlockEntity) level.getBlockEntity(vault)).table().equals(GreekRuins.VAULT_TABLE), "宝库表");
        for (BlockPos l : info.anchors("loot")) {
            h.assertTrue(level.getBlockEntity(l) instanceof ChestBlockEntity c && c.getLootTable() != null, "箱子战利品表：" + l);
        }
        cleanup(level, start);
        h.succeed();
    }

    /** TC-T22-46：四个旋转都能从前庭入口走到宝库与侧门（倒塌物不封门）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void temple_reachable_all_rotations(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (Turn t : Turn.values()) {
            StructureStart start = place(h, t, false);
            StructureInfoPiece.Info info = info(start);
            BlockPos entrance = info.anchors("entrance").get(0);
            h.assertTrue(standable(level, entrance), t + " 入口站得住：" + entrance);
            h.assertTrue(reachable(level, entrance, info.anchors("vault").get(0)), t + " 走得到宝库");
            h.assertTrue(reachable(level, entrance, info.anchors("exit").get(0)), t + " 走得到侧门");
            cleanup(level, start);
        }
        h.succeed();
    }

    /** TC-T22-54：占地下有坑：整地补到实地，台基下不悬空。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void temple_fills_ground(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        StructureStart start = place(h, Turn.R0, true);
        BoundingBox bb = start.getBoundingBox();
        int minX = bb.minX() + 2;
        int minZ = bb.minZ() + 2;
        for (int x = minX + 8; x <= minX + 12; x++) {
            for (int z = minZ + 8; z <= minZ + 12; z++) {
                for (int y = FLOOR - 3; y <= FLOOR - 1; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    h.assertTrue(!level.getBlockState(p).isAir(), "坑被补上：" + p);
                }
            }
        }
        cleanup(level, start);
        h.succeed();
    }

    /** TC-T22-51：打碎一尊，再登记一次：不重生。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void sentinel_no_respawn(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        StructureStart start = place(h, Turn.R90, false);
        StructureRegistration.register(level, start);
        sentinels(level, start).get(0).discard();
        h.assertTrue(StructureRegistration.register(level, start) == StructureRegistration.Result.ALREADY, "不再处理");
        h.assertTrue(sentinels(level, start).size() == 2, "还是 2 尊：" + sentinels(level, start).size());
        cleanup(level, start);
        h.succeed();
    }

    // ───────────── 哨兵（场地内） ─────────────

    static RuinSentinelEntity sentinel(GameTestHelper h, int x, int z) {
        RuinSentinelEntity s = GreekRuins.RUIN_SENTINEL.get().create(h.getLevel());
        s.moveTo(h.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5)));
        s.standing();
        h.getLevel().addFreshEntity(s);
        return s;
    }

    static Wolf pet(GameTestHelper h, FakePlayer owner, int x, int z) {
        Wolf w = EntityType.WOLF.create(h.getLevel());
        w.moveTo(h.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5)));
        w.setNoAi(true);
        w.setOwnerUUID(owner.getUUID());
        w.setTame(true, false);
        h.getLevel().addFreshEntity(w);
        return w;
    }

    static boolean arrowFrom(GameTestHelper h, RuinSentinelEntity s) {
        return !h.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(s.blockPosition()).inflate(20),
                a -> a.getOwner() == s).isEmpty();
    }

    /** TC-T22-48：14 格内看得见的玩家宠物：2 秒内射出一箭。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void sentinel_shoots_visible(GameTestHelper h) {
        FakePlayer a = QuestGameTests.player(h, "s48", 8, 30);
        RuinSentinelEntity s = sentinel(h, 8, 4);
        Wolf w = pet(h, a, 8, 14);
        h.succeedWhen(() -> {
            h.assertTrue(arrowFrom(h, s), "射了");
            s.discard();
            w.discard();
            OnlinePlayers.forTest(a, false);
        });
    }

    /** TC-T22-49：中间隔一面石墙：不射。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void sentinel_blocked_by_wall(GameTestHelper h) {
        FakePlayer a = QuestGameTests.player(h, "s49", 8, 30);
        RuinSentinelEntity s = sentinel(h, 8, 4);
        Wolf w = pet(h, a, 8, 14);
        for (int x = 5; x <= 11; x++) {
            for (int y = 1; y <= 4; y++) {
                h.setBlock(new BlockPos(x, y, 9), Blocks.STONE);
            }
        }
        h.runAtTickTime(80, () -> {
            h.assertTrue(!arrowFrom(h, s), "隔墙不射");
            s.discard();
            w.discard();
            OnlinePlayers.forTest(a, false);
            h.succeed();
        });
    }

    /** TC-T22-50、52：创造模式、旁观的玩家不打；打碎哨兵不改善恶。 */
    @GameTest(template = T, batch = BATCH)
    public static void sentinel_targets_and_conduct(GameTestHelper h) {
        FakePlayer a = QuestGameTests.player(h, "s50", 8, 10);
        RuinSentinelEntity s = sentinel(h, 8, 4);
        h.assertTrue(s.shouldShootForTest(h.getLevel(), a), "生存玩家要打");
        a.setGameMode(GameType.CREATIVE);
        h.assertTrue(!s.shouldShootForTest(h.getLevel(), a), "创造不打");
        a.setGameMode(GameType.SPECTATOR);
        h.assertTrue(!s.shouldShootForTest(h.getLevel(), a), "旁观不打");
        a.setGameMode(GameType.SURVIVAL);
        int before = ConductService.score(a);
        s.hurt(a.damageSources().playerAttack(a), 1000);
        h.assertTrue(!s.isAlive(), "打碎了");
        h.assertTrue(ConductService.score(a) == before, "善恶不变");
        OnlinePlayers.forTest(a, false);
        h.succeed();
    }

    /** TC-T22-55：结构集：40 / 12，离村庄结构集 8 区块内不放（数据在结构集 JSON 里）。 */
    @GameTest(template = T, batch = BATCH)
    public static void temple_structure_set(GameTestHelper h) {
        var set = h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "abandoned_temples"));
        h.assertTrue(set != null && set.placement() instanceof RandomSpreadStructurePlacement, "结构集");
        RandomSpreadStructurePlacement p = (RandomSpreadStructurePlacement) set.placement();
        h.assertTrue(p.spacing() == 40 && p.separation() == 12, "40 / 12：" + p.spacing() + "/" + p.separation());
        h.succeed();
    }
}
