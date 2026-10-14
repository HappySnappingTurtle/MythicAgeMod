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
        for (ContentEntry e : Content.entries()) {
            if (e.block().isPresent()) {
                add(e.block().get().get(), e.name().get(locale));
            } else {
                add(e.item().get(), e.name().get(locale));
            }
        }
        for (Map.Entry<String, Lang3> k : LangKeys.all().entrySet()) {
            add(k.getKey(), k.getValue().get(locale));
        }
    }
}
