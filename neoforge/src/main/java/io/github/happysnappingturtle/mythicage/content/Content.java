package io.github.happysnappingturtle.mythicage.content;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * 内容声明 DSL（T6 设计文档 §2.2）：每个方块或物品在一处写全注册、模型、战利品、标签与三语名称。
 * datagen、创造模式物品栏与一致性测试都只读 {@link #entries()}。
 * <pre>
 * public static final DeferredBlock&lt;Block&gt; TIN_ORE = Content.block("tin_ore", GREEK)
 *         .properties(() -&gt; BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE))
 *         .texture(Recolor.sprite("minecraft:block/iron_ore", "tin_ore"))
 *         .loot(LootSpec.ore(RAW_TIN))
 *         .tags(...).itemTags(...)
 *         .lang("锡石矿", "Tin Ore", "錫石鉱石")
 *         .register();
 * </pre>
 * 声明顺序就是创造模式物品栏中的顺序。
 */
public final class Content {
    private static final List<ContentEntry> ENTRIES = new ArrayList<>();
    private static final Set<String> IDS = new HashSet<>();

    private Content() {
    }

    public static List<ContentEntry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<ContentEntry> entries(Civilization civ) {
        return ENTRIES.stream().filter(e -> e.civ() == civ).toList();
    }

    public static BlockBuilder<Block> block(String id, Civilization civ) {
        return new BlockBuilder<>(id, civ, Block::new);
    }

    public static <B extends Block> BlockBuilder<B> block(String id, Civilization civ,
                                                          Function<BlockBehaviour.Properties, B> factory) {
        return new BlockBuilder<>(id, civ, factory);
    }

    public static ItemBuilder<Item> item(String id, Civilization civ) {
        return new ItemBuilder<>(id, civ, Item::new);
    }

    public static <I extends Item> ItemBuilder<I> item(String id, Civilization civ,
                                                       Function<Item.Properties, I> factory) {
        return new ItemBuilder<>(id, civ, factory);
    }

    private static void claim(String id) {
        if (!id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("内容 id 只能包含小写字母、数字和下划线：" + id);
        }
        if (!IDS.add(id)) {
            throw new IllegalStateException("重复的内容 id：" + id);
        }
    }

    public static final class BlockBuilder<B extends Block> {
        private final String id;
        private final Civilization civ;
        private final Function<BlockBehaviour.Properties, B> factory;
        private Supplier<BlockBehaviour.Properties> properties;
        private UnaryOperator<Item.Properties> itemProperties = UnaryOperator.identity();
        private BlockModelSpec model = BlockModelSpec.cubeAll();
        private LootSpec loot;
        private final List<TagKey<Block>> blockTags = new ArrayList<>();
        private final List<TagKey<Item>> itemTags = new ArrayList<>();
        private Lang3 name;
        private ResourceLocation texture;
        private ResourceLocation itemTexture;
        private BiFunction<B, Item.Properties, ? extends Item> itemFactory = BlockItem::new;
        private boolean hasItem = true;

        private BlockBuilder(String id, Civilization civ, Function<BlockBehaviour.Properties, B> factory) {
            this.id = id;
            this.civ = civ;
            this.factory = factory;
        }

        public BlockBuilder<B> properties(Supplier<BlockBehaviour.Properties> properties) {
            this.properties = properties;
            return this;
        }

        public BlockBuilder<B> itemProperties(UnaryOperator<Item.Properties> itemProperties) {
            this.itemProperties = itemProperties;
            return this;
        }

        /** 六面贴图（只对 cubeAll 有效），例如 {@code Recolor.sprite("minecraft:block/calcite", "marble")}。 */
        public BlockBuilder<B> texture(ResourceLocation texture) {
            this.texture = texture;
            return this;
        }

        public BlockBuilder<B> model(BlockModelSpec model) {
            this.model = model;
            return this;
        }

        /** 方块物品使用自定义物品类（门、告示牌等），默认 {@link BlockItem}。 */
        public BlockBuilder<B> item(BiFunction<B, Item.Properties, ? extends Item> factory) {
            this.itemFactory = factory;
            return this;
        }

        /** 不注册方块物品（墙上告示牌、盆栽等由其他物品放置的方块）。 */
        public BlockBuilder<B> noItem() {
            this.hasItem = false;
            return this;
        }

        /** 方块物品用平面图标（例如门、树苗）。 */
        public BlockBuilder<B> itemTexture(ResourceLocation itemTexture) {
            this.itemTexture = itemTexture;
            return this;
        }

        /** 不声明时：台阶模型用 {@link LootSpec#slab()}，其他用 {@link LootSpec#self()}。 */
        public BlockBuilder<B> loot(LootSpec loot) {
            this.loot = loot;
            return this;
        }

        @SafeVarargs
        public final BlockBuilder<B> tags(TagKey<Block>... tags) {
            blockTags.addAll(List.of(tags));
            return this;
        }

        @SafeVarargs
        public final BlockBuilder<B> itemTags(TagKey<Item>... tags) {
            itemTags.addAll(List.of(tags));
            return this;
        }

        public BlockBuilder<B> lang(String zh, String en, String ja) {
            this.name = new Lang3(zh, en, ja);
            return this;
        }

        public DeferredBlock<B> register() {
            claim(id);
            if (properties == null) {
                throw new IllegalStateException(id + "：缺少 properties");
            }
            if (name == null) {
                throw new IllegalStateException(id + "：缺少三语名称 lang(...)");
            }
            if (texture != null && !(model instanceof BlockModelSpec.CubeAll)) {
                throw new IllegalStateException(id + "：texture(...) 只对 cubeAll 模型有效");
            }
            if (!hasItem && (!itemTags.isEmpty() || itemTexture != null)) {
                throw new IllegalStateException(id + "：noItem() 的方块不能声明物品标签或物品图标");
            }
            // 形状类模型自动带上原版的形状标签
            switch (model) {
                case BlockModelSpec.Slab s -> addShapeTags(BlockTags.SLABS, ItemTags.SLABS);
                case BlockModelSpec.Stairs s -> addShapeTags(BlockTags.STAIRS, ItemTags.STAIRS);
                case BlockModelSpec.StairsTextured s -> addShapeTags(BlockTags.STAIRS, ItemTags.STAIRS);
                case BlockModelSpec.Wall w -> addShapeTags(BlockTags.WALLS, ItemTags.WALLS);
                default -> {
                }
            }
            LootSpec resolvedLoot = loot != null ? loot
                    : model instanceof BlockModelSpec.Slab ? LootSpec.slab() : LootSpec.self();
            Supplier<BlockBehaviour.Properties> props = properties;
            DeferredBlock<B> block = ModRegistries.BLOCKS.register(id, () -> factory.apply(props.get()));
            Optional<DeferredItem<? extends Item>> item = Optional.empty();
            if (hasItem) {
                BiFunction<B, Item.Properties, ? extends Item> factory = itemFactory;
                UnaryOperator<Item.Properties> itemProps = itemProperties;
                item = Optional.of(ModRegistries.ITEMS.register(id,
                        () -> factory.apply(block.get(), itemProps.apply(new Item.Properties()))));
            }
            ItemModelSpec itemModel = itemTexture != null ? ItemModelSpec.GENERATED : ItemModelSpec.BLOCK;
            ENTRIES.add(new ContentEntry(id, civ, Optional.of(block), item, Optional.of(model), itemModel,
                    Optional.of(resolvedLoot), List.copyOf(blockTags), List.copyOf(itemTags), name,
                    Optional.ofNullable(texture), Optional.ofNullable(itemTexture)));
            return block;
        }

        private void addShapeTags(TagKey<Block> blockTag, TagKey<Item> itemTag) {
            if (!blockTags.contains(blockTag)) {
                blockTags.add(blockTag);
            }
            if (!itemTags.contains(itemTag)) {
                itemTags.add(itemTag);
            }
        }
    }

    public static final class ItemBuilder<I extends Item> {
        private final String id;
        private final Civilization civ;
        private final Function<Item.Properties, I> factory;
        private UnaryOperator<Item.Properties> properties = UnaryOperator.identity();
        private ItemModelSpec model = ItemModelSpec.GENERATED;
        private final List<TagKey<Item>> tags = new ArrayList<>();
        private Lang3 name;
        private ResourceLocation texture;

        private ItemBuilder(String id, Civilization civ, Function<Item.Properties, I> factory) {
            this.id = id;
            this.civ = civ;
            this.factory = factory;
        }

        public ItemBuilder<I> properties(UnaryOperator<Item.Properties> properties) {
            this.properties = properties;
            return this;
        }

        /** 图标贴图，例如 {@code Recolor.sprite("minecraft:item/iron_ingot", "tin_ingot")}。 */
        public ItemBuilder<I> texture(ResourceLocation texture) {
            this.texture = texture;
            return this;
        }

        public ItemBuilder<I> model(ItemModelSpec model) {
            this.model = model;
            return this;
        }

        @SafeVarargs
        public final ItemBuilder<I> tags(TagKey<Item>... tags) {
            this.tags.addAll(List.of(tags));
            return this;
        }

        public ItemBuilder<I> lang(String zh, String en, String ja) {
            this.name = new Lang3(zh, en, ja);
            return this;
        }

        public DeferredItem<I> register() {
            claim(id);
            if (name == null) {
                throw new IllegalStateException(id + "：缺少三语名称 lang(...)");
            }
            UnaryOperator<Item.Properties> props = properties;
            DeferredItem<I> item = ModRegistries.ITEMS.register(id, () -> factory.apply(props.apply(new Item.Properties())));
            ENTRIES.add(new ContentEntry(id, civ, Optional.empty(), Optional.of(item), Optional.empty(), model,
                    Optional.empty(), List.of(), List.copyOf(tags), name, Optional.ofNullable(texture), Optional.empty()));
            return item;
        }
    }
}
