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

import java.util.EnumMap;
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
 * - 埃及神力证明 -> 埃及神明法术书
 * - 中国神力证明 -> 中国神明法术书
 * - 亚特兰蒂斯神力证明 -> 亚特兰蒂斯法术书
 * - 北欧神力证明 -> 北欧神明法术书
 * 
 * 多方块结构：
 * - 中心为神像核心方块
 * - 周围需要特定的装饰方块
 * - 完整结构才能正常工作
 */
public class StatueBlock extends Block {
    
    private final GodPowerCertificateItem.MythologyType mythologyType;
    
    // 兑换价格表 (文明 -> 法术书 -> 神力证明数量)，与 GodPowerGuideScreen 的 1.0 图鉴矩阵保持一致。
    private static final Map<GodPowerCertificateItem.MythologyType, Map<Item, Integer>> EXCHANGE_PRICES = new EnumMap<>(GodPowerCertificateItem.MythologyType.class);
    
    static {
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_RESTORATION, 2);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_LURE, 2);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_LIGHTNING_STORM, 3);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_PESTILENCE, 3);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_BRONZE, 3);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_CEASEFIRE, 3);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_CURSE, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_LIGHTNING_STORM_HERA, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_UNDERWORLD_ARMY, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_PLENTY, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_EARTHQUAKE, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.GREEK, ModItems.SPELLBOOK_UNDERWORLD_PASSAGE, 7);

        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_RA_RADIANCE, 2);
        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_DESERT_STORM, 3);
        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_EYE_OF_HORUS, 3);
        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_SUN_RAIN, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_PROSPERITY, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_ANUBIS_JUDGMENT, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_LOCUST_SWARM, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_TORNADO, 6);
        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_ISIS_RESURRECTION, 7);
        registerExchange(GodPowerCertificateItem.MythologyType.EGYPTIAN, ModItems.SPELLBOOK_METEOR, 8);

        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_RAIN_PRAYER, 2);
        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_EARTH_GOD_BLESSING, 3);
        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_FIVE_THUNDER, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_ENLIGHTENMENT, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_ABUNDANCE, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_CRANE_BLESSING, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_FLOOD, 6);
        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_CALL_TO_ARMS, 6);
        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_TAIJI_SHIELD, 7);
        registerExchange(GodPowerCertificateItem.MythologyType.CHINESE, ModItems.SPELLBOOK_RECREATION, 8);

        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_TIDAL_SURGE, 2);
        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_ABYSSAL_CURRENT, 3);
        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_CRYSTAL_BARRIER, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_PEARL_HARVEST, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_ORICHALCUM_FORGE, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_SIREN_SONG, 6);
        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_LEVIATHAN_CALL, 7);
        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_ABYSSAL_RIFT, 7);
        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_CHRONO_STASIS, 8);
        registerExchange(GodPowerCertificateItem.MythologyType.ATLANTEAN, ModItems.SPELLBOOK_OCEANIC_WRATH, 8);

        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_FENRIR_HOWL, 2);
        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_ODIN_EYE, 3);
        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_THOR_WRATH, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_GREAT_HUNT, 4);
        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_DWARVEN_MINE, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_VALHALLA_CALL, 5);
        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_FROST, 6);
        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_WALKING_WOODS, 6);
        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_RAGNAROK, 7);
        registerExchange(GodPowerCertificateItem.MythologyType.NORDIC, ModItems.SPELLBOOK_FIMBULWINTER, 8);
    }

    private static void registerExchange(GodPowerCertificateItem.MythologyType mythologyType, Item spellbook, int price) {
        EXCHANGE_PRICES.computeIfAbsent(mythologyType, ignored -> new HashMap<>()).put(spellbook, price);
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
        Map<Item, Integer> exchangePrices = getExchangePrices();
        
        // 遍历所有可兑换的法术书
        for (Map.Entry<Item, Integer> entry : exchangePrices.entrySet()) {
            Item spellbook = entry.getKey();
            int price = entry.getValue();
            String spellbookName = spellbook.getName().getString();

            if (handStack.getCount() >= price) {
                player.sendMessage(Text.translatable("mythic_age.statue.exchange_option",
                    spellbookName, price).formatted(Formatting.YELLOW), false);
                hasExchange = true;
            } else {
                player.sendMessage(Text.translatable("mythic_age.statue.exchange_insufficient",
                    spellbookName, price, handStack.getCount()).formatted(Formatting.GRAY), false);
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
        
        player.sendMessage(Text.translatable("mythic_age.statue.menu.civilization_powers", mythologyType.getChineseName())
            .formatted(Formatting.BLUE), false);

        int shown = 0;
        for (Map.Entry<Item, Integer> entry : getExchangePrices().entrySet()) {
            player.sendMessage(Text.translatable("mythic_age.statue.exchange_option",
                entry.getKey().getName().getString(), entry.getValue()).formatted(Formatting.WHITE), false);
            shown++;
            if (shown >= 4) {
                break;
            }
        }
        
        player.sendMessage(Text.translatable("mythic_age.statue.menu.instruction")
            .formatted(Formatting.YELLOW), false);
    }

    private Map<Item, Integer> getExchangePrices() {
        return EXCHANGE_PRICES.getOrDefault(mythologyType, Map.of());
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
