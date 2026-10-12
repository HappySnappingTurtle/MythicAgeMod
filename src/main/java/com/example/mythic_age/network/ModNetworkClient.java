package com.example.mythic_age.network;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.gui.CustomTempleGuardianTradeScreen;
import com.example.mythic_age.client.gui.GodPowerGuideScreen;
import com.example.mythic_age.entity.CustomTempleGuardianEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;

/**
 * 神话时代 Mod 客户端网络处理类
 * 
 * 负责处理从服务端接收到的网络数据包，并在客户端执行相应操作。
 * 此类只在客户端环境中被调用。
 */
public class ModNetworkClient {
    
    /**
     * 注册所有客户端网络数据包处理器
     * 在客户端Mod初始化时调用
     */
    public static void registerClientNetworkHandlers() {
        MythicAgeMod.LOGGER.info("Registering client network handlers for Mythic Age Mod");
        
        // 注册打开自定义神殿守护者交易界面的数据包处理器
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_CUSTOM_TEMPLE_GUARDIAN_TRADE_SCREEN, 
            (client, handler, buf, responseSender) -> {
                // 从数据包中读取数据
                int entityId = buf.readInt();
                String mythologyType = buf.readString();
                int tradeCount = buf.readInt();
                
                System.out.println("[调试] 客户端接收到交易GUI数据包: 实体ID=" + entityId + 
                    ", 文明类型=" + mythologyType + ", 交易数量=" + tradeCount);
                
                // 读取交易数据
                java.util.Map<net.minecraft.item.ItemStack, net.minecraft.item.ItemStack> receivedTrades = 
                    new java.util.HashMap<>();
                
                for (int i = 0; i < tradeCount; i++) {
                    net.minecraft.item.ItemStack input = buf.readItemStack();
                    net.minecraft.item.ItemStack output = buf.readItemStack();
                    
                    if (!input.isEmpty() && !output.isEmpty()) {
                        receivedTrades.put(input, output);
                        System.out.println("[调试] 客户端接收交易" + (i+1) + ": " + 
                            input.getCount() + "个 " + input.getItem().getName().getString() + 
                            " -> " + output.getCount() + "个 " + output.getItem().getName().getString());
                    }
                }
                
                // 在游戏线程中执行，确保线程安全
                client.execute(() -> {
                    // 获取实体
                    Entity entity = client.world.getEntityById(entityId);
                    
                    // 确保实体存在且是自定义神殿守护者
                    if (entity instanceof CustomTempleGuardianEntity guardian) {
                        // 将接收到的交易数据设置到守护者实体
                        guardian.getTradeOffers().clear();
                        guardian.getTradeOffers().putAll(receivedTrades);
                        
                        System.out.println("[调试] 客户端更新守护者交易数据，数量: " + guardian.getTradeOffers().size());
                        
                        // 打开交易界面
                        MinecraftClient.getInstance().setScreen(
                            new CustomTempleGuardianTradeScreen(guardian, client.player)
                        );
                    } else {
                        System.out.println("[调试] 警告: 无法找到实体ID " + entityId + " 对应的CustomTempleGuardianEntity");
                    }
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_GOD_POWER_GUIDE_SCREEN,
            (client, handler, buf, responseSender) -> {
                String civilization = buf.readString();
                client.execute(() -> MinecraftClient.getInstance().setScreen(
                        new GodPowerGuideScreen(civilization, client.player)
                ));
            });
    }
}
