package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.equipment.ShieldAttack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 举盾时攻击的客户端入口（T17 §4.3）：原版在使用物品期间把攻击键的点击直接吞掉，所以在每刻开头、
 * 原版处理按键之前，先把这些点击取走，对准实体时改发 {@link ShieldAttack.Payload}。是否真的出手由服务器判断。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class ShieldAttackClient {
    private ShieldAttackClient() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !ShieldAttack.ready(mc.player)) {
            return;
        }
        boolean clicked = false;
        while (mc.options.keyAttack.consumeClick()) {
            clicked = true;
        }
        if (clicked && mc.hitResult instanceof EntityHitResult hit) {
            PacketDistributor.sendToServer(new ShieldAttack.Payload(hit.getEntity().getId()));
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }
}
