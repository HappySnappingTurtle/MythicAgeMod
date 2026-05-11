package com.example.mythic_age.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * 神力指南书物品
 * 
 * 使用书的界面来显示神力系统的详细指南信息
 * 根据不同文明类型显示对应的内容
 */
public class GodPowerGuideItem extends Item {

    public GodPowerGuideItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        if (!world.isClient) {
            // 检查物品NBT中是否有文明类型信息
            String civilization = "general";
            if (itemStack.hasNbt() && itemStack.getNbt().contains("civilization")) {
                civilization = itemStack.getNbt().getString("civilization");
            }
            
            // 根据文明类型创建对应的指南书
            ItemStack book = createGuideBook(civilization);
            
            // 替换当前物品为书
            user.setStackInHand(hand, book);
            
            return TypedActionResult.success(book);
        }
        
        return TypedActionResult.success(itemStack);
    }
    
    /**
     * 根据文明类型创建包含对应神力指南内容的书
     */
    private ItemStack createGuideBook(String civilization) {
        ItemStack book = new ItemStack(net.minecraft.item.Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        
        // 根据文明类型设置书的基本信息
        switch (civilization) {
            case "greek" -> {
                nbt.putString("title", "希腊神力指南");
                nbt.putString("author", "希腊神殿守护者");
            }
            case "egyptian" -> {
                nbt.putString("title", "埃及神力指南");
                nbt.putString("author", "埃及神殿守护者");
            }
            case "chinese" -> {
                nbt.putString("title", "中国神力指南");
                nbt.putString("author", "中国神殿守护者");
            }
            case "atlantean" -> {
                nbt.putString("title", "亚特兰蒂斯神力指南");
                nbt.putString("author", "亚特兰蒂斯神殿守护者");
            }
            case "nordic" -> {
                nbt.putString("title", "北欧神力指南");
                nbt.putString("author", "北欧神殿守护者");
            }
            default -> {
                nbt.putString("title", "神力证明系统指南");
                nbt.putString("author", "神殿守护者");
            }
        }
        
        nbt.putInt("generation", 0);
        nbt.putBoolean("resolved", true);
        
        // 创建页面内容
        NbtList pages = new NbtList();
        
        // 根据文明类型添加对应的页面内容
        switch (civilization) {
            case "greek" -> addGreekPages(pages);
            case "egyptian" -> addEgyptianPages(pages);
            case "chinese" -> addChinesePages(pages);
            case "atlantean" -> addAtlanteanPages(pages);
            case "nordic" -> addNordicPages(pages);
            default -> addGeneralPages(pages);
        }
        
        nbt.put("pages", pages);
        return book;
    }
    
    /**
     * 添加希腊文明的指南页面
     */
    private void addGreekPages(NbtList pages) {
        String page1 = """
        {"text":"§6§l希腊神力指南\\n\\n","extra":[
        {"text":"§e欢迎来到希腊神殿！\\n\\n","color":"yellow"},
        {"text":"§f作为智慧与力量的象征，希腊神明将赐予您强大的神力。\\n\\n","color":"white"},
        {"text":"§a主要内容：\\n","color":"green"},
        {"text":"1. 神力证明获取\\n","color":"white"},
        {"text":"2. 法术书兑换\\n","color":"white"},
        {"text":"3. 神殿守护者服务\\n","color":"white"},
        {"text":"4. 神像祭祀\\n","color":"white"}
        ]}""";
        
        String page2 = """
        {"text":"§e§l希腊神力证明获取\\n\\n","extra":[
        {"text":"§b主要来源：\\n","color":"aqua"},
        {"text":"§f• 神庙周围的希腊村民聚集信仰\\n","color":"white"},
        {"text":"§f• 空闲村民会随机前往附近神庙\\n","color":"white"},
        {"text":"§f• 周围村民越多，周期产出越高\\n\\n","color":"white"},
        {"text":"§6特殊提示：\\n","color":"gold"},
        {"text":"§f建好神庙与神像基座后，引导村民聚集即可稳定获得证明。","color":"white"}
        ]}""";
        
        String page3 = """
        {"text":"§e§l希腊法术书价格表\\n\\n","extra":[
        {"text":"§a2个证明：§f复原术、动物招引\\n\\n","color":"green"},
        {"text":"§e3个证明：§f闪电风暴、瘟疫、青铜、停火\\n\\n","color":"yellow"},
        {"text":"§64个证明：§f诅咒、赫拉雷暴\\n\\n","color":"gold"},
        {"text":"§c5个证明：§f冥府军团、丰饶\\n\\n","color":"red"},
        {"text":"§55个证明：§f地震\\n\\n","color":"light_purple"},
        {"text":"§b7个证明：§f冥界通道","color":"aqua"}
        ]}""";
        
        String page4 = """
        {"text":"§e§l希腊神像系统\\n\\n","extra":[
        {"text":"§f在神像前祭祀神力证明，可以获得强大的法术书。\\n\\n","color":"white"},
        {"text":"§a建造方式：\\n","color":"green"},
        {"text":"§f1. 放置神像基座\\n","color":"white"},
        {"text":"§f2. 依次放置腿部、身体、头部\\n","color":"white"},
        {"text":"§f3. 右键点击头部开始祭祀\\n\\n","color":"white"},
        {"text":"§6愿诸神庇佑您的冒险之旅！","color":"gold"}
        ]}""";
        
        pages.add(NbtString.of(page1));
        pages.add(NbtString.of(page2));
        pages.add(NbtString.of(page3));
        pages.add(NbtString.of(page4));
    }
    
    /**
     * 添加埃及文明的指南页面
     */
    private void addEgyptianPages(NbtList pages) {
        String page1 = """
        {"text":"§6§l埃及神力指南\\n\\n","extra":[
        {"text":"§e欢迎来到埃及神殿！\\n\\n","color":"yellow"},
        {"text":"§f在永恒的尼罗河畔，法老的神力将永不衰竭。\\n\\n","color":"white"},
        {"text":"§a主要内容：\\n","color":"green"},
        {"text":"1. 神像自动产出系统\\n","color":"white"},
        {"text":"2. 金锭兑换机制\\n","color":"white"},
        {"text":"3. 神殿守护者交易\\n","color":"white"},
        {"text":"4. 永恒的守护\\n","color":"white"}
        ]}""";
        
        String page2 = """
        {"text":"§e§l埃及神力证明获取\\n\\n","extra":[
        {"text":"§6主要来源：\\n","color":"gold"},
        {"text":"§f• 神像自动产出系统\\n","color":"white"},
        {"text":"§f• 使用金锭激活神像\\n","color":"white"},
        {"text":"§f• 神像会周期性产出证明\\n\\n","color":"white"},
        {"text":"§c重要：\\n","color":"red"},
        {"text":"§f神像等级越高，产出越多！建议优先建造法老神像。","color":"white"}
        ]}""";
        
        String page3 = """
        {"text":"§e§l埃及神像系统\\n\\n","extra":[
        {"text":"§f不同等级的神像有不同的产出效率：\\n\\n","color":"white"},
        {"text":"§7奴隶神像：§f基础产出\\n","color":"gray"},
        {"text":"§f平民神像：§f中等产出\\n","color":"white"},
        {"text":"§e士兵神像：§f良好产出\\n","color":"yellow"},
        {"text":"§6王室神像：§f优秀产出\\n","color":"gold"},
        {"text":"§c法老神像：§f最高产出\\n\\n","color":"red"},
        {"text":"§6合理规划神像布局，获得最大收益！","color":"gold"}
        ]}""";
        
        String page4 = """
        {"text":"§e§l永恒的荣耀\\n\\n","extra":[
        {"text":"§f埃及的神力来源于不朽的信仰和永恒的建筑。\\n\\n","color":"white"},
        {"text":"§6建议：\\n","color":"gold"},
        {"text":"§f• 在沙漠中建立神殿群\\n","color":"white"},
        {"text":"§f• 使用金锭维护神像运转\\n","color":"white"},
        {"text":"§f• 收集足够证明兑换法术\\n\\n","color":"white"},
        {"text":"§6愿拉神的光辉照耀您的道路！","color":"gold"}
        ]}""";
        
        pages.add(NbtString.of(page1));
        pages.add(NbtString.of(page2));
        pages.add(NbtString.of(page3));
        pages.add(NbtString.of(page4));
    }
    
    /**
     * 添加中国文明的指南页面
     */
    private void addChinesePages(NbtList pages) {
        String page1 = """
        {"text":"§c§l中国神力指南\\n\\n","extra":[
        {"text":"§e欢迎来到中国神殿！\\n\\n","color":"yellow"},
        {"text":"§f天地有正气，杂然赋流形。中华神力源于民心所向。\\n\\n","color":"white"},
        {"text":"§a主要内容：\\n","color":"green"},
        {"text":"1. 人口繁荣系统\\n","color":"white"},
        {"text":"2. 自动神力产出\\n","color":"white"},
        {"text":"3. 村庄发展\\n","color":"white"},
        {"text":"4. 天道酬勤\\n","color":"white"}
        ]}""";
        
        String page2 = """
        {"text":"§e§l中国神力证明获取\\n\\n","extra":[
        {"text":"§c主要来源：\\n","color":"red"},
        {"text":"§f• 村庄人口繁荣度\\n","color":"white"},
        {"text":"§f• 人口越多，产出越高\\n","color":"white"},
        {"text":"§f• 每20个村民产出1个证明\\n\\n","color":"white"},
        {"text":"§a策略：\\n","color":"green"},
        {"text":"§f发展村庄人口是获得神力的关键！保护村民，建设家园。","color":"white"}
        ]}""";
        
        String page3 = """
        {"text":"§e§l村庄发展指南\\n\\n","extra":[
        {"text":"§f提升村庄神力产出的方法：\\n\\n","color":"white"},
        {"text":"§a• 建造更多房屋\\n","color":"green"},
        {"text":"§a• 保护村民安全\\n","color":"green"},
        {"text":"§a• 提供充足食物\\n","color":"green"},
        {"text":"§a• 发展贸易往来\\n\\n","color":"green"},
        {"text":"§c记住：民为贵，社稷次之！","color":"red"}
        ]}""";
        
        String page4 = """
        {"text":"§e§l天道酬勤\\n\\n","extra":[
        {"text":"§f中华文明讲究厚德载物，自强不息。\\n\\n","color":"white"},
        {"text":"§c智慧箴言：\\n","color":"red"},
        {"text":"§f• 仁政爱民，神力自然\\n","color":"white"},
        {"text":"§f• 勤劳致富，福泽绵长\\n","color":"white"},
        {"text":"§f• 和谐共处，天下大同\\n\\n","color":"white"},
        {"text":"§c愿您的家园繁荣昌盛！","color":"red"}
        ]}""";
        
        pages.add(NbtString.of(page1));
        pages.add(NbtString.of(page2));
        pages.add(NbtString.of(page3));
        pages.add(NbtString.of(page4));
    }
    
    /**
     * 添加亚特兰蒂斯文明的指南页面
     */
    private void addAtlanteanPages(NbtList pages) {
        String page1 = """
        {"text":"§b§l亚特兰蒂斯神力指南\\n\\n","extra":[
        {"text":"§e欢迎来到亚特兰蒂斯神殿！\\n\\n","color":"yellow"},
        {"text":"§f在深海的秘密中，古老的水晶蕴含着无限力量。\\n\\n","color":"white"},
        {"text":"§a主要内容：\\n","color":"green"},
        {"text":"1. 海洋探险\\n","color":"white"},
        {"text":"2. 水下活动\\n","color":"white"},
        {"text":"3. 水晶能量\\n","color":"white"},
        {"text":"4. 深海之谜\\n","color":"white"}
        ]}""";
        
        String page2 = """
        {"text":"§e§l亚特兰蒂斯神力证明获取\\n\\n","extra":[
        {"text":"§b主要来源：\\n","color":"aqua"},
        {"text":"§f• 海洋探险和深海潜水\\n","color":"white"},
        {"text":"§f• 水下遗迹探索\\n","color":"white"},
        {"text":"§f• 海晶石碎片交易\\n\\n","color":"white"},
        {"text":"§3技巧：\\n","color":"dark_aqua"},
        {"text":"§f装备水下呼吸药剂，探索海底神殿获得更多收益！","color":"white"}
        ]}""";
        
        String page3 = """
        {"text":"§e§l深海探索指南\\n\\n","extra":[
        {"text":"§f亚特兰蒂斯的神力隐藏在深海中：\\n\\n","color":"white"},
        {"text":"§b• 海底神殿\\n","color":"aqua"},
        {"text":"§b• 珊瑚花园\\n","color":"aqua"},
        {"text":"§b• 水晶洞穴\\n","color":"aqua"},
        {"text":"§b• 沉没遗迹\\n\\n","color":"aqua"},
        {"text":"§3准备好氧气瓶，开始您的深海之旅！","color":"dark_aqua"}
        ]}""";
        
        String page4 = """
        {"text":"§e§l水晶的力量\\n\\n","extra":[
        {"text":"§f亚特兰蒂斯文明依靠水晶的能量延续至今。\\n\\n","color":"white"},
        {"text":"§3神秘知识：\\n","color":"dark_aqua"},
        {"text":"§f• 海洋是生命之源\\n","color":"white"},
        {"text":"§f• 水晶连接天地灵气\\n","color":"white"},
        {"text":"§f• 深海蕴含古老智慧\\n\\n","color":"white"},
        {"text":"§3愿海洋之神庇佑您的征程！","color":"dark_aqua"}
        ]}""";
        
        pages.add(NbtString.of(page1));
        pages.add(NbtString.of(page2));
        pages.add(NbtString.of(page3));
        pages.add(NbtString.of(page4));
    }
    
    /**
     * 添加北欧文明的指南页面
     */
    private void addNordicPages(NbtList pages) {
        String page1 = """
        {"text":"§8§l北欧神力指南\\n\\n","extra":[
        {"text":"§e欢迎来到北欧神殿！\\n\\n","color":"yellow"},
        {"text":"§f在冰雪与战火中淬炼的神力，如钢铁般坚韧不摧。\\n\\n","color":"white"},
        {"text":"§a主要内容：\\n","color":"green"},
        {"text":"1. 战斗获得神力\\n","color":"white"},
        {"text":"2. 怪物狩猎\\n","color":"white"},
        {"text":"3. 勇士之路\\n","color":"white"},
        {"text":"4. 英灵殿的召唤\\n","color":"white"}
        ]}""";
        
        String page2 = """
        {"text":"§e§l北欧神力证明获取\\n\\n","extra":[
        {"text":"§8主要来源：\\n","color":"dark_gray"},
        {"text":"§f• 击杀各种敌对怪物\\n","color":"white"},
        {"text":"§f• 击败其他玩家必定掉落\\n","color":"white"},
        {"text":"§f• 不同怪物有不同掉率\\n","color":"white"},
        {"text":"§f• BOSS怪物掉率最高\\n\\n","color":"white"},
        {"text":"§c战士守则：\\n","color":"red"},
        {"text":"§f只有在战斗中才能获得真正的荣耀和神力！","color":"white"}
        ]}""";
        
        String page3 = """
        {"text":"§e§l怪物狩猎指南\\n\\n","extra":[
        {"text":"§f各种怪物的神力掉率：\\n\\n","color":"white"},
        {"text":"§7普通怪物：§f3-5%\\n","color":"gray"},
        {"text":"§e精英怪物：§f8-15%\\n","color":"yellow"},
        {"text":"§c地狱怪物：§f20%\\n","color":"red"},
        {"text":"§5末影龙：§f100% (50个)\\n","color":"light_purple"},
        {"text":"§4凋零：§f100% (30个)\\n\\n","color":"dark_red"},
        {"text":"§8准备好武器，成为真正的战士！","color":"dark_gray"}
        ]}""";
        
        String page4 = """
        {"text":"§e§l英灵殿的荣耀\\n\\n","extra":[
        {"text":"§f北欧勇士相信，只有在战斗中死去的英雄才能进入英灵殿。\\n\\n","color":"white"},
        {"text":"§8战士之道：\\n","color":"dark_gray"},
        {"text":"§f• 勇敢面对任何敌人\\n","color":"white"},
        {"text":"§f• 保护弱小和无辜\\n","color":"white"},
        {"text":"§f• 用钢铁意志战胜困难\\n\\n","color":"white"},
        {"text":"§8愿奥丁的智慧指引您的征途！","color":"dark_gray"}
        ]}""";
        
        pages.add(NbtString.of(page1));
        pages.add(NbtString.of(page2));
        pages.add(NbtString.of(page3));
        pages.add(NbtString.of(page4));
    }
    
    /**
     * 添加通用的指南页面
     */
    private void addGeneralPages(NbtList pages) {
        // 使用原来的通用内容
        String page1 = """
        {"text":"§6§l神力证明系统指南\\n\\n","extra":[
        {"text":"§e目录：\\n","color":"yellow"},
        {"text":"1. 神力证明获取\\n","color":"white"},
        {"text":"2. 法术书兑换\\n","color":"white"},
        {"text":"3. 神殿守护者交易\\n","color":"white"},
        {"text":"4. 神像系统\\n","color":"white"},
        {"text":"5. 圣物系统\\n","color":"white"},
        {"text":"6. 神话村庄\\n","color":"white"},
        {"text":"\\n§7版本：1.0.0\\n作者：神殿守护者","color":"gray"}
        ]}""";
        
        // 添加其他通用页面...（省略）
        pages.add(NbtString.of(page1));
    }
}
