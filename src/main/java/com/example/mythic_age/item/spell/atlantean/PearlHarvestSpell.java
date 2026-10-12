package com.example.mythic_age.item.spell.atlantean;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.particle.ModParticles;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class PearlHarvestSpell extends AbstractSpell {
    public PearlHarvestSpell() { super(SpellType.PEARL_HARVEST, MythologyType.ATLANTEAN, SpellCategory.RESOURCE, 200); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        ItemStack pearls = new ItemStack(Items.ENDER_PEARL, 2);
        ItemStack nautilus = new ItemStack(Items.NAUTILUS_SHELL, 1);
        ItemStack prismarine = new ItemStack(Items.PRISMARINE_CRYSTALS, 8);
        world.spawnEntity(new ItemEntity(world, targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5, pearls));
        world.spawnEntity(new ItemEntity(world, targetPos.getX() + 0.8, targetPos.getY() + 1.0, targetPos.getZ() + 0.2, nautilus));
        world.spawnEntity(new ItemEntity(world, targetPos.getX() + 0.2, targetPos.getY() + 1.0, targetPos.getZ() + 0.8, prismarine));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 3600, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 2400, 0));

        MythicParticleEffects.spawnMajor(world, ParticleTypes.NAUTILUS,
            targetPos.getX() + 0.5, targetPos.getY() + 1.1, targetPos.getZ() + 0.5,
            80, 2.2, 1.4, 2.2, 0.08);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.HAPPY_VILLAGER,
            targetPos.getX() + 0.5, targetPos.getY() + 1.3, targetPos.getZ() + 0.5,
            45, 1.8, 1.2, 1.8, 0.05);
        MythicParticleEffects.spawnNormal(world, ModParticles.DIVINE_SPARKLE,
            targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5,
            35, 1.8, 1.1, 1.8, 0.04);
        world.playSound(null, targetPos, SoundEvents.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 1.1f, 1.35f);
        world.playSound(null, targetPos, SoundEvents.ITEM_BUCKET_FILL_FISH, SoundCategory.PLAYERS, 1.0f, 1.1f);
        sendMessage(caster, "mythic_age.spellbook.message.pearl_harvest.success");
        return true;
    }
}
