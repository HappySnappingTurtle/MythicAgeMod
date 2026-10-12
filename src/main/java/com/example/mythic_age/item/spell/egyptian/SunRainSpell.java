package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class SunRainSpell extends AbstractSpell {
    public SunRainSpell() { super(SpellType.SUN_RAIN, MythologyType.EGYPTIAN, SpellCategory.HEALING, 180); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Promote plant growth in target area
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                BlockPos pos = targetPos.add(x, 0, z);
                BlockState groundState = world.getBlockState(pos.down());
                BlockState currentState = world.getBlockState(pos);

                // Convert dirt to farmland
                if (groundState.isOf(Blocks.DIRT) || groundState.isOf(Blocks.GRASS_BLOCK)) {
                    world.setBlockState(pos.down(), Blocks.FARMLAND.getDefaultState());
                }

                // Plant crops
                if (currentState.isAir() && world.random.nextFloat() < 0.4f) {
                    Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
                    Block selectedCrop = crops[world.random.nextInt(crops.length)];
                    world.setBlockState(pos, selectedCrop.getDefaultState());
                }

                // Ripen existing crops
                if (currentState.getBlock() instanceof CropBlock) {
                    CropBlock crop = (CropBlock) currentState.getBlock();
                    int maxAge = crop.getMaxAge();
                    world.setBlockState(pos, currentState.with(Properties.AGE_7, maxAge));
                }

                // Spawn flowers
                if (currentState.isAir() && world.random.nextFloat() < 0.2f) {
                    Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM};
                    Block selectedFlower = flowers[world.random.nextInt(flowers.length)];
                    world.setBlockState(pos, selectedFlower.getDefaultState());
                }
            }
        }

        // Heal nearby animals
        List<LivingEntity> animals = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(10),
            entity -> entity instanceof AnimalEntity && entity.isAlive());

        for (LivingEntity animal : animals) {
            animal.heal(6.0f);
            animal.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 3000, 0));

            // Put animals into breeding mode
            if (animal instanceof AnimalEntity) {
                ((AnimalEntity) animal).setLoveTicks(600);
            }
        }

        // Golden rain particle effect
        for (int i = 0; i < 100; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 24;
            double y = targetPos.getY() + world.random.nextDouble() * 10 + 5;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 24;
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.DRIPPING_HONEY, x, y, z, 1, 0, -0.2, 0, 0.1);
        }

        sendMessage(caster, "mythic_age.spellbook.message.sun_rain.success");
        return true;
    }
}
