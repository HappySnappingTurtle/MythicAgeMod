package io.github.happysnappingturtle.mythicage.config;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigIssue;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;

/**
 * {@code /mythicage config reload [名称|all]} 与 {@code /mythicage config show <名称>}，需要权限等级 2。
 */
public final class ConfigCommand {
    /** 命令反馈中最多列出的问题条数，其余只计数。 */
    private static final int MAX_LISTED_ISSUES = 10;

    private ConfigCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mythicage")
                .then(Commands.literal("config")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("reload")
                                .executes(ctx -> reload(ctx, "all"))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                new String[]{"all", "runtime", "world_rules", "conduct"}, b))
                                        .executes(ctx -> reload(ctx, StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("show")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                ModConfigs.all().keySet(), b))
                                        .executes(ctx -> show(ctx, StringArgumentType.getString(ctx, "name")))))));
    }

    private static int reload(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        Map<String, ConfigHolder<?>> all = ModConfigs.all();
        if (!name.equals("all") && !all.containsKey(name)) {
            source.sendFailure(unknown(name));
            return 0;
        }
        int ok = 0;
        for (Map.Entry<String, ConfigHolder<?>> entry : all.entrySet()) {
            if (!name.equals("all") && !entry.getKey().equals(name)) {
                continue;
            }
            ConfigHolder<?> holder = entry.getValue();
            ConfigHolder.ReloadOutcome outcome = holder.reloadFile(ModConfigs.file(holder.schema()));
            ModConfigs.logIssues(entry.getKey(), outcome.issues());
            boolean success = outcome.status() == ConfigHolder.Status.APPLIED
                    || outcome.status() == ConfigHolder.Status.UNCHANGED;
            Component summary = Component.translatableWithFallback("commands.mythic_age.config.result",
                    "%s: %s (version %s, %s issue(s)) %s",
                    entry.getKey(), outcome.status().name(), outcome.version(), outcome.issues().size(),
                    outcome.reason());
            if (success) {
                ok++;
                source.sendSuccess(() -> summary, true);
            } else {
                source.sendFailure(summary);
            }
            int listed = 0;
            for (ConfigIssue issue : outcome.issues()) {
                if (listed++ == MAX_LISTED_ISSUES) {
                    source.sendSystemMessage(Component.literal("  ... +" + (outcome.issues().size() - MAX_LISTED_ISSUES)));
                    break;
                }
                source.sendSystemMessage(Component.literal("  " + issue));
            }
        }
        return ok;
    }

    private static int show(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        ConfigHolder<?> holder = ModConfigs.all().get(name);
        if (holder == null) {
            source.sendFailure(unknown(name));
            return 0;
        }
        String json = render(holder);
        source.sendSuccess(() -> Component.translatableWithFallback("commands.mythic_age.config.show",
                "%s (version %s):\n%s", name, holder.version(), json), false);
        return 1;
    }

    private static <T> String render(ConfigHolder<T> holder) {
        return ConfigLoader.toPrettyJson(holder.schema(), holder.current());
    }

    private static Component unknown(String name) {
        return Component.translatableWithFallback("commands.mythic_age.config.unknown",
                "Unknown config '%s'. Valid: all, runtime, world_rules, conduct", name);
    }
}
