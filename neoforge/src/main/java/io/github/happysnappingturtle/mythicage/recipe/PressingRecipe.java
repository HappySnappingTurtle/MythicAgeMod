package io.github.happysnappingturtle.mythicage.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 压榨配方 {@code mythic_age:pressing}（T6b 设计文档 §5）：一批原料（ingredient × count）加一个容器 → 产物，
 * 用时 time 刻，不用燃料。由数据包驱动。
 * <pre>
 * {"type": "mythic_age:pressing", "ingredient": {...}, "count": 4, "container": {...},
 *  "result": {"id": "mythic_age:olive_oil"}, "time": 40}
 * </pre>
 */
public record PressingRecipe(Ingredient ingredient, int count, Ingredient container, ItemStack result, int time)
        implements Recipe<PressingRecipe.Input> {

    /** 压榨器的输入：原料槽与容器槽。 */
    public record Input(ItemStack ingredient, ItemStack container) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? ingredient : container;
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static final MapCodec<PressingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PressingRecipe::ingredient),
            Codec.intRange(1, 64).fieldOf("count").forGetter(PressingRecipe::count),
            Ingredient.CODEC_NONEMPTY.fieldOf("container").forGetter(PressingRecipe::container),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PressingRecipe::result),
            Codec.intRange(1, 72000).fieldOf("time").forGetter(PressingRecipe::time)
    ).apply(i, PressingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PressingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, PressingRecipe::ingredient,
            ByteBufCodecs.VAR_INT, PressingRecipe::count,
            Ingredient.CONTENTS_STREAM_CODEC, PressingRecipe::container,
            ItemStack.STREAM_CODEC, PressingRecipe::result,
            ByteBufCodecs.VAR_INT, PressingRecipe::time,
            PressingRecipe::new);

    /** 原料与容器都够一批。 */
    @Override
    public boolean matches(Input input, Level level) {
        return ingredient.test(input.ingredient()) && input.ingredient().getCount() >= count
                && container.test(input.container());
    }

    /** 只看原料种类（用于漏斗判断某个物品能不能放进原料槽）。 */
    public boolean acceptsIngredient(ItemStack stack) {
        return ingredient.test(stack);
    }

    public boolean acceptsContainer(ItemStack stack) {
        return container.test(stack);
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PRESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.PRESSING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
