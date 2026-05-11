package com.example.mythic_age.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 神力证明物品类
 * 
 * 神力证明是一种特殊的证明文件，代表玩家获得了特定神话背景的神明认可。
 * 玩家可以使用神力证明在神殿中的神像处兑换对应的神力法术书。
 * 
 * 主要功能：
 * - 作为货币系统，用于兑换神力法术书
 * - 区分不同神话背景（希腊、埃及、中国、亚特兰蒂斯）
 * - 显示详细的tooltip信息
 * - 支持堆叠，方便交易和存储
 * 
 * 获取方式：
 * - 神殿守护者村民交易获得
 * - 村民在神殿范围内时自动生成
 * - 完成特定任务或挑战
 */
public class GodPowerCertificateItem extends Item {
    
    /**
     * 神话背景类型枚举
     * 
     * 定义了五种主要的神话文明背景：
     * - GREEK: 希腊神话，以奥林匹斯十二主神为核心
     * - EGYPTIAN: 埃及神话，以太阳神拉和冥神奥西里斯为核心
     * - CHINESE: 中国神话，以道教和民间神话为核心
     * - ATLANTEAN: 亚特兰蒂斯神话，以海洋和古代科技为核心
     * - NORDIC: 北欧神话，以阿萨神族和诸神黄昏为核心
     */
    public enum MythologyType {
        GREEK("希腊", "Greek", Formatting.BLUE, "奥林匹斯众神的认可"),
        EGYPTIAN("埃及", "Egyptian", Formatting.GOLD, "法老王朝的神明祝福"),
        CHINESE("中国", "Chinese", Formatting.RED, "东方仙神的印证"),
        ATLANTEAN("亚特兰蒂斯", "Atlantean", Formatting.AQUA, "古代文明的智慧结晶"),
        NORDIC("北欧", "Nordic", Formatting.DARK_GRAY, "阿萨神族的战士荣耀");
        
        private final String chineseName;
        private final String englishName;
        private final Formatting color;
        private final String description;
        
        MythologyType(String chineseName, String englishName, Formatting color, String description) {
            this.chineseName = chineseName;
            this.englishName = englishName;
            this.color = color;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getEnglishName() { return englishName; }
        public Formatting getColor() { return color; }
        public String getDescription() { return description; }
    }
    
    private final MythologyType mythologyType;
    
    /**
     * 构造函数
     * 
     * @param settings 物品设置
     * @param mythologyType 神话背景类型
     */
    public GodPowerCertificateItem(Settings settings, MythologyType mythologyType) {
        super(settings);
        this.mythologyType = mythologyType;
    }
    
    /**
     * 获取神话背景类型
     * 
     * @return 神话背景类型
     */
    public MythologyType getMythologyType() {
        return mythologyType;
    }
    
    /**
     * 添加物品tooltip信息
     * 
     * 显示神力证明的详细信息，包括：
     * - 神话背景类型和描述
     * - 使用方法说明
     * - 获取来源提示
     * - 兑换价值信息
     */
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        // 神话背景信息
        tooltip.add(Text.translatable("mythic_age.certificate.type", mythologyType.getChineseName())
            .formatted(mythologyType.getColor()));
        
        // 描述信息
        tooltip.add(Text.translatable("mythic_age.certificate.description." + mythologyType.name().toLowerCase())
            .formatted(Formatting.GRAY));
        
        // 空行分隔
        tooltip.add(Text.empty());
        
        // 使用说明
        tooltip.add(Text.translatable("mythic_age.certificate.usage")
            .formatted(Formatting.YELLOW));
        tooltip.add(Text.translatable("mythic_age.certificate.usage.description")
            .formatted(Formatting.GRAY));
        
        // 获取来源
        tooltip.add(Text.translatable("mythic_age.certificate.source")
            .formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("mythic_age.certificate.source.description")
            .formatted(Formatting.GRAY));
        
        // 兑换价值
        tooltip.add(Text.translatable("mythic_age.certificate.value")
            .formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("mythic_age.certificate.value.description")
            .formatted(Formatting.GRAY));
    }
    
    /**
     * 检查两个神力证明是否为相同类型
     * 
     * @param other 另一个神力证明物品
     * @return 是否为相同神话背景类型
     */
    public boolean isSameType(GodPowerCertificateItem other) {
        return this.mythologyType == other.mythologyType;
    }
    
    /**
     * 获取物品的显示名称
     * 
     * 根据神话背景类型返回对应的本地化名称
     */
    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable("item.mythic_age.god_power_certificate_" + mythologyType.name().toLowerCase());
    }
} 