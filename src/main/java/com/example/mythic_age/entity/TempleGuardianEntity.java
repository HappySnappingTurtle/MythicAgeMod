package com.example.mythic_age.entity;

import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.item.ModItems;
import com.example.mythic_age.system.GodPowerCertificateManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.village.VillagerType;

/**
 * 神殿守护者实体类
 * 
 * 神殿守护者是一种特殊的村民，具有以下特点：
 * - 外观使用图书管理员模型 (临时)
 * - 在神殿1格范围内时会给附近村民生成神力证明
 * - 可以与玩家交易神力相关物品
 * - 具有独特的AI行为模式
 * - 不会离开神殿太远
 * 
 * 主要功能：
 * - 神力证明生成器：定期为范围内村民生成证明
 * - 神力交易商：提供神力相关物品的交易
 * - 神殿维护者：保持神殿的神圣氛围
 * - 知识传授者：向玩家介绍神话背景知识
 */
public class TempleGuardianEntity extends VillagerEntity {
    
    private final GodPowerCertificateItem.MythologyType mythologyType;
    private BlockPos templePos; // 神殿位置
    private int certificateGenerationTimer = 0; // 神力证明生成计时器
    private static final int CERTIFICATE_GENERATION_INTERVAL = 1200; // 60秒 (20 ticks/秒 * 60)
    private static final int TEMPLE_RANGE = 16; // 神殿影响范围
    private String civilization = "greek"; // 默认希腊文明
    
    /**
     * 构造函数（用于实体工厂）
     * 
     * @param entityType 实体类型
     * @param world 世界
     */
    public TempleGuardianEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
        this.mythologyType = GodPowerCertificateItem.MythologyType.GREEK; // 默认希腊类型
        this.templePos = null;
        // 关键：强制设置为图书管理员职业
        this.setVillagerData(this.getVillagerData().withProfession(VillagerProfession.LIBRARIAN));
        this.setCustomName(Text.translatable("entity.mythic_age.temple_guardian_" + mythologyType.name().toLowerCase()));
        System.out.println("[调试] 构造函数: 设置职业为LIBRARIAN");
        
        // 初始化交易列表
        if (!world.isClient) {
            this.initializeTrades();
            System.out.println("[调试] 构造函数: 初始化交易完成，交易数量: " + this.getOffers().size());
        }
    }
    
    /**
     * 构造函数（完整参数）
     * 
     * @param entityType 实体类型
     * @param world 世界
     * @param mythologyType 神话背景类型
     * @param templePos 神殿位置
     */
    public TempleGuardianEntity(EntityType<? extends VillagerEntity> entityType, World world, 
                               GodPowerCertificateItem.MythologyType mythologyType, BlockPos templePos) {
        super(entityType, world);
        this.mythologyType = mythologyType != null ? mythologyType : GodPowerCertificateItem.MythologyType.GREEK;
        this.templePos = templePos;
        // 关键：强制设置为图书管理员职业
        this.setVillagerData(this.getVillagerData().withProfession(VillagerProfession.LIBRARIAN));
        this.setCustomName(Text.translatable("entity.mythic_age.temple_guardian_" + this.mythologyType.name().toLowerCase()));
        System.out.println("[调试] 构造函数: 设置职业为LIBRARIAN");
        
        // 初始化交易列表
        if (!world.isClient) {
            this.initializeTrades();
            System.out.println("[调试] 构造函数: 初始化交易完成，交易数量: " + this.getOffers().size());
        }
    }
    
    /**
     * 初始化AI目标
     * 
     * 设置神殿守护者的行为模式：
     * - 优先在神殿附近活动
     * - 与玩家交互
     * - 避免危险
     * - 定期巡视神殿
     */
    @Override
    protected void initGoals() {
        // 基础AI目标
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.5D));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(3, new WanderAroundPointOfInterestGoal(this, 0.6F, false));
        this.goalSelector.add(4, new WanderAroundGoal(this, 0.6F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        
        // 防御AI
        this.targetSelector.add(1, new RevengeGoal(this));
    }
    
    /**
     * 每tick更新逻辑
     * 
     * 处理神力证明生成和其他定期任务
     */
    @Override
    public void tick() {
        super.tick();
        
        if (!this.getWorld().isClient && this.isAlive()) {
            // 更新神力证明生成计时器
            certificateGenerationTimer++;
            
            if (certificateGenerationTimer >= CERTIFICATE_GENERATION_INTERVAL) {
                generateCertificatesForNearbyVillagers();
                certificateGenerationTimer = 0;
            }
            
            // 检查是否离神殿太远
            if (templePos != null && this.squaredDistanceTo(templePos.getX(), templePos.getY(), templePos.getZ()) > TEMPLE_RANGE * TEMPLE_RANGE) {
                // 如果离神殿太远，尝试返回
                this.getNavigation().startMovingTo(templePos.getX(), templePos.getY(), templePos.getZ(), 1.0D);
            }
        }
        
        if (!this.getWorld().isClient) {
            if (this.getCustomer() != null) {
                System.out.println("[调试] tick: 当前customer=" + this.getCustomer().getName().getString() +
                    " 职业=" + this.getVillagerData().getProfession() +
                    " 交易数量=" + this.getOffers().size());
            }
        }
    }
    
    /**
     * 为附近的村民生成神力证明
     * 
     * 使用新的阶梯性生成系统，根据希腊村民数量生成证明并存储到神像基座
     */
    private void generateCertificatesForNearbyVillagers() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        
        if (mythologyType != GodPowerCertificateItem.MythologyType.GREEK) {
            return;
        }

        BlockPos sourceTemplePos = templePos != null ? templePos : this.getBlockPos();
        com.example.mythic_age.system.CertificateGenerationManager.generateCertificatesForTemple(
            serverWorld, sourceTemplePos, GodPowerCertificateItem.MythologyType.GREEK);
    }
    
    /**
     * 创建对应类型的神力证明
     * 
     * @return 神力证明物品堆
     */
    private ItemStack createCertificate() {
        switch (mythologyType) {
            case GREEK:
                return new ItemStack(ModItems.GOD_POWER_CERTIFICATE_GREEK, 1);
            case EGYPTIAN:
                return new ItemStack(ModItems.GOD_POWER_CERTIFICATE_EGYPTIAN, 1);
            case CHINESE:
                return new ItemStack(ModItems.GOD_POWER_CERTIFICATE_CHINESE, 1);
            case ATLANTEAN:
                return new ItemStack(ModItems.GOD_POWER_CERTIFICATE_ATLANTEAN, 1);
            case NORDIC:
                return new ItemStack(ModItems.GOD_POWER_CERTIFICATE_NORDIC, 1);
            default:
                return new ItemStack(ModItems.GOD_POWER_CERTIFICATE_GREEK, 1);
        }
    }
    
    /**
     * 玩家与神殿守护者交互
     * 
     * 提供交易界面和信息对话
     */
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (this.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        
        // 确保交易列表已初始化
        if (this.getOffers().isEmpty()) {
            initializeTrades();
        }
        
        // 检查玩家是否已有该文明的神力指南
        boolean hasGuide = hasPlayerGuide(player);
        
        if (!hasGuide) {
            // 第一次交互：给予神力指南
            givePlayerGuide(player);
            return ActionResult.SUCCESS;
        } else {
            // 已有指南，直接打开交易界面
            return openTradingInterface(player);
        }
    }
    
    /**
     * 检查玩家是否已有该文明的神力指南
     */
    private boolean hasPlayerGuide(PlayerEntity player) {
        String civilizationKey = this.mythologyType.name().toLowerCase();
        
        // 检查玩家背包中的神力指南
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.GOD_POWER_GUIDE)) {
                NbtCompound nbt = stack.getNbt();
                if (nbt != null && civilizationKey.equals(nbt.getString("civilization"))) {
                    return true;
                }
            }
        }
        
        // 检查玩家主手和副手
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        
        if (mainHand.isOf(ModItems.GOD_POWER_GUIDE)) {
            NbtCompound nbt = mainHand.getNbt();
            if (nbt != null && civilizationKey.equals(nbt.getString("civilization"))) {
                return true;
            }
        }
        
        if (offHand.isOf(ModItems.GOD_POWER_GUIDE)) {
            NbtCompound nbt = offHand.getNbt();
            if (nbt != null && civilizationKey.equals(nbt.getString("civilization"))) {
                return true;
            }
        }
        
        // 检查玩家是否已经获得过该文明的指南（通过书的形式）
        // 神力指南被使用后会变成书，我们需要检查书的标题
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
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
        
        // 检查主手和副手的书
        if (mainHand.isOf(Items.WRITTEN_BOOK)) {
            NbtCompound nbt = mainHand.getNbt();
            if (nbt != null && nbt.contains("title")) {
                String title = nbt.getString("title");
                if (title.contains(getCivilizationName() + "神力指南")) {
                    return true;
                }
            }
        }
        
        if (offHand.isOf(Items.WRITTEN_BOOK)) {
            NbtCompound nbt = offHand.getNbt();
            if (nbt != null && nbt.contains("title")) {
                String title = nbt.getString("title");
                if (title.contains(getCivilizationName() + "神力指南")) {
                    return true;
                }
            }
        }
        
        // 调试信息：如果没有找到指南，输出调试信息
        if (this.getWorld() instanceof ServerWorld) {
            this.getWorld().getServer().getPlayerManager().getPlayerList().forEach(p -> {
                if (p.equals(player)) {
                    p.sendMessage(Text.literal("§e[调试] 未找到" + civilizationKey + "文明的神力指南"), false);
                }
            });
        }
        
        return false;
    }
    
    /**
     * 给予玩家神力指南
     */
    private void givePlayerGuide(PlayerEntity player) {
        // 发送欢迎消息
        String welcomeMessage = getWelcomeMessage();
        player.sendMessage(Text.literal(welcomeMessage), false);
        
        // 创建该文明的神力指南
        ItemStack guide = new ItemStack(ModItems.GOD_POWER_GUIDE);
        NbtCompound guideNbt = guide.getOrCreateNbt();
        guideNbt.putString("civilization", this.mythologyType.name().toLowerCase());
        
        // 尝试添加到玩家背包，如果背包满了就掉落在地上
        if (!player.getInventory().insertStack(guide)) {
            player.dropItem(guide, false);
        }
        
        player.sendMessage(Text.literal("获得了来自" + getCivilizationName() + "神殿守护者的神力指南！"), false);
    }
    
    /**
     * 初始化交易列表
     */
    private void initializeTrades() {
        TradeOfferList offers = new TradeOfferList();
        
        System.out.println("[调试] initializeTrades开始，当前mythologyType: " + mythologyType);
        
        try {
            ItemStack certificate = createCertificate();
            System.out.println("[调试] 创建神力证明: " + certificate.getItem().toString());

            // 基础交易：钻石 → 绿宝石
            TradeOffer basicTrade = new TradeOffer(
                new ItemStack(Items.DIAMOND, 1),    // 1个钻石
                new ItemStack(Items.EMERALD, 8),    // 8个绿宝石
                12, 10, 0.1f
            );
            offers.add(basicTrade);
            System.out.println("[调试] 添加基础交易: 钻石->绿宝石");
            
            // 根据文明类型添加特殊交易
            addCivilizationSpecificTrades(offers);
            
            // 设置交易列表
            this.setOffers(offers);
            System.out.println("[调试] initializeTrades完成，总交易数: " + offers.size());
            
        } catch (Exception e) {
            System.out.println("[调试] initializeTrades出现异常: " + e.getMessage());
            e.printStackTrace();
            
            // 如果出现异常，至少提供基础交易
            TradeOfferList fallbackOffers = new TradeOfferList();
            fallbackOffers.add(new TradeOffer(
                new ItemStack(Items.EMERALD, 5),
                new ItemStack(Items.BOOK, 1),
                10, 2, 0.05f
            ));
            this.setOffers(fallbackOffers);
        }
    }
    
    /**
     * 根据文明类型添加特殊交易
     */
    private void addCivilizationSpecificTrades(TradeOfferList offers) {
        ItemStack certificate = createCertificate();
        System.out.println("[调试] addCivilizationSpecificTrades开始，文明类型: " + mythologyType);
        
        try {
            switch (mythologyType) {
                case GREEK -> {
                    System.out.println("[调试] 添加希腊文明交易");
                    
                    // 希腊魔法书交易：使用神力证明兑换希腊法术书
                    addSafeTradeOffer(offers, certificate.copyWithCount(2), 
                        ModItems.SPELLBOOK_RESTORATION, "复原术");
                    
                    addSafeTradeOffer(offers, certificate.copyWithCount(2), 
                        ModItems.SPELLBOOK_LURE, "动物招引");
                    
                    addSafeTradeOffer(offers, certificate.copyWithCount(3), 
                        ModItems.SPELLBOOK_LIGHTNING_STORM, "闪电风暴");
                    
                    addSafeTradeOffer(offers, certificate.copyWithCount(3), 
                        ModItems.SPELLBOOK_PESTILENCE, "瘟疫");
                    
                    addSafeTradeOffer(offers, certificate.copyWithCount(4), 
                        ModItems.SPELLBOOK_CURSE, "诅咒");
                    
                    addSafeTradeOffer(offers, certificate.copyWithCount(5), 
                        ModItems.SPELLBOOK_UNDERWORLD_ARMY, "冥府军团");
                    
                    addSafeTradeOffer(offers, certificate.copyWithCount(3), 
                        ModItems.SPELLBOOK_CEASEFIRE, "停火");
                    
                    addSafeTradeOffer(offers, certificate.copyWithCount(3), 
                        ModItems.SPELLBOOK_BRONZE, "青铜护甲");
                    
                    addSafeTradeOffer(offers, certificate.copyWithCount(7), 
                        ModItems.SPELLBOOK_UNDERWORLD_PASSAGE, "冥界通道");
                    
                    addSafeTradeOffer(offers, certificate.copyWithCount(4), 
                        ModItems.SPELLBOOK_LIGHTNING_STORM_HERA, "赫拉雷暴");
                    
                    // 高级法术书（限量）
                    if (Math.random() < 0.3) {
                        addSafeTradeOffer(offers, certificate.copyWithCount(5), 
                            ModItems.SPELLBOOK_EARTHQUAKE, "地震");
                    }
                }
                case EGYPTIAN -> {
                    System.out.println("[调试] 添加埃及文明交易");
                    
                    // 埃及魔法书交易：埃及神殿生成法术书
                    addSafeTradeOffer(offers, certificate.copyWithCount(3), 
                        ModItems.SPELLBOOK_GENERATE_EGYPTIAN_TEMPLE, "埃及神殿生成");
                    
                    // 埃及特色：金锭优惠交易
                    offers.add(new TradeOffer(
                        new ItemStack(Items.GOLD_INGOT, 1),
                        certificate,
                        8, 5, 0.05f
                    ));
                    System.out.println("[调试] 添加埃及特色交易: 金锭->神力证明");
                    
                    // 埃及特色：增强再生效果
                    offers.add(new TradeOffer(
                        certificate.copyWithCount(2),
                        new ItemStack(Items.GOLDEN_APPLE),
                        5, 10, 0.1f
                    ));
                    System.out.println("[调试] 添加埃及特色交易: 神力证明->金苹果");
                }
                case CHINESE -> {
                    System.out.println("[调试] 添加中国文明交易");

                    offers.add(new TradeOffer(
                        new ItemStack(Items.PAPER, 12),
                        certificate,
                        8, 5, 0.05f
                    ));
                    System.out.println("[调试] 添加中国特色交易: 纸->神力证明");
                    
                    // 中国魔法书交易：中国神殿生成法术书
                    addSafeTradeOffer(offers, certificate.copyWithCount(3), 
                        ModItems.SPELLBOOK_GENERATE_CHINESE_TEMPLE, "中国神殿生成");
                    
                    // 中国特色：丰饶法术书（农业相关）
                    addSafeTradeOffer(offers, certificate.copyWithCount(3), 
                        ModItems.SPELLBOOK_PLENTY, "丰饶");
                    
                    // 中国特色：食物和人口相关
                    offers.add(new TradeOffer(
                        certificate.copyWithCount(1),
                        new ItemStack(Items.COOKED_BEEF, 8),
                        12, 5, 0.05f
                    ));
                    System.out.println("[调试] 添加中国特色交易: 神力证明->熟牛肉");
                    
                    offers.add(new TradeOffer(
                        certificate.copyWithCount(2),
                        new ItemStack(Items.VILLAGER_SPAWN_EGG),
                        2, 15, 0.2f
                    ));
                    System.out.println("[调试] 添加中国特色交易: 神力证明->村民刷怪蛋");
                }
                case ATLANTEAN -> {
                    System.out.println("[调试] 添加亚特兰蒂斯文明交易");
                    
                    // 亚特兰蒂斯魔法书交易
                    addSafeTradeOffer(offers, certificate.copyWithCount(3), 
                        ModItems.SPELLBOOK_GENERATE_ATLANTEAN_TEMPLE, "亚特兰蒂斯神殿生成");
                    
                    // 亚特兰蒂斯特色：海洋物品交换
                    offers.add(new TradeOffer(
                        new ItemStack(Items.PRISMARINE_SHARD, 16),
                        certificate,
                        8, 8, 0.1f
                    ));
                    System.out.println("[调试] 添加亚特兰蒂斯特色交易: 海晶碎片->神力证明");
                    
                    // 亚特兰蒂斯特色：水下呼吸药水
                    offers.add(new TradeOffer(
                        certificate.copyWithCount(2),
                        new ItemStack(Items.POTION), // 水下呼吸药水
                        4, 15, 0.15f
                    ));
                    System.out.println("[调试] 添加亚特兰蒂斯特色交易: 神力证明->药水");
                    
                    // 亚特兰蒂斯特色：海洋神殿相关物品
                    offers.add(new TradeOffer(
                        certificate.copyWithCount(1),
                        new ItemStack(Items.SEA_LANTERN, 4),
                        6, 10, 0.1f
                    ));
                    System.out.println("[调试] 添加亚特兰蒂斯特色交易: 神力证明->海灯");
                }
                case NORDIC -> {
                    System.out.println("[调试] 添加北欧文明交易");
                    
                    // 北欧魔法书交易
                    addSafeTradeOffer(offers, certificate.copyWithCount(3), 
                        ModItems.SPELLBOOK_GENERATE_NORDIC_TEMPLE, "北欧神殿生成");
                    
                    // 北欧特色：战斗相关物品
                    offers.add(new TradeOffer(
                        certificate.copyWithCount(2),
                        new ItemStack(Items.IRON_AXE),
                        4, 12, 0.15f
                    ));
                    System.out.println("[调试] 添加北欧特色交易: 神力证明->铁斧");
                    
                    offers.add(new TradeOffer(
                        certificate.copyWithCount(2),
                        new ItemStack(Items.IRON_SWORD),
                        4, 12, 0.15f
                    ));
                    System.out.println("[调试] 添加北欧特色交易: 神力证明->铁剑");
                    
                    // 北欧特色：强化武器
                    offers.add(new TradeOffer(
                        certificate.copyWithCount(4),
                        new ItemStack(Items.DIAMOND_SWORD),
                        2, 20, 0.2f
                    ));
                    System.out.println("[调试] 添加北欧特色交易: 神力证明->钻石剑");
                    
                    // 北欧特色：抗性装备
                    offers.add(new TradeOffer(
                        certificate.copyWithCount(3),
                        new ItemStack(Items.IRON_CHESTPLATE),
                        3, 15, 0.15f
                    ));
                    System.out.println("[调试] 添加北欧特色交易: 神力证明->铁胸甲");
                }
            }
        } catch (Exception e) {
            System.out.println("[调试] addCivilizationSpecificTrades出现异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("[调试] addCivilizationSpecificTrades完成，当前交易总数: " + offers.size());
    }
    
    /**
     * 安全地添加法术书交易
     */
    private void addSafeTradeOffer(TradeOfferList offers, ItemStack input, Item outputItem, String itemName) {
        try {
            if (outputItem != null) {
                TradeOffer offer = new TradeOffer(
                    input,
                    new ItemStack(outputItem),
                    3, 10, 0.1f
                );
                offers.add(offer);
                System.out.println("[调试] 成功添加交易: " + input.getCount() + "个神力证明 -> " + itemName);
            } else {
                System.out.println("[调试] 警告: " + itemName + " 物品为null，跳过添加");
            }
        } catch (Exception e) {
            System.out.println("[调试] 添加" + itemName + "交易时出现异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 打开交易界面
     */
    private ActionResult openTradingInterface(PlayerEntity player) {
        System.out.println("[调试] openTradingInterface开始: customer=" + player.getName().getString());
        
        // 检查是否已经有客户
        if (this.getCustomer() != null && this.getCustomer().equals(player)) {
            System.out.println("[调试] 玩家已经是客户，跳过重复调用");
            return ActionResult.SUCCESS;
        }
        
        // 确保交易列表已初始化并且不为空
        if (this.getOffers().isEmpty()) {
            System.out.println("[调试] 交易列表为空，初始化交易");
            initializeTrades();
        }
        
        // 再次检查交易列表
        if (this.getOffers().isEmpty()) {
            System.out.println("[调试] 交易列表仍然为空，初始化失败");
            player.sendMessage(Text.literal("交易系统暂时不可用，请稍后再试。"), true);
            return ActionResult.FAIL;
        }
        
        // 发送欢迎消息
        String welcomeMessage = getWelcomeMessage();
        player.sendMessage(Text.literal(welcomeMessage), false);
        
        // 设置村民状态，确保可以交易
        this.setCustomer(player);
        
        // 确保村民数据正确，使用图书管理员职业
        this.setVillagerData(this.getVillagerData().withProfession(VillagerProfession.LIBRARIAN));
        
        System.out.println("[调试] openTradingInterface: customer=" + player.getName().getString() +
            " 职业=" + this.getVillagerData().getProfession() +
            " 交易数量=" + this.getOffers().size());
        
        // 发送交易offer到客户端
        if (player instanceof ServerPlayerEntity serverPlayer) {
            try {
                // 确保交易界面正确打开
                this.sendOffers(serverPlayer, this.getDisplayName(), 1);
                
                // 调试信息
                player.sendMessage(Text.literal("§a[调试] 交易界面已打开，交易数量: " + this.getOffers().size()), false);
                
                System.out.println("[调试] 交易界面打开成功");
                // 返回CONSUME确保交互被正确处理
                return ActionResult.CONSUME;
            } catch (Exception e) {
                System.out.println("[调试] 交易界面打开失败: " + e.getMessage());
                e.printStackTrace();
                return ActionResult.FAIL;
            }
        }
        
        // 客户端返回SUCCESS
        return ActionResult.SUCCESS;
    }
    
    /**
     * 获取文明名称
     */
    private String getCivilizationName() {
        return switch (this.mythologyType) {
            case GREEK -> "希腊";
            case EGYPTIAN -> "埃及";
            case CHINESE -> "中国";
            case ATLANTEAN -> "亚特兰蒂斯";
            case NORDIC -> "北欧";
        };
    }
    
    /**
     * 获取欢迎消息
     */
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
     * 设置守护者的文明类型
     */
    public void setCivilization(String civilization) {
        this.civilization = civilization;
    }
    
    public String getCivilization() {
        return civilization;
    }
    
    /**
     * 获取神话背景类型
     * 
     * @return 神话背景类型
     */
    public GodPowerCertificateItem.MythologyType getMythologyType() {
        return mythologyType;
    }
    
    /**
     * 设置神殿位置
     * 
     * @param templePos 神殿位置
     */
    public void setTemplePos(BlockPos templePos) {
        this.templePos = templePos;
    }
    
    public BlockPos getTemplePos() {
        return templePos;
    }
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        // 确保初始化时设置为图书管理员
        this.setVillagerData(this.getVillagerData().withProfession(VillagerProfession.LIBRARIAN));
    }
    
    @Override
    public VillagerData getVillagerData() {
        // 强制使用图书管理员职业
        return super.getVillagerData().withProfession(VillagerProfession.LIBRARIAN);
    }
    
    @Override
    public void setCustomer(PlayerEntity customer) {
        // 防止重复设置相同的客户
        if (this.getCustomer() == customer) {
            System.out.println("[调试] setCustomer: 跳过重复设置相同客户");
            return;
        }
        
        super.setCustomer(customer);
        System.out.println("[调试] setCustomer: customer=" + (customer != null ? customer.getName().getString() : "null") +
            " 职业=" + this.getVillagerData().getProfession());
    }
}
