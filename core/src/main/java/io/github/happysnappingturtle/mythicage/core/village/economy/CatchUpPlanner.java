package io.github.happysnappingturtle.mythicage.core.village.economy;

import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 离线补算（T19 设计文档 §7，V1 §9，T5 方案 E）：村庄从「未运行」回到「真实运行」时，按离开天数一次性估算产量入库。
 * <ul>
 *   <li>最多补 {@code maxDays}（3 个 MC 日）；</li>
 *   <li>每个生产者按「日产率 × 天数」推进，受原料、资源点余量（含恢复）、仓储容量限制；</li>
 *   <li>先算没有原料的（采集），再按上下游依次算加工，不凭空多出中间品；</li>
 *   <li>口粮按人数 × 天数扣，不够只记缺口（不饿死）；</li>
 *   <li>不改任何方块。</li>
 * </ul>
 */
public final class CatchUpPlanner {
    /**
     * 一个岗位的产率。
     *
     * @param inputs       每循环消耗
     * @param outputs      每循环产出
     * @param cyclesPerDay 每 MC 日循环次数（由资源工作区与配方算出，不是看动画）
     * @param point        采集点 id（为空表示不取资源点）
     * @param pointPerCycle 每循环消耗的资源点余量
     */
    public record Producer(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, double cyclesPerDay,
                           String point, int pointPerCycle) {
        public Producer {
            inputs = Map.copyOf(inputs);
            outputs = Map.copyOf(outputs);
        }
    }

    /** @param gained 实际入库；discarded 因满仓作废；rationShort 口粮缺口（份） */
    public record Result(double days, Map<String, Integer> gained, Map<String, Integer> discarded,
                         Map<String, ResourcePoint> points, double rationShort) {
    }

    private CatchUpPlanner() {
    }

    public static Result plan(VillageStock stock, Map<String, ResourcePoint> points, List<Producer> producers,
                              double awayDays, double maxDays, long today, int people, double perPerson, Rations rations) {
        double days = Math.max(0, Math.min(awayDays, maxDays));
        Map<String, ResourcePoint> pts = new LinkedHashMap<>(points);
        Map<String, Integer> gained = new LinkedHashMap<>();
        Map<String, Integer> discarded = new LinkedHashMap<>();
        if (days > 0) {
            for (Producer p : order(producers)) {
                int cycles = (int) Math.floor(p.cyclesPerDay() * days + 1e-9);
                for (Map.Entry<String, Integer> in : p.inputs().entrySet()) {
                    if (in.getValue() > 0) {
                        cycles = Math.min(cycles, stock.count(in.getKey()) / in.getValue());
                    }
                }
                if (p.point() != null && !p.point().isEmpty()) {
                    ResourcePoint rp = pts.get(p.point());
                    if (rp == null) {
                        continue;
                    }
                    rp = rp.recover(today);
                    if (p.pointPerCycle() > 0) {
                        cycles = Math.min(cycles, rp.remaining() / p.pointPerCycle());
                    }
                    pts.put(p.point(), rp.take(Math.max(0, cycles) * p.pointPerCycle()));
                }
                if (cycles <= 0) {
                    continue;
                }
                for (Map.Entry<String, Integer> in : p.inputs().entrySet()) {
                    stock.take(in.getKey(), in.getValue() * cycles, 0);
                }
                for (Map.Entry<String, Integer> out : p.outputs().entrySet()) {
                    int want = out.getValue() * cycles;
                    int got = stock.deposit(out.getKey(), want);
                    if (got > 0) {
                        gained.merge(out.getKey(), got, Integer::sum);
                    }
                    if (want > got) {
                        discarded.merge(out.getKey(), want - got, Integer::sum);
                    }
                }
            }
        }
        double shortage = days > 0 ? rations.consume(stock, people * perPerson * days) : 0;
        return new Result(days, gained, discarded, pts, shortage);
    }

    /** 上游在前：没有原料的先算；其余按「原料都已有生产者算过或不由任何人生产」依次放入；有环时保留原顺序。 */
    static List<Producer> order(List<Producer> in) {
        List<Producer> left = new ArrayList<>(in);
        List<Producer> out = new ArrayList<>();
        Set<String> produced = new HashSet<>();
        Set<String> anyOutput = new HashSet<>();
        in.forEach(p -> anyOutput.addAll(p.outputs().keySet()));
        while (!left.isEmpty()) {
            Producer next = null;
            for (Producer p : left) {
                boolean ready = p.inputs().keySet().stream().allMatch(i -> produced.contains(i) || !anyOutput.contains(i)
                        || left.stream().noneMatch(q -> q != p && q.outputs().containsKey(i)));
                if (ready) {
                    next = p;
                    break;
                }
            }
            if (next == null) {
                next = left.get(0);
            }
            left.remove(next);
            out.add(next);
            produced.addAll(next.outputs().keySet());
        }
        return out;
    }
}
