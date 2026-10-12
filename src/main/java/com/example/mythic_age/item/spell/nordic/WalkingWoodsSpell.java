package com.example.mythic_age.item.spell.nordic;

import com.example.mythic_age.entity.ModEntities;
import com.example.mythic_age.entity.WalkingTreeEntity;
import com.example.mythic_age.entity.WalkingTreeVariant;
import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WalkingWoodsSpell extends AbstractSpell {

    private static final int WALKING_WOODS_SEARCH_RADIUS = 14;
    private static final int WALKING_WOODS_VERTICAL_RADIUS = 10;
    private static final int WALKING_WOODS_MIN_LOGS_PER_TREE = 3;
    private static final int WALKING_WOODS_MAX_TREES = 8;

    public WalkingWoodsSpell() { super(SpellType.WALKING_WOODS, MythologyType.NORDIC, SpellCategory.UTILITY, 400); }

    private static class WalkingWoodSource {
        private final BlockPos basePos;
        private final WalkingTreeVariant variant;
        private final List<BlockPos> logPositions;
        private final Set<BlockPos> leafPositions;

        private WalkingWoodSource(BlockPos basePos, WalkingTreeVariant variant, List<BlockPos> logPositions, Set<BlockPos> leafPositions) {
            this.basePos = basePos;
            this.variant = variant;
            this.logPositions = logPositions;
            this.leafPositions = leafPositions;
        }
    }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        BlockPos center = caster.getBlockPos();
        List<WalkingWoodSource> nearbyTrees = findWalkingWoodSources(world, center);

        if (nearbyTrees.isEmpty()) {
            sendMessage(caster, "mythic_age.spellbook.message.walking_woods.no_trees");
            return false;
        }

        int awakenedCount = Math.min(nearbyTrees.size(), WALKING_WOODS_MAX_TREES);
        for (int i = 0; i < awakenedCount; i++) {
            WalkingWoodSource source = nearbyTrees.get(i);
            removeWalkingWoodSource(world, source);

            BlockPos spawnPos = source.basePos;
            WalkingTreeEntity walkingTree = new WalkingTreeEntity(ModEntities.WALKING_TREE, world);
            walkingTree.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            walkingTree.setGuardPos(spawnPos);
            walkingTree.setSummoner(caster);
            walkingTree.setVariant(source.variant);

            world.spawnEntity(walkingTree);

            MythicParticleEffects.spawnMajor(
                world,
                new BlockStateParticleEffect(ParticleTypes.BLOCK, getWalkingTreeLogBlock(source.variant).getDefaultState()),
                spawnPos.getX() + 0.5,
                spawnPos.getY() + 1.0,
                spawnPos.getZ() + 0.5,
                24,
                0.7,
                1.0,
                0.7,
                0.08
            );
        }

        // Grant nature blessing
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));

        world.playSound(null, center, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.PLAYERS, 2.0f, 0.8f);
        sendMessage(caster, "mythic_age.spellbook.message.walking_woods.success", awakenedCount);

        return true;
    }

    private List<WalkingWoodSource> findWalkingWoodSources(ServerWorld world, BlockPos center) {
        Set<BlockPos> logPositions = new HashSet<>();
        for (int dx = -WALKING_WOODS_SEARCH_RADIUS; dx <= WALKING_WOODS_SEARCH_RADIUS; dx++) {
            for (int dz = -WALKING_WOODS_SEARCH_RADIUS; dz <= WALKING_WOODS_SEARCH_RADIUS; dz++) {
                if (dx * dx + dz * dz > WALKING_WOODS_SEARCH_RADIUS * WALKING_WOODS_SEARCH_RADIUS) {
                    continue;
                }
                for (int dy = -WALKING_WOODS_VERTICAL_RADIUS; dy <= WALKING_WOODS_VERTICAL_RADIUS; dy++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    if (isWalkingTreeLogBlock(world.getBlockState(pos).getBlock())) {
                        logPositions.add(pos.toImmutable());
                    }
                }
            }
        }

        List<WalkingWoodSource> sources = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos start : logPositions) {
            if (visited.contains(start)) {
                continue;
            }

            List<BlockPos> cluster = new ArrayList<>();
            List<BlockPos> queue = new ArrayList<>();
            queue.add(start);
            visited.add(start);
            for (int i = 0; i < queue.size(); i++) {
                BlockPos current = queue.get(i);
                cluster.add(current);
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.offset(direction).toImmutable();
                    if (logPositions.contains(next) && visited.add(next)) {
                        queue.add(next);
                    }
                }
            }

            if (cluster.size() < WALKING_WOODS_MIN_LOGS_PER_TREE) {
                continue;
            }

            WalkingTreeVariant variant = determineWalkingTreeVariantFromCluster(world, cluster);
            Set<BlockPos> leaves = collectWalkingTreeLeaves(world, cluster, variant);
            if (leaves.isEmpty()) {
                continue;
            }

            BlockPos basePos = cluster.stream()
                .min(Comparator.comparingInt(BlockPos::getY))
                .orElse(start)
                .toImmutable();
            sources.add(new WalkingWoodSource(basePos, variant, cluster, leaves));
        }

        sources.sort(Comparator.comparingDouble(source -> source.basePos.getSquaredDistance(center)));
        return sources;
    }

    private WalkingTreeVariant determineWalkingTreeVariantFromCluster(ServerWorld world, List<BlockPos> cluster) {
        Map<WalkingTreeVariant, Integer> scores = new EnumMap<>(WalkingTreeVariant.class);
        for (WalkingTreeVariant value : WalkingTreeVariant.values()) {
            scores.put(value, 0);
        }
        for (BlockPos pos : cluster) {
            WalkingTreeVariant variant = getWalkingTreeVariantForBlock(world.getBlockState(pos).getBlock());
            if (variant != null) {
                scores.put(variant, scores.get(variant) + 1);
            }
        }

        WalkingTreeVariant bestVariant = WalkingTreeVariant.OAK;
        int bestScore = -1;
        for (Map.Entry<WalkingTreeVariant, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestVariant = entry.getKey();
                bestScore = entry.getValue();
            }
        }
        return bestVariant;
    }

    private Set<BlockPos> collectWalkingTreeLeaves(ServerWorld world, List<BlockPos> cluster, WalkingTreeVariant variant) {
        Set<BlockPos> leaves = new HashSet<>();
        for (BlockPos logPos : cluster) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = -2; dy <= 7; dy++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        BlockPos pos = logPos.add(dx, dy, dz);
                        Block block = world.getBlockState(pos).getBlock();
                        if (isWalkingTreeLeafBlock(block) && getWalkingTreeVariantForBlock(block) == variant) {
                            leaves.add(pos.toImmutable());
                        }
                    }
                }
            }
        }
        return leaves;
    }

    private void removeWalkingWoodSource(ServerWorld world, WalkingWoodSource source) {
        for (BlockPos pos : source.logPositions) {
            if (isWalkingTreeLogBlock(world.getBlockState(pos).getBlock())) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        for (BlockPos pos : source.leafPositions) {
            if (isWalkingTreeLeafBlock(world.getBlockState(pos).getBlock())) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }

    private @Nullable WalkingTreeVariant getWalkingTreeVariantForBlock(Block block) {
        if (block == Blocks.OAK_LOG || block == Blocks.OAK_WOOD ||
            block == Blocks.STRIPPED_OAK_LOG || block == Blocks.STRIPPED_OAK_WOOD ||
            block == Blocks.OAK_LEAVES) {
            return WalkingTreeVariant.OAK;
        }
        if (block == Blocks.SPRUCE_LOG || block == Blocks.SPRUCE_WOOD ||
            block == Blocks.STRIPPED_SPRUCE_LOG || block == Blocks.STRIPPED_SPRUCE_WOOD ||
            block == Blocks.SPRUCE_LEAVES) {
            return WalkingTreeVariant.SPRUCE;
        }
        if (block == Blocks.BIRCH_LOG || block == Blocks.BIRCH_WOOD ||
            block == Blocks.STRIPPED_BIRCH_LOG || block == Blocks.STRIPPED_BIRCH_WOOD ||
            block == Blocks.BIRCH_LEAVES) {
            return WalkingTreeVariant.BIRCH;
        }
        if (block == Blocks.JUNGLE_LOG || block == Blocks.JUNGLE_WOOD ||
            block == Blocks.STRIPPED_JUNGLE_LOG || block == Blocks.STRIPPED_JUNGLE_WOOD ||
            block == Blocks.JUNGLE_LEAVES) {
            return WalkingTreeVariant.JUNGLE;
        }
        if (block == Blocks.ACACIA_LOG || block == Blocks.ACACIA_WOOD ||
            block == Blocks.STRIPPED_ACACIA_LOG || block == Blocks.STRIPPED_ACACIA_WOOD ||
            block == Blocks.ACACIA_LEAVES) {
            return WalkingTreeVariant.ACACIA;
        }
        if (block == Blocks.DARK_OAK_LOG || block == Blocks.DARK_OAK_WOOD ||
            block == Blocks.STRIPPED_DARK_OAK_LOG || block == Blocks.STRIPPED_DARK_OAK_WOOD ||
            block == Blocks.DARK_OAK_LEAVES) {
            return WalkingTreeVariant.DARK_OAK;
        }
        if (block == Blocks.MANGROVE_LOG || block == Blocks.MANGROVE_WOOD ||
            block == Blocks.STRIPPED_MANGROVE_LOG || block == Blocks.STRIPPED_MANGROVE_WOOD ||
            block == Blocks.MANGROVE_LEAVES || block == Blocks.MANGROVE_ROOTS ||
            block == Blocks.MUDDY_MANGROVE_ROOTS) {
            return WalkingTreeVariant.MANGROVE;
        }
        if (block == Blocks.CHERRY_LOG || block == Blocks.CHERRY_WOOD ||
            block == Blocks.STRIPPED_CHERRY_LOG || block == Blocks.STRIPPED_CHERRY_WOOD ||
            block == Blocks.CHERRY_LEAVES) {
            return WalkingTreeVariant.CHERRY;
        }
        return null;
    }

    private boolean isWalkingTreeLogBlock(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.OAK_WOOD ||
            block == Blocks.STRIPPED_OAK_LOG || block == Blocks.STRIPPED_OAK_WOOD ||
            block == Blocks.SPRUCE_LOG || block == Blocks.SPRUCE_WOOD ||
            block == Blocks.STRIPPED_SPRUCE_LOG || block == Blocks.STRIPPED_SPRUCE_WOOD ||
            block == Blocks.BIRCH_LOG || block == Blocks.BIRCH_WOOD ||
            block == Blocks.STRIPPED_BIRCH_LOG || block == Blocks.STRIPPED_BIRCH_WOOD ||
            block == Blocks.JUNGLE_LOG || block == Blocks.JUNGLE_WOOD ||
            block == Blocks.STRIPPED_JUNGLE_LOG || block == Blocks.STRIPPED_JUNGLE_WOOD ||
            block == Blocks.ACACIA_LOG || block == Blocks.ACACIA_WOOD ||
            block == Blocks.STRIPPED_ACACIA_LOG || block == Blocks.STRIPPED_ACACIA_WOOD ||
            block == Blocks.DARK_OAK_LOG || block == Blocks.DARK_OAK_WOOD ||
            block == Blocks.STRIPPED_DARK_OAK_LOG || block == Blocks.STRIPPED_DARK_OAK_WOOD ||
            block == Blocks.MANGROVE_LOG || block == Blocks.MANGROVE_WOOD ||
            block == Blocks.STRIPPED_MANGROVE_LOG || block == Blocks.STRIPPED_MANGROVE_WOOD ||
            block == Blocks.MANGROVE_ROOTS || block == Blocks.MUDDY_MANGROVE_ROOTS ||
            block == Blocks.CHERRY_LOG || block == Blocks.CHERRY_WOOD ||
            block == Blocks.STRIPPED_CHERRY_LOG || block == Blocks.STRIPPED_CHERRY_WOOD;
    }

    private boolean isWalkingTreeLeafBlock(Block block) {
        return block == Blocks.OAK_LEAVES ||
            block == Blocks.SPRUCE_LEAVES ||
            block == Blocks.BIRCH_LEAVES ||
            block == Blocks.JUNGLE_LEAVES ||
            block == Blocks.ACACIA_LEAVES ||
            block == Blocks.DARK_OAK_LEAVES ||
            block == Blocks.MANGROVE_LEAVES ||
            block == Blocks.CHERRY_LEAVES;
    }

    private Block getWalkingTreeLogBlock(WalkingTreeVariant variant) {
        return switch (variant) {
            case OAK -> Blocks.OAK_LOG;
            case SPRUCE -> Blocks.SPRUCE_LOG;
            case BIRCH -> Blocks.BIRCH_LOG;
            case JUNGLE -> Blocks.JUNGLE_LOG;
            case ACACIA -> Blocks.ACACIA_LOG;
            case DARK_OAK -> Blocks.DARK_OAK_LOG;
            case MANGROVE -> Blocks.MANGROVE_LOG;
            case CHERRY -> Blocks.CHERRY_LOG;
        };
    }
}
