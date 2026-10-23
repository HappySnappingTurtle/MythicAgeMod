package io.github.happysnappingturtle.mythicage.relic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.happysnappingturtle.mythicage.core.relic.RelicBinding;
import io.github.happysnappingturtle.mythicage.core.relic.RelicCard;
import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;
import java.util.Optional;

/**
 * 圣物的管理命令（T16 设计文档 §8），权限等级 2：
 * <pre>
 * /mythicage relic give &lt;圣物&gt;          给自己一件（带新的实例 UUID）
 * /mythicage relic where [玩家]          列出绑定表：圣物 → 状态 / 位置
 * /mythicage relic reconcile [玩家]      立即对账
 * </pre>
 */
public final class RelicCommand {
    private RelicCommand() {
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mythicage")
                .then(Commands.literal("relic")
                        .requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("give")
                                .then(Commands.argument("relic", StringArgumentType.greedyString())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(RelicCards.all().keySet(), b))
                                        .executes(RelicCommand::give)))
                        .then(Commands.literal("where")
                                .executes(c -> where(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> where(c, EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("reconcile")
                                .executes(c -> reconcile(c, c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(c -> reconcile(c, EntityArgument.getPlayer(c, "player")))))));
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "relic");
        Optional<RelicCard> card = RelicCards.byId(id);
        if (card.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("commands.mythic_age.relic.unknown", id));
            return 0;
        }
        ItemStack s = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(card.get().item())));
        RelicItems.ensureInstance(s);
        if (!p.getInventory().add(s)) {
            p.drop(s, false);
        }
        return 1;
    }

    private static int where(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        Map<String, RelicBinding> view = MythData.get(p).relics().view();
        if (view.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.relic.none", p.getDisplayName()), false);
        }
        view.forEach((id, b) -> ctx.getSource().sendSuccess(() -> Component.literal(
                id + " → " + b.state().name().toLowerCase(java.util.Locale.ROOT) + " @ " + b.where()), false));
        return view.size();
    }

    private static int reconcile(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        RelicService.reconcile(p);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mythic_age.relic.reconciled", p.getDisplayName()), false);
        return 1;
    }
}
