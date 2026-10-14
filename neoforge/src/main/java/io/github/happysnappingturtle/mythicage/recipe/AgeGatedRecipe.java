package io.github.happysnappingturtle.mythicage.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 时代受限配方 {@code mythic_age:age_gated}（T6 设计文档 §7）：包住一个普通合成配方，
 * 要求合成者在某文明达到某时代。
 * <pre>
 * { "type": "mythic_age:age_gated", "civilization": "greek", "age": "classical",
 *   "recipe": { "type": "minecraft:crafting_shapeless", ... } }
 * </pre>
 * 服务端是权威：客户端上只做普通匹配，不判定时代。
 */
public record AgeGatedRecipe(Civilization civilization, Age age, CraftingRecipe inner) implements CraftingRecipe {

    private static final Codec<Civilization> CIV_CODEC = Codec.STRING.comapFlatMap(
            id -> Civilization.byId(id).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "未知文明：" + id)),
            Civilization::id);
    private static final Codec<Age> AGE_CODEC = Codec.STRING.comapFlatMap(
            id -> Age.byId(id).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "未知时代：" + id)),
            Age::id);
    private static final Codec<CraftingRecipe> INNER_CODEC = Recipe.CODEC.comapFlatMap(
            r -> r instanceof CraftingRecipe c && !(r instanceof AgeGatedRecipe) ? DataResult.success(c)
                    : DataResult.error(() -> "age_gated 只能包住普通合成配方"),
            c -> c);

    public static final MapCodec<AgeGatedRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CIV_CODEC.fieldOf("civilization").forGetter(AgeGatedRecipe::civilization),
            AGE_CODEC.fieldOf("age").forGetter(AgeGatedRecipe::age),
            INNER_CODEC.fieldOf("recipe").forGetter(AgeGatedRecipe::inner)
    ).apply(i, AgeGatedRecipe::new));

    @SuppressWarnings("unchecked")
    public static final StreamCodec<RegistryFriendlyByteBuf, AgeGatedRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(id -> Civilization.byId(id).orElseThrow(), Civilization::id),
            AgeGatedRecipe::civilization,
            ByteBufCodecs.STRING_UTF8.map(id -> Age.byId(id).orElseThrow(), Age::id),
            AgeGatedRecipe::age,
            Recipe.STREAM_CODEC.map(r -> (CraftingRecipe) r, r -> r),
            AgeGatedRecipe::inner,
            AgeGatedRecipe::new);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!inner.matches(input, level)) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        return AgeGate.allows(civilization, age, inner.getResultItem(level.registryAccess()));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return inner.assemble(input, registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return inner.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return inner.getResultItem(registries);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return inner.getRemainingItems(input);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inner.getIngredients();
    }

    @Override
    public CraftingBookCategory category() {
        return inner.category();
    }

    @Override
    public String getGroup() {
        return inner.getGroup();
    }

    @Override
    public boolean showNotification() {
        return inner.showNotification();
    }

    @Override
    public boolean isIncomplete() {
        return inner.isIncomplete();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.AGE_GATED.get();
    }
}
