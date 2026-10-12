package com.example.mythic_age.validation;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.block.ModBlocks;
import com.example.mythic_age.block.entity.AbundanceAltarCoreBlockEntity;
import com.example.mythic_age.block.entity.EgyptianStatuePedestalBlockEntity;
import com.example.mythic_age.block.entity.RelicPedestalBlockEntity;
import com.example.mythic_age.block.entity.StatueBaseBlockEntity;
import com.example.mythic_age.entity.CustomTempleGuardianEntity;
import com.example.mythic_age.entity.ModEntities;
import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.item.ModItems;
import com.example.mythic_age.item.SpellbookItem;
import com.example.mythic_age.system.GodPowerCertificateManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side validation hooks used by local MCP visual/NBT gates.
 */
public final class ValidationCommand {
    private static final String REPORT_FILE = "visual_probe/entity_nbt_report.json";
    private static final String RESTART_REPORT_FILE = "visual_probe/nbt_restart_report.json";
    private static final String RESTART_PREPARE_REPORT_FILE = "visual_probe/nbt_restart_prepare_report.json";
    private static final String RESTART_VERIFY_REPORT_FILE = "visual_probe/nbt_restart_verify_report.json";
    private static final String GOD_POWER_PLAYTEST_REPORT_FILE = "visual_probe/god_power_playtest_report.json";
    private static final BlockPos RESTART_PROBE_ORIGIN = new BlockPos(0, 90, 0);
    private static final BlockPos GOD_POWER_PROBE_ORIGIN = new BlockPos(192, 120, 192);
    private static final int GOD_POWER_PROBE_CELL_SIZE = 40;
    private static final UUID RESTART_PROBE_GUARDIAN_UUID = UUID.fromString("7c6fd0a1-f496-4f7f-b59a-a05281b27771");
    private static final String RESTART_PROBE_GUARDIAN_NAME = "mythic_age_nbt_restart_probe_guardian";
    private static final BlockPos RESTART_PROBE_GUARDIAN_POS = RESTART_PROBE_ORIGIN.add(0, 0, 0);
    private static final BlockPos RESTART_PROBE_STATUE_BASE_POS = RESTART_PROBE_ORIGIN.add(2, 0, 0);
    private static final BlockPos RESTART_PROBE_RELIC_PEDESTAL_POS = RESTART_PROBE_ORIGIN.add(4, 0, 0);
    private static final BlockPos RESTART_PROBE_EGYPTIAN_STATUE_POS = RESTART_PROBE_ORIGIN.add(6, 0, 0);
    private static final BlockPos RESTART_PROBE_ABUNDANCE_ALTAR_POS = RESTART_PROBE_ORIGIN.add(8, 0, 0);
    private static final int RESTART_EXPECTED_STATUE_CERTIFICATES = 5;
    private static final long RESTART_EXPECTED_ABUNDANCE_TIME = 12345L;
    private static final String[] ENTITY_IDS = {
            "mythic_age:minotaur",
            "mythic_age:cyclops",
            "mythic_age:centaur",
            "mythic_age:hydra",
            "mythic_age:medusa",
            "mythic_age:nemean_lion",
            "mythic_age:manticore",
            "mythic_age:chimera",
            "mythic_age:colossus",
            "mythic_age:pegasus",
            "mythic_age:anubis_warrior",
            "mythic_age:scarab_guardian",
            "mythic_age:mummy_soldier",
            "mythic_age:sphinx_sentinel",
            "mythic_age:sun_falcon",
            "mythic_age:frost_giant",
            "mythic_age:draugr_warrior",
            "mythic_age:valkyrie",
            "mythic_age:fenrir_wolf",
            "mythic_age:dwarf_berserker",
            "mythic_age:azure_dragon",
            "mythic_age:qilin_guardian",
            "mythic_age:jiangshi",
            "mythic_age:nine_tailed_fox",
            "mythic_age:stone_lion_guardian",
            "mythic_age:crystal_leviathan",
            "mythic_age:orichalcum_golem",
            "mythic_age:siren_priestess",
            "mythic_age:tidal_guardian",
            "mythic_age:abyssal_ray",
            "mythic_age:walking_tree",
            "mythic_age:temple_guardian",
            "mythic_age:temple_guardian_greek",
            "mythic_age:temple_guardian_egyptian",
            "mythic_age:temple_guardian_chinese",
            "mythic_age:temple_guardian_atlantean",
            "mythic_age:temple_guardian_nordic",
            "mythic_age:custom_temple_guardian_greek",
            "mythic_age:custom_temple_guardian_egyptian",
            "mythic_age:custom_temple_guardian_chinese",
            "mythic_age:custom_temple_guardian_atlantean",
            "mythic_age:custom_temple_guardian_nordic",
            "mythic_age:meteor"
    };

    private ValidationCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(ValidationCommand::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         net.minecraft.command.CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("mythicage_validate")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("nbt")
                        .executes(context -> runNbtValidation(context.getSource())))
                .then(CommandManager.literal("nbt_restart")
                        .executes(context -> runRestartNbtValidation(context.getSource())))
                .then(CommandManager.literal("nbt_restart_prepare")
                        .executes(context -> runRestartCyclePrepare(context.getSource())))
                .then(CommandManager.literal("nbt_restart_verify")
                        .executes(context -> runRestartCycleVerify(context.getSource())))
                .then(CommandManager.literal("god_power_playtest")
                        .executes(context -> runGodPowerPlaytest(context.getSource()))));
    }

    private static int runNbtValidation(ServerCommandSource source) {
        NbtValidationReport report;
        try {
            report = writeNbtReport(source.getServer(), source.getWorld());
        } catch (IOException e) {
            source.sendError(Text.literal("Failed to write validation report: " + e.getMessage()));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Mythic Age NBT validation: " + report.passed + "/" + report.total
                + " passed, report=" + report.output.getAbsolutePath()), false);
        return report.failed == 0 ? 1 : 0;
    }

    private static int runRestartNbtValidation(ServerCommandSource source) {
        RestartNbtValidationReport report;
        try {
            report = writeRestartNbtReport(source.getServer(), source.getWorld());
        } catch (IOException e) {
            source.sendError(Text.literal("Failed to write restart NBT validation report: " + e.getMessage()));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Mythic Age restart NBT validation: " + report.passed + "/" + report.total
                + " passed, report=" + report.output.getAbsolutePath()), false);
        return report.failed == 0 ? 1 : 0;
    }

    private static int runRestartCyclePrepare(ServerCommandSource source) {
        RestartNbtValidationReport report;
        try {
            report = writeRestartCyclePrepareReport(source.getServer(), source.getWorld());
        } catch (IOException e) {
            source.sendError(Text.literal("Failed to write restart-cycle prepare report: " + e.getMessage()));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Mythic Age restart-cycle prepare: " + report.passed + "/" + report.total
                + " prepared, report=" + report.output.getAbsolutePath()), false);
        return report.failed == 0 ? 1 : 0;
    }

    private static int runRestartCycleVerify(ServerCommandSource source) {
        RestartNbtValidationReport report;
        try {
            report = writeRestartCycleVerifyReport(source.getServer(), source.getWorld());
        } catch (IOException e) {
            source.sendError(Text.literal("Failed to write restart-cycle verify report: " + e.getMessage()));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Mythic Age restart-cycle verify: " + report.passed + "/" + report.total
                + " passed, report=" + report.output.getAbsolutePath()), false);
        return report.failed == 0 ? 1 : 0;
    }

    private static int runGodPowerPlaytest(ServerCommandSource source) {
        GodPowerPlaytestReport report;
        try {
            report = writeGodPowerPlaytestReport(source.getServer());
        } catch (IOException e) {
            source.sendError(Text.literal("Failed to write god power playtest report: " + e.getMessage()));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Mythic Age god power runtime playtest: " + report.passed + "/" + report.total
                + " passed, report=" + report.output.getAbsolutePath()), false);
        return report.failed == 0 ? 1 : 0;
    }

    public static NbtValidationReport writeNbtReport(MinecraftServer server) throws IOException {
        return writeNbtReport(server, server.getOverworld());
    }

    public static NbtValidationReport writeNbtReport(MinecraftServer server, ServerWorld world) throws IOException {
        List<String> entries = new ArrayList<>();
        int passCount = 0;
        for (String entityId : ENTITY_IDS) {
            ValidationResult result = validateEntity(world, entityId);
            if (result.ok) {
                passCount++;
            }
            entries.add(result.toJson());
        }
        String payload = "{"
                + "\"version\":1,"
                + "\"timestamp\":\"" + jsonEscape(Instant.now().toString()) + "\","
                + "\"process_id\":" + currentProcessId() + ","
                + "\"total\":" + ENTITY_IDS.length + ","
                + "\"passed\":" + passCount + ","
                + "\"failed\":" + (ENTITY_IDS.length - passCount) + ","
                + "\"entities\":[" + String.join(",", entries) + "]"
                + "}";
        File output = new File(server.getRunDirectory(), REPORT_FILE);
        output.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(output, StandardCharsets.UTF_8)) {
            writer.write(payload);
            writer.write("\n");
        }
        return new NbtValidationReport(ENTITY_IDS.length, passCount, ENTITY_IDS.length - passCount, output, payload);
    }

    public static RestartNbtValidationReport writeRestartNbtReport(MinecraftServer server) throws IOException {
        return writeRestartNbtReport(server, server.getOverworld());
    }

    public static RestartNbtValidationReport writeRestartNbtReport(MinecraftServer server, ServerWorld world) throws IOException {
        List<ValidationResult> results = new ArrayList<>();
        results.add(validateCustomGuardianRestart(world, "mythic_age:custom_temple_guardian_atlantean", "ATLANTEAN"));
        results.add(validateStatueBaseRestart());
        results.add(validateRelicPedestalRestart());
        results.add(validateEgyptianStatuePedestalRestart());
        results.add(validateAbundanceAltarRestart());

        int passCount = 0;
        List<String> entries = new ArrayList<>();
        for (ValidationResult result : results) {
            if (result.ok) {
                passCount++;
            }
            entries.add(result.toJson());
        }

        String payload = "{"
                + "\"version\":1,"
                + "\"timestamp\":\"" + jsonEscape(Instant.now().toString()) + "\","
                + "\"process_id\":" + currentProcessId() + ","
                + "\"scope\":\"runtime_restart_nbt_roundtrip\","
                + "\"total\":" + results.size() + ","
                + "\"passed\":" + passCount + ","
                + "\"failed\":" + (results.size() - passCount) + ","
                + "\"checks\":[" + String.join(",", entries) + "]"
                + "}";
        File output = new File(server.getRunDirectory(), RESTART_REPORT_FILE);
        output.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(output, StandardCharsets.UTF_8)) {
            writer.write(payload);
            writer.write("\n");
        }
        return new RestartNbtValidationReport(results.size(), passCount, results.size() - passCount, output, payload);
    }

    public static RestartNbtValidationReport writeRestartCyclePrepareReport(MinecraftServer server) throws IOException {
        return writeRestartCyclePrepareReport(server, server.getOverworld());
    }

    public static RestartNbtValidationReport writeRestartCyclePrepareReport(MinecraftServer server, ServerWorld world) throws IOException {
        List<ValidationResult> results = prepareRestartProbeWorld(world);
        try {
            boolean saved = server.saveAll(true, true, true);
            results.add(saved
                    ? ValidationResult.pass("world_save_flush", 1, 1)
                    : ValidationResult.fail("world_save_flush", "server_saveAll_returned_false"));
        } catch (Exception e) {
            results.add(ValidationResult.fail("world_save_flush", e.getClass().getSimpleName() + ":" + e.getMessage()));
        }
        return writeRestartCycleReport(server, RESTART_PREPARE_REPORT_FILE, "runtime_restart_cycle_prepare", "prepare", results);
    }

    public static RestartNbtValidationReport writeRestartCycleVerifyReport(MinecraftServer server) throws IOException {
        return writeRestartCycleVerifyReport(server, server.getOverworld());
    }

    public static RestartNbtValidationReport writeRestartCycleVerifyReport(MinecraftServer server, ServerWorld world) throws IOException {
        List<ValidationResult> results = verifyRestartProbeWorld(world);
        return writeRestartCycleReport(server, RESTART_VERIFY_REPORT_FILE, "runtime_restart_cycle_verify", "verify", results);
    }

    public static GodPowerPlaytestReport writeGodPowerPlaytestReport(MinecraftServer server) throws IOException {
        List<GodPowerPlaytestCase> cases = godPowerPlaytestCases();
        List<GodPowerPlaytestResult> results = new ArrayList<>();
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            for (GodPowerPlaytestCase testCase : cases) {
                results.add(GodPowerPlaytestResult.fail(testCase, "player_missing"));
            }
        } else {
            ServerPlayerEntity player = players.get(0);
            ServerWorld world = player.getServerWorld();
            for (int index = 0; index < cases.size(); index++) {
                results.add(validateGodPowerRuntimeCast(world, player, cases.get(index), index));
            }
        }

        int passCount = 0;
        List<String> entries = new ArrayList<>();
        for (GodPowerPlaytestResult result : results) {
            if (result.ok) {
                passCount++;
            }
            entries.add(result.toJson());
        }
        String payload = "{"
                + "\"version\":1,"
                + "\"timestamp\":\"" + jsonEscape(Instant.now().toString()) + "\","
                + "\"process_id\":" + currentProcessId() + ","
                + "\"scope\":\"runtime_god_power_playtest_smoke\","
                + "\"probe_origin\":" + blockPosJson(GOD_POWER_PROBE_ORIGIN) + ","
                + "\"cell_size\":" + GOD_POWER_PROBE_CELL_SIZE + ","
                + "\"total\":" + results.size() + ","
                + "\"passed\":" + passCount + ","
                + "\"failed\":" + (results.size() - passCount) + ","
                + "\"checks\":[" + String.join(",", entries) + "]"
                + "}";
        File output = new File(server.getRunDirectory(), GOD_POWER_PLAYTEST_REPORT_FILE);
        output.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(output, StandardCharsets.UTF_8)) {
            writer.write(payload);
            writer.write("\n");
        }
        return new GodPowerPlaytestReport(results.size(), passCount, results.size() - passCount, output, payload);
    }

    private static GodPowerPlaytestResult validateGodPowerRuntimeCast(ServerWorld world, ServerPlayerEntity player,
                                                                      GodPowerPlaytestCase testCase, int index) {
        long start = System.currentTimeMillis();
        BlockPos origin = godPowerProbeOrigin(index);
        int spawnedEntities = 0;
        try {
            prepareGodPowerProbeCell(world, origin);
            spawnedEntities = spawnGodPowerProbeEntities(world, origin);
            player.getItemCooldownManager().remove(testCase.item());
            player.clearStatusEffects();
            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);
            player.getHungerManager().setSaturationLevel(20.0F);
            player.teleport(world, origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + 0.5D, 0.0F, 0.0F);
            player.setStackInHand(Hand.MAIN_HAND, new ItemStack(testCase.item()));

            TypedActionResult<ItemStack> action = ((SpellbookItem) testCase.item()).use(world, player, Hand.MAIN_HAND);
            boolean accepted = action.getResult().isAccepted();
            boolean cooldownSet = player.getItemCooldownManager().isCoolingDown(testCase.item());
            player.getItemCooldownManager().remove(testCase.item());
            cleanupGodPowerProbeEntities(world, origin);
            long durationMs = System.currentTimeMillis() - start;
            return new GodPowerPlaytestResult(
                    testCase.civilization(),
                    Registries.ITEM.getId(testCase.item()).toString(),
                    testCase.spellType().name(),
                    accepted,
                    action.getResult().toString(),
                    cooldownSet,
                    spawnedEntities,
                    durationMs,
                    accepted ? "" : "typed_action_result_not_accepted",
                    origin
            );
        } catch (Exception e) {
            cleanupGodPowerProbeEntities(world, origin);
            return new GodPowerPlaytestResult(
                    testCase.civilization(),
                    Registries.ITEM.getId(testCase.item()).toString(),
                    testCase.spellType().name(),
                    false,
                    "exception",
                    false,
                    spawnedEntities,
                    System.currentTimeMillis() - start,
                    e.getClass().getSimpleName() + ":" + e.getMessage(),
                    origin
            );
        }
    }

    private static BlockPos godPowerProbeOrigin(int index) {
        int column = index % 8;
        int row = index / 8;
        return GOD_POWER_PROBE_ORIGIN.add(column * GOD_POWER_PROBE_CELL_SIZE, 0, row * GOD_POWER_PROBE_CELL_SIZE);
    }

    private static void prepareGodPowerProbeCell(ServerWorld world, BlockPos origin) {
        forceProbeChunk(world, origin);
        cleanupGodPowerProbeEntities(world, origin);
        for (int x = -14; x <= 14; x++) {
            for (int z = -14; z <= 14; z++) {
                world.setBlockState(origin.add(x, 0, z), Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
                for (int y = 1; y <= 12; y++) {
                    world.setBlockState(origin.add(x, y, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                }
            }
        }
        prepareGodPowerTerrainDetails(world, origin);
    }

    private static void prepareGodPowerTerrainDetails(ServerWorld world, BlockPos origin) {
        for (int x = 5; x <= 8; x++) {
            for (int z = -5; z <= -2; z++) {
                world.setBlockState(origin.add(x, 0, z), Blocks.FARMLAND.getDefaultState(), Block.NOTIFY_ALL);
                world.setBlockState(origin.add(x, 1, z), Blocks.WHEAT.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        for (int x = 2; x <= 5; x++) {
            for (int z = 8; z <= 11; z++) {
                world.setBlockState(origin.add(x, 0, z), Blocks.DIRT.getDefaultState(), Block.NOTIFY_ALL);
                world.setBlockState(origin.add(x, 1, z), Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        for (int x = -9; x <= -6; x++) {
            for (int z = -9; z <= -6; z++) {
                world.setBlockState(origin.add(x, 0, z), Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
                world.setBlockState(origin.add(x, -1, z), Blocks.IRON_ORE.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        placeProbeTree(world, origin.add(-7, 1, 6));
        placeProbeTree(world, origin.add(-10, 1, 3));
    }

    private static void placeProbeTree(ServerWorld world, BlockPos base) {
        for (int y = 0; y < 5; y++) {
            world.setBlockState(base.add(0, y, 0), Blocks.OAK_LOG.getDefaultState(), Block.NOTIFY_ALL);
        }
        for (int x = -2; x <= 2; x++) {
            for (int y = 3; y <= 5; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) + Math.abs(z) <= 3) {
                        world.setBlockState(base.add(x, y, z), Blocks.OAK_LEAVES.getDefaultState(), Block.NOTIFY_ALL);
                    }
                }
            }
        }
    }

    private static int spawnGodPowerProbeEntities(ServerWorld world, BlockPos origin) {
        int spawned = 0;
        spawned += spawnProbeEntity(world, EntityType.ZOMBIE, origin.add(0, 1, 7));
        spawned += spawnProbeEntity(world, EntityType.SKELETON, origin.add(2, 1, 8));
        spawned += spawnProbeEntity(world, EntityType.CREEPER, origin.add(-2, 1, 8));
        spawned += spawnProbeEntity(world, EntityType.COW, origin.add(5, 1, 4));
        spawned += spawnProbeEntity(world, EntityType.SHEEP, origin.add(7, 1, 4));
        spawned += spawnProbeEntity(world, EntityType.PIG, origin.add(9, 1, 4));
        return spawned;
    }

    private static int spawnProbeEntity(ServerWorld world, EntityType<?> type, BlockPos pos) {
        Entity entity = type.create(world);
        if (entity == null) {
            return 0;
        }
        entity.setCustomName(Text.literal("mythic_age_god_power_probe"));
        entity.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 180.0F, 0.0F);
        return world.spawnEntity(entity) ? 1 : 0;
    }

    private static void cleanupGodPowerProbeEntities(ServerWorld world, BlockPos origin) {
        Box box = new Box(origin).expand(24.0D, 18.0D, 24.0D);
        for (Entity entity : world.getOtherEntities(null, box, entity -> !(entity instanceof ServerPlayerEntity))) {
            entity.discard();
        }
    }

    private static List<GodPowerPlaytestCase> godPowerPlaytestCases() {
        return List.of(
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.LIGHTNING_STORM, ModItems.SPELLBOOK_LIGHTNING_STORM),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.RESTORATION, ModItems.SPELLBOOK_RESTORATION),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.LURE, ModItems.SPELLBOOK_LURE),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.UNDERWORLD_ARMY, ModItems.SPELLBOOK_UNDERWORLD_ARMY),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.PESTILENCE, ModItems.SPELLBOOK_PESTILENCE),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.CURSE, ModItems.SPELLBOOK_CURSE),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.EARTHQUAKE, ModItems.SPELLBOOK_EARTHQUAKE),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.CEASEFIRE, ModItems.SPELLBOOK_CEASEFIRE),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.BRONZE, ModItems.SPELLBOOK_BRONZE),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.UNDERWORLD_PASSAGE, ModItems.SPELLBOOK_UNDERWORLD_PASSAGE),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.LIGHTNING_STORM_HERA, ModItems.SPELLBOOK_LIGHTNING_STORM_HERA),
                new GodPowerPlaytestCase("greek", SpellbookItem.SpellType.PLENTY, ModItems.SPELLBOOK_PLENTY),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.RAIN_PRAYER, ModItems.SPELLBOOK_RAIN_PRAYER),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.EARTH_GOD_BLESSING, ModItems.SPELLBOOK_EARTH_GOD_BLESSING),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.FIVE_THUNDER, ModItems.SPELLBOOK_FIVE_THUNDER),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.ENLIGHTENMENT, ModItems.SPELLBOOK_ENLIGHTENMENT),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.ABUNDANCE, ModItems.SPELLBOOK_ABUNDANCE),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.CRANE_BLESSING, ModItems.SPELLBOOK_CRANE_BLESSING),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.FLOOD, ModItems.SPELLBOOK_FLOOD),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.CALL_TO_ARMS, ModItems.SPELLBOOK_CALL_TO_ARMS),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.TAIJI_SHIELD, ModItems.SPELLBOOK_TAIJI_SHIELD),
                new GodPowerPlaytestCase("chinese", SpellbookItem.SpellType.RECREATION, ModItems.SPELLBOOK_RECREATION),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.RA_RADIANCE, ModItems.SPELLBOOK_RA_RADIANCE),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.DESERT_STORM, ModItems.SPELLBOOK_DESERT_STORM),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.EYE_OF_HORUS, ModItems.SPELLBOOK_EYE_OF_HORUS),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.SUN_RAIN, ModItems.SPELLBOOK_SUN_RAIN),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.PROSPERITY, ModItems.SPELLBOOK_PROSPERITY),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.ANUBIS_JUDGMENT, ModItems.SPELLBOOK_ANUBIS_JUDGMENT),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.LOCUST_SWARM, ModItems.SPELLBOOK_LOCUST_SWARM),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.TORNADO, ModItems.SPELLBOOK_TORNADO),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.ISIS_RESURRECTION, ModItems.SPELLBOOK_ISIS_RESURRECTION),
                new GodPowerPlaytestCase("egyptian", SpellbookItem.SpellType.METEOR, ModItems.SPELLBOOK_METEOR),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.FENRIR_HOWL, ModItems.SPELLBOOK_FENRIR_HOWL),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.ODIN_EYE, ModItems.SPELLBOOK_ODIN_EYE),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.THOR_WRATH, ModItems.SPELLBOOK_THOR_WRATH),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.GREAT_HUNT, ModItems.SPELLBOOK_GREAT_HUNT),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.DWARVEN_MINE, ModItems.SPELLBOOK_DWARVEN_MINE),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.VALHALLA_CALL, ModItems.SPELLBOOK_VALHALLA_CALL),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.FROST, ModItems.SPELLBOOK_FROST),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.WALKING_WOODS, ModItems.SPELLBOOK_WALKING_WOODS),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.RAGNAROK, ModItems.SPELLBOOK_RAGNAROK),
                new GodPowerPlaytestCase("nordic", SpellbookItem.SpellType.FIMBULWINTER, ModItems.SPELLBOOK_FIMBULWINTER),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.TIDAL_SURGE, ModItems.SPELLBOOK_TIDAL_SURGE),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.ABYSSAL_CURRENT, ModItems.SPELLBOOK_ABYSSAL_CURRENT),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.CRYSTAL_BARRIER, ModItems.SPELLBOOK_CRYSTAL_BARRIER),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.PEARL_HARVEST, ModItems.SPELLBOOK_PEARL_HARVEST),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.ORICHALCUM_FORGE, ModItems.SPELLBOOK_ORICHALCUM_FORGE),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.SIREN_SONG, ModItems.SPELLBOOK_SIREN_SONG),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.LEVIATHAN_CALL, ModItems.SPELLBOOK_LEVIATHAN_CALL),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.ABYSSAL_RIFT, ModItems.SPELLBOOK_ABYSSAL_RIFT),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.CHRONO_STASIS, ModItems.SPELLBOOK_CHRONO_STASIS),
                new GodPowerPlaytestCase("atlantean", SpellbookItem.SpellType.OCEANIC_WRATH, ModItems.SPELLBOOK_OCEANIC_WRATH)
        );
    }

    private static RestartNbtValidationReport writeRestartCycleReport(MinecraftServer server, String reportFile, String scope,
                                                                      String phase, List<ValidationResult> results) throws IOException {
        int passCount = 0;
        List<String> entries = new ArrayList<>();
        for (ValidationResult result : results) {
            if (result.ok) {
                passCount++;
            }
            entries.add(result.toJson());
        }
        String payload = "{"
                + "\"version\":1,"
                + "\"timestamp\":\"" + jsonEscape(Instant.now().toString()) + "\","
                + "\"process_id\":" + currentProcessId() + ","
                + "\"scope\":\"" + jsonEscape(scope) + "\","
                + "\"phase\":\"" + jsonEscape(phase) + "\","
                + "\"probe_origin\":" + blockPosJson(RESTART_PROBE_ORIGIN) + ","
                + "\"guardian_uuid\":\"" + jsonEscape(RESTART_PROBE_GUARDIAN_UUID.toString()) + "\","
                + "\"expected\":{"
                + "\"guardian_mythology\":\"ATLANTEAN\","
                + "\"statue_certificates\":" + RESTART_EXPECTED_STATUE_CERTIFICATES + ","
                + "\"relic_item\":\"" + jsonEscape(ModItems.RELIC_ANKH_OF_RA.toString()) + "\","
                + "\"egyptian_statue_type\":\"pharaoh\","
                + "\"abundance_next_generation_time\":" + RESTART_EXPECTED_ABUNDANCE_TIME
                + "},"
                + "\"total\":" + results.size() + ","
                + "\"passed\":" + passCount + ","
                + "\"failed\":" + (results.size() - passCount) + ","
                + "\"checks\":[" + String.join(",", entries) + "]"
                + "}";
        File output = new File(server.getRunDirectory(), reportFile);
        output.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(output, StandardCharsets.UTF_8)) {
            writer.write(payload);
            writer.write("\n");
        }
        return new RestartNbtValidationReport(results.size(), passCount, results.size() - passCount, output, payload);
    }

    private static List<ValidationResult> prepareRestartProbeWorld(ServerWorld world) {
        List<ValidationResult> results = new ArrayList<>();
        results.add(prepareGuardianRestartCycle(world));
        results.add(prepareStatueBaseRestartCycle(world));
        results.add(prepareRelicPedestalRestartCycle(world));
        results.add(prepareEgyptianStatueRestartCycle(world));
        results.add(prepareAbundanceAltarRestartCycle(world));
        return results;
    }

    private static List<ValidationResult> verifyRestartProbeWorld(ServerWorld world) {
        List<ValidationResult> results = new ArrayList<>();
        results.add(verifyGuardianRestartCycle(world));
        results.add(verifyStatueBaseRestartCycle(world));
        results.add(verifyRelicPedestalRestartCycle(world));
        results.add(verifyEgyptianStatueRestartCycle(world));
        results.add(verifyAbundanceAltarRestartCycle(world));
        return results;
    }

    private static ValidationResult prepareGuardianRestartCycle(ServerWorld world) {
        try {
            forceProbeChunk(world, RESTART_PROBE_GUARDIAN_POS);
            Entity existing = world.getEntity(RESTART_PROBE_GUARDIAN_UUID);
            if (existing != null) {
                existing.discard();
            }
            CustomTempleGuardianEntity guardian = ModEntities.CUSTOM_TEMPLE_GUARDIAN_ATLANTEAN.create(world);
            if (guardian == null) {
                return ValidationResult.fail("restart_cycle_guardian_prepare", "create_returned_null");
            }
            guardian.setUuid(RESTART_PROBE_GUARDIAN_UUID);
            guardian.setCustomName(Text.literal(RESTART_PROBE_GUARDIAN_NAME));
            guardian.setCustomNameVisible(false);
            guardian.refreshPositionAndAngles(
                    RESTART_PROBE_GUARDIAN_POS.getX() + 0.5D,
                    RESTART_PROBE_GUARDIAN_POS.getY(),
                    RESTART_PROBE_GUARDIAN_POS.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
            NbtCompound sample = new NbtCompound();
            sample.putString("mythologyType", "ATLANTEAN");
            sample.putInt("certificateGenerationTimer", 77);
            sample.putBoolean("hasInitializedTrades", true);
            sample.putInt("templePosX", RESTART_PROBE_STATUE_BASE_POS.getX());
            sample.putInt("templePosY", RESTART_PROBE_STATUE_BASE_POS.getY());
            sample.putInt("templePosZ", RESTART_PROBE_STATUE_BASE_POS.getZ());
            NbtCompound cooldowns = new NbtCompound();
            cooldowns.putLong(ModItems.PEGASUS_SPAWN_EGG.toString(), 2400L);
            sample.put("tradeCooldowns", cooldowns);
            guardian.readNbt(sample);
            guardian.setTemplePos(RESTART_PROBE_STATUE_BASE_POS);
            if (!world.spawnEntity(guardian)) {
                return ValidationResult.fail("restart_cycle_guardian_prepare", "spawnEntity_returned_false");
            }
            return verifyGuardianRestartCycle(world).rename("restart_cycle_guardian_prepare");
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_guardian_prepare", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult prepareStatueBaseRestartCycle(ServerWorld world) {
        try {
            BlockEntity blockEntity = prepareProbeBlockEntity(world, RESTART_PROBE_STATUE_BASE_POS,
                    ModBlocks.STATUE_GREEK_BASE.getDefaultState());
            if (!(blockEntity instanceof StatueBaseBlockEntity statueBase)) {
                return ValidationResult.fail("restart_cycle_statue_base_prepare",
                        blockEntityProbeError(world, RESTART_PROBE_STATUE_BASE_POS, blockEntity));
            }
            statueBase.addCertificates(RESTART_EXPECTED_STATUE_CERTIFICATES);
            return verifyStatueBaseRestartCycle(world).rename("restart_cycle_statue_base_prepare");
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_statue_base_prepare", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult prepareRelicPedestalRestartCycle(ServerWorld world) {
        try {
            BlockEntity blockEntity = prepareProbeBlockEntity(world, RESTART_PROBE_RELIC_PEDESTAL_POS,
                    ModBlocks.RELIC_PEDESTAL.getDefaultState());
            if (!(blockEntity instanceof RelicPedestalBlockEntity relicPedestal)) {
                return ValidationResult.fail("restart_cycle_relic_pedestal_prepare",
                        blockEntityProbeError(world, RESTART_PROBE_RELIC_PEDESTAL_POS, blockEntity));
            }
            relicPedestal.setRelicStack(new ItemStack(ModItems.RELIC_ANKH_OF_RA));
            return verifyRelicPedestalRestartCycle(world).rename("restart_cycle_relic_pedestal_prepare");
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_relic_pedestal_prepare", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult prepareEgyptianStatueRestartCycle(ServerWorld world) {
        try {
            BlockEntity blockEntity = prepareProbeBlockEntity(world, RESTART_PROBE_EGYPTIAN_STATUE_POS,
                    ModBlocks.EGYPTIAN_STATUE_PEDESTAL.getDefaultState());
            if (!(blockEntity instanceof EgyptianStatuePedestalBlockEntity egyptianStatue)) {
                return ValidationResult.fail("restart_cycle_egyptian_statue_prepare",
                        blockEntityProbeError(world, RESTART_PROBE_EGYPTIAN_STATUE_POS, blockEntity));
            }
            egyptianStatue.setStatue(GodPowerCertificateManager.EgyptianStatueType.PHARAOH);
            return verifyEgyptianStatueRestartCycle(world).rename("restart_cycle_egyptian_statue_prepare");
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_egyptian_statue_prepare", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult prepareAbundanceAltarRestartCycle(ServerWorld world) {
        try {
            BlockEntity blockEntity = prepareProbeBlockEntity(world, RESTART_PROBE_ABUNDANCE_ALTAR_POS,
                    ModBlocks.ABUNDANCE_ALTAR_CORE.getDefaultState());
            if (!(blockEntity instanceof AbundanceAltarCoreBlockEntity abundanceAltar)) {
                return ValidationResult.fail("restart_cycle_abundance_altar_prepare",
                        blockEntityProbeError(world, RESTART_PROBE_ABUNDANCE_ALTAR_POS, blockEntity));
            }
            NbtCompound nbt = new NbtCompound();
            nbt.putLong("NextGenerationTime", RESTART_EXPECTED_ABUNDANCE_TIME);
            abundanceAltar.readNbt(nbt);
            abundanceAltar.markDirty();
            return verifyAbundanceAltarRestartCycle(world).rename("restart_cycle_abundance_altar_prepare");
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_abundance_altar_prepare", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult verifyGuardianRestartCycle(ServerWorld world) {
        try {
            forceProbeChunk(world, RESTART_PROBE_GUARDIAN_POS);
            Entity entity = world.getEntity(RESTART_PROBE_GUARDIAN_UUID);
            if (!(entity instanceof CustomTempleGuardianEntity)) {
                return ValidationResult.fail("restart_cycle_guardian_verify", "guardian_missing_after_restart");
            }
            NbtCompound output = new NbtCompound();
            entity.writeNbt(output);
            List<String> missing = missingKeys(output, List.of(
                    "mythologyType",
                    "certificateGenerationTimer",
                    "hasInitializedTrades",
                    "templePosX",
                    "templePosY",
                    "templePosZ",
                    "tradeCooldowns"
            ));
            if (!"ATLANTEAN".equals(output.getString("mythologyType"))) {
                missing.add("mythologyType_value");
            }
            if (output.getInt("templePosX") != RESTART_PROBE_STATUE_BASE_POS.getX()
                    || output.getInt("templePosY") != RESTART_PROBE_STATUE_BASE_POS.getY()
                    || output.getInt("templePosZ") != RESTART_PROBE_STATUE_BASE_POS.getZ()) {
                missing.add("templePos_value");
            }
            if (!output.getCompound("tradeCooldowns").contains(ModItems.PEGASUS_SPAWN_EGG.toString())) {
                missing.add("tradeCooldowns.pegasus_spawn_egg");
            }
            if (!missing.isEmpty()) {
                return ValidationResult.fail("restart_cycle_guardian_verify", "missing_or_mismatched:" + String.join("|", missing));
            }
            return ValidationResult.pass("restart_cycle_guardian_verify", output.getKeys().size(), output.getKeys().size());
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_guardian_verify", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult verifyStatueBaseRestartCycle(ServerWorld world) {
        try {
            BlockEntity blockEntity = getProbeBlockEntity(world, RESTART_PROBE_STATUE_BASE_POS);
            if (!(blockEntity instanceof StatueBaseBlockEntity statueBase)) {
                return ValidationResult.fail("restart_cycle_statue_base_verify",
                        "block_entity_missing_after_restart:" + blockEntityProbeError(world, RESTART_PROBE_STATUE_BASE_POS, blockEntity));
            }
            NbtCompound output = statueBase.createNbt();
            List<String> missing = missingKeys(output, List.of("StoredCertificates", "LastUpdateTime", "MythologyType"));
            if (output.getInt("StoredCertificates") != RESTART_EXPECTED_STATUE_CERTIFICATES) {
                missing.add("StoredCertificates_value");
            }
            if (!"GREEK".equals(output.getString("MythologyType"))) {
                missing.add("MythologyType_value");
            }
            if (!missing.isEmpty()) {
                return ValidationResult.fail("restart_cycle_statue_base_verify", "missing_or_mismatched:" + String.join("|", missing));
            }
            return ValidationResult.pass("restart_cycle_statue_base_verify", output.getKeys().size(), output.getKeys().size());
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_statue_base_verify", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult verifyRelicPedestalRestartCycle(ServerWorld world) {
        try {
            BlockEntity blockEntity = getProbeBlockEntity(world, RESTART_PROBE_RELIC_PEDESTAL_POS);
            if (!(blockEntity instanceof RelicPedestalBlockEntity relicPedestal)) {
                return ValidationResult.fail("restart_cycle_relic_pedestal_verify",
                        "block_entity_missing_after_restart:" + blockEntityProbeError(world, RESTART_PROBE_RELIC_PEDESTAL_POS, blockEntity));
            }
            NbtCompound output = relicPedestal.createNbt();
            List<String> missing = missingKeys(output, List.of("RelicItem", "IsActiveTemple"));
            ItemStack relic = output.contains("RelicItem")
                    ? ItemStack.fromNbt(output.getCompound("RelicItem"))
                    : ItemStack.EMPTY;
            if (relic.getItem() != ModItems.RELIC_ANKH_OF_RA) {
                missing.add("RelicItem_value");
            }
            if (!missing.isEmpty()) {
                return ValidationResult.fail("restart_cycle_relic_pedestal_verify", "missing_or_mismatched:" + String.join("|", missing));
            }
            return ValidationResult.pass("restart_cycle_relic_pedestal_verify", output.getKeys().size(), output.getKeys().size());
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_relic_pedestal_verify", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult verifyEgyptianStatueRestartCycle(ServerWorld world) {
        try {
            BlockEntity blockEntity = getProbeBlockEntity(world, RESTART_PROBE_EGYPTIAN_STATUE_POS);
            if (!(blockEntity instanceof EgyptianStatuePedestalBlockEntity egyptianStatue)) {
                return ValidationResult.fail("restart_cycle_egyptian_statue_verify",
                        "block_entity_missing_after_restart:" + blockEntityProbeError(world, RESTART_PROBE_EGYPTIAN_STATUE_POS, blockEntity));
            }
            NbtCompound output = egyptianStatue.createNbt();
            List<String> missing = missingKeys(output, List.of("hasStatue", "productionTick", "statueType"));
            if (!output.getBoolean("hasStatue")) {
                missing.add("hasStatue_value");
            }
            if (!GodPowerCertificateManager.EgyptianStatueType.PHARAOH.getName().equals(output.getString("statueType"))) {
                missing.add("statueType_value");
            }
            if (!missing.isEmpty()) {
                return ValidationResult.fail("restart_cycle_egyptian_statue_verify", "missing_or_mismatched:" + String.join("|", missing));
            }
            return ValidationResult.pass("restart_cycle_egyptian_statue_verify", output.getKeys().size(), output.getKeys().size());
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_egyptian_statue_verify", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult verifyAbundanceAltarRestartCycle(ServerWorld world) {
        try {
            BlockEntity blockEntity = getProbeBlockEntity(world, RESTART_PROBE_ABUNDANCE_ALTAR_POS);
            if (!(blockEntity instanceof AbundanceAltarCoreBlockEntity abundanceAltar)) {
                return ValidationResult.fail("restart_cycle_abundance_altar_verify",
                        "block_entity_missing_after_restart:" + blockEntityProbeError(world, RESTART_PROBE_ABUNDANCE_ALTAR_POS, blockEntity));
            }
            NbtCompound output = abundanceAltar.createNbt();
            List<String> missing = missingKeys(output, List.of("NextGenerationTime"));
            if (output.getLong("NextGenerationTime") != RESTART_EXPECTED_ABUNDANCE_TIME) {
                missing.add("NextGenerationTime_value");
            }
            if (!missing.isEmpty()) {
                return ValidationResult.fail("restart_cycle_abundance_altar_verify", "missing_or_mismatched:" + String.join("|", missing));
            }
            return ValidationResult.pass("restart_cycle_abundance_altar_verify", output.getKeys().size(), output.getKeys().size());
        } catch (Exception e) {
            return ValidationResult.fail("restart_cycle_abundance_altar_verify", e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static ValidationResult validateCustomGuardianRestart(ServerWorld world, String entityId, String expectedMythologyType) {
        Identifier identifier = Identifier.tryParse(entityId);
        if (identifier == null) {
            return ValidationResult.fail(entityId, "invalid_identifier");
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(identifier);
        if (type == null) {
            return ValidationResult.fail(entityId, "entity_type_missing");
        }
        Entity entity = type.create(world);
        if (!(entity instanceof CustomTempleGuardianEntity guardian)) {
            return ValidationResult.fail(entityId, "not_custom_temple_guardian");
        }
        try {
            guardian.refreshPositionAndAngles(0.0D, 80.0D, 0.0D, 0.0F, 0.0F);
            NbtCompound sample = new NbtCompound();
            sample.putString("mythologyType", expectedMythologyType);
            sample.putInt("certificateGenerationTimer", 77);
            sample.putBoolean("hasInitializedTrades", true);
            sample.putInt("templePosX", 10);
            sample.putInt("templePosY", 72);
            sample.putInt("templePosZ", -10);
            NbtCompound cooldowns = new NbtCompound();
            cooldowns.putLong(ModItems.PEGASUS_SPAWN_EGG.toString(), 2400L);
            sample.put("tradeCooldowns", cooldowns);
            guardian.readNbt(sample);
            guardian.setTemplePos(new BlockPos(10, 72, -10));
            NbtCompound first = new NbtCompound();
            guardian.writeNbt(first);

            Entity roundTrip = type.create(world);
            if (!(roundTrip instanceof CustomTempleGuardianEntity roundTripGuardian)) {
                return ValidationResult.fail(entityId, "round_trip_not_custom_temple_guardian");
            }
            roundTripGuardian.readNbt(first.copy());
            NbtCompound second = new NbtCompound();
            roundTripGuardian.writeNbt(second);

            List<String> missing = missingKeys(second, List.of(
                    "mythologyType",
                    "certificateGenerationTimer",
                    "hasInitializedTrades",
                    "templePosX",
                    "templePosY",
                    "templePosZ",
                    "tradeCooldowns"
            ));
            if (!expectedMythologyType.equals(second.getString("mythologyType"))) {
                missing.add("mythologyType_value");
            }
            if (!second.getCompound("tradeCooldowns").contains(ModItems.PEGASUS_SPAWN_EGG.toString())) {
                missing.add("tradeCooldowns.pegasus_spawn_egg");
            }
            if (!missing.isEmpty()) {
                return ValidationResult.fail(entityId, "missing_after_restart_round_trip:" + String.join("|", missing));
            }
            return ValidationResult.pass(entityId, first.getKeys().size(), second.getKeys().size());
        } catch (Exception e) {
            return ValidationResult.fail(entityId, e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static BlockEntity prepareProbeBlockEntity(ServerWorld world, BlockPos pos, BlockState state) {
        forceProbeChunk(world, pos);
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        if (!world.setBlockState(pos, state, Block.NOTIFY_ALL)) {
            return null;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null && state.getBlock() instanceof BlockEntityProvider provider) {
            blockEntity = provider.createBlockEntity(pos, state);
            if (blockEntity != null) {
                world.addBlockEntity(blockEntity);
            }
        }
        if (blockEntity != null) {
            blockEntity.markDirty();
        }
        return blockEntity;
    }

    private static BlockEntity getProbeBlockEntity(ServerWorld world, BlockPos pos) {
        forceProbeChunk(world, pos);
        return world.getBlockEntity(pos);
    }

    private static void forceProbeChunk(ServerWorld world, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        world.setChunkForced(chunkX, chunkZ, true);
        world.getChunk(chunkX, chunkZ);
    }

    private static String blockEntityProbeError(ServerWorld world, BlockPos pos, BlockEntity blockEntity) {
        return "block_entity_missing_or_wrong_type:block="
                + Registries.BLOCK.getId(world.getBlockState(pos).getBlock())
                + ",block_entity="
                + (blockEntity == null ? "null" : blockEntity.getClass().getName());
    }

    private static ValidationResult validateStatueBaseRestart() {
        StatueBaseBlockEntity first = new StatueBaseBlockEntity(BlockPos.ORIGIN, ModBlocks.STATUE_GREEK_BASE.getDefaultState(), GodPowerCertificateItem.MythologyType.GREEK);
        first.addCertificates(5);
        NbtCompound nbt = first.createNbt();
        StatueBaseBlockEntity second = new StatueBaseBlockEntity(BlockPos.ORIGIN, ModBlocks.STATUE_GREEK_BASE.getDefaultState(), GodPowerCertificateItem.MythologyType.GREEK);
        second.readNbt(nbt.copy());
        NbtCompound output = second.createNbt();
        return resultForKeys("statue_base", nbt, output, List.of("StoredCertificates", "LastUpdateTime", "MythologyType"));
    }

    private static ValidationResult validateRelicPedestalRestart() {
        RelicPedestalBlockEntity first = new RelicPedestalBlockEntity(BlockPos.ORIGIN, ModBlocks.RELIC_PEDESTAL.getDefaultState());
        first.setRelicStack(new ItemStack(ModItems.RELIC_ANKH_OF_RA));
        NbtCompound nbt = first.createNbt();
        RelicPedestalBlockEntity second = new RelicPedestalBlockEntity(BlockPos.ORIGIN, ModBlocks.RELIC_PEDESTAL.getDefaultState());
        second.readNbt(nbt.copy());
        NbtCompound output = second.createNbt();
        return resultForKeys("relic_pedestal", nbt, output, List.of("RelicItem", "IsActiveTemple"));
    }

    private static ValidationResult validateEgyptianStatuePedestalRestart() {
        EgyptianStatuePedestalBlockEntity first = new EgyptianStatuePedestalBlockEntity(BlockPos.ORIGIN, ModBlocks.EGYPTIAN_STATUE_PEDESTAL.getDefaultState());
        first.setStatue(GodPowerCertificateManager.EgyptianStatueType.PHARAOH);
        NbtCompound nbt = first.createNbt();
        EgyptianStatuePedestalBlockEntity second = new EgyptianStatuePedestalBlockEntity(BlockPos.ORIGIN, ModBlocks.EGYPTIAN_STATUE_PEDESTAL.getDefaultState());
        second.readNbt(nbt.copy());
        NbtCompound output = second.createNbt();
        return resultForKeys("egyptian_statue_pedestal", nbt, output, List.of("hasStatue", "productionTick", "statueType"));
    }

    private static ValidationResult validateAbundanceAltarRestart() {
        AbundanceAltarCoreBlockEntity first = new AbundanceAltarCoreBlockEntity(BlockPos.ORIGIN, ModBlocks.ABUNDANCE_ALTAR_CORE.getDefaultState());
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("NextGenerationTime", 12345L);
        first.readNbt(nbt.copy());
        NbtCompound firstOutput = first.createNbt();
        AbundanceAltarCoreBlockEntity second = new AbundanceAltarCoreBlockEntity(BlockPos.ORIGIN, ModBlocks.ABUNDANCE_ALTAR_CORE.getDefaultState());
        second.readNbt(firstOutput.copy());
        NbtCompound output = second.createNbt();
        return resultForKeys("abundance_altar_core", firstOutput, output, List.of("NextGenerationTime"));
    }

    private static ValidationResult resultForKeys(String id, NbtCompound first, NbtCompound second, List<String> keys) {
        List<String> missing = missingKeys(second, keys);
        if (!missing.isEmpty()) {
            return ValidationResult.fail(id, "missing_after_restart_round_trip:" + String.join("|", missing));
        }
        return ValidationResult.pass(id, first.getKeys().size(), second.getKeys().size());
    }

    private static List<String> missingKeys(NbtCompound nbt, List<String> keys) {
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (!nbt.contains(key)) {
                missing.add(key);
            }
        }
        return missing;
    }

    private static ValidationResult validateEntity(ServerWorld world, String entityId) {
        Identifier identifier = Identifier.tryParse(entityId);
        if (identifier == null) {
            return ValidationResult.fail(entityId, "invalid_identifier");
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(identifier);
        if (type == null) {
            return ValidationResult.fail(entityId, "entity_type_missing");
        }
        Entity entity = type.create(world);
        if (entity == null) {
            return ValidationResult.fail(entityId, "entity_create_returned_null");
        }
        try {
            entity.refreshPositionAndAngles(0.0D, 80.0D, 0.0D, 0.0F, 0.0F);
            applySampleNbt(entityId, entity);
            NbtCompound first = new NbtCompound();
            entity.writeNbt(first);
            Entity roundTripEntity = type.create(world);
            if (roundTripEntity == null) {
                return ValidationResult.fail(entityId, "round_trip_create_returned_null");
            }
            roundTripEntity.refreshPositionAndAngles(0.0D, 80.0D, 0.0D, 0.0F, 0.0F);
            roundTripEntity.readNbt(first.copy());
            NbtCompound second = new NbtCompound();
            roundTripEntity.writeNbt(second);
            List<String> required = requiredKeys(entityId);
            List<String> missing = new ArrayList<>();
            for (String key : required) {
                if (!second.contains(key)) {
                    missing.add(key);
                }
            }
            if (!missing.isEmpty()) {
                return ValidationResult.fail(entityId, "missing_keys_after_round_trip:" + String.join("|", missing));
            }
            return ValidationResult.pass(entityId, first.getKeys().size(), second.getKeys().size());
        } catch (Exception e) {
            return ValidationResult.fail(entityId, e.getClass().getSimpleName() + ":" + e.getMessage());
        }
    }

    private static void applySampleNbt(String entityId, Entity entity) {
        NbtCompound sample = new NbtCompound();
        if ("mythic_age:walking_tree".equals(entityId)) {
            sample.putString("walkingTreeVariant", "oak");
            sample.putInt("walkingTreeLifetimeTicks", 120);
            sample.putInt("guardPosX", 2);
            sample.putInt("guardPosY", 80);
            sample.putInt("guardPosZ", -2);
        } else if ("mythic_age:meteor".equals(entityId)) {
            sample.putInt("Life", 12);
            sample.putFloat("ExplosionPower", 4.0F);
            sample.putBoolean("PlayedFallingSound", true);
            sample.putBoolean("PlayedWarningSound", false);
        } else if (entityId.startsWith("mythic_age:custom_temple_guardian")) {
            sample.putString("mythologyType", mythologyTypeFor(entityId));
            sample.putInt("certificateGenerationTimer", 42);
            sample.putBoolean("hasInitializedTrades", true);
            sample.putInt("templePosX", 4);
            sample.putInt("templePosY", 80);
            sample.putInt("templePosZ", 4);
        }
        if (!sample.isEmpty()) {
            entity.readNbt(sample);
        }
    }

    private static List<String> requiredKeys(String entityId) {
        List<String> keys = new ArrayList<>();
        if ("mythic_age:walking_tree".equals(entityId)) {
            keys.add("walkingTreeVariant");
            keys.add("walkingTreeLifetimeTicks");
            keys.add("guardPosX");
            keys.add("guardPosY");
            keys.add("guardPosZ");
        } else if ("mythic_age:meteor".equals(entityId)) {
            keys.add("Life");
            keys.add("ExplosionPower");
            keys.add("PlayedFallingSound");
            keys.add("PlayedWarningSound");
        } else if (entityId.startsWith("mythic_age:custom_temple_guardian")) {
            keys.add("mythologyType");
            keys.add("certificateGenerationTimer");
            keys.add("hasInitializedTrades");
            keys.add("templePosX");
            keys.add("templePosY");
            keys.add("templePosZ");
        }
        return keys;
    }

    private static String mythologyTypeFor(String entityId) {
        if (entityId.endsWith("_egyptian")) {
            return "EGYPTIAN";
        }
        if (entityId.endsWith("_chinese")) {
            return "CHINESE";
        }
        if (entityId.endsWith("_atlantean")) {
            return "ATLANTEAN";
        }
        if (entityId.endsWith("_nordic")) {
            return "NORDIC";
        }
        return "GREEK";
    }

    private static String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String blockPosJson(BlockPos pos) {
        return "{"
                + "\"x\":" + pos.getX() + ","
                + "\"y\":" + pos.getY() + ","
                + "\"z\":" + pos.getZ()
                + "}";
    }

    private static long currentProcessId() {
        return ProcessHandle.current().pid();
    }

    public record NbtValidationReport(int total, int passed, int failed, File output, String payload) {
    }

    public record RestartNbtValidationReport(int total, int passed, int failed, File output, String payload) {
    }

    public record GodPowerPlaytestReport(int total, int passed, int failed, File output, String payload) {
    }

    private record GodPowerPlaytestCase(String civilization, SpellbookItem.SpellType spellType, Item item) {
    }

    private record GodPowerPlaytestResult(String civilization, String itemId, String spellType, boolean ok,
                                          String actionResult, boolean cooldownSet, int spawnedEntities,
                                          long durationMs, String error, BlockPos probeOrigin) {
        static GodPowerPlaytestResult fail(GodPowerPlaytestCase testCase, String error) {
            return new GodPowerPlaytestResult(
                    testCase.civilization(),
                    Registries.ITEM.getId(testCase.item()).toString(),
                    testCase.spellType().name(),
                    false,
                    "not_run",
                    false,
                    0,
                    0L,
                    error,
                    GOD_POWER_PROBE_ORIGIN
            );
        }

        String toJson() {
            return "{"
                    + "\"civilization\":\"" + jsonEscape(civilization) + "\","
                    + "\"item_id\":\"" + jsonEscape(itemId) + "\","
                    + "\"spell_type\":\"" + jsonEscape(spellType) + "\","
                    + "\"ok\":" + ok + ","
                    + "\"action_result\":\"" + jsonEscape(actionResult) + "\","
                    + "\"cooldown_set\":" + cooldownSet + ","
                    + "\"spawned_entities\":" + spawnedEntities + ","
                    + "\"duration_ms\":" + durationMs + ","
                    + "\"probe_origin\":" + blockPosJson(probeOrigin) + ","
                    + "\"error\":\"" + jsonEscape(error) + "\""
                    + "}";
        }
    }

    private record ValidationResult(String entityId, boolean ok, String error, int firstKeyCount, int secondKeyCount) {
        static ValidationResult pass(String entityId, int firstKeyCount, int secondKeyCount) {
            return new ValidationResult(entityId, true, "", firstKeyCount, secondKeyCount);
        }

        static ValidationResult fail(String entityId, String error) {
            return new ValidationResult(entityId, false, error, 0, 0);
        }

        ValidationResult rename(String newEntityId) {
            return new ValidationResult(newEntityId, ok, error, firstKeyCount, secondKeyCount);
        }

        String toJson() {
            return "{"
                    + "\"entity_id\":\"" + jsonEscape(entityId) + "\","
                    + "\"ok\":" + ok + ","
                    + "\"error\":\"" + jsonEscape(error) + "\","
                    + "\"first_key_count\":" + firstKeyCount + ","
                    + "\"second_key_count\":" + secondKeyCount
                    + "}";
        }
    }
}
