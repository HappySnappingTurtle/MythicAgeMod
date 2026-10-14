package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.content.Lang3;
import io.github.happysnappingturtle.mythicage.content.LangKeys;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Map;

/** 三语语言文件：内容条目名称（来自 {@link Content}）+ 其余键（来自 {@link LangKeys}）。 */
public final class ModLang extends LanguageProvider {
    private final String locale;

    public ModLang(PackOutput output, String locale) {
        super(output, MythicAgeIds.MOD_ID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        // 墙上告示牌等方块的名称键与其物品相同（原版 getDescriptionId 委托给物品），只写一次
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ContentEntry e : Content.entries()) {
            String key = e.block().isPresent() ? e.block().get().get().getDescriptionId()
                    : e.item().orElseThrow().get().getDescriptionId();
            if (seen.add(key)) {
                add(key, e.name().get(locale));
            }
        }
        for (Map.Entry<String, Lang3> k : LangKeys.all().entrySet()) {
            add(k.getKey(), k.getValue().get(locale));
        }
    }
}
