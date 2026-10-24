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
 * </ul>
 * 贴图：{@code textures/entity/resident/resident_<m|f><服色>.png}（缺失时退回基础贴图）。
 */
public class ResidentGeoModel extends MythGeoModel<ResidentEntity> {
    public static final String MALE = "outfit_male";
    public static final String FEMALE = "outfit_female";
    public static final String CARRY_PREFIX = "carry_";

    public ResidentGeoModel() {
        super("resident", Set.of());
    }

    @Override
    public void setCustomAnimations(ResidentEntity r, long instanceId, AnimationState<ResidentEntity> state) {
        super.setCustomAnimations(r, instanceId, state);
        boolean female = r.female();
        String carry = r.carrying();
        for (GeoBone b : getAnimationProcessor().getRegisteredBones()) {
            String n = b.getName();
            if (n.startsWith(MALE)) {
                b.setHidden(female);
            } else if (n.startsWith(FEMALE)) {
                b.setHidden(!female);
            } else if (n.startsWith(CARRY_PREFIX)) {
                b.setHidden(!n.equals(CARRY_PREFIX + carry));
            }
        }
    }
}
