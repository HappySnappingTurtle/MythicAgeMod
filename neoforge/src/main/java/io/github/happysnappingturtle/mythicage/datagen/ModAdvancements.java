package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.advancement.MythEventTrigger;
import io.github.happysnappingturtle.mythicage.advancement.MythEvents;
import io.github.happysnappingturtle.mythicage.content.greek.GreekArchitecture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 进度（T11b 设计文档 §5）：希腊「英雄之路」中已有内容的 6 条。条件都用通用触发器 {@code mythic_age:myth_event}。
 * 标题与说明的语言键 {@code advancements.mythic_age.<路径>.title / .description} 在 LangKeys 里。
 */
public final class ModAdvancements implements AdvancementProvider.AdvancementGenerator {
    private static final Civilization GR = Civilization.GREEK;

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper files) {
        AdvancementHolder root = node(saver, "greek/root", null, GreekTemple.GREEK_ALTAR.get(), AdvancementType.TASK,
                MythEventTrigger.of(MythEvents.VILLAGE_ENTERED, GR), true);
        AdvancementHolder blessed = node(saver, "greek/blessed", root, GreekMaterials.LIBATION.get(), AdvancementType.TASK,
                MythEventTrigger.of(MythEvents.BLESSED, GR), false);
        AdvancementHolder favor = node(saver, "greek/first_favor", blessed, GreekTemple.GREEK_BRAZIER.get(),
                AdvancementType.TASK, MythEventTrigger.of(MythEvents.FAVOR_GAINED, GR), false);
        AdvancementHolder temple = node(saver, "greek/temple", favor, GreekArchitecture.MARBLE_COLUMN.get(),
                AdvancementType.TASK, MythEventTrigger.atLeast(MythEvents.TEMPLE_LEVEL, GR, 1), false);
        AdvancementHolder classical = node(saver, "greek/classical", temple, GreekArchitecture.IONIC_CAPITAL.get(),
                AdvancementType.GOAL, MythEventTrigger.atLeast(MythEvents.AGE_REACHED, GR, Age.CLASSICAL.ordinal()), false);
        node(saver, "greek/heroic", classical, GreekArchitecture.CORINTHIAN_CAPITAL.get(), AdvancementType.GOAL,
                MythEventTrigger.atLeast(MythEvents.AGE_REACHED, GR, Age.HEROIC.ordinal()), false);
        bestiary(saver);
    }

    /**
     * 图鉴的隐藏进度（T24 §6.3）：每个条目三级各一条，无显示、不进进度界面，只给《神话手札》锁页用。
     * 条件是事件 {@code bestiary:<条目>} 的值至少到该级。
     */
    static void bestiary(Consumer<AdvancementHolder> saver) {
        for (String entry : io.github.happysnappingturtle.mythicage.content.greek.GreekBestiary.ENTRIES) {
            for (var tier : java.util.List.of(io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules.Tier.SIGHTED,
                    io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules.Tier.ENGAGED,
                    io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules.Tier.DEFEATED)) {
                Advancement.Builder.advancement()
                        .addCriterion("reached", MythEventTrigger.atLeast("bestiary:" + entry, GR, tier.ordinal()))
                        .save(saver, MythicAgeIds.MOD_ID + ":bestiary/" + entry + "/" + tier.id());
            }
        }
    }

    private static AdvancementHolder node(Consumer<AdvancementHolder> saver, String path, AdvancementHolder parent,
                                          ItemLike icon, AdvancementType type, Criterion<?> criterion, boolean root) {
        String key = "advancements." + MythicAgeIds.MOD_ID + "." + path.replace('/', '.');
        Advancement.Builder b = Advancement.Builder.advancement()
                .display(icon, Component.translatable(key + ".title"), Component.translatable(key + ".description"),
                        root ? ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/block/marble.png") : null,
                        type, !root, !root, false)
                .addCriterion("event", criterion);
        if (parent != null) {
            b.parent(parent);
        }
        return b.save(saver, MythicAgeIds.MOD_ID + ":" + path);
    }
}
