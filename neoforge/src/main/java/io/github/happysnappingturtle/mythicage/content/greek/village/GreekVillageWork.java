package io.github.happysnappingturtle.mythicage.content.greek.village;

import io.github.happysnappingturtle.mythicage.content.greek.agri.GrapeTrellisBlock;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OliveLeavesBlock;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.village.behavior.VillageBehavior;
import io.github.happysnappingturtle.mythicage.village.work.BasicJobTypes;
import io.github.happysnappingturtle.mythicage.village.work.FruitHarvester;
import io.github.happysnappingturtle.mythicage.village.work.LandJobTypes;
import io.github.happysnappingturtle.mythicage.village.work.NaturalResources;
import io.github.happysnappingturtle.mythicage.village.work.PointVisuals;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 希腊村庄接入居民框架的代码部分（T19 阶段 ②，V3-希腊-居民 §2.2、§3.1）：
 * <ul>
 *   <li>采果器：橄榄树（{@code olive}）、葡萄架（{@code grape}）；</li>
 *   <li>采石点露头的分档外观；</li>
 *   <li>希腊行为模块 {@code greek}：按历法期调工作权重——生长期修枝、收割期收麦、葡萄与橄榄期采果，都 ×3。
 *       历法只改先后，不影响作物生长（H2）。祈祷、节庆等在阶段 ④⑤ 加进这个模块。</li>
 * </ul>
 */
public final class GreekVillageWork {
    public static final double PERIOD_BOOST = 3;

    private GreekVillageWork() {
    }

    public static void init() {
        FruitHarvester.register("olive", new OliveHarvester());
        FruitHarvester.register("grape", new GrapeHarvester());
        PointVisuals.register("quarry", NaturalResources::quarryVisual);
        VillageBehavior.register(new Behavior());
    }

    /** 橄榄树：锚点是树干底，熟果长在树冠的树叶上（{@link OliveLeavesBlock#FRUIT} = 熟）。每片熟叶 1–2 颗，与玩家采摘一致。 */
    static final class OliveHarvester implements FruitHarvester {
        @Override
        public List<BlockPos> ripe(ServerLevel level, BlockPos anchor) {
            List<BlockPos> out = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(anchor.offset(-3, 1, -3), anchor.offset(3, 6, 3))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof OliveLeavesBlock && s.getValue(OliveLeavesBlock.FRUIT) == OliveLeavesBlock.RIPE) {
                    out.add(p.immutable());
                }
            }
            return out;
        }

        @Override
        public Map<String, Integer> harvest(ServerLevel level, List<BlockPos> ripe) {
            int olives = 0;
            for (BlockPos p : ripe) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof OliveLeavesBlock && s.getValue(OliveLeavesBlock.FRUIT) == OliveLeavesBlock.RIPE) {
                    level.setBlock(p, s.setValue(OliveLeavesBlock.FRUIT, 0), Block.UPDATE_CLIENTS);
                    olives += 1 + level.random.nextInt(2);
                }
            }
            Map<String, Integer> m = new LinkedHashMap<>();
            if (olives > 0) {
                m.put("mythic_age:olive", olives);
            }
            return m;
        }
    }

    /** 葡萄架：锚点是架前站位，架子在旁边一格；整架以底格的阶段为准，采后回到「抽藤」（与玩家采摘一致，每架 2–3 串）。 */
    static final class GrapeHarvester implements FruitHarvester {
        @Override
        public List<BlockPos> ripe(ServerLevel level, BlockPos anchor) {
            List<BlockPos> out = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(anchor.offset(-1, 0, -1), anchor.offset(1, 0, 1))) {
                bottom(level, p).filter(b -> !out.contains(b)).ifPresent(out::add);
            }
            return out;
        }

        static Optional<BlockPos> bottom(ServerLevel level, BlockPos p) {
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof GrapeTrellisBlock)) {
                return Optional.empty();
            }
            BlockPos b = GrapeTrellisBlock.bottomOf(p, s);
            BlockState bs = level.getBlockState(b);
            return bs.getBlock() instanceof GrapeTrellisBlock && bs.getValue(GrapeTrellisBlock.STAGE) == GrapeTrellisBlock.MATURE
                    ? Optional.of(b.immutable()) : Optional.empty();
        }

        @Override
        public Map<String, Integer> harvest(ServerLevel level, List<BlockPos> ripe) {
            int grapes = 0;
            for (BlockPos b : ripe) {
                BlockState bs = level.getBlockState(b);
                if (bs.getBlock() instanceof GrapeTrellisBlock && bs.getValue(GrapeTrellisBlock.STAGE) == GrapeTrellisBlock.MATURE) {
                    level.setBlock(b, bs.setValue(GrapeTrellisBlock.STAGE, 2), Block.UPDATE_CLIENTS);
                    grapes += 2 + level.random.nextInt(2);
                }
            }
            Map<String, Integer> m = new LinkedHashMap<>();
            if (grapes > 0) {
                m.put("mythic_age:grape", grapes);
            }
            return m;
        }
    }

    /** 希腊行为模块：历法期调权重（V3-希腊-居民 §2.2）。 */
    static final class Behavior implements VillageBehavior {
        @Override
        public String id() {
            return "greek";
        }

        @Override
        public Optional<Job> modify(VillageContext ctx, Job job) {
            String period = ctx.date().period();
            boolean boost = switch (period) {
                case "growing" -> job.type().equals(BasicJobTypes.GATHER) && job.ref().startsWith("prune");
                case "harvest" -> job.type().equals(BasicJobTypes.HARVEST);
                case "vintage" -> job.type().equals(LandJobTypes.PICK);
                default -> false;
            };
            return Optional.of(boost ? job.withWeight(job.weight() * PERIOD_BOOST) : job);
        }
    }
}
