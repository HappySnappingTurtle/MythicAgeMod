package io.github.happysnappingturtle.mythicage.handbook;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * 《神话手札》（T24 设计文档 §6.1、§6.2，D5）：首次进入世界给一本（收据防重复，老存档首次登录也补一本），
 * 丢了用「书 + 羽毛」合成，管理员可 {@code /mythicage handbook give <玩家>} 补发。
 * 书只是查看界面：图鉴进度记在玩家数据里，丢书不丢进度（12 号 §3）。
 */
public final class Handbook {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "handbook");
    public static final String RECEIPT = "handbook:given";

    private Handbook() {
    }

    public static ItemStack stack() {
        return PatchouliAPI.get().getBookStack(ID);
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            giveOnce(p);
        }
    }

    /** 没给过就给一本并记收据；返回这次是否给了。背包满时掉在脚下，仍记收据。 */
    public static boolean giveOnce(ServerPlayer p) {
        if (MythData.get(p).receipts().contains(ReceiptBook.Scope.UNIQUE, RECEIPT)
                || MythData.transact(p, Transactions.Request.unique(RECEIPT)).status() != Transactions.Status.COMMITTED) {
            return false;
        }
        give(p);
        p.sendSystemMessage(Component.translatable("mythic_age.handbook.given"));
        return true;
    }

    static void give(ServerPlayer p) {
        ItemStack book = stack();
        if (!p.getInventory().add(book)) {
            p.drop(book, false);
        }
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mythicage").then(Commands.literal("handbook")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .executes(Handbook::giveCommand)))));
    }

    private static int giveCommand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        give(p);
        c.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.handbook.given", p.getDisplayName()), true);
        return 1;
    }
}
