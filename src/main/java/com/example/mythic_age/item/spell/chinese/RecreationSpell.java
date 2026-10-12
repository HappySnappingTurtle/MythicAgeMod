package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class RecreationSpell extends AbstractSpell {
    public RecreationSpell() { super(SpellType.RECREATION, MythologyType.CHINESE, SpellCategory.HEALING, 500); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 造化 - 创造生命和植物
        // 在周围生成各种植物
        for (int i = 0; i < 30; i++) {
            double angle = (i * 12.0) * Math.PI / 180.0;
            double radius = 3.0 + world.random.nextDouble() * 5.0;
            int x = (int) (targetPos.getX() + Math.cos(angle) * radius);
            int z = (int) (targetPos.getZ() + Math.sin(angle) * radius);

            BlockPos plantPos = new BlockPos(x, targetPos.getY(), z);

            if (world.getBlockState(plantPos).isAir() &&
                (world.getBlockState(plantPos.down()).getBlock() == Blocks.GRASS_BLOCK ||
                 world.getBlockState(plantPos.down()).getBlock() == Blocks.DIRT)) {

                // 随机生成不同的植物
                Block[] plants = {Blocks.GRASS, Blocks.FERN, Blocks.DANDELION,
                                Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM};
                Block plant = plants[world.random.nextInt(plants.length)];
                world.setBlockState(plantPos, plant.getDefaultState());
            }
        }

        // 召唤一些友好生物
        for (int i = 0; i < 3; i++) {
            double angle = (i * 120.0) * Math.PI / 180.0;
            double x = targetPos.getX() + Math.cos(angle) * 4;
            double z = targetPos.getZ() + Math.sin(angle) * 4;

            // 随机召唤不同的动物
            if (world.random.nextBoolean()) {
                PigEntity pig = new PigEntity(EntityType.PIG, world);
                pig.refreshPositionAfterTeleport(x, targetPos.getY(), z);
                world.spawnEntity(pig);
            } else {
                WolfEntity wolf = new WolfEntity(EntityType.WOLF, world);
                wolf.refreshPositionAfterTeleport(x, targetPos.getY(), z);
                wolf.setOwner(caster);
                world.spawnEntity(wolf);
            }
        }

        // 给予玩家造化之力
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 3000, 1));

        MythicParticleEffects.spawnMajor(world, ParticleTypes.HEART,
            targetPos.getX(), targetPos.getY() + 1, targetPos.getZ(),
            50, 5.0, 3.0, 5.0, 0.1);

        world.playSound(null, targetPos, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.PLAYERS, 1.5f, 1.2f);
        sendMessage(caster, "mythic_age.spellbook.message.recreation.success");

        return true;
    }
}
