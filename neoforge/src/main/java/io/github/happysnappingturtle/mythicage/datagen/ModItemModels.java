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
                case GENERATED -> basicItem(e.item().get());
                case HANDHELD -> withExistingParent(e.id(), mcLoc("item/handheld"))
                        .texture("layer0", modLoc("item/" + e.id()));
                case BLOCK -> {
                    boolean wall = e.model().orElse(null) instanceof BlockModelSpec.Wall;
                    boolean manual = e.model().orElse(null) instanceof BlockModelSpec.Manual;
                    if (!manual) {
                        var model = withExistingParent(e.id(), modLoc("block/" + e.id() + (wall ? "_inventory" : "")));
                        // 物品模型不会继承父模型的 render_type：叠层矿石要显式声明，否则透明像素在物品栏里显示成黑色
                        if (e.model().orElse(null) instanceof BlockModelSpec.OreOverlay) {
                            model.renderType("cutout");
                        }
                    }
                }
                case MANUAL -> {
                }
            }
        }
    }
}
