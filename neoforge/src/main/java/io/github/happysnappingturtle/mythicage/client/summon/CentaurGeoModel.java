package io.github.happysnappingturtle.mythicage.client.summon;

import io.github.happysnappingturtle.mythicage.client.geo.MythGeoModel;
import io.github.happysnappingturtle.mythicage.entity.centaur.AbstractCentaurEntity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Set;

/**
 * 半人马模型（T14 §6.3）：视线的水平偏转加在腰（waist）上——人上身独立转向目标（±90°，超出部分由马身整体转身），
 * 俯仰加在头上。野外版（T15）隐藏豹皮骨骼 pelt。
 */
public class CentaurGeoModel<T extends AbstractCentaurEntity> extends MythGeoModel<T> {
    private final Set<String> hidden;

    public CentaurGeoModel(Set<String> hiddenBones) {
        super("centaur", hiddenBones);
        this.hidden = Set.copyOf(hiddenBones);
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> state) {
        for (String bone : hidden) {
            getBone(bone).ifPresent(b -> b.setHidden(true));
        }
        EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (look != null) {
            float yaw = Mth.clamp(look.netHeadYaw(), -90.0F, 90.0F) * Mth.DEG_TO_RAD;
            getBone("waist").ifPresent(b -> b.setRotY(b.getRotY() + yaw * 0.8F));
            getBone("head").ifPresent(b -> {
                b.setRotY(b.getRotY() + yaw * 0.2F);
                b.setRotX(b.getRotX() + look.headPitch() * Mth.DEG_TO_RAD * 0.6F);
            });
        }
    }
}
