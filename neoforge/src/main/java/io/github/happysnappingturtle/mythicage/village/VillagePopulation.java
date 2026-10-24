package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.rite.MigrationRules;
import io.github.happysnappingturtle.mythicage.core.rite.VillageStageTable;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.rite.RiteTables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 村庄人口与移民（T11a 设计文档 §8.1；D9）：古典起，村庄按「基线 + 各阶段人口加成」吸纳移民——
 * 在钟外 32–40 格的地表生成一名无业村民，聚会点设为本钟并走向钟；必须有空床。
 * 只处理附近有玩家、钟已加载的村庄；不离线补算。
 */
public final class VillagePopulation {
    public static final int INTERVAL = 100;
    public static final int PLAYER_RANGE = 128;
    public static final int MEMBER_RANGE = 64;
    public static final int BED_RANGE = 48;
    public static final int SPAWN_MIN = 32;
    public static final int SPAWN_MAX = 40;
    /** 有村口锚点时，移民在村口外这个距离范围内出现。 */
    public static final int ENTRANCE_MIN = 4;
    public static final int ENTRANCE_MAX = 10;
    private static final int SPOT_TRIES = 24;
    private static final float WALK_SPEED = 0.5F;
    /** 新移民走进村：每秒重设一次走路目标，直到离钟 ARRIVED 格内或超过 GUIDE_TICKS（原版闲逛会改掉走路目标）。 */
    private static final int GUIDE_INTERVAL = 20;
    private static final int GUIDE_TICKS = 1200;
    public static final int ARRIVED = 6;

    private record Arrival(GlobalPos bell, long until) {
    }

    private static final java.util.Map<UUID, Arrival> ARRIVING = new ConcurrentHashMap<>();

    /** 成员未到目标却没有空床的村庄（祭司对话页提示「需要更多住居」）；只在内存里，重启后下一次检查重算。 */
    private static final Set<UUID> NEEDS_HOUSING = ConcurrentHashMap.newKeySet();

    private VillagePopulation() {
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % GUIDE_INTERVAL == 0) {
            guide(server);
        }
        if (server.getTickCount() % INTERVAL != 0) {
            return;
        }
        for (VillageRegistry.Village v : VillageRegistry.get(server).all()) {
            if (residentFramework(v)) {
                continue;
            }
            ServerLevel level = server.getLevel(v.bell().dimension());
            if (level != null && level.isLoaded(v.bell().pos())) {
                step(level, v, playerNear(level, v.bell().pos()), level.getRandom(), SPAWN_MIN, SPAWN_MAX);
            }
        }
    }

    private static void guide(MinecraftServer server) {
        ARRIVING.entrySet().removeIf(e -> {
            ServerLevel level = server.getLevel(e.getValue().bell().dimension());
            if (level == null || level.getGameTime() > e.getValue().until()
                    || !(level.getEntity(e.getKey()) instanceof Villager v) || !v.isAlive()) {
                return true;
            }
            BlockPos bell = e.getValue().bell().pos();
            if (v.blockPosition().closerThan(bell, ARRIVED)) {
                return true;
            }
            boolean toBell = v.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                    .filter(t -> t.getTarget().currentBlockPosition().closerThan(bell, ARRIVED)).isPresent();
            if (!toBell && !v.isSleeping() && !v.isTrading()) {
                v.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(bell, WALK_SPEED, 3));
            }
            return false;
        });
    }

    /** 生成的文明村庄由居民框架管人口（T19），这里不再往里放原版无业村民。 */
    public static boolean residentFramework(VillageRegistry.Village v) {
        return v.origin() == VillageRegistry.Origin.GENERATED
                && io.github.happysnappingturtle.mythicage.village.work.VillageData.civ(v.civ()).isPresent();
    }

    /** 一次检查：必要时补记基线、更新「需要更多住居」、按判定生成移民。返回判定。 */
    public static MigrationRules.Decision step(ServerLevel level, VillageRegistry.Village village, boolean playerNear,
                                               RandomSource random, int minR, int maxR) {
        Optional<Civilization> civ = Civilization.byId(village.civ());
        VillageRegistry.Village v = recordBaseline(level, village);
        if (civ.isEmpty()) {
            NEEDS_HOUSING.remove(v.id());
            return MigrationRules.Decision.INACTIVE;
        }
        VillageStageTable table = RiteTables.stages(civ.get());
        int members = members(level, v.bell());
        long today = MythDays.today(level.getServer());
        MigrationRules.Decision d = MigrationRules.decide(new MigrationRules.Input(v.stage(), members, target(v, members),
                playerNear ? freeBeds(level, v.bell().pos()) : 0, today, v.lastMigrationDay(),
                table.migrationInterval(v.stage()), playerNear));
        if (d == MigrationRules.Decision.NO_BED) {
            NEEDS_HOUSING.add(v.id());
        } else if (d != MigrationRules.Decision.NO_PLAYER) {
            NEEDS_HOUSING.remove(v.id());
        }
        if (d == MigrationRules.Decision.MIGRATE) {
            if (spawnMigrant(level, v, random, minR, maxR).isEmpty()) {
                return MigrationRules.Decision.WAITING;
            }
            VillageRegistry.get(level.getServer()).update(v.withPopulation(v.baseline(), today));
        }
        return d;
    }

    /** 首次到古典（或之后第一次加载）时记下当时的成员数作为基线。 */
    public static VillageRegistry.Village recordBaseline(ServerLevel level, VillageRegistry.Village v) {
        if (v.baseline() >= 0 || !v.stage().atLeast(Age.CLASSICAL) || !level.isLoaded(v.bell().pos())) {
            return v;
        }
        VillageRegistry.Village updated = v.withPopulation(members(level, v.bell()), v.lastMigrationDay());
        VillageRegistry.get(level.getServer()).update(updated);
        return updated;
    }

    /** 移民目标；还没有基线时以当前成员数为基线。 */
    public static int target(VillageRegistry.Village v, int members) {
        return Civilization.byId(v.civ())
                .map(c -> RiteTables.stages(c).populationTarget(v.baseline() >= 0 ? v.baseline() : members, v.stage()))
                .orElse(members);
    }

    /** 成员：64 格内聚会点指向本钟的成年村民（只数已加载的）。 */
    public static int members(ServerLevel level, GlobalPos bell) {
        if (!bell.dimension().equals(level.dimension())) {
            return 0;
        }
        return level.getEntitiesOfClass(Villager.class, new AABB(bell.pos()).inflate(MEMBER_RANGE),
                x -> x.isAlive() && !x.isBaby() && x.getBrain().getMemory(MemoryModuleType.MEETING_POINT)
                        .filter(bell::equals).isPresent()).size();
    }

    /** 48 格内还没有主人的床。 */
    public static int freeBeds(ServerLevel level, BlockPos bell) {
        return (int) level.getPoiManager().getCountInRange(t -> t.is(PoiTypes.HOME), bell, BED_RANGE,
                PoiManager.Occupancy.HAS_SPACE);
    }

    public static boolean needsHousing(UUID village) {
        return NEEDS_HOUSING.contains(village);
    }

    /** 卫兵上限（T20 的士兵使用；T11a 只在晋升页显示）。 */
    public static int guardCap(ServerLevel level, VillageRegistry.Village v, Age stage) {
        return Civilization.byId(v.civ()).map(c -> RiteTables.stages(c).guardCap(members(level, v.bell()), stage)).orElse(0);
    }

    private static boolean playerNear(ServerLevel level, BlockPos bell) {
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && Mth.square(p.getX() - bell.getX()) + Mth.square(p.getZ() - bell.getZ())
                    <= Mth.square(PLAYER_RANGE)) {
                return true;
            }
        }
        return false;
    }

    /** 在钟外 minR–maxR 格找一个已加载、干燥、能站人的地表点生成移民，并让他走向钟。 */
    public static Optional<Villager> spawnMigrant(ServerLevel level, VillageRegistry.Village v, RandomSource random,
                                                  int minR, int maxR) {
        BlockPos bell = v.bell().pos();
        // 文明村庄有村口锚点：在村口外 4–10 格生成（T13b §3.5）
        Optional<BlockPos> entrance = VillagePlans.get(level.getServer()).byVillage(v.id())
                .flatMap(p -> p.anchors("entrance").stream().findFirst())
                .map(io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece::block);
        BlockPos base = entrance.orElse(bell);
        int lo = entrance.isPresent() ? ENTRANCE_MIN : minR;
        int hi = entrance.isPresent() ? ENTRANCE_MAX : maxR;
        for (int i = 0; i < SPOT_TRIES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = lo + random.nextDouble() * (hi - lo);
            int x = base.getX() + Mth.floor(Math.cos(angle) * r);
            int z = base.getZ() + Mth.floor(Math.sin(angle) * r);
            if (!level.isLoaded(new BlockPos(x, bell.getY(), z))) {
                continue;
            }
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!standable(level, pos)) {
                continue;
            }
            Villager villager = EntityType.VILLAGER.create(level);
            if (villager == null) {
                return Optional.empty();
            }
            villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
            villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            if (!level.addFreshEntity(villager)) {
                return Optional.empty();
            }
            villager.getBrain().setMemory(MemoryModuleType.MEETING_POINT, v.bell());
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(bell, WALK_SPEED, 3));
            ARRIVING.put(villager.getUUID(), new Arrival(v.bell(), level.getGameTime() + GUIDE_TICKS));
            return Optional.of(villager);
        }
        return Optional.empty();
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getFluidState(pos).isEmpty() && level.getFluidState(below).isEmpty()
                && !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.noCollision(EntityType.VILLAGER.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    /** 只供 GameTest。 */
    public static void clearForTest() {
        NEEDS_HOUSING.clear();
        ARRIVING.clear();
    }
}
