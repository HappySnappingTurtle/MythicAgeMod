package com.example.mythic_age.block;

import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.item.ModItems;
import com.example.mythic_age.item.SpellbookItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * 神像方块类
 * 
 * 神像是神殿中的核心多方块结构，具有以下功能：
 * - 接受神力证明兑换神力法术书
 * - 根据神话背景类型提供不同的兑换选项
 * - 显示可用的兑换列表和价格
 * - 提供神圣的视觉效果
 * 
 * 兑换系统：
 * - 希腊神力证明 -> 希腊神明法术书
 * - 埃及神力证明 -> 埃及神明法术书 (待实现)
 * - 中国神力证明 -> 中国神明法术书 (待实现)
 * - 亚特兰蒂斯神力证明 -> 亚特兰蒂斯法术书 (待实现)
 * 
 * 多方块结构：
 * - 中心为神像核心方块
 * - 周围需要特定的装饰方块
 * - 完整结构才能正常工作
 */
public class StatueBlock extends Block {
    
    private final GodPowerCertificateItem.MythologyType mythologyType;
    
    // 兑换价格表 (神力证明数量 -> 法术书)
    private static final Map<Item, Integer> EXCHANGE_PRICES = new HashMap<>();
    
    static {
        // 希腊神明法术书兑换价格
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_LIGHTNING_STORM, 3);      // 闪电风暴 - 3个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_RESTORATION, 2);          // 复原术 - 2个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_LURE, 2);                 // 动物招引 - 2个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_UNDERWORLD_ARMY, 5);      // 冥府军团 - 5个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_PESTILENCE, 3);           // 瘟疫 - 3个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_CURSE, 4);                // 诅咒 - 4个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_EARTHQUAKE, 6);           // 地震 - 6个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_CEASEFIRE, 3);            // 停火 - 3个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_BRONZE, 3);               // 青铜 - 3个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_UNDERWORLD_PASSAGE, 7);   // 冥界通道 - 7个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_LIGHTNING_STORM_HERA, 4); // 赫拉雷暴 - 4个证明
        EXCHANGE_PRICES.put(ModItems.SPELLBOOK_PLENTY, 5);               // 丰饶 - 5个证明
    }
    
    /**
     * 构造函数
     * 
     * @param settings 方块设置
     * @param mythologyType 神话背景类型
     */
    public StatueBlock(Settings settings, GodPowerCertificateItem.MythologyType mythologyType) {
        super(settings);
        this.mythologyType = mythologyType;
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
     * 玩家右键点击神像时的处理
     * 
     * 根据玩家手持的物品执行不同的操作：
     * - 手持神力证明：尝试兑换法术书
     * - 空手：显示兑换菜单
     */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        
        ItemStack handStack = player.getStackInHand(hand);
        
        // 检查多方块结构是否完整
        if (!isStructureComplete(world, pos)) {
            player.sendMessage(Text.translatable("mythic_age.statue.structure_incomplete")
                .formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        }
        
        // 如果手持神力证明，尝试兑换
        if (handStack.getItem() instanceof GodPowerCertificateItem certificate) {
            if (certificate.getMythologyType() == this.mythologyType) {
                return handleExchange(world, player, hand, certificate, handStack);
            } else {
                player.sendMessage(Text.translatable("mythic_age.statue.wrong_certificate_type", 
                    certificate.getMythologyType().getChineseName(), mythologyType.getChineseName())
                    .formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            }
        }
        
        // 空手点击显示兑换菜单
        showExchangeMenu(player);
        return ActionResult.SUCCESS;
    }
    
    /**
     * 处理神力证明兑换法术书
     * 
     * @param world 世界
     * @param player 玩家
     * @param hand 使用的手
     * @param certificate 神力证明物品
     * @param handStack 手持物品堆
     * @return 操作结果
     */
    private ActionResult handleExchange(World world, PlayerEntity player, Hand hand, 
                                      GodPowerCertificateItem certificate, ItemStack handStack) {
        
        // 显示可兑换的法术书列表
        player.sendMessage(Text.translatable("mythic_age.statue.exchange_available")
            .formatted(Formatting.GREEN), false);
        
        boolean hasExchange = false;
        
        // 遍历所有可兑换的法术书
        for (Map.Entry<Item, Integer> entry : EXCHANGE_PRICES.entrySet()) {
            Item spellbook = entry.getKey();
            int price = entry.getValue();
            
            // 只显示希腊神明的法术书 (目前只实现了希腊系统)
            if (mythologyType == GodPowerCertificateItem.MythologyType.GREEK) {
                String spellbookName = spellbook.getName().getString();
                
                if (handStack.getCount() >= price) {
                    // 可以兑换
                    player.sendMessage(Text.translatable("mythic_age.statue.exchange_option", 
                        spellbookName, price).formatted(Formatting.YELLOW), false);
                    hasExchange = true;
                } else {
                    // 证明不足
                    player.sendMessage(Text.translatable("mythic_age.statue.exchange_insufficient", 
                        spellbookName, price, handStack.getCount()).formatted(Formatting.GRAY), false);
                }
            }
        }
        
        if (!hasExchange) {
            player.sendMessage(Text.translatable("mythic_age.statue.no_exchange_available")
                .formatted(Formatting.RED), false);
        } else {
            player.sendMessage(Text.translatable("mythic_age.statue.exchange_hint")
                .formatted(Formatting.AQUA), false);
        }
        
        return ActionResult.SUCCESS;
    }
    
    /**
     * 显示兑换菜单
     * 
     * @param player 玩家
     */
    private void showExchangeMenu(PlayerEntity player) {
        player.sendMessage(Text.translatable("mythic_age.statue.welcome", mythologyType.getChineseName())
            .formatted(mythologyType.getColor()), false);
        
        player.sendMessage(Text.translatable("mythic_age.statue.menu.title")
            .formatted(Formatting.GOLD), false);
        
        // 显示当前可用的兑换选项
        if (mythologyType == GodPowerCertificateItem.MythologyType.GREEK) {
            player.sendMessage(Text.translatable("mythic_age.statue.menu.greek_powers")
                .formatted(Formatting.BLUE), false);
            
            // 显示部分法术书作为示例
            player.sendMessage(Text.literal("- 闪电风暴法术书: 3个希腊神力证明")
                .formatted(Formatting.WHITE), false);
            player.sendMessage(Text.literal("- 复原术法术书: 2个希腊神力证明")
                .formatted(Formatting.WHITE), false);
            player.sendMessage(Text.literal("- 地震法术书: 6个希腊神力证明")
                .formatted(Formatting.WHITE), false);
        } else {
            player.sendMessage(Text.translatable("mythic_age.statue.menu.coming_soon")
                .formatted(Formatting.GRAY), false);
        }
        
        player.sendMessage(Text.translatable("mythic_age.statue.menu.instruction")
            .formatted(Formatting.YELLOW), false);
    }
    
    /**
     * 检查神像多方块结构是否完整
     * 
     * 简化版本的结构检查，实际实现中应该检查周围的装饰方块
     * 
     * @param world 世界
     * @param pos 神像位置
     * @return 结构是否完整
     */
    private boolean isStructureComplete(World world, BlockPos pos) {
        // 简化实现：检查神像下方是否有基座
        BlockPos basePos = pos.down();
        BlockState baseState = world.getBlockState(basePos);
        
        // 这里应该检查更复杂的多方块结构
        // 目前简单检查下方是否为固体方块
        return baseState.isSolidBlock(world, basePos);
    }
} 