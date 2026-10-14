package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.content.BlockModelSpec;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** 物品模型：平面图标用 item/generated，方块物品继承方块模型（墙用 inventory 模型）。 */
public final class ModItemModels extends ItemModelProvider {
    public ModItemModels(PackOutput output, ExistingFileHelper files) {
        super(output, MythicAgeIds.MOD_ID, files);
    }

    @Override
    protected void registerModels() {
        for (ContentEntry e : Content.entries()) {
            switch (e.itemModel()) {
                case GENERATED -> withExistingParent(e.id(), mcLoc("item/generated"))
                        .texture("layer0", e.texture().orElse(modLoc("item/" + e.id())));
                case HANDHELD -> withExistingParent(e.id(), mcLoc("item/handheld"))
                        .texture("layer0", e.texture().orElse(modLoc("item/" + e.id())));
                case BLOCK -> {
                    BlockModelSpec model = e.model().orElse(null);
                    if (!(model instanceof BlockModelSpec.Manual)) {
                        boolean wall = model instanceof BlockModelSpec.Wall;
                        withExistingParent(e.id(), modLoc("block/" + e.id() + (wall ? "_inventory" : "")));
                    }
                }
                case MANUAL -> {
                }
            }
        }
    }
}
