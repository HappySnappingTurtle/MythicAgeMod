package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ProsperitySpell extends AbstractSpell {
    public ProsperitySpell() { super(SpellType.PROSPERITY, MythologyType.EGYPTIAN, SpellCategory.RESOURCE, 200); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Generate precious resources around the area
        for (int i = 0; i < 12; i++) {
            BlockPos resourcePos = targetPos.add(
                world.random.nextInt(11) - 5,
                world.random.nextInt(3) - 1,
                world.random.nextInt(11) - 5
            );

            if (world.getBlockState(resourcePos).isAir() || world.getBlockState(resourcePos).isOf(Blocks.STONE)) {
                Block[] ores = {Blocks.GOLD_ORE, Blocks.IRON_ORE, Blocks.COAL_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE};
                Block selectedOre = ores[world.random.nextInt(ores.length)];
                world.setBlockState(resourcePos, selectedOre.getDefaultState());
            }
        }

        // Grant luck effects to caster
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 12000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 12000, 1));

        // Add valuable items to caster's inventory
        ItemStack[] treasures = {
            new ItemStack(Items.GOLD_INGOT, 2 + world.random.nextInt(4)),
            new ItemStack(Items.DIAMOND, 1 + world.random.nextInt(2)),
            new ItemStack(Items.EMERALD, 1 + world.random.nextInt(3)),
            new ItemStack(Items.EXPERIENCE_BOTTLE, 3 + world.random.nextInt(5))
        };

        for (ItemStack treasure : treasures) {
            if (!caster.getInventory().insertStack(treasure)) {
                // If inventory is full, drop on the ground
                ItemEntity itemEntity = new ItemEntity(world, caster.getX(), caster.getY(), caster.getZ(), treasure);
                world.spawnEntity(itemEntity);
            }
        }

        // Golden wealth particle effect
        for (int i = 0; i < 80; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 20;
            double y = targetPos.getY() + world.random.nextDouble() * 6;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 20;
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 1, 0, 0.1, 0, 0.1);
        }

        sendMessage(caster, "mythic_age.spellbook.message.prosperity.success");
        return true;
    }
}
