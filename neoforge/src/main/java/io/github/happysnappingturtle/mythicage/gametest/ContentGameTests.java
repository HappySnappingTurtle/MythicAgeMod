package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.content.LootSpec;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.recipe.AgeGate;
import io.github.happysnappingturtle.mythicage.recipe.CraftingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * T6 内容注册、掉落与配方的集成测试（T6 测试用例文档 §3）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContentGameTests {
    private static final String BATCH = "content";
    /** 会热更 world_rules.json 的用例单独成批。 */
    private static final String CONFIG_BATCH = "content_config";

    private ContentGameTests() {
    }

    /** TC-T6-20：每个声明的条目都已注册，有英文名称，方块有战利品表。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void entries_complete(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(!Content.entries().isEmpty(), "Content 不应为空");
        for (ContentEntry e : Content.entries()) {
            helper.assertTrue(e.item().isBound(), e.id() + " 物品未注册");
            String key = e.block().map(b -> b.get().getDescriptionId()).orElse(e.item().get().getDescriptionId());
            helper.assertTrue(Language.getInstance().has(key), e.id() + " 缺少英文名称 " + key);
            helper.assertTrue(!e.name().zh().isBlank() && !e.name().ja().isBlank(), e.id() + " 三语名称不完整");
            if (e.block().isPresent() && !(e.loot().orElseThrow() instanceof LootSpec.None)) {
                LootTable table = level.getServer().reloadableRegistries()
                        .getLootTable(e.block().get().get().getLootTable());
                helper.assertTrue(table != LootTable.EMPTY, e.id() + " 缺少战利品表");
            }
        }
        helper.succeed();
    }

    /** TC-T6-21：本模组注册的方块和物品都来自 Content（没有绕过 DSL 的注册）。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void no_registration_outside_content(GameTestHelper helper) {
        Set<String> declared = new HashSet<>();
        Content.entries().forEach(e -> declared.add(e.id()));
        List<String> stray = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if (id.getNamespace().equals(MythicAgeIds.MOD_ID) && !declared.contains(id.getPath())) {
                stray.add("item " + id);
            }
        }
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            if (id.getNamespace().equals(MythicAgeIds.MOD_ID) && !declared.contains(id.getPath())) {
                stray.add("block " + id);
            }
        }
        helper.assertTrue(stray.isEmpty(), "未经 Content 声明的注册：" + stray);
        helper.succeed();
    }

    private static List<ItemStack> drops(ServerLevel level, BlockState state, ItemStack tool) {
        return state.getDrops(new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.TOOL, tool));
    }

    private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** TC-T6-22 至 TC-T6-25：锡石矿的工具要求、普通掉落、精准采集与时运。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void tin_ore_drops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState ore = GreekMaterials.TIN_ORE.get().defaultBlockState();
        ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
        helper.assertTrue(iron.isCorrectToolForDrops(ore), "铁镐应能采集锡石矿");
        helper.assertTrue(!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ore), "石镐不应能采集锡石矿");

        List<ItemStack> plain = drops(level, ore, iron);
        helper.assertTrue(count(plain, GreekMaterials.RAW_TIN.get()) == 1 && plain.size() == 1,
                "铁镐应掉 1 个粗锡，实际 " + plain);

        var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> silkDrops = drops(level, ore, silk);
        helper.assertTrue(count(silkDrops, GreekMaterials.TIN_ORE.get().asItem()) == 1,
                "精准采集应掉锡石矿本体，实际 " + silkDrops);

        ItemStack fortune = new ItemStack(Items.IRON_PICKAXE);
        fortune.enchant(enchantments.getHolderOrThrow(Enchantments.FORTUNE), 3);
        int total = 0;
        int max = 0;
        for (int i = 0; i < 100; i++) {
            int n = count(drops(level, ore, fortune), GreekMaterials.RAW_TIN.get());
            total += n;
            max = Math.max(max, n);
        }
        helper.assertTrue(total > 100, "时运 III 平均掉落应大于 1，实际总数 " + total);
        helper.assertTrue(max <= 4, "时运 III 单次最多 4 个，实际 " + max);
        helper.succeed();
    }

    /** TC-T6-26：熔炉与高炉配方。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void smelting_recipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var rm = level.getServer().getRecipeManager();
        record Case(net.minecraft.world.item.Item in, net.minecraft.world.item.Item out) {
        }
        for (Case c : List.of(new Case(GreekMaterials.RAW_TIN.get(), GreekMaterials.TIN_INGOT.get()),
                new Case(GreekMaterials.BRONZE_MIX.get(), GreekMaterials.BRONZE_INGOT.get()))) {
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(c.in()));
            for (var type : List.of(RecipeType.SMELTING, RecipeType.BLASTING)) {
                var r = rm.getRecipeFor(type, input, level);
                helper.assertTrue(r.isPresent() && r.get().value().getResultItem(level.registryAccess()).is(c.out()),
                        c.in() + " 在 " + type + " 中应得到 " + c.out());
            }
        }
        helper.succeed();
    }

    /** TC-T6-31：标签。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void tags(GameTestHelper helper) {
        BlockState ore = GreekMaterials.TIN_ORE.get().defaultBlockState();
        helper.assertTrue(ore.is(ModTags.cBlock("ores/tin")), "锡石矿应在 c:ores/tin");
        helper.assertTrue(ore.is(BlockTags.MINEABLE_WITH_PICKAXE) && ore.is(BlockTags.NEEDS_IRON_TOOL), "锡石矿工具标签");
        helper.assertTrue(new ItemStack(GreekMaterials.TIN_INGOT.get()).is(ModTags.cItem("ingots/tin")), "锡锭应在 c:ingots/tin");
        helper.assertTrue(new ItemStack(GreekMaterials.BRONZE_INGOT.get()).is(ModTags.cItem("ingots/bronze")),
                "青铜锭应在 c:ingots/bronze");
        helper.assertTrue(new ItemStack(GreekMaterials.RAW_TIN.get()).is(ModTags.cItem("raw_materials/tin")),
                "粗锡应在 c:raw_materials/tin");
        helper.succeed();
    }

    /** TC-T6-26（续）：只走熔炉的配方——陶坯构件烧成陶制构件、大理石烧成平滑大理石；高炉不接受。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void furnace_only_recipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var rm = level.getServer().getRecipeManager();
        record Case(net.minecraft.world.item.Item in, net.minecraft.world.item.Item out) {
        }
        for (Case c : List.of(new Case(GreekMaterials.RAW_ROOF_TILE.get(), GreekMaterials.CERAMIC_COMPONENT.get()),
                new Case(GreekMaterials.MARBLE.get().asItem(), GreekMaterials.SMOOTH_MARBLE.get().asItem()))) {
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(c.in()));
            var r = rm.getRecipeFor(RecipeType.SMELTING, input, level);
            helper.assertTrue(r.isPresent() && r.get().value().getResultItem(level.registryAccess()).is(c.out()),
                    c.in() + " 在熔炉中应得到 " + c.out());
            helper.assertTrue(rm.getRecipeFor(RecipeType.BLASTING, input, level).isEmpty(), c.in() + " 不应能用高炉");
        }
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, int w, int h, List<ItemStack> items) {
        ServerLevel level = helper.getLevel();
        CraftingInput input = CraftingInput.of(w, h, items);
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static ItemStack potion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        return net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, potion);
    }

    /** TC-T6-28：苹果 + 2 个水瓶 → 奠酒 ×2；换成粗制的药水不匹配。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void libation_recipe(GameTestHelper helper) {
        var water = net.minecraft.world.item.alchemy.Potions.WATER;
        var awkward = net.minecraft.world.item.alchemy.Potions.AWKWARD;
        ItemStack ok = craft(helper, 3, 1, List.of(new ItemStack(Items.APPLE), potion(water), potion(water)));
        helper.assertTrue(ok.is(GreekMaterials.LIBATION.get()) && ok.getCount() == 2, "应得到奠酒 ×2，实际 " + ok);
        ItemStack bad = craft(helper, 3, 1, List.of(new ItemStack(Items.APPLE), potion(awkward), potion(awkward)));
        helper.assertTrue(bad.isEmpty(), "粗制的药水不应能做奠酒，实际 " + bad);
        helper.succeed();
    }

    /** TC-T6-29：两种不同木头的台阶 → 木盘 ×2。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void platter_recipe(GameTestHelper helper) {
        ItemStack r = craft(helper, 2, 1, List.of(new ItemStack(Items.OAK_SLAB), new ItemStack(Items.SPRUCE_SLAB)));
        helper.assertTrue(r.is(GreekMaterials.WOODEN_PLATTER.get()) && r.getCount() == 2, "应得到木盘 ×2，实际 " + r);
        helper.succeed();
    }

    /** TC-T6-30：切石机能从大理石原石与平滑大理石切出对应的形状，产量符合原版惯例。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void marble_stonecutting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var rm = level.getServer().getRecipeManager();
        record Case(net.minecraft.world.level.ItemLike in, net.minecraft.world.level.ItemLike out, int count) {
        }
        List<Case> cases = List.of(
                new Case(GreekMaterials.MARBLE.get(), GreekMaterials.MARBLE_SLAB.get(), 2),
                new Case(GreekMaterials.MARBLE.get(), GreekMaterials.MARBLE_STAIRS.get(), 1),
                new Case(GreekMaterials.MARBLE.get(), GreekMaterials.MARBLE_WALL.get(), 1),
                new Case(GreekMaterials.MARBLE.get(), GreekMaterials.POLYGONAL_MARBLE.get(), 1),
                new Case(GreekMaterials.SMOOTH_MARBLE.get(), GreekMaterials.SMOOTH_MARBLE_SLAB.get(), 2),
                new Case(GreekMaterials.SMOOTH_MARBLE.get(), GreekMaterials.MARBLE_ASHLAR.get(), 1),
                new Case(GreekMaterials.SMOOTH_MARBLE.get(), GreekMaterials.POLISHED_MARBLE.get(), 1),
                new Case(GreekMaterials.MARBLE_ASHLAR.get(), GreekMaterials.MARBLE_ASHLAR_WALL.get(), 1));
        for (Case c : cases) {
            var results = rm.getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(new ItemStack(c.in())), level)
                    .stream().map(r -> r.value().getResultItem(level.registryAccess())).toList();
            boolean found = results.stream().anyMatch(st -> st.is(c.out().asItem()) && st.getCount() == c.count());
            helper.assertTrue(found, c.in() + " 应能切出 " + c.count() + " 个 " + c.out() + "，实际 " + results);
        }
        helper.assertTrue(rm.getRecipesFor(RecipeType.STONECUTTING,
                        new SingleRecipeInput(new ItemStack(GreekMaterials.MARBLE.get())), level).stream()
                        .noneMatch(r -> r.value().getResultItem(level.registryAccess()).is(GreekMaterials.SMOOTH_MARBLE.get().asItem())),
                "原石不能直接切出平滑大理石（必须烧制）");
        helper.succeed();
    }

    /** TC-T6-31（续）：大理石族标签与原版形状标签。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void marble_family_tags(GameTestHelper helper) {
        for (ContentEntry e : Content.entries()) {
            if (e.block().isPresent() && e.id().contains("marble")) {
                helper.assertTrue(e.block().get().get().defaultBlockState().is(GreekMaterials.MARBLE_FAMILY),
                        e.id() + " 应在 mythic_age:marble_family");
            }
        }
        helper.assertTrue(GreekMaterials.MARBLE_WALL.get().defaultBlockState().is(BlockTags.WALLS), "大理石墙应在 walls");
        helper.assertTrue(GreekMaterials.MARBLE_ASHLAR_SLAB.get().defaultBlockState().is(BlockTags.SLABS), "方石台阶应在 slabs");
        helper.assertTrue(GreekMaterials.POLISHED_MARBLE_STAIRS.get().defaultBlockState().is(BlockTags.STAIRS), "磨光楼梯应在 stairs");
        helper.succeed();
    }

    /** TC-T6-32：双层台阶掉 2 个。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void double_slab_drops_two(GameTestHelper helper) {
        BlockState doubleSlab = GreekMaterials.SMOOTH_MARBLE_SLAB.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
        List<ItemStack> got = drops(helper.getLevel(), doubleSlab, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(count(got, GreekMaterials.SMOOTH_MARBLE_SLAB.get().asItem()) == 2, "双层台阶应掉 2 个，实际 " + got);
        helper.succeed();
    }

    // ---------------------------------------------------------------- 时代受限配方

    private static FakePlayer player(GameTestHelper helper, String name, Age greekAge) {
        FakePlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        MythData.get(p).civ(Civilization.GREEK).setAge(greekAge);
        return p;
    }

    private static CraftingInput bronzeInput() {
        return CraftingInput.of(2, 2, List.of(new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.COPPER_INGOT), new ItemStack(GreekMaterials.TIN_INGOT.get())));
    }

    /** 以 who 的身份（null 表示没有玩家）查找铜 3 + 锡 1 的合成结果。 */
    private static ItemStack craftAs(GameTestHelper helper, FakePlayer who) {
        ServerLevel level = helper.getLevel();
        CraftingInput input = bronzeInput();
        if (who != null) {
            CraftingContext.enter(who);
        }
        try {
            return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                    .map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
        } finally {
            CraftingContext.exit();
        }
    }

    /** TC-T6-27、TC-T6-27a：古典可以合成青铜混合物 ×4；远古不匹配并收到提示（节流 3 秒）。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void bronze_requires_classical(GameTestHelper helper) {
        FakePlayer classical = player(helper, "gt_classical", Age.CLASSICAL);
        ItemStack ok = craftAs(helper, classical);
        helper.assertTrue(ok.is(GreekMaterials.BRONZE_MIX.get()) && ok.getCount() == 4, "古典应得到青铜混合物 ×4，实际 " + ok);

        FakePlayer archaic = player(helper, "gt_archaic", Age.ARCHAIC);
        helper.assertTrue(!AgeGate.noticedRecently(archaic.getUUID()), "前提：尚未提示");
        helper.assertTrue(craftAs(helper, archaic).isEmpty(), "远古不应能合成青铜混合物");
        helper.assertTrue(AgeGate.noticedRecently(archaic.getUUID()), "远古玩家应收到时代不足提示");
        helper.succeed();
    }

    /** TC-T6-27（实际合成流程）：工作台中取出结果后原料被正确扣除；远古时结果格为空。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void bronze_crafting_menu(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        for (Age age : List.of(Age.ARCHAIC, Age.CLASSICAL)) {
            FakePlayer p = player(helper, "gt_menu_" + age.id(), age);
            CraftingMenu menu = new CraftingMenu(0, p.getInventory(), ContainerLevelAccess.create(helper.getLevel(), pos));
            menu.getSlot(1).set(new ItemStack(Items.COPPER_INGOT));
            menu.getSlot(2).set(new ItemStack(Items.COPPER_INGOT));
            menu.getSlot(4).set(new ItemStack(Items.COPPER_INGOT));
            menu.getSlot(5).set(new ItemStack(GreekMaterials.TIN_INGOT.get()));
            ItemStack result = menu.getSlot(0).getItem();
            if (age == Age.ARCHAIC) {
                helper.assertTrue(result.isEmpty(), "远古时结果格应为空，实际 " + result);
                helper.assertTrue(menu.getSlot(5).getItem().getCount() == 1, "远古时原料不应被扣除");
            } else {
                helper.assertTrue(result.is(GreekMaterials.BRONZE_MIX.get()) && result.getCount() == 4,
                        "古典时结果应为青铜混合物 ×4，实际 " + result);
                ItemStack taken = menu.getSlot(0).remove(result.getCount());
                menu.getSlot(0).onTake(p, taken);
                for (int slot : new int[]{1, 2, 4, 5}) {
                    helper.assertTrue(menu.getSlot(slot).getItem().isEmpty(),
                            "取出后第 " + slot + " 格原料应被扣除，实际 " + menu.getSlot(slot).getItem());
                }
            }
            menu.removed(p);
        }
        helper.succeed();
    }

    /** TC-T6-27b、TC-T6-27c：没有玩家时默认拒绝；allowAutomation / enabled=false 时放行，热更后立即生效。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = CONFIG_BATCH)
    public static void age_gate_config(GameTestHelper helper) {
        ConfigHolder<WorldRulesConfig> holder = ModConfigs.worldRules();
        WorldRulesConfig original = holder.current();
        FakePlayer archaic = player(helper, "gt_cfg_archaic", Age.ARCHAIC);
        try {
            helper.assertTrue(craftAs(helper, null).isEmpty(), "默认配置下自动合成应被拒绝");
            reload(holder, original, true, true);
            helper.assertTrue(!craftAs(helper, null).isEmpty(), "allowAutomation=true 时自动合成应放行");
            helper.assertTrue(craftAs(helper, archaic).isEmpty(), "allowAutomation 不影响玩家手工合成的限制");
            reload(holder, original, false, false);
            helper.assertTrue(!craftAs(helper, archaic).isEmpty(), "enabled=false 时远古也能合成");
            helper.assertTrue(!craftAs(helper, null).isEmpty(), "enabled=false 时自动合成也放行");
        } finally {
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(), original));
        }
        helper.assertTrue(holder.current().equals(original), "测试后应恢复原配置");
        helper.assertTrue(craftAs(helper, archaic).isEmpty(), "恢复后远古重新受限");
        helper.succeed();
    }

    private static void reload(ConfigHolder<WorldRulesConfig> holder, WorldRulesConfig base, boolean enabled,
                               boolean automation) {
        WorldRulesConfig next = new WorldRulesConfig(base.beneficialConversions(), base.mobBlockDamage(),
                base.blockEntityStall(), base.godPowers(),
                new WorldRulesConfig.Recipes(new WorldRulesConfig.AgeGated(enabled, automation)));
        var outcome = holder.reload(ConfigLoader.toPrettyJson(holder.schema(), next));
        if (outcome.status() != ConfigHolder.Status.APPLIED) {
            throw new IllegalStateException("热更失败：" + outcome);
        }
    }

    static Optional<ContentEntry> entry(String id) {
        return Content.entries().stream().filter(e -> e.id().equals(id)).findFirst();
    }
}
