package io.github.happysnappingturtle.mythicage.client.equipment;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.content.greek.equipment.GreekEquipment;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.equipment.AttackStyle;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** 文明装备的客户端扩展（T17 §8.2）：武器的手持姿势、第一人称攻击动作、复合弓的第一人称拉弓；青铜甲的 3D 模型。 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class WeaponClientExtensions {
    private WeaponClientExtensions() {
    }

    /** 按攻击方式标签给出手持姿势与第一人称动作；各文明的武器通用。 */
    public static final IClientItemExtensions STYLED = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            if (entity.isUsingItem() && entity.getUsedItemHand() == hand) {
                return null;
            }
            return AttackStyle.of(stack).map(s -> switch (s) {
                case STAB -> WeaponPoses.stab();
                case THRUST -> WeaponPoses.thrust();
                case SMASH -> WeaponPoses.smash();
            }).orElse(null);
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                                               float partialTick, float equipProcess, float swingProcess) {
            return WeaponAnimations.firstPerson(poseStack, player, arm, itemInHand, partialTick, equipProcess, swingProcess);
        }
    };

    /** 青铜甲：换成带 3D 部件的模型。 */
    public static final IClientItemExtensions BRONZE_ARMOR = new IClientItemExtensions() {
        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot,
                                                      HumanoidModel<?> original) {
            return BronzeArmorModel.forSlot(slot);
        }
    };

    @SubscribeEvent
    static void onRegister(RegisterClientExtensionsEvent event) {
        event.registerItem(BRONZE_ARMOR, GreekEquipment.BRONZE_HELMET.get(), GreekEquipment.BRONZE_CHESTPLATE.get(),
                GreekEquipment.BRONZE_LEGGINGS.get(), GreekEquipment.BRONZE_BOOTS.get());
        event.registerItem(STYLED, GreekEquipment.XIPHOS.get(), GreekEquipment.DORY.get(), GreekEquipment.OLIVE_CLUB.get(),
                GreekEquipment.COMPOSITE_BOW.get());
    }
}
