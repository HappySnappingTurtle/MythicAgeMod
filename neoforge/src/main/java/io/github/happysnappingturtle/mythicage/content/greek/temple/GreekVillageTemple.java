package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 希腊的村庄神庙（GA-13 §2.1）：卫城。等级取村里在场的卫城建筑（{@code temple/greek.json} 每级的 {@code building}），
 * 祭坛是这座卫城的 {@code ritual} 锚点，48 格圆心是卫城包围盒的中心；还没有卫城时（新奠基的村子）祭坛 = 奠基石（钟），0 级。
 * 完好度按卫城关键方块的损坏记录算（{@link TempleIntegrity}）。
 */
public final class GreekVillageTemple implements VillageTemples.Provider {
    public static final GreekVillageTemple INSTANCE = new GreekVillageTemple();

    private GreekVillageTemple() {
    }

    /** 一座在场的卫城：规格、位置、对应的神庙等级。 */
    record Acropolis(BuildingSpec spec, BuildPlacement.Placed at, int level) {
    }

    @Override
    public Optional<VillageTemple> resolve(ServerLevel level, VillageRegistry.Village village, VillagePlans.Plan plan,
                                           VillageEconomy.State state) {
        TempleRules rules = TempleService.rules();
        Optional<Acropolis> best = best(rules, acropolises(rules, plan, state));
        BlockPos altar;
        BlockPos center;
        int lvl;
        double integrity = 1;
        if (best.isPresent()) {
            Acropolis a = best.get();
            lvl = a.level();
            integrity = TempleIntegrity.of(level.getServer(), village.id(), a.spec(), a.at().box());
            List<Pos> ritual = BuildPlacement.worldAnchors(a.spec(), a.at()).getOrDefault("ritual", List.of());
            Box b = a.at().box();
            center = new BlockPos((b.minX() + b.maxX()) / 2, b.minY(), (b.minZ() + b.maxZ()) / 2);
            BlockPos anchor = ritual.isEmpty() ? center : new BlockPos(ritual.getFirst().x(), ritual.getFirst().y(), ritual.getFirst().z());
            altar = findAltar(level, anchor).orElse(anchor);
        } else {
            lvl = 0;
            altar = village.bell().pos();
            center = altar;
        }
        lvl = VillageTemples.forcedLevel(village.id()).orElse(lvl);
        integrity = VillageTemples.forcedIntegrity(village.id()).orElse(integrity);
        // 存一份最近算出的完好度（存档、检视用）
        state.temple.setIntegrity(integrity);
        TempleRules.Tier tier = rules.tier(Math.min(lvl, rules.maxLevel()));
        List<BlockPos> slots = VillageTemples.prayerSlots(level, altar, tier.prayerSlots());
        return Optional.of(new VillageTemple(village.id(), village.civ(), GlobalPos.of(level.dimension(), altar), center,
                tier.level(), tier.name(), tier.coefficient(), integrity, tier.prayerSlots(), slots));
    }

    /**
     * 锚点附近的村庄祭坛方块：{@code ritual} 锚点是祭司与游行站的位置，祭坛在它旁边一格（模板里是 x − 1、y + 1）。
     * 在水平 2 格、高差 −1～+2 内找，取最近的；区块没加载或找不到为空。
     */
    static Optional<BlockPos> findAltar(ServerLevel level, BlockPos anchor) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(anchor.offset(-2, -1, -2), anchor.offset(2, 2, 2))) {
            if (level.isLoaded(p) && level.getBlockState(p).is(io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks.VILLAGE_ALTAR.get())) {
                double d = p.distSqr(anchor);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * 这座建筑对应的神庙等级：短名等于某一级的 {@code building}，或是它的主神变体（{@code acropolis_archaic_zeus}）；不是卫城为 0。
     * 变体的规格里不一定写了 patron，{@code shortBase()} 去不掉后缀，所以按前缀认。
     */
    public static int levelOf(TempleRules rules, BuildingSpec s) {
        String name = s.id().substring(s.id().indexOf(':') + 1);
        int level = 0;
        for (TempleRules.Tier t : rules.tiers()) {
            String b = t.building();
            if (!b.isEmpty() && (name.equals(b) || name.startsWith(b + "_"))) {
                level = Math.max(level, t.level());
            }
        }
        return level;
    }

    /** 村里在场的卫城：施工建成的直接取位置，生成的按锚点反推。 */
    static List<Acropolis> acropolises(TempleRules rules, VillagePlans.Plan plan, VillageEconomy.State state) {
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        List<Acropolis> out = new ArrayList<>();
        for (VillageEconomy.Built b : state.constructed) {
            BuildingSpec s = specs.get(b.building());
            if (s != null) {
                int lvl = levelOf(rules, s);
                if (lvl > 0) {
                    out.add(new Acropolis(s, b.placed(), lvl));
                }
            }
        }
        for (BuildingSpec s : plan == null ? List.<BuildingSpec>of() : specs.values()) {
            if (!s.generated() || !"greek".equals(s.civ())) {
                continue;
            }
            int lvl = levelOf(rules, s);
            if (lvl <= 0) {
                continue;
            }
            for (BuildPlacement.Placed at : BuildPlacement.locateAll(s, plan.info().anchors(), plan.info().built())) {
                out.add(new Acropolis(s, at, lvl));
            }
        }
        return out;
    }

    /** 等级最高的一座；同级取施工记录里靠后（新）的那座。被扩建替换掉的旧卫城记录可能还在，取高的就对了。 */
    static Optional<Acropolis> best(TempleRules rules, List<Acropolis> all) {
        Acropolis best = null;
        for (Acropolis a : all) {
            if (best == null || a.level() > best.level()) {
                best = a;
            }
        }
        return Optional.ofNullable(best);
    }
}
