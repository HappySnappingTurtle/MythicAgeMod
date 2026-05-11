package com.example.mythic_age.entity;

import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.item.ModItems;
import com.example.mythic_age.system.CertificateGenerationManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义神殿守护者实体
 * 
 * 这是一个专门为神殿守护者设计的实体，不继承自VillagerEntity，
 * 因此不受Minecraft原版村民交易系统的限制。
 * 
 * 主要功能：
 * - 完全自定义的交易系统
 * - 神力证明生成器
 * - 多文明支持
 * - 自定义GUI交易界面
 * - 神殿范围活动限制
 */
public class CustomTempleGuardianEntity extends MobEntity {
    
    private final GodPowerCertificateItem.MythologyType mythologyType;
    private BlockPos templePos;
    private int certificateGenerationTimer = 0;
    private static final int CERTIFICATE_GENERATION_INTERVAL = 1200;
    private static final int TEMPLE_RANGE = 16;
    
    // 自定义交易系统
    private final Map<ItemStack, ItemStack> tradeOffers = new HashMap<>();
    private final Map<String, Long> tradeCooldowns = new HashMap<>();
    private boolean hasInitializedTrades = false;
    private static final int MYTHIC_CREATURE_TRADE_COOLDOWN = 1200;
    
    /**
     * 构造函数
     */
    public CustomTempleGuardianEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
        this.mythologyType = GodPowerCertificateItem.MythologyType.GREEK;
        this.setCustomName(Text.translatable("entity.mythic_age.custom_temple_guardian_" + mythologyType.name().toLowerCase()));
    }
    
    /**
     * 完整参数构造函数
     */
    public CustomTempleGuardianEntity(EntityType<? extends MobEntity> entityType, World world, 
                                     GodPowerCertificateItem.MythologyType mythologyType, BlockPos templePos) {
        super(entityType, world);
        this.mythologyType = mythologyType != null ? mythologyType : GodPowerCertificateItem.MythologyType.GREEK;
        this.templePos = templePos;
        this.setCustomName(Text.translatable("entity.mythic_age.custom_temple_guardian_" + this.mythologyType.name().toLowerCase()));
    }
    
    /**
     * 设置实体属性
     */
    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5D)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D)
            .add(EntityAttributes.GENERIC_ARMOR, 5.0D);
    }
    
    /**
     * 初始化AI目标
     */
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        // 移除EscapeDangerGoal，因为它需要PathAwareEntity
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        // 移除WanderAroundPointOfInterestGoal，因为它需要PathAwareEntity
        // 移除WanderAroundGoal，因为它需要PathAwareEntity
        this.goalSelector.add(5, new LookAroundGoal(this));
        
        // 移除RevengeGoal，因为它需要PathAwareEntity
    }
    
    /**
     * 每tick更新
     */
    @Override
    public void tick() {
        super.tick();
        
        if (!this.getWorld().isClient && this.isAlive()) {
            // 初始化交易（延迟到tick中确保所有物品都已注册）
            if (!hasInitializedTrades) {
                initializeCustomTrades();
                hasInitializedTrades = true;
            }
            
            // 神力证明生成
            certificateGenerationTimer++;
            if (certificateGenerationTimer >= CERTIFICATE_GENERATION_INTERVAL) {
                generateCertificatesForNearbyVillagers();
                certificateGenerationTimer = 0;
            }
            
            // 范围限制
            if (templePos != null && this.squaredDistanceTo(templePos.getX(), templePos.getY(), templePos.getZ()) > TEMPLE_RANGE * TEMPLE_RANGE) {
                this.getNavigation().startMovingTo(templePos.getX(), templePos.getY(), templePos.getZ(), 1.0D);
            }
        }
    }
    
    /**
     * 玩家交互
     */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        
        // 检查玩家是否已有指南
        if (!hasPlayerGuide(player)) {
            givePlayerGuide(player);
            return ActionResult.SUCCESS;
        }
        
        // 打开GUI交易界面
        openTradeGUI(player);
        return ActionResult.SUCCESS;
    }
    
    /**
     * 打开交易GUI
     */
    private void openTradeGUI(PlayerEntity player) {
        if (this.getWorld().isClient) {
            // 客户端：打开GUI界面
            net.minecraft.client.MinecraftClient.getInstance().setScreen(
                new com.example.mythic_age.client.gui.CustomTempleGuardianTradeScreen(this, player)
            );
        } else {
            // 服务端：发送打开GUI的数据包，包含交易数据
            ServerWorld serverWorld = (ServerWorld) this.getWorld();
            
            // 创建数据包并发送到客户端
            net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
            buf.writeInt(this.getId()); // 写入实体ID
            buf.writeString(this.mythologyType.name()); // 写入文明类型
            
            // 写入交易数据
            buf.writeInt(tradeOffers.size()); // 写入交易数量
            System.out.println("[调试] 准备发送 " + tradeOffers.size() + " 个交易到客户端");
            
            for (Map.Entry<ItemStack, ItemStack> trade : tradeOffers.entrySet()) {
                ItemStack input = trade.getKey();
                ItemStack output = trade.getValue();
                
                // 写入输入物品
                buf.writeItemStack(input);
                // 写入输出物品
                buf.writeItemStack(output);
                
                System.out.println("[调试] 发送交易: " + input.getCount() + "个 " + 
                    input.getItem().getName().getString() + " -> " + 
                    output.getCount() + "个 " + output.getItem().getName().getString());
            }
            
            // 发送数据包到客户端
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                (net.minecraft.server.network.ServerPlayerEntity) player,
                com.example.mythic_age.network.ModNetworking.OPEN_CUSTOM_TEMPLE_GUARDIAN_TRADE_SCREEN,
                buf
            );
            
            // 记录日志
            System.out.println("[调试] 发送打开交易GUI数据包到客户端，玩家: " + player.getName().getString() + 
                ", 实体ID: " + this.getId() + ", 交易数量: " + tradeOffers.size());
        }
    }
    
    /**
     * 初始化自定义交易
     */
    private void initializeCustomTrades() {
        System.out.println("[调试] ===== CustomTempleGuardian 交易初始化开始 =====");
        tradeOffers.clear();
        
        try {
            ItemStack certificate = createCertificate();
            System.out.println("[调试] 文明类型: " + mythologyType);
            System.out.println("[调试] 创建的神力证明: " + certificate.getItem().getName().getString());
            System.out.println("[调试] 神力证明物品: " + certificate.getItem());
            
            // 检查ModItems是否正确初始化
            System.out.println("[调试] 检查ModItems初始化状态:");
            System.out.println("[调试]   SPELLBOOK_RESTORATION: " + ModItems.SPELLBOOK_RESTORATION);
            System.out.println("[调试]   SPELLBOOK_LURE: " + ModItems.SPELLBOOK_LURE);
            System.out.println("[调试]   SPELLBOOK_LIGHTNING_STORM: " + ModItems.SPELLBOOK_LIGHTNING_STORM);
            
            // 只添加文明特定交易（使用神力证明兑换法术书）
            addCivilizationTrades(certificate);
            
            System.out.println("[调试] 交易初始化完成，交易数量: " + tradeOffers.size());
            System.out.println("[调试] 交易Map内容: " + tradeOffers);
            
            // 详细打印每个交易
            int index = 1;
            for (Map.Entry<ItemStack, ItemStack> trade : tradeOffers.entrySet()) {
                ItemStack input = trade.getKey();
                ItemStack output = trade.getValue();
                System.out.println("[调试] 交易" + index + ": " + input.getCount() + "个 " + 
                    input.getItem().getName().getString() + " -> " + 
                    output.getCount() + "个 " + output.getItem().getName().getString());
                index++;
            }
            
        } catch (Exception e) {
            System.out.println("[调试] CustomTempleGuardian 交易初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("[调试] ===== CustomTempleGuardian 交易初始化结束 =====");
    }
    
    /**
     * 添加文明特定交易
     */
    private void addCivilizationTrades(ItemStack certificate) {
        switch (mythologyType) {
            case GREEK -> {
                // 希腊法术书交易
                safeAddTrade(certificate, 2, ModItems.SPELLBOOK_RESTORATION, "复原术");
                safeAddTrade(certificate, 2, ModItems.SPELLBOOK_LURE, "动物招引");
                safeAddTrade(certificate, 3, ModItems.SPELLBOOK_LIGHTNING_STORM, "闪电风暴");
                safeAddTrade(certificate, 3, ModItems.SPELLBOOK_PESTILENCE, "瘟疫");
                safeAddTrade(certificate, 4, ModItems.SPELLBOOK_CURSE, "诅咒");
                safeAddTrade(certificate, 5, ModItems.SPELLBOOK_UNDERWORLD_ARMY, "冥府军团");
                safeAddTrade(certificate, 3, ModItems.SPELLBOOK_CEASEFIRE, "停火");
                safeAddTrade(certificate, 3, ModItems.SPELLBOOK_BRONZE, "青铜护甲");
                safeAddTrade(certificate, 7, ModItems.SPELLBOOK_UNDERWORLD_PASSAGE, "冥界通道");
                safeAddTrade(certificate, 4, ModItems.SPELLBOOK_LIGHTNING_STORM_HERA, "赫拉雷暴");
                safeAddTrade(certificate, 6, ModItems.SPELLBOOK_EARTHQUAKE, "地震");
                safeAddTrade(certificate, 5, ModItems.CENTAUR_SPAWN_EGG, "半人马召唤蛋");
                safeAddTrade(certificate, 6, ModItems.MEDUSA_SPAWN_EGG, "美杜莎召唤蛋");
                safeAddTrade(certificate, 7, ModItems.CYCLOPS_SPAWN_EGG, "独眼巨人召唤蛋");
                safeAddTrade(certificate, 8, ModItems.HYDRA_SPAWN_EGG, "九头蛇召唤蛋");
            }
            case EGYPTIAN -> {
                safeAddTrade(certificate, 3, ModItems.SPELLBOOK_GENERATE_EGYPTIAN_TEMPLE, "埃及神殿生成");
                tradeOffers.put(new ItemStack(Items.GOLD_INGOT, 1), certificate.copy());
                tradeOffers.put(new ItemStack(certificate.getItem(), 2), new ItemStack(Items.GOLDEN_APPLE));
            }
            case CHINESE -> {
                tradeOffers.put(new ItemStack(Items.PAPER, 12), certificate.copy());

                safeAddTrade(certificate, 3, ModItems.SPELLBOOK_GENERATE_CHINESE_TEMPLE, "中国神殿生成");
                safeAddTrade(certificate, 3, ModItems.SPELLBOOK_PLENTY, "丰饶");
                tradeOffers.put(new ItemStack(certificate.getItem(), 1), new ItemStack(Items.COOKED_BEEF, 8));
                tradeOffers.put(new ItemStack(certificate.getItem(), 2), new ItemStack(Items.VILLAGER_SPAWN_EGG));
            }
            case ATLANTEAN -> {
                safeAddTrade(certificate, 3, ModItems.SPELLBOOK_GENERATE_ATLANTEAN_TEMPLE, "亚特兰蒂斯神殿生成");
                tradeOffers.put(new ItemStack(Items.PRISMARINE_SHARD, 16), certificate.copy());
                tradeOffers.put(new ItemStack(certificate.getItem(), 2), new ItemStack(Items.POTION));
                tradeOffers.put(new ItemStack(certificate.getItem(), 1), new ItemStack(Items.SEA_LANTERN, 4));
            }
            case NORDIC -> {
                safeAddTrade(certificate, 3, ModItems.SPELLBOOK_GENERATE_NORDIC_TEMPLE, "北欧神殿生成");
                tradeOffers.put(new ItemStack(certificate.getItem(), 2), new ItemStack(Items.IRON_AXE));
                tradeOffers.put(new ItemStack(certificate.getItem(), 2), new ItemStack(Items.IRON_SWORD));
                tradeOffers.put(new ItemStack(certificate.getItem(), 4), new ItemStack(Items.DIAMOND_SWORD));
                tradeOffers.put(new ItemStack(certificate.getItem(), 3), new ItemStack(Items.IRON_CHESTPLATE));
            }
        }
    }
    
    /**
     * 安全添加交易
     */
    private void safeAddTrade(ItemStack certificate, int count, net.minecraft.item.Item outputItem, String itemName) {
        try {
            if (outputItem != null) {
                tradeOffers.put(new ItemStack(certificate.getItem(), count), new ItemStack(outputItem));
                System.out.println("[调试] 成功添加交易: " + count + "个神力证明 -> " + itemName);
            } else {
                System.out.println("[调试] 警告: " + itemName + " 物品为null");
            }
        } catch (Exception e) {
            System.out.println("[调试] 添加" + itemName + "交易失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示自定义交易界面
     */
    private void showCustomTradeInterface(PlayerEntity player) {
        player.sendMessage(Text.literal("=== " + getCivilizationName() + "神殿守护者 交易列表 ===")
            .formatted(Formatting.GOLD), false);
        
        int index = 1;
        for (Map.Entry<ItemStack, ItemStack> trade : tradeOffers.entrySet()) {
            ItemStack input = trade.getKey();
            ItemStack output = trade.getValue();
            
            player.sendMessage(Text.literal(index + ". " + 
                input.getCount() + "个 " + input.getItem().getName().getString() + 
                " -> " + 
                output.getCount() + "个 " + output.getItem().getName().getString())
                .formatted(Formatting.YELLOW), false);
            index++;
        }
        
        player.sendMessage(Text.literal("在聊天中输入 '/trade <序号>' 来进行交易")
            .formatted(Formatting.GREEN), false);
        player.sendMessage(Text.literal("或者手持对应物品右键点击我进行交易")
            .formatted(Formatting.GREEN), false);
    }
    
    /**
     * 处理物品交换
     */
    public boolean attemptTrade(PlayerEntity player, ItemStack heldItem) {
        for (Map.Entry<ItemStack, ItemStack> trade : tradeOffers.entrySet()) {
            ItemStack requiredInput = trade.getKey();
            ItemStack output = trade.getValue();
            
            if (heldItem.getItem() == requiredInput.getItem() && heldItem.getCount() >= requiredInput.getCount()) {
                return completeTrade(player, heldItem, requiredInput, output);
            }
        }
        
        player.sendMessage(Text.literal("无法进行此交易！请检查交易列表。")
            .formatted(Formatting.RED), false);
        return false;
    }

    public boolean attemptTrade(PlayerEntity player, ItemStack requiredInput, ItemStack expectedOutput) {
        for (Map.Entry<ItemStack, ItemStack> trade : tradeOffers.entrySet()) {
            ItemStack offerInput = trade.getKey();
            ItemStack offerOutput = trade.getValue();

            if (offerInput.getItem() == requiredInput.getItem()
                    && offerInput.getCount() == requiredInput.getCount()
                    && offerOutput.getItem() == expectedOutput.getItem()
                    && offerOutput.getCount() == expectedOutput.getCount()) {
                if (countPlayerItems(player, offerInput) < offerInput.getCount()) {
                    player.sendMessage(Text.literal("交易失败：缺少所需物品。")
                        .formatted(Formatting.RED), false);
                    return false;
                }
                return completeTradeFromInventory(player, offerInput, offerOutput);
            }
        }

        player.sendMessage(Text.literal("交易失败：神殿守护者没有提供此交易。")
            .formatted(Formatting.RED), false);
        return false;
    }

    private int countPlayerItems(PlayerEntity player, ItemStack requiredInput) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == requiredInput.getItem()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void removePlayerItems(PlayerEntity player, ItemStack requiredInput) {
        int remaining = requiredInput.getCount();
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == requiredInput.getItem()) {
                int removed = Math.min(remaining, stack.getCount());
                stack.decrement(removed);
                remaining -= removed;
            }
        }
    }

    private boolean completeTradeFromInventory(PlayerEntity player, ItemStack requiredInput, ItemStack output) {
        if (!canCompleteTrade(player, output)) {
            return false;
        }

        removePlayerItems(player, requiredInput);
        finishTrade(player, output);
        return true;
    }

    private boolean completeTrade(PlayerEntity player, ItemStack paymentStack, ItemStack requiredInput, ItemStack output) {
        if (!canCompleteTrade(player, output)) {
            return false;
        }

        paymentStack.decrement(requiredInput.getCount());
        finishTrade(player, output);
        return true;
    }

    private boolean canCompleteTrade(PlayerEntity player, ItemStack output) {
        String cooldownKey = getCooldownKey(output);
        if (cooldownKey != null) {
            long now = this.getWorld().getTime();
            long readyAt = tradeCooldowns.getOrDefault(cooldownKey, 0L);
            if (now < readyAt) {
                long secondsLeft = Math.max(1L, (readyAt - now + 19L) / 20L);
                player.sendMessage(Text.literal("神话生物召唤仍在冷却中，还需 " + secondsLeft + " 秒。")
                    .formatted(Formatting.RED), false);
                return false;
            }
        }

        return true;
    }

    private void finishTrade(PlayerEntity player, ItemStack output) {
        ItemStack result = output.copy();
        if (!player.getInventory().insertStack(result)) {
            player.dropItem(result, false);
        }

        String cooldownKey = getCooldownKey(output);
        if (cooldownKey != null) {
            tradeCooldowns.put(cooldownKey, this.getWorld().getTime() + MYTHIC_CREATURE_TRADE_COOLDOWN);
        }

        player.sendMessage(Text.literal("交易成功！获得了 " +
            result.getCount() + "个 " + result.getItem().getName().getString())
            .formatted(Formatting.GREEN), false);
    }

    private String getCooldownKey(ItemStack output) {
        if (output.isOf(ModItems.CYCLOPS_SPAWN_EGG)
                || output.isOf(ModItems.CENTAUR_SPAWN_EGG)
                || output.isOf(ModItems.HYDRA_SPAWN_EGG)
                || output.isOf(ModItems.MEDUSA_SPAWN_EGG)) {
            return output.getItem().toString();
        }
        return null;
    }
    
    /**
     * 其他辅助方法
     */
    private ItemStack createCertificate() {
        return switch (mythologyType) {
            case GREEK -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_GREEK, 1);
            case EGYPTIAN -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_EGYPTIAN, 1);
            case CHINESE -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_CHINESE, 1);
            case ATLANTEAN -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_ATLANTEAN, 1);
            case NORDIC -> new ItemStack(ModItems.GOD_POWER_CERTIFICATE_NORDIC, 1);
        };
    }
    
    private boolean hasPlayerGuide(PlayerEntity player) {
        String civilizationKey = this.mythologyType.name().toLowerCase();
        
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.GOD_POWER_GUIDE)) {
                NbtCompound nbt = stack.getNbt();
                if (nbt != null && civilizationKey.equals(nbt.getString("civilization"))) {
                    return true;
                }
            }
            
            if (stack.isOf(Items.WRITTEN_BOOK)) {
                NbtCompound nbt = stack.getNbt();
                if (nbt != null && nbt.contains("title")) {
                    String title = nbt.getString("title");
                    if (title.contains(getCivilizationName() + "神力指南")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private void givePlayerGuide(PlayerEntity player) {
        String welcomeMessage = getWelcomeMessage();
        player.sendMessage(Text.literal(welcomeMessage), false);
        
        ItemStack guide = new ItemStack(ModItems.GOD_POWER_GUIDE);
        NbtCompound guideNbt = guide.getOrCreateNbt();
        guideNbt.putString("civilization", this.mythologyType.name().toLowerCase());
        
        if (!player.getInventory().insertStack(guide)) {
            player.dropItem(guide, false);
        }
        
        player.sendMessage(Text.literal("获得了来自" + getCivilizationName() + "神殿守护者的神力指南！"), false);
    }
    
    private void generateCertificatesForNearbyVillagers() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        if (this.mythologyType != GodPowerCertificateItem.MythologyType.GREEK) {
            return;
        }

        BlockPos sourceTemplePos = templePos != null ? templePos : this.getBlockPos();
        CertificateGenerationManager.generateCertificatesForTemple(
            serverWorld,
            sourceTemplePos,
            GodPowerCertificateItem.MythologyType.GREEK
        );
    }
    
    private String getCivilizationName() {
        return switch (this.mythologyType) {
            case GREEK -> "希腊";
            case EGYPTIAN -> "埃及";
            case CHINESE -> "中国";
            case ATLANTEAN -> "亚特兰蒂斯";
            case NORDIC -> "北欧";
        };
    }
    
    private String getWelcomeMessage() {
        return switch (this.mythologyType) {
            case GREEK -> "欢迎来到希腊神殿！聚集神庙周围的希腊村民即可凝聚希腊神力证明。";
            case EGYPTIAN -> "欢迎来到埃及神殿！法老的力量将庇佑您的旅程。";
            case CHINESE -> "欢迎来到中国神殿！天道酬勤，勤者必得其报。";
            case ATLANTEAN -> "欢迎来到亚特兰蒂斯神殿！深海的秘密等待您来发掘。";
            case NORDIC -> "欢迎来到北欧神殿！勇士的荣耀在战斗中获得！";
        };
    }
    
    /**
     * NBT数据序列化 - 保存实体数据到NBT
     */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        
        System.out.println("[调试] CustomTempleGuardian 开始写入NBT数据");
        
        // 保存基本数据
        nbt.putString("mythologyType", mythologyType.name());
        nbt.putInt("certificateGenerationTimer", certificateGenerationTimer);
        nbt.putBoolean("hasInitializedTrades", hasInitializedTrades);
        
        if (templePos != null) {
            nbt.putInt("templePosX", templePos.getX());
            nbt.putInt("templePosY", templePos.getY());
            nbt.putInt("templePosZ", templePos.getZ());
        }
        
        // 保存交易数据
        if (!tradeOffers.isEmpty()) {
            NbtCompound tradesNbt = new NbtCompound();
            int index = 0;
            
            for (Map.Entry<ItemStack, ItemStack> trade : tradeOffers.entrySet()) {
                NbtCompound tradeNbt = new NbtCompound();
                
                // 保存输入物品
                NbtCompound inputNbt = new NbtCompound();
                trade.getKey().writeNbt(inputNbt);
                tradeNbt.put("input", inputNbt);
                
                // 保存输出物品
                NbtCompound outputNbt = new NbtCompound();
                trade.getValue().writeNbt(outputNbt);
                tradeNbt.put("output", outputNbt);
                
                tradesNbt.put("trade_" + index, tradeNbt);
                index++;
            }
            
            nbt.put("tradeOffers", tradesNbt);
            nbt.putInt("tradeCount", index);
            
            System.out.println("[调试] 保存了 " + index + " 个交易到NBT");
        }

        if (!tradeCooldowns.isEmpty()) {
            NbtCompound cooldownNbt = new NbtCompound();
            for (Map.Entry<String, Long> cooldown : tradeCooldowns.entrySet()) {
                cooldownNbt.putLong(cooldown.getKey(), cooldown.getValue());
            }
            nbt.put("tradeCooldowns", cooldownNbt);
        }
        
        System.out.println("[调试] CustomTempleGuardian NBT写入完成");
    }
    
    /**
     * NBT数据反序列化 - 从NBT恢复实体数据
     */
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        
        System.out.println("[调试] CustomTempleGuardian 开始读取NBT数据");
        
        // 读取基本数据
        certificateGenerationTimer = nbt.getInt("certificateGenerationTimer");
        hasInitializedTrades = nbt.getBoolean("hasInitializedTrades");
        
        if (nbt.contains("templePosX")) {
            templePos = new BlockPos(
                nbt.getInt("templePosX"),
                nbt.getInt("templePosY"),
                nbt.getInt("templePosZ")
            );
        }
        
        // 读取交易数据
        if (nbt.contains("tradeOffers")) {
            tradeOffers.clear();
            NbtCompound tradesNbt = nbt.getCompound("tradeOffers");
            int tradeCount = nbt.getInt("tradeCount");
            
            for (int i = 0; i < tradeCount; i++) {
                String tradeKey = "trade_" + i;
                if (tradesNbt.contains(tradeKey)) {
                    NbtCompound tradeNbt = tradesNbt.getCompound(tradeKey);
                    
                    // 读取输入物品
                    ItemStack input = ItemStack.fromNbt(tradeNbt.getCompound("input"));
                    
                    // 读取输出物品
                    ItemStack output = ItemStack.fromNbt(tradeNbt.getCompound("output"));
                    
                    if (!input.isEmpty() && !output.isEmpty()) {
                        tradeOffers.put(input, output);
                    }
                }
            }
            
            System.out.println("[调试] 从NBT恢复了 " + tradeOffers.size() + " 个交易");
        }

        if (nbt.contains("tradeCooldowns")) {
            tradeCooldowns.clear();
            NbtCompound cooldownNbt = nbt.getCompound("tradeCooldowns");
            for (String key : cooldownNbt.getKeys()) {
                tradeCooldowns.put(key, cooldownNbt.getLong(key));
            }
        }
        
        System.out.println("[调试] CustomTempleGuardian NBT读取完成");
    }
    
    // Getter和Setter方法
    public GodPowerCertificateItem.MythologyType getMythologyType() {
        return mythologyType;
    }
    
    public void setTemplePos(BlockPos templePos) {
        this.templePos = templePos;
    }
    
    public BlockPos getTemplePos() {
        return templePos;
    }
    
    public Map<ItemStack, ItemStack> getTradeOffers() {
        return tradeOffers;
    }
}
