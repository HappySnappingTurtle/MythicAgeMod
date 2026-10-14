package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * TC-T2-04：证明 GameTest 框架可用，且 mod 与 core 在同一运行时中加载。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BootstrapGameTests {
    private BootstrapGameTests() {
    }

    @GameTest(template = GameTestTemplates.EMPTY_3X3)
    public static void mod_is_loaded(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(MythicAgeIds.MOD_ID), "mythic_age 应已加载");
        helper.assertTrue(ModList.get().isLoaded("geckolib"), "GeckoLib 应已加载");
        helper.assertTrue(ModList.get().isLoaded("curios"), "Curios 应已加载");
        helper.assertTrue(ModList.get().isLoaded("patchouli"), "Patchouli 应已加载");
        helper.succeed();
    }
}
