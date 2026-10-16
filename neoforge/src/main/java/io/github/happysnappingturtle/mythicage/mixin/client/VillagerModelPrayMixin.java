package io.github.happysnappingturtle.mythicage.mixin.client;

import io.github.happysnappingturtle.mythicage.client.ClientBelievers;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 信徒跪拜的姿势（T10 设计文档 §2 D2）：双膝跪地、小腿平放在身后，身体下沉并略向前倾，低头，双手前伸。
 * 只在祈祷中生效。模型实例被所有村民共用，所以每帧先把改动过的部件复位。
 * <p>
 * 模型坐标：y 向下，脚底在 y = 24；躯干的枢轴在脖子处，袍子（jacket）从脖子垂下 20。
 */
@Mixin(VillagerModel.class)
public abstract class VillagerModelPrayMixin {
    /** 身体下沉（模型单位）。 */
    private static final float DROP = 5.0F;
    /** 躯干前倾（弧度）。 */
    private static final float LEAN = 0.2F;
    /** 膝盖枢轴高度：小腿厚 4，平放时中线离地 2。 */
    private static final float KNEE_Y = 22.0F;
    private static final float HEAD_BOW = 0.55F;
    /** 双手前伸：原版交叉的双臂是 −0.75。 */
    private static final float ARMS_REACH = -1.15F;

    @Shadow
    @Final
    private ModelPart root;
    @Shadow
    @Final
    private ModelPart head;
    @Shadow
    @Final
    private ModelPart rightLeg;
    @Shadow
    @Final
    private ModelPart leftLeg;

    @Inject(method = "setupAnim", at = @At("HEAD"))
    private void mythicAge$resetPose(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                     float netHeadYaw, float headPitch, CallbackInfo ci) {
        head.resetPose();
        root.getChild("body").resetPose();
        root.getChild("body").getChild("jacket").resetPose();
        root.getChild("arms").resetPose();
        rightLeg.resetPose();
        leftLeg.resetPose();
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void mythicAge$kneel(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                 float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Villager) || !ClientBelievers.praying(entity)) {
            return;
        }
        ModelPart body = root.getChild("body");
        ModelPart jacket = body.getChild("jacket");
        ModelPart arms = root.getChild("arms");
        float sin = (float) Math.sin(LEAN);
        float cos = (float) Math.cos(LEAN);

        body.y = DROP;
        body.xRot = LEAN;
        // 袍子垂到地面
        jacket.yScale = (24.0F - DROP) / (20.0F * cos);

        head.y = DROP;
        head.xRot = HEAD_BOW;
        head.yRot = 0;
        head.zRot = 0;

        // 双臂随躯干前倾，并向前伸出
        arms.y = DROP + 3.0F * cos;
        arms.z = -1.0F + 3.0F * sin;
        arms.xRot = ARMS_REACH + LEAN;

        // 膝盖在躯干前倾线上、离地 2；小腿向后平放
        float kneeZ = (KNEE_Y - DROP) * (float) Math.tan(LEAN);
        for (ModelPart leg : new ModelPart[]{rightLeg, leftLeg}) {
            leg.y = KNEE_Y;
            leg.z = kneeZ;
            leg.xRot = (float) (Math.PI / 2);
            leg.yRot = 0;
        }
    }
}
