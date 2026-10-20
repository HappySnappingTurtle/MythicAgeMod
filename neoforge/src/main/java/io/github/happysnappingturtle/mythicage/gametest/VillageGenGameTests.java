package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverEvents;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianRegistry;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillagePopulation;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageResolver;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import io.github.happysnappingturtle.mythicage.village.VillageSpace;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.gen.VillageRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T13b 阶段 ① 文明村庄生成与登记（TC-T13b-40～50）。村庄放在远离 GameTest 网格的地方（x = 20000 起，每个用例错开 512 格），
 * 用与 {@code /place structure} 相同的代码路径生成；用例结束清掉登记、规划与实体。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageGenGameTests {
    private static final String BATCH = "village_gen";
    private static final String T = GameTestTemplates.EMPTY_3X3;
    private static final AtomicInteger SLOT = new AtomicInteger();

    private VillageGenGameTests() {
    }

    // ───────────── 搭建 ─────────────

    private static Structure structure(ServerLevel level, String id) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, id)));
    }

    private static StructureStart generate(ServerLevel level, Structure s, int x, int z) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        return s.generate(level.registryAccess(), gen, gen.getBiomeSource(), level.getChunkSource().randomState(),
                level.getStructureManager(), level.getSeed(), new ChunkPos(x >> 4, z >> 4), 0, level, b -> true);
    }

    /** 生成并放置一座希腊村庄；返回结构起点。 */
    private static StructureStart place(ServerLevel level, int x, int z) {
        StructureStart start = generate(level, structure(level, "greek_village"), x, z);
        if (!start.isValid()) {
            throw new GameTestAssertException("希腊村庄生成失败");
        }
        BoundingBox bb = start.getBoundingBox();
        ChunkPos a = new ChunkPos(SectionPos.blockToSectionCoord(bb.minX()), SectionPos.blockToSectionCoord(bb.minZ()));
        ChunkPos b = new ChunkPos(SectionPos.blockToSectionCoord(bb.maxX()), SectionPos.blockToSectionCoord(bb.maxZ()));
        ChunkPos.rangeClosed(a, b).forEach(c -> {
            level.getChunk(c.x, c.z);
            start.placeInChunk(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(),
                    new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(),
                            level.getMaxBuildHeight(), c.getMaxBlockZ()), c);
        });
        return start;
    }

    private record Placed(StructureStart start, VillageRegistry.Village village, VillagePlans.Plan plan) {
    }

    private static Placed placeAndRegister(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int n = SLOT.getAndIncrement();
        StructureStart start = place(level, 20000 + n * 512, 20000);
        VillageRegistry.Village v = VillageRegistration.register(level, start)
                .orElseThrow(() -> new GameTestAssertException("登记失败"));
        VillagePlans.Plan plan = VillagePlans.get(level.getServer()).byVillage(v.id())
                .orElseThrow(() -> new GameTestAssertException("缺规划数据"));
        return new Placed(start, v, plan);
    }

    private static void cleanup(GameTestHelper h, Placed p) {
        ServerLevel level = h.getLevel();
        BoundingBox bb = p.start().getBoundingBox();
        AABB box = new AABB(bb.minX() - 16, bb.minY() - 16, bb.minZ() - 16, bb.maxX() + 16, bb.maxY() + 16, bb.maxZ() + 16);
        GuardianRegistry gr = GuardianRegistry.get(level.getServer());
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> e instanceof Villager || e instanceof GuardianEntity)) {
            if (e instanceof GuardianEntity g && g.guardianId() != null) {
                gr.remove(g.guardianId());
            }
            e.discard();
        }
        VillagePlans.get(level.getServer()).remove(p.village().id());
        VillageRegistry.get(level.getServer()).remove(p.village().bell());
    }

    private static BlockPos block(Pos p) {
        return VillageInfoPiece.block(p);
    }

    // ───────────── 3.1 阶段 ① ─────────────

    /** TC-T13b-40、42、43：登记、锚点、祭司、过渡期居民与信徒身份。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void generated_village_registers_and_populates(GameTestHelper h) {
        Placed p = placeAndRegister(h);
        try {
            VillageRegistry.Village v = p.village();
            h.assertTrue(v.origin() == VillageRegistry.Origin.GENERATED, "来源 GENERATED");
            h.assertTrue(v.civ().equals("greek"), "文明希腊：" + v.civ());
            h.assertTrue(!v.patron().isEmpty() && v.patron().equals(p.plan().info().patron()), "主神与信息片段一致：" + v.patron());
            h.assertTrue(v.stage() == Age.ARCHAIC, "阶段远古");
            h.assertTrue(h.getLevel().getBlockState(v.bell().pos()).is(net.minecraft.world.level.block.Blocks.BELL),
                    "钟坐标上是钟");
            List<Pos> homes = p.plan().anchors("home");
            h.assertTrue(homes.size() == 8, "8 个床位锚点：" + homes.size());
            VillageRegistration.populateDue(h.getLevel());
            h.assertTrue(VillagePlans.get(h.getLevel().getServer()).byVillage(v.id()).orElseThrow().populated(), "已生成居民");
            BlockPos g = block(p.plan().anchors("guardian").get(0));
            List<GuardianEntity> guardians = h.getLevel().getEntitiesOfClass(GuardianEntity.class, new AABB(g).inflate(2));
            h.assertTrue(guardians.size() == 1, "祭司在神庙锚点 2 格内：" + guardians.size());
            int near = 0;
            for (Pos home : homes) {
                List<Villager> vs = h.getLevel().getEntitiesOfClass(Villager.class, new AABB(block(home)).inflate(1));
                near += vs.isEmpty() ? 0 : 1;
                for (Villager villager : vs) {
                    h.assertTrue(VillageResolver.villageOf(villager).map(x -> x.id().equals(v.id())).orElse(false),
                            "村民归属本村");
                    h.assertTrue(BelieverEvents.isBeliever(villager), "希腊村庄的村民是信徒");
                }
            }
            h.assertTrue(near == homes.size(), "每个床位一名村民：" + near);
            VillageRegistration.populateDue(h.getLevel());
            h.assertTrue(h.getLevel().getEntitiesOfClass(GuardianEntity.class, new AABB(g).inflate(2)).size() == 1,
                    "不重复生成");
        } finally {
            cleanup(h, p);
        }
        h.succeed();
    }

    /** TC-T13b-41：区块载入路径；重复载入不重复登记。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void chunk_load_registers_once(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int n = SLOT.getAndIncrement();
        StructureStart start = place(level, 20000 + n * 512, 20000);
        LevelChunk chunk = level.getChunk(start.getChunkPos().x, start.getChunkPos().z);
        chunk.setStartForStructure(start.getStructure(), start);
        int before = VillageRegistry.get(level.getServer()).all().size();
        for (int i = 0; i < 3; i++) {
            VillageRegistration.scan(level, chunk);
            VillageRegistration.drain(level.getServer());
        }
        List<VillageRegistry.Village> after = VillageRegistry.get(level.getServer()).all();
        h.assertTrue(after.size() == before + 1, "只登记一次：" + (after.size() - before));
        VillageRegistry.Village v = after.get(after.size() - 1);
        cleanup(h, new Placed(start, v, VillagePlans.get(level.getServer()).byVillage(v.id()).orElseThrow()));
        h.succeed();
    }

    /** TC-T13b-44、45：原版钟扫描不重复登记；兜底按结构片段登记。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void bell_scan_and_fallback(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int n = SLOT.getAndIncrement();
        StructureStart start = place(level, 20000 + n * 512, 20000);
        // 兜底需要结构引用：把起点与引用写进区块（自然生成时由世界生成完成）
        LevelChunk chunk = level.getChunk(start.getChunkPos().x, start.getChunkPos().z);
        chunk.setStartForStructure(start.getStructure(), start);
        chunk.addReferenceForStructure(start.getStructure(), start.getChunkPos().toLong());
        VillageInfoPiece.Info info = start.getPieces().stream().filter(x -> x instanceof VillageInfoPiece)
                .map(x -> ((VillageInfoPiece) x).info()).findFirst().orElseThrow();
        BlockPos bell = block(info.center());
        h.assertTrue(bell.getX() >> 4 == start.getChunkPos().x && bell.getZ() >> 4 == start.getChunkPos().z,
                "钟在起点区块里");
        Optional<VillageRegistry.Village> v = VillageService.register(level, bell);
        h.assertTrue(v.isPresent() && v.get().origin() == VillageRegistry.Origin.GENERATED, "兜底登记为生成村庄");
        int count = VillageRegistry.get(level.getServer()).all().size();
        VillageService.registerNear(level, bell);
        VillageRegistration.registerAt(level, bell);
        h.assertTrue(VillageRegistry.get(level.getServer()).all().size() == count, "钟扫描与兜底不重复登记");
        cleanup(h, new Placed(start, v.get(), VillagePlans.get(level.getServer()).byVillage(v.get().id()).orElseThrow()));
        h.succeed();
    }

    /** TC-T13b-46、47：已建范围；没有规划数据的村庄退回 48 格。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void built_area_and_fallback(GameTestHelper h) {
        Placed p = placeAndRegister(h);
        ServerLevel level = h.getLevel();
        try {
            // 最东边的建筑外 3 格在村里、外 5 格不在
            Box east = p.plan().info().built().stream().max(java.util.Comparator.comparingInt(Box::maxX)).orElseThrow();
            int z = east.centerZ();
            int y = east.minY();
            h.assertTrue(VillageSpace.villageAt(level, new BlockPos(east.maxX() + 3, y, z)).map(v -> v.id().equals(p.village().id()))
                    .orElse(false), "建筑外 3 格在村里");
            h.assertTrue(VillageSpace.villageAt(level, new BlockPos(east.maxX() + 5, y, z)).isEmpty(), "建筑外 5 格不在村里");
            // 旧村庄：只有登记、没有规划
            BlockPos oldBell = new BlockPos(east.maxX() + 300, y, z);
            VillageRegistry.Village old = new VillageRegistry.Village(UUID.randomUUID(), GlobalPos.of(level.dimension(), oldBell),
                    "greek", "zeus", VillageRegistry.Origin.VANILLA, null, "plains", -1);
            VillageRegistry.get(level.getServer()).add(old);
            try {
                h.assertTrue(VillageSpace.villageAt(level, oldBell.east(40)).map(v -> v.id().equals(old.id())).orElse(false),
                        "旧村庄钟外 40 格在村里");
                h.assertTrue(VillageSpace.villageAt(level, oldBell.east(50)).isEmpty(), "旧村庄钟外 50 格不在");
            } finally {
                VillageRegistry.get(level.getServer()).remove(old.bell());
            }
        } finally {
            cleanup(h, p);
        }
        h.succeed();
    }

    /** TC-T13b-48：进村标题按已建范围；离开 16 格后再进来再显示。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void title_uses_built_area(GameTestHelper h) {
        Placed p = placeAndRegister(h);
        try {
            FakePlayer fp = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_gen_title"));
            Box east = p.plan().info().built().stream().max(java.util.Comparator.comparingInt(Box::maxX)).orElseThrow();
            double y = east.minY() + 1;
            double z = east.centerZ() + 0.5;
            fp.moveTo(east.maxX() + 30.5, y, z);
            h.assertTrue(VillageService.titleFor(fp).isEmpty(), "村外不显示");
            fp.moveTo(east.maxX() + 1.5, y, z);
            h.assertTrue(VillageService.titleFor(fp).map(v -> v.id().equals(p.village().id())).orElse(false), "进入时显示");
            fp.moveTo(east.maxX() + 15.5, y, z);
            h.assertTrue(VillageService.titleFor(fp).isEmpty(), "离开 16 格以内不重复");
            fp.moveTo(east.maxX() + 26.5, y, z);
            VillageService.titleFor(fp);
            fp.moveTo(east.maxX() + 1.5, y, z);
            h.assertTrue(VillageService.titleFor(fp).isPresent(), "离开后再进入，再次显示");
        } finally {
            cleanup(h, p);
        }
        h.succeed();
    }

    /** TC-T13b-49：有村口锚点时移民在村口 4–10 格内出现。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void migrant_spawns_at_entrance(GameTestHelper h) {
        Placed p = placeAndRegister(h);
        try {
            BlockPos entrance = block(p.plan().anchors("entrance").get(0));
            Optional<Villager> m = VillagePopulation.spawnMigrant(h.getLevel(), p.village(), h.getLevel().getRandom(),
                    VillagePopulation.SPAWN_MIN, VillagePopulation.SPAWN_MAX);
            h.assertTrue(m.isPresent(), "生成了移民");
            double d = Math.sqrt(m.get().blockPosition().distSqr(new BlockPos(entrance.getX(), m.get().getBlockY(), entrance.getZ())));
            h.assertTrue(d <= VillagePopulation.ENTRANCE_MAX + 1.5, "移民离村口 " + d + " 格");
        } finally {
            cleanup(h, p);
        }
        h.succeed();
    }

    /** TC-T13b-50：默认配置下五种原版村庄一律不生成；打开开关后平原（希腊地域，希腊有模板）让给文明村庄。 */
    @GameTest(template = T, batch = "village_gen_gate", timeoutTicks = 200)
    public static void vanilla_gate(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ConfigHolder<VillagesConfig> holder = ModConfigs.villages();
        VillagesConfig original = holder.current();
        try {
            holder.reload("{\"schemaVersion\":3,\"generateVanillaVillages\":false}");
            for (String t : List.of("plains", "desert", "savanna", "snowy", "taiga")) {
                h.assertFalse(generate(level, structure(level, "vanilla_gate_" + t), 30000, 30000).isValid(), "关：" + t);
            }
            holder.reload("{\"schemaVersion\":3,\"generateVanillaVillages\":true}");
            h.assertTrue(generate(level, structure(level, "vanilla_gate_plains"), 30000, 30000).isValid()
                    == !level.getBiome(new BlockPos(30008, 64, 30008)).is(
                    net.minecraft.tags.TagKey.create(Registries.BIOME, ResourceLocation.parse("mythic_age:region/greek"))),
                    "开：希腊地域里让给文明村庄，其余交给原版");
        } finally {
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(), original));
        }
        h.succeed();
    }
}
