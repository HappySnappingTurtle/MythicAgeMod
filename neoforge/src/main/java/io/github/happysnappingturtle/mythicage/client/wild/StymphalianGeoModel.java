package io.github.happysnappingturtle.mythicage.client.wild;

import io.github.happysnappingturtle.mythicage.client.geo.MythGeoModel;
import io.github.happysnappingturtle.mythicage.entity.stymphalian.StymphalianBirdEntity;
import software.bernie.geckolib.animation.AnimationState;

import java.util.Set;

/** 斯廷法利斯怪鸟模型（制作卡 07）：冠羽骨骼 crest 只在领头鸟身上显示。 */
public class StymphalianGeoModel extends MythGeoModel<StymphalianBirdEntity> {
    public StymphalianGeoModel() {
        super("stymphalian_bird", Set.of());
    }

    @Override
    public void setCustomAnimations(StymphalianBirdEntity animatable, long instanceId, AnimationState<StymphalianBirdEntity> state) {
        super.setCustomAnimations(animatable, instanceId, state);
        getBone("crest").ifPresent(b -> b.setHidden(!animatable.leader()));
    }
}
