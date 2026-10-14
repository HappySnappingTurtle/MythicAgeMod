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
            if (e.item().isEmpty()) {
                continue;
            }
            switch (e.itemModel()) {
                case GENERATED -> withExistingParent(e.id(), mcLoc("item/generated"))
                        .texture("layer0", e.itemTexture().or(e::texture).orElse(modLoc("item/" + e.id())));
                case HANDHELD -> withExistingParent(e.id(), mcLoc("item/handheld"))
                        .texture("layer0", e.texture().orElse(modLoc("item/" + e.id())));
                case BLOCK -> {
                    String parent = switch (e.model().orElseThrow()) {
                        case BlockModelSpec.Wall w -> e.id() + "_inventory";
                        case BlockModelSpec.Fence f -> e.id() + "_inventory";
                        case BlockModelSpec.Button b -> e.id() + "_inventory";
                        case BlockModelSpec.Trapdoor t -> e.id() + "_bottom";
                        case BlockModelSpec.Leaves l -> e.id();
                        default -> e.id();
                    };
                    withExistingParent(e.id(), modLoc("block/" + parent));
                }
                case MANUAL -> {
                }
            }
        }
    }
}
