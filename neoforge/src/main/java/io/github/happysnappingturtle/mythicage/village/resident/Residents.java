package io.github.happysnappingturtle.mythicage.village.resident;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 居民的生成与名册（T19 设计文档 §10.1）。
 * <ul>
 *   <li>生成村庄：每个 {@code home} 锚点 1 名，职业按文明数据的 {@code residents.initial} 分配，其余是基础劳力；
 *       第一名基础劳力兼谷仓管事（D4，村务界面入口）；</li>
 *   <li>D3：T13b–T18 生成的村庄里住着过渡用的原版无业村民。它们进入世界时，若在某座有本框架数据的生成村庄的已建范围里，
 *       而名册还没住满，就排队换成居民（下一刻处理，不在实体加入事件里改世界）。</li>
 * </ul>
 */
public final class Residents {
    public static final String HOME_ANCHOR = "home";
    public static final String KEEPER_ANCHOR = "storage_keeper";

    private record Conversion(ServerLevel level, UUID villager, UUID village) {
    }

    private static final ConcurrentLinkedQueue<Conversion> QUEUE = new ConcurrentLinkedQueue<>();

    private Residents() {
    }

    /** 生成一名居民并登记进名册。 */
    public static Optional<ResidentEntity> spawn(ServerLevel level, VillageRegistry.Village village, CivVillageData data,
                                                 String profession, BlockPos home, BlockPos at, boolean keeper) {
        ResidentEntity r = ModEntities.RESIDENT.get().create(level);
        if (r == null) {
            return Optional.empty();
        }
        RandomSource rnd = level.getRandom();
        boolean female = rnd.nextBoolean();
        List<String> names = female ? data.residents().female() : data.residents().male();
        String name = names.isEmpty() ? "" : names.get(rnd.nextInt(names.size()));
        r.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd.nextFloat() * 360, 0);
        r.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        r.setup(village.id(), village.civ(), profession, home, name, female, rnd.nextInt(6));
        if (!level.addFreshEntity(r)) {
            return Optional.empty();
        }
        VillageEconomy eco = VillageEconomy.get(level.getServer());
        eco.state(village.id()).roster.put(r.getUUID(), new VillageEconomy.Member(r.getUUID(), profession, home.immutable(), keeper));
        eco.setDirty();
        return Optional.of(r);
    }

    /** 生成一名在途移民（T19 §1.6 H4）：床位先分好，但不进名册，走到了才登记。 */
    public static Optional<ResidentEntity> spawnMigrant(ServerLevel level, VillageRegistry.Village village, CivVillageData data,
                                                        String profession, BlockPos home, BlockPos at) {
        ResidentEntity r = ModEntities.RESIDENT.get().create(level);
        if (r == null) {
            return Optional.empty();
        }
        RandomSource rnd = level.getRandom();
        boolean female = rnd.nextBoolean();
        List<String> names = female ? data.residents().female() : data.residents().male();
        String name = names.isEmpty() ? "" : names.get(rnd.nextInt(names.size()));
        r.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, rnd.nextFloat() * 360, 0);
        r.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        r.setup(village.id(), village.civ(), profession, home, name, female, rnd.nextInt(6));
        return level.addFreshEntity(r) ? Optional.of(r) : Optional.empty();
    }

    /** 生成村庄的初始居民：每个床位锚点 1 名。返回生成数。 */
    public static int spawnInitial(ServerLevel level, VillageRegistry.Village village, VillagePlans.Plan plan,
                                   CivVillageData data) {
        List<Pos> homes = plan.anchors(HOME_ANCHOR);
        List<String> profs = data.initialProfessions(homes.size());
        boolean keeperGiven = false;
        int n = 0;
        for (int i = 0; i < homes.size(); i++) {
            String prof = profs.get(i);
            boolean keeper = !keeperGiven && prof.equals(keeperProfession(data));
            BlockPos home = VillageInfoPiece.block(homes.get(i));
            if (spawn(level, village, data, prof, home, home, keeper).isPresent()) {
                n++;
                keeperGiven |= keeper;
            }
        }
        return n;
    }

    static String keeperProfession(CivVillageData data) {
        return data.residents().keeper().isEmpty() ? data.residents().fallback() : data.residents().keeper();
    }

    /** 名册里没人睡的床位锚点。 */
    public static List<BlockPos> freeHomes(VillagePlans.Plan plan, VillageEconomy.State state) {
        Set<Long> used = new HashSet<>();
        state.roster.values().forEach(m -> used.add(m.home().asLong()));
        List<BlockPos> out = new ArrayList<>();
        for (Pos p : plan.anchors(HOME_ANCHOR)) {
            BlockPos b = VillageInfoPiece.block(p);
            if (!used.contains(b.asLong())) {
                out.add(b);
            }
        }
        return out;
    }

    /** 下一个该补的职业：按初始分配表，缺谁补谁。 */
    static String nextProfession(CivVillageData data, VillageEconomy.State state, int homes) {
        Map<String, Integer> have = new LinkedHashMap<>();
        state.roster.values().forEach(m -> have.merge(m.profession(), 1, Integer::sum));
        for (String p : data.initialProfessions(homes)) {
            if (have.merge(p, -1, Integer::sum) < 0) {
                return p;
            }
        }
        return data.residents().fallback();
    }

    // ───────────── D3：过渡期原版村民 ─────────────

    public static void maybeQueueConversion(ServerLevel level, Villager v) {
        if (v.isBaby() || v.getVillagerData().getProfession() != VillagerProfession.NONE) {
            return;
        }
        VillagePlans plans = VillagePlans.get(level.getServer());
        Optional<UUID> village = plans.at(level.dimension(), v.getBlockX(), v.getBlockZ());
        if (village.isEmpty()) {
            return;
        }
        Optional<VillagePlans.Plan> plan = plans.byVillage(village.get());
        Optional<VillageRegistry.Village> reg = VillageRegistry.get(level.getServer()).byId(village.get());
        if (plan.isEmpty() || reg.isEmpty() || reg.get().origin() != VillageRegistry.Origin.GENERATED
                || VillageData.civ(reg.get().civ()).isEmpty() || !insideBuilt(plan.get(), v.blockPosition())) {
            return;
        }
        QUEUE.add(new Conversion(level, v.getUUID(), village.get()));
    }

    private static boolean insideBuilt(VillagePlans.Plan plan, BlockPos p) {
        for (Box b : plan.info().built()) {
            if (p.getX() >= b.minX() - 4 && p.getX() <= b.maxX() + 4 && p.getZ() >= b.minZ() - 4 && p.getZ() <= b.maxZ() + 4) {
                return true;
            }
        }
        return false;
    }

    public static void drainQueue(MinecraftServer server) {
        Conversion c;
        while ((c = QUEUE.poll()) != null) {
            convert(c);
        }
    }

    private static void convert(Conversion c) {
        if (!(c.level().getEntity(c.villager()) instanceof Villager v) || !v.isAlive()) {
            return;
        }
        MinecraftServer server = c.level().getServer();
        Optional<VillagePlans.Plan> plan = VillagePlans.get(server).byVillage(c.village());
        Optional<VillageRegistry.Village> village = VillageRegistry.get(server).byId(c.village());
        Optional<CivVillageData> data = village.flatMap(x -> VillageData.civ(x.civ()));
        if (plan.isEmpty() || data.isEmpty()) {
            return;
        }
        VillageEconomy.State state = VillageEconomy.get(server).state(c.village());
        List<BlockPos> free = freeHomes(plan.get(), state);
        if (free.isEmpty()) {
            return;
        }
        BlockPos home = free.stream().min(java.util.Comparator.comparingDouble(b -> b.distSqr(v.blockPosition()))).orElseThrow();
        String prof = nextProfession(data.get(), state, plan.get().anchors(HOME_ANCHOR).size());
        boolean keeper = state.keeper().isEmpty() && prof.equals(keeperProfession(data.get()));
        // 生成在床位锚点上，不在原版村民的位置：它可能正躺在床里，居民生成在床方块里会卡住、寻路一步都走不出去
        v.discard();
        spawn(c.level(), village.get(), data.get(), prof, home, home, keeper);
    }

    public static void clearQueue() {
        QUEUE.clear();
    }
}
