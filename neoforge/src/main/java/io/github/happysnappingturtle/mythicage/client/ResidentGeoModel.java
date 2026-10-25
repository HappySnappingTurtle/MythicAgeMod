package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.client.geo.MythGeoModel;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Set;

/**
 * 居民模型（T19 设计文档 §3.1、§12）：一套身体，服装组与搬运附件靠骨骼显隐切换。
 * <ul>
 *   <li>{@code outfit_male_*} / {@code outfit_female_*}：男装 / 女装各部分（H3），按外观第 0 位二选一；</li>
 *   <li>{@code carry_<名>}：手上搬的东西，只显示与 {@link ResidentEntity#carrying()} 同名的那个；</li>
 *   <li>{@code tool_<名>}：手持工具，只在用到它的动作里显示（{@link #TOOLS}）；</li>
 * </ul>
 * 贴图：{@code textures/entity/resident/resident_<m|f><服色>.png}（缺失时退回基础贴图）。
 */
public class ResidentGeoModel extends MythGeoModel<ResidentEntity> {
    public static final String MALE = "outfit_male";
    public static final String FEMALE = "outfit_female";
    public static final String CARRY_PREFIX = "carry_";
    public static final String TOOL_PREFIX = "tool_";
    /** 动作 → 手上的工具（只在这些动作里显示，其余时候空手）。 */
    static final java.util.Map<String, java.util.Set<String>> TOOLS = java.util.Map.ofEntries(
            java.util.Map.entry("olive_beat", java.util.Set.of("tool_pole")),
            java.util.Map.entry("prune", java.util.Set.of("tool_hook")),
            java.util.Map.entry("shear", java.util.Set.of("tool_shears")),
            java.util.Map.entry("scythe_harvest", java.util.Set.of("tool_sickle")),
            java.util.Map.entry("winnow", java.util.Set.of("tool_fork")),
            java.util.Map.entry("dig", java.util.Set.of("tool_spade")),
            java.util.Map.entry("quarry_wedge", java.util.Set.of("tool_mallet", "tool_wedge")),
            java.util.Map.entry("chisel_strike", java.util.Set.of("tool_hammer", "tool_chisel")),
            java.util.Map.entry("build_hammer", java.util.Set.of("tool_hammer")),
            java.util.Map.entry("chop", java.util.Set.of("tool_axe")),
            java.util.Map.entry("saw", java.util.Set.of("tool_saw")),
            // 生活与节庆（T19 阶段 ⑤b）：接水的水罐、奠酒浅碗、薄饼、投网、戏剧面具
            java.util.Map.entry("water_fill", java.util.Set.of("tool_hydria")),
            java.util.Map.entry("libation_home", java.util.Set.of("tool_cup")),
            java.util.Map.entry("eat", java.util.Set.of("tool_bread")),
            java.util.Map.entry("fish_cast", java.util.Set.of("tool_net")),
            java.util.Map.entry("fish_reel", java.util.Set.of("tool_net")),
            java.util.Map.entry("perform", java.util.Set.of("tool_mask")));

    public ResidentGeoModel() {
        super("resident", Set.of());
    }

    @Override
    public void setCustomAnimations(ResidentEntity r, long instanceId, AnimationState<ResidentEntity> state) {
        super.setCustomAnimations(r, instanceId, state);
        boolean female = r.female();
        String carry = r.carrying();
        java.util.Set<String> tools = TOOLS.getOrDefault(r.anim(), java.util.Set.of());
        for (GeoBone b : getAnimationProcessor().getRegisteredBones()) {
            String n = b.getName();
            if (n.startsWith(MALE)) {
                b.setHidden(female);
            } else if (n.startsWith(FEMALE)) {
                b.setHidden(!female);
            } else if (n.startsWith(CARRY_PREFIX)) {
                b.setHidden(!n.equals(CARRY_PREFIX + carry));
            } else if (n.startsWith(TOOL_PREFIX)) {
                b.setHidden(!tools.contains(n));
            }
        }
    }
}
