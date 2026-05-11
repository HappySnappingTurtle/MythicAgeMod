package com.example.mythic_age.client.gui;

import com.example.mythic_age.entity.CustomTempleGuardianEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简化的神殿守护者交易界面
 * 参考原版交易界面设计，简洁易用
 */
public class CustomTempleGuardianTradeScreen extends Screen {
    
    // 固定GUI尺寸，参考原版交易界面
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final Identifier TEXTURE = new Identifier("textures/gui/container/generic_54.png");
    
    private final CustomTempleGuardianEntity guardian;
    private final PlayerEntity player;
    private final List<TradeEntry> tradeEntries = new ArrayList<>();
    
    private int guiLeft;
    private int guiTop;
    private int selectedTradeIndex = 0;
    private int scrollOffset = 0;
    
    public CustomTempleGuardianTradeScreen(CustomTempleGuardianEntity guardian, PlayerEntity player) {
        super(Text.literal("神殿守护者交易"));
        this.guardian = guardian;
        this.player = player;
        this.initTradeEntries();
    }
    

    
    /**
     * 初始化交易条目，从守护者实体获取真实交易数据
     */
    private void initTradeEntries() {
        tradeEntries.clear();
        
        System.out.println("[调试] GUI初始化交易条目开始");
        System.out.println("[调试] 守护者实体: " + guardian);
        System.out.println("[调试] 守护者实体是否为null: " + (guardian == null));
        
        // 从守护者实体获取交易数据
        if (guardian != null) {
            System.out.println("[调试] 守护者实体类型: " + guardian.getClass().getSimpleName());
            
            try {
                var tradeOffers = guardian.getTradeOffers();
                System.out.println("[调试] 获取到交易Map: " + tradeOffers);
                System.out.println("[调试] 获取到交易数量: " + tradeOffers.size());
                
                if (tradeOffers.isEmpty()) {
                    System.out.println("[调试] 警告: 交易Map为空，可能交易未初始化");
                } else {
                    for (var entry : tradeOffers.entrySet()) {
                        ItemStack input = entry.getKey();
                        ItemStack output = entry.getValue();
                        
                        System.out.println("[调试] 处理交易条目:");
                        System.out.println("[调试]   输入: " + input.getCount() + "个 " + input.getItem().getName().getString());
                        System.out.println("[调试]   输出: " + output.getCount() + "个 " + output.getItem().getName().getString());
                        
                        tradeEntries.add(new TradeEntry(input.copy(), output.copy()));
                        System.out.println("[调试] 成功添加交易条目");
                    }
                }
            } catch (Exception e) {
                System.out.println("[调试] 获取交易数据时出现异常: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[调试] 错误: 守护者实体为null");
        }
        
        // 如果没有交易数据，添加提示
        if (tradeEntries.isEmpty()) {
            System.out.println("[调试] 警告: 没有找到交易数据，添加默认提示");
            tradeEntries.add(new TradeEntry(
                new ItemStack(Items.BARRIER, 1),
                new ItemStack(Items.BARRIER, 1)
            ));
        }
        
        System.out.println("[调试] GUI交易条目初始化完成，总数: " + tradeEntries.size());
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // 交易按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("交易"), 
            button -> executeSelectedTrade())
            .dimensions(guiLeft + 60, guiTop + 120, 50, 20)
            .build());
        
        // 上一个交易按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("上一个"), 
            button -> {
                if (selectedTradeIndex > 0) {
                    selectedTradeIndex--;
                }
            })
            .dimensions(guiLeft + 10, guiTop + 90, 50, 20)
            .build());
        
        // 下一个交易按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("下一个"), 
            button -> {
                if (selectedTradeIndex < tradeEntries.size() - 1) {
                    selectedTradeIndex++;
                }
            })
            .dimensions(guiLeft + 116, guiTop + 90, 50, 20)
            .build());
        
        // 关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("关闭"), 
            button -> this.close())
            .dimensions(guiLeft + 60, guiTop + 140, 50, 20)
            .build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染背景
        this.renderBackground(context);
        
        // 绘制GUI背景
        context.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010);
        context.drawBorder(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, 0xFFFFFFFF);
        
        // 绘制标题
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.literal("神殿守护者交易")
                .formatted(Formatting.GOLD), 
            guiLeft + GUI_WIDTH / 2, guiTop + 10, 0xFFD700);
        
        // 渲染当前选中的交易
        if (!tradeEntries.isEmpty() && selectedTradeIndex >= 0 && selectedTradeIndex < tradeEntries.size()) {
            TradeEntry trade = tradeEntries.get(selectedTradeIndex);
            
            // 绘制输入物品槽背景
            context.fill(guiLeft + 20, guiTop + 40, guiLeft + 38, guiTop + 58, 0xFF8B4513);
            context.drawBorder(guiLeft + 20, guiTop + 40, 18, 18, 0xFFFFFFFF);
            
            // 绘制输出物品槽背景
            context.fill(guiLeft + 120, guiTop + 40, guiLeft + 138, guiTop + 58, 0xFF8B4513);
            context.drawBorder(guiLeft + 120, guiTop + 40, 18, 18, 0xFFFFFFFF);
            
            // 渲染输入物品
            context.drawItem(trade.input, guiLeft + 21, guiTop + 41);
            context.drawItemInSlot(this.textRenderer, trade.input, guiLeft + 21, guiTop + 41);
            
            // 渲染输出物品
            context.drawItem(trade.output, guiLeft + 121, guiTop + 41);
            context.drawItemInSlot(this.textRenderer, trade.output, guiLeft + 121, guiTop + 41);
            
            // 渲染箭头
            context.drawTextWithShadow(this.textRenderer, 
                Text.literal("→").formatted(Formatting.YELLOW), 
                guiLeft + 70, guiTop + 45, 0xFFFF00);
            
            // 渲染交易信息
            String tradeInfo = String.format("交易 %d/%d", selectedTradeIndex + 1, tradeEntries.size());
            context.drawText(this.textRenderer, tradeInfo, guiLeft + 10, guiTop + 70, 0xFFFFFF, false);
            
            // 渲染物品名称
            String inputName = trade.input.getItem().getName().getString();
            String outputName = trade.output.getItem().getName().getString();
            context.drawText(this.textRenderer, inputName, guiLeft + 10, guiTop + 25, 0xFFFFFF, false);
            context.drawText(this.textRenderer, outputName, guiLeft + 100, guiTop + 25, 0xFFFFFF, false);
        }
        
        // 渲染父类组件
        super.render(context, mouseX, mouseY, delta);
    }
    

    
    /**
     * 执行选中的交易
     */
    private void executeSelectedTrade() {
        if (selectedTradeIndex >= 0 && selectedTradeIndex < tradeEntries.size()) {
            TradeEntry trade = tradeEntries.get(selectedTradeIndex);
            if (this.client != null && this.client.player != null && this.guardian != null) {
                PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                buf.writeInt(this.guardian.getId());
                buf.writeItemStack(trade.input.copy());
                buf.writeItemStack(trade.output.copy());
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    com.example.mythic_age.network.ModNetworking.EXECUTE_CUSTOM_TEMPLE_GUARDIAN_TRADE,
                    buf
                );
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
     

      
     @Override
     public boolean shouldPause() {
         return false;
     }
     
     /**
      * 交易条目内部类
      */
     private static class TradeEntry {
         final ItemStack input;
         final ItemStack output;
         
         TradeEntry(ItemStack input, ItemStack output) {
             this.input = input;
             this.output = output;
         }
     }
}
