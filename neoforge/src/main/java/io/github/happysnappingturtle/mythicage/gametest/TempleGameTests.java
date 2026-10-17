package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.temple.AltarSurveyPayload;
import io.github.happysnappingturtle.mythicage.content.greek.temple.BrazierBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.StatueBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleSurvey;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * T8a 神庙判定的集成测试（TC-T8a-60～78，修订 01）。结构模板 17×16×20：地面方块在 y=2（站立高度 3），
 * 神坛在 (8, 3, 2) 朝南（+z），神庙在神坛前方：柱子围成 9×9（x 4–12、z 6–14）。
 * 2 级起的算例建在 1 格高的台基上（x 2–14、z 4–16），7 个规则算例都用真实方块搭出来。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TempleGameTests {
    private static final String BATCH = "temple";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final BlockPos ALTAR = new BlockPos(8, 3, 2);
    private static final int GROUND = 3;

    private TempleGameTests() {
    }

    // ───────────── 搭建 ─────────────

    /**
     * 一座测试神庙的组成；数量与算例 json 的输入一一对应。
     *
     * @param platform 台基高度（0 为贴地）；stairs 为台基正面是否有台阶
     */
    private record Build(int columns, int roof, int braziers, int statues, int pediments, int floor, Age age,
                         int platform, boolean stairs) {
        Build(int columns, int roof, int braziers, int statues, int pediments, int floor, Age age) {
            this(columns, roof, braziers, statues, pediments, floor, age, age.atLeast(Age.CLASSICAL) ? 1 : 0, true);
        }
    }

    private static final int[][] COLUMN_SPOTS = {
            {4, 6}, {12, 6}, {4, 14}, {12, 14}, {8, 6}, {8, 14}, {4, 10}, {12, 10},
            {6, 6}, {10, 6}, {6, 14}, {10, 14}, {4, 8}, {4, 12}};
    private static final int[][] BRAZIER_SPOTS = {{6, 12}, {10, 12}, {6, 8}, {10, 8}};
    private static final int[] STATUE_SPOT = {8, 12};

    private static void ground(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 20; z++) {
                h.setBlock(x, 2, z, Blocks.STONE);
            }
        }
    }

    /** 台基：x 2–14、z 4–16，高 height；stairs 时正面（z=3、2…）中间三格逐级降到地面。 */
    private static void platform(GameTestHelper h, int height, boolean stairs) {
        for (int x = 2; x <= 14; x++) {
            for (int z = 4; z <= 16; z++) {
                for (int y = GROUND; y < GROUND + height; y++) {
                    h.setBlock(x, y, z, Blocks.STONE);
                }
            }
        }
        if (stairs && height > 1) {
            for (int step = 1; step < height; step++) {
                for (int x = 7; x <= 9; x++) {
                    for (int y = GROUND; y < GROUND + height - step; y++) {
                        h.setBlock(x, y, 4 - step, Blocks.STONE);
                    }
                }
            }
        }
    }

    /** 屋顶：柱围 9×9（x 4–12、z 6–14）按行优先铺前 n 格，放在柱顶上方。 */
    private static void roof(GameTestHelper h, int n, int y, BlockState block) {
        int i = 0;
        for (int z = 6; z <= 14 && i < n; z++) {
            for (int x = 4; x <= 12 && i < n; x++, i++) {
                h.setBlock(x, y, z, block);
            }
        }
    }

    private static void columns(GameTestHelper h, int n, int base, Block block) {
        for (int i = 0; i < n; i++) {
            for (int y = base; y < base + 3; y++) {
                h.setBlock(COLUMN_SPOTS[i][0], y, COLUMN_SPOTS[i][1], block);
            }
        }
    }

    private static boolean occupied(int x, int z, Build b) {
        for (int i = 0; i < b.columns(); i++) {
            if (COLUMN_SPOTS[i][0] == x && COLUMN_SPOTS[i][1] == z) {
                return true;
            }
        }
        for (int i = 0; i < b.braziers(); i++) {
            if (BRAZIER_SPOTS[i][0] == x && BRAZIER_SPOTS[i][1] == z) {
                return true;
            }
        }
        return b.statues() > 0 && STATUE_SPOT[0] == x && STATUE_SPOT[1] == z;
    }

    private static void build(GameTestHelper h, Build b) {
        ground(h);
        int base = GROUND + b.platform();
        if (b.platform() > 0) {
            platform(h, b.platform(), b.stairs());
        }
        // 大理石地面：在站立面上按行铺 n 格，避开柱、火盆与神像（贴地时铺在神庙后方的空地）
        int placed = 0;
        int floorY = base - 1;
        if (b.platform() > 0) {
            for (int z = 4; z <= 16 && placed < b.floor(); z++) {
                for (int x = 2; x <= 14 && placed < b.floor(); x++) {
                    if (!occupied(x, z, b)) {
                        h.setBlock(x, floorY, z, GreekMaterials.SMOOTH_MARBLE.get());
                        placed++;
                    }
                }
            }
        } else {
            for (int z = 15; z <= 18 && placed < b.floor(); z++) {
                for (int x = 1; x <= 15 && placed < b.floor(); x++, placed++) {
                    h.setBlock(x, floorY, z, GreekMaterials.SMOOTH_MARBLE.get());
                }
            }
        }
        columns(h, b.columns(), base, GreekTemple.MARBLE_PILLAR.get());
        roof(h, b.roof(), base + 3, GreekMaterials.SMOOTH_MARBLE.get().defaultBlockState());
        BlockState lit = GreekTemple.GREEK_BRAZIER.get().defaultBlockState().setValue(BrazierBlock.LIT, true);
        for (int i = 0; i < b.braziers(); i++) {
            h.setBlock(BRAZIER_SPOTS[i][0], base, BRAZIER_SPOTS[i][1], lit);
        }
        if (b.statues() > 0) {
            statue(h, new BlockPos(STATUE_SPOT[0], base, STATUE_SPOT[1]));
        }
        // 楣石放在柱围矩形之外的高处（只计数，不影响屋顶）
        for (int i = 0; i < b.pediments(); i++) {
            h.setBlock(1 + i, 12, 17, GreekTemple.GREEK_PEDIMENT.get());
        }
        altar(h, b.age());
    }

    private static void statue(GameTestHelper h, BlockPos lower) {
        BlockState s = GreekTemple.GREEK_STATUE.get().defaultBlockState().setValue(StatueBlock.FACING, Direction.NORTH);
        h.setBlock(lower, s.setValue(StatueBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(lower.above(), s.setValue(StatueBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static UUID altar(GameTestHelper h, Age age) {
        h.setBlock(ALTAR, GreekTemple.GREEK_ALTAR.get().defaultBlockState()
                .setValue(io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock.FACING, Direction.SOUTH));
        UUID owner = UUID.randomUUID();
        ShrineService.bindForTest(h.getLevel(), h.absolutePos(ALTAR), owner, "tester", age, Direction.SOUTH);
        return owner;
    }

    private static GreekAltarBlockEntity be(GameTestHelper h) {
        if (h.getLevel().getBlockEntity(h.absolutePos(ALTAR)) instanceof GreekAltarBlockEntity be) {
            return be;
        }
        throw new net.minecraft.gametest.framework.GameTestAssertException("没有神坛方块实体");
    }

    /** 扫描完成后执行断言；未完成时 succeedWhen 下一刻重试。 */
    private static void whenScanned(GameTestHelper h, Consumer<GreekAltarBlockEntity> check) {
        h.succeedWhen(() -> {
            GreekAltarBlockEntity be = be(h);
            h.assertTrue(be.hasResult() && !TempleService.pending(h.getLevel(), h.absolutePos(ALTAR)), "扫描尚未完成");
            check.accept(be);
        });
    }

    private static void expectLevel(GameTestHelper h, Build b, int expected) {
        build(h, b);
        whenScanned(h, be -> {
            int level = be.verdict(TempleService.rules()).grantedLevel();
            h.assertTrue(level == expected, "认定等级应为 " + expected + "，实际 " + level + "，统计 " + be.survey());
            assertRegistryLevel(h, be);
        });
    }

    /** TC-T11b-10：扫描完成后，登记里缓存的是结构等级（不受主人时代封顶）。 */
    private static void assertRegistryLevel(GameTestHelper h, GreekAltarBlockEntity be) {
        int cached = io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry.get(h.getLevel().getServer())
                .level(net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), h.absolutePos(ALTAR)));
        int structural = be.verdict(TempleService.rules()).structuralLevel();
        h.assertTrue(cached == structural, "登记等级应为结构等级 " + structural + "，实际 " + cached);
    }

    // ───────────── 7 个规则算例（TC-T8a-60～66） ─────────────

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void gr_l0(GameTestHelper h) {
        expectLevel(h, new Build(0, 0, 0, 0, 0, 0, Age.ARCHAIC), 0);
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void gr_l1(GameTestHelper h) {
        expectLevel(h, new Build(4, 49, 1, 0, 0, 20, Age.ARCHAIC), 1);
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void gr_l2(GameTestHelper h) {
        expectLevel(h, new Build(8, 60, 2, 1, 2, 40, Age.CLASSICAL), 2);
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void gr_l3(GameTestHelper h) {
        expectLevel(h, new Build(14, 70, 4, 1, 4, 80, Age.HEROIC), 3);
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void gr_bad_roof(GameTestHelper h) {
        expectLevel(h, new Build(4, 48, 1, 0, 0, 20, Age.ARCHAIC), 0);
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void gr_bad_fire(GameTestHelper h) {
        expectLevel(h, new Build(8, 70, 0, 1, 2, 50, Age.CLASSICAL), 0);
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void gr_age_cap(GameTestHelper h) {
        build(h, new Build(14, 70, 4, 1, 4, 80, Age.ARCHAIC, 1, true));
        whenScanned(h, be -> {
            var v = be.verdict(TempleService.rules());
            h.assertTrue(v.structuralLevel() == 3 && v.grantedLevel() == 1,
                    "结构 3 级、认定 1 级，实际 " + v.structuralLevel() + "/" + v.grantedLevel() + " " + be.survey());
            h.assertTrue(v.ageRequired().orElse(null) == Age.CLASSICAL, "应提示需要古典时代");
            assertRegistryLevel(h, be);
        });
    }

    /** 算例的完整统计核对：GR-L3 的六项计数与搭建一致。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void survey_counts_match_build(GameTestHelper h) {
        build(h, new Build(14, 70, 4, 1, 4, 80, Age.HEROIC));
        whenScanned(h, be -> h.assertTrue(be.survey().equals(new TempleSurvey(14, 14, 70, 81, true, true, 4, 1, 4, 80)),
                "统计应为 14 柱（均在台基上）/70 of 81/露天/可达/4/1/4/80，实际 " + be.survey()));
    }

    // ───────────── 口径（TC-T8a-67～71、修订 01） ─────────────

    /** 贴地小神庙：4 柱、49 格屋顶、1 个点燃的圣火盆；可替换柱子与屋顶方块。 */
    private static void naiskos(GameTestHelper h, Block column, BlockState roofBlock, int roofCells, boolean lit) {
        ground(h);
        columns(h, 4, GROUND, column);
        roof(h, roofCells, GROUND + 3, roofBlock);
        h.setBlock(BRAZIER_SPOTS[0][0], GROUND, BRAZIER_SPOTS[0][1],
                GreekTemple.GREEK_BRAZIER.get().defaultBlockState().setValue(BrazierBlock.LIT, lit));
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void quartz_pillars_count(GameTestHelper h) {
        naiskos(h, Blocks.QUARTZ_PILLAR, GreekMaterials.SMOOTH_MARBLE.get().defaultBlockState(), 49, true);
        altar(h, Age.ARCHAIC);
        whenScanned(h, be -> h.assertTrue(be.survey().columns() == 4
                && be.verdict(TempleService.rules()).grantedLevel() == 1, "石英柱应计数：" + be.survey()));
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void glass_roof_not_cover(GameTestHelper h) {
        naiskos(h, GreekTemple.MARBLE_PILLAR.get(), Blocks.GLASS.defaultBlockState(), 81, true);
        altar(h, Age.ARCHAIC);
        whenScanned(h, be -> h.assertTrue(be.survey().roofCovered() == 0, "玻璃不算遮盖：" + be.survey()));
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void slab_roof_covers(GameTestHelper h) {
        naiskos(h, GreekTemple.MARBLE_PILLAR.get(), Blocks.AIR.defaultBlockState(), 0, true);
        BlockState slab = GreekMaterials.SMOOTH_MARBLE_SLAB.get().defaultBlockState();
        int i = 0;
        for (int z = 6; z <= 14; z++) {
            for (int x = 4; x <= 12; x++, i++) {
                h.setBlock(x, GROUND + 3, z, slab.setValue(SlabBlock.TYPE, i % 2 == 0 ? SlabType.TOP : SlabType.BOTTOM));
            }
        }
        altar(h, Age.ARCHAIC);
        whenScanned(h, be -> h.assertTrue(be.survey().roofCovered() == 81, "上下半台阶都算遮盖：" + be.survey()));
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void brazier_lighting_changes_level(GameTestHelper h) {
        naiskos(h, GreekTemple.MARBLE_PILLAR.get(), GreekMaterials.SMOOTH_MARBLE.get().defaultBlockState(), 49, false);
        BlockPos brazier = new BlockPos(BRAZIER_SPOTS[0][0], GROUND, BRAZIER_SPOTS[0][1]);
        altar(h, Age.ARCHAIC);
        FakePlayer player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_lighter"));
        boolean[] lit = {false};
        h.succeedWhen(() -> {
            GreekAltarBlockEntity be = be(h);
            h.assertTrue(be.hasResult() && !TempleService.pending(h.getLevel(), h.absolutePos(ALTAR)), "扫描尚未完成");
            int level = be.verdict(TempleService.rules()).grantedLevel();
            if (!lit[0]) {
                h.assertTrue(level == 0, "熄灭时应为 0 级，实际 " + level);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
                BlockPos abs = h.absolutePos(brazier);
                BlockState state = h.getLevel().getBlockState(abs);
                state.useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
                h.assertTrue(h.getLevel().getBlockState(abs).getValue(BrazierBlock.LIT), "打火石应点燃圣火盆");
                lit[0] = true;
                throw new net.minecraft.gametest.framework.GameTestAssertException("等待重扫");
            }
            h.assertTrue(level == 1, "点燃后应为 1 级，实际 " + level + " " + be.survey());
            // TC-T11b-11：重扫后登记跟着变
            assertRegistryLevel(h, be);
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void statue_counts_once(GameTestHelper h) {
        ground(h);
        statue(h, new BlockPos(8, GROUND, 12));
        altar(h, Age.ARCHAIC);
        whenScanned(h, be -> h.assertTrue(be.survey().statues() == 1, "两格高的神像只计 1：" + be.survey()));
    }

    /** 修订 01：神坛头顶有遮挡 → 不认定神庙（祭坛应露天）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void covered_altar_falls_to_zero(GameTestHelper h) {
        naiskos(h, GreekTemple.MARBLE_PILLAR.get(), GreekMaterials.SMOOTH_MARBLE.get().defaultBlockState(), 49, true);
        h.setBlock(ALTAR.above(4), GreekMaterials.SMOOTH_MARBLE.get());
        altar(h, Age.ARCHAIC);
        whenScanned(h, be -> {
            var v = be.verdict(TempleService.rules());
            h.assertTrue(v.grantedLevel() == 0 && !be.survey().openAltar(), "祭坛被遮应为 0 级：" + be.survey());
            h.assertTrue(v.missing().contains(new io.github.happysnappingturtle.mythicage.core.greek.temple.TempleVerdict
                    .Missing("openAltar", 0, 1)), "应提示祭坛应露天");
        });
    }

    /** 修订 01：中神庙的其余条件都满足，但柱子贴地 → 只到小神庙。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void classical_without_crepidoma(GameTestHelper h) {
        expectLevel(h, new Build(8, 60, 2, 1, 2, 40, Age.CLASSICAL, 0, false), 1);
    }

    /** 修订 01：3 格高的台基没有台阶 → 走不上去，只到小神庙；有台阶 → 中神庙。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void tall_platform_needs_stairs(GameTestHelper h) {
        expectLevel(h, new Build(8, 60, 2, 1, 2, 40, Age.CLASSICAL, 3, false), 1);
    }

    /** 修订 02：2 格高台基，四周用半砖逐级（地面 → 半砖 → 整格 + 半砖 → 柱座），不设正门台阶也能认定中神庙。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void half_slab_steps_all_round(GameTestHelper h) {
        Build b = new Build(8, 60, 2, 1, 2, 40, Age.CLASSICAL, 2, false);
        build(h, b);
        BlockState slab = GreekMaterials.SMOOTH_MARBLE_SLAB.get().defaultBlockState();
        // 台基 x 2–14、z 4–16：把最外一圈改成「整格 + 半砖」，再在外面加一圈贴地半砖
        for (int x = 1; x <= 15; x++) {
            for (int z = 3; z <= 17; z++) {
                boolean edge = x == 2 || x == 14 || z == 4 || z == 16;
                boolean outside = x == 1 || x == 15 || z == 3 || z == 17;
                if (edge && x >= 2 && x <= 14 && z >= 4 && z <= 16) {
                    h.setBlock(x, GROUND + 1, z, slab);
                } else if (outside && !(x == 8 && z <= 3)) {
                    h.setBlock(x, GROUND, z, slab);
                }
            }
        }
        whenScanned(h, be -> {
            int level = be.verdict(TempleService.rules()).grantedLevel();
            h.assertTrue(be.survey().platformReachable() && level == 2,
                    "半砖逐级应可走上台基并认定中神庙，实际 " + level + " " + be.survey());
        });
    }

    /** 修订 02 §5：神像在神坛前方 28 格（长内殿后墙处）仍在扫描体内，计入后认定中神庙。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void statue_at_back_of_long_cella(GameTestHelper h) {
        statueAtDepth(h, 28, 1, 2);
    }

    /** 修订 02 §5：扫描体前沿是神坛前方 32 格；33 格处的神像不计，停在小神庙。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void statue_beyond_scan_front_not_counted(GameTestHelper h) {
        statueAtDepth(h, 33, 0, 1);
    }

    private static void statueAtDepth(GameTestHelper h, int depth, int statues, int level) {
        build(h, new Build(8, 60, 2, 0, 2, 40, Age.CLASSICAL));
        statue(h, new BlockPos(8, GROUND + 1, ALTAR.getZ() + depth));
        whenScanned(h, be -> {
            int got = be.verdict(TempleService.rules()).grantedLevel();
            h.assertTrue(be.survey().statues() == statues && got == level,
                    "前方 " + depth + " 格的神像：应计 " + statues + " 尊、认定 " + level + " 级，实际 " + got + " " + be.survey());
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void tall_platform_with_stairs(GameTestHelper h) {
        expectLevel(h, new Build(8, 60, 2, 1, 2, 40, Age.CLASSICAL, 3, true), 2);
    }

    // ───────────── 放置与保护（TC-T8a-72～75） ─────────────

    private static FakePlayer player(GameTestHelper h, String name, Age age) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        MythData.get(p).civ(Civilization.GREEK).setAge(age);
        return p;
    }

    /** 让玩家对着 below 的顶面放一座神坛，返回是否放下。 */
    private static boolean place(GameTestHelper h, FakePlayer p, BlockPos below) {
        ItemStack stack = new ItemStack(GreekTemple.GREEK_ALTAR.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(below);
        stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        return h.getLevel().getBlockState(abs.above()).is(GreekTemple.GREEK_ALTAR.get());
    }

    @GameTest(template = T, batch = "temple_place", timeoutTicks = 100)
    public static void place_limit_and_spacing(GameTestHelper h) {
        ground(h);
        // FakePlayer 偏航 0：朝南放置，神坛朝南
        FakePlayer archaic = player(h, "ma_archaic", Age.ARCHAIC);
        h.assertTrue(place(h, archaic, new BlockPos(1, 2, 1)), "远古第 1 座应能放下");
        h.assertTrue(!place(h, archaic, new BlockPos(16, 2, 19)), "远古第 2 座应被拒绝");
        FakePlayer mythic = player(h, "ma_mythic", Age.MYTHIC);
        h.assertTrue(!place(h, mythic, new BlockPos(11, 2, 1)), "距离 10 格应被拒绝");
        h.assertTrue(!place(h, mythic, new BlockPos(1, 2, 16)), "距离 15 格但扫描体相交应被拒绝");
        h.assertTrue(place(h, mythic, new BlockPos(16, 2, 1)), "距离 15 格且扫描体不相交应能放下");
        FakePlayer none = player(h, "ma_none", Age.NONE);
        h.assertTrue(!place(h, none, new BlockPos(8, 2, 19)), "未接触希腊不能放");
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(1, 3, 1)))
                .getValue(io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock.FACING) == Direction.SOUTH,
                "神坛朝向应取放置者的朝向");
        h.succeed();
    }

    @GameTest(template = T, batch = "temple_place", timeoutTicks = 100)
    public static void ownerless_altar_removed(GameTestHelper h) {
        ground(h);
        h.setBlock(ALTAR, GreekTemple.GREEK_ALTAR.get());
        h.runAfterDelay(5, () -> {
            h.assertBlockNotPresent(GreekTemple.GREEK_ALTAR.get(), ALTAR);
            h.succeed();
        });
    }

    @GameTest(template = T, batch = "temple_place", timeoutTicks = 100)
    public static void only_owner_breaks(GameTestHelper h) {
        ground(h);
        FakePlayer owner = player(h, "ma_owner", Age.ARCHAIC);
        h.assertTrue(place(h, owner, ALTAR.below()), "主人应能放下");
        FakePlayer other = player(h, "ma_other", Age.ARCHAIC);
        BlockPos abs = h.absolutePos(ALTAR);
        h.assertTrue(!other.gameMode.destroyBlock(abs), "非主人不能挖");
        h.assertBlockPresent(GreekTemple.GREEK_ALTAR.get(), ALTAR);
        h.assertTrue(owner.gameMode.destroyBlock(abs), "主人可以挖");
        h.assertBlockNotPresent(GreekTemple.GREEK_ALTAR.get(), ALTAR);
        h.assertTrue(ShrineRegistry.get(h.getLevel().getServer()).get(GlobalPos.of(h.getLevel().dimension(), abs)).isEmpty(),
                "挖掉后应从登记中移除");
        h.succeed();
    }

    // ───────────── 预算、存档、检视（TC-T8a-76～78） ─────────────

    private static void setBudget(ConfigHolder<WorldRulesConfig> holder, WorldRulesConfig base, int budget) {
        WorldRulesConfig.TempleBudget t = base.greek().temple();
        WorldRulesConfig next = base.withGreek(base.greek().withTemple(
                new WorldRulesConfig.TempleBudget(budget, t.rescanIntervalTicks())));
        holder.reload(ConfigLoader.toPrettyJson(holder.schema(), next));
    }

    @GameTest(template = T, batch = "temple_budget", timeoutTicks = 100)
    public static void scan_respects_budget(GameTestHelper h) {
        ConfigHolder<WorldRulesConfig> holder = ModConfigs.worldRules();
        WorldRulesConfig original = holder.current();
        setBudget(holder, original, 512);
        ground(h);
        altar(h, Age.ARCHAIC);
        int[] ticks = {0};
        int[] maxCells = {0};
        h.onEachTick(() -> {
            if (TempleService.pending(h.getLevel(), h.absolutePos(ALTAR))) {
                ticks[0]++;
            }
            maxCells[0] = Math.max(maxCells[0], TempleService.cellsLastTick());
        });
        h.succeedWhen(() -> {
            h.assertTrue(be(h).hasResult(), "扫描尚未完成");
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(), original));
            h.assertTrue(ticks[0] >= 7, "4050 格、每刻 512 格应至少跨 8 刻，实际排队 " + ticks[0] + " 刻");
            h.assertTrue(maxCells[0] <= 512, "每刻不应超过 512 格，实际 " + maxCells[0]);
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void save_load_keeps_state(GameTestHelper h) {
        build(h, new Build(4, 49, 1, 0, 0, 20, Age.ARCHAIC));
        whenScanned(h, be -> {
            CompoundTag tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
            GreekAltarBlockEntity copy = new GreekAltarBlockEntity(be.getBlockPos(), be.getBlockState());
            copy.loadWithComponents(tag, h.getLevel().registryAccess());
            h.assertTrue(copy.owner().equals(be.owner()) && copy.ownerAge() == Age.ARCHAIC, "主人与时代应保留");
            h.assertTrue(copy.survey().equals(be.survey()), "统计应保留");
            h.assertTrue(copy.prayerSlots(TempleService.rules()).equals(be.prayerSlots(TempleService.rules())),
                    "祈祷位应保留");
            h.assertTrue(copy.prayerSlots(TempleService.rules()).size() == 4, "小神庙应有 4 个祈祷位，实际 "
                    + copy.prayerSlots(TempleService.rules()).size());
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void survey_payload_roundtrip(GameTestHelper h) {
        build(h, new Build(4, 48, 1, 0, 0, 20, Age.ARCHAIC));
        FakePlayer viewer = player(h, "ma_viewer", Age.ARCHAIC);
        whenScanned(h, be -> {
            AltarSurveyPayload p = AltarSurveyPayload.of(be, viewer, TempleService.rules());
            h.assertTrue(p.grantedLevel() == 0 && p.rows().stream()
                    .anyMatch(r -> r.key().equals("roof") && r.have() == 48 && r.need() == 49), "应列出屋顶 48/49");
            h.assertTrue(p.uncoveredRoof().size() == 33, "无屋顶的列应为 33，实际 " + p.uncoveredRoof().size());
            h.assertTrue(p.shortColumns().size() == 4 && p.countedColumns().isEmpty(), "贴地的 4 根柱应标黄");
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            AltarSurveyPayload.STREAM_CODEC.encode(buf, p);
            AltarSurveyPayload back = AltarSurveyPayload.STREAM_CODEC.decode(buf);
            h.assertTrue(back.equals(p), "编解码应往返一致");
        });
    }
}
