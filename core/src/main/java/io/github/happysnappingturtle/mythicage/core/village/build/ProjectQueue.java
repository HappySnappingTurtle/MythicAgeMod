package io.github.happysnappingturtle.mythicage.core.village.build;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 项目队列（GA-11 §2.2、§2.3）：每村 1 个进行中的项目 + 1 个「下一个」位。纯函数。
 * <ol>
 *   <li>有进行中的项目：什么都不做（开工后不能换，D3）；</li>
 *   <li>次神祠插队：有能开工的祠就先建祠，「下一个」位保留；</li>
 *   <li>「下一个」位有值：能开工就开工它并清空；变得不能开工就清空并记原因（缺粮是暂时的，保留等它），黎明时村子接着自己选；</li>
 *   <li>「下一个」位有值时村子不自己选（D2）；空着时只在黎明由村子按原规则自己选。</li>
 * </ol>
 */
public final class ProjectQueue {
    /** 缺粮只是暂停：「下一个」位保留。 */
    private static final Set<String> TEMPORARY = Set.of(ProjectPicker.FOOD_SHORT);
    /** 点名的候选已经不在列表里（唯一建筑建好了、旧建筑没了）。 */
    public static final String GONE = "gone";

    /**
     * {@code village_civ} 的 {@code projectPick} 节。
     *
     * @param minContribution 点名门槛：在这个村子的累计贡献
     * @param patronFactor    村子的主神认可你（供奉者）时门槛乘这个数
     */
    public record Settings(int minContribution, double patronFactor) {
        public static final Settings DEFAULT = new Settings(64, 0.5);

        public Settings {
            if (minContribution < 0 || !(patronFactor >= 0 && patronFactor <= 1)) {
                throw new IllegalArgumentException("Invalid projectPick settings");
            }
        }

        public static Settings parse(JsonObject o) {
            return new Settings(o.has("minContribution") ? o.get("minContribution").getAsInt() : DEFAULT.minContribution,
                    o.has("patronFactor") ? o.get("patronFactor").getAsDouble() : DEFAULT.patronFactor);
        }

        /** 这名玩家的门槛（供奉者减半，向上取整）。 */
        public int threshold(boolean patron) {
            return patron ? (int) Math.ceil(minContribution * patronFactor) : minContribution;
        }

        public boolean allows(int contribution, boolean patron) {
            return contribution >= threshold(patron);
        }
    }

    /**
     * 「下一个」位。
     *
     * @param key      候选键（{@link ProjectPicker#key}）
     * @param building 点名时的建筑 id（显示用）
     * @param god      次神祠的神；其他为空串
     */
    public record Next(String key, String building, String god, UUID player, String playerName, long day) {
    }

    /**
     * @param pick     要开工的项目；没有为空
     * @param fromNext 开工的是「下一个」位里的项目
     * @param dropNext 「下一个」位要清空（开工了，或变得不能开工）
     * @param dropReason 因为不能开工而清空时的原因；其他为空串
     */
    public record Decision(Optional<ProjectPicker.Pick> pick, boolean fromNext, boolean dropNext, String dropReason) {
        static final Decision NONE = new Decision(Optional.empty(), false, false, "");
    }

    private ProjectQueue() {
    }

    /**
     * @param dawn 黎明（每 MC 日一次）：「下一个」位空着时村子才自己选；项目落成、取消、刚点名时为 false
     */
    public static Decision decide(ProjectPicker.Input in, Optional<Next> next, boolean dawn) {
        if (in.busy()) {
            return Decision.NONE;
        }
        List<ProjectPicker.Candidate> all = ProjectPicker.candidates(in);
        Optional<ProjectPicker.Candidate> shrine = all.stream()
                .filter(c -> c.kind() == ProjectPicker.Kind.SHRINE && c.available()).findFirst();
        if (shrine.isPresent()) {
            return new Decision(Optional.of(shrine.get().pick()), false, false, "");
        }
        if (next.isPresent()) {
            Optional<ProjectPicker.Candidate> c = all.stream().filter(x -> x.key().equals(next.get().key())).findFirst();
            if (c.isPresent() && c.get().available()) {
                return new Decision(Optional.of(c.get().pick()), true, true, "");
            }
            String reason = c.map(ProjectPicker.Candidate::reason).orElse(GONE);
            if (TEMPORARY.contains(reason)) {
                return Decision.NONE;
            }
            // 清空之后「下一个」位就空了：黎明时村子照常自己选
            return new Decision(dawn ? villagePick(all) : Optional.empty(), false, true, reason);
        }
        return dawn ? new Decision(villagePick(all), false, false, "") : Decision.NONE;
    }

    private static Optional<ProjectPicker.Pick> villagePick(List<ProjectPicker.Candidate> all) {
        return all.stream().filter(ProjectPicker.Candidate::available).findFirst().map(ProjectPicker.Candidate::pick);
    }
}
