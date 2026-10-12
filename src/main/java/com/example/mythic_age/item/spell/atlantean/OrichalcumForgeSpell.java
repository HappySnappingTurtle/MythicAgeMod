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

public class OrichalcumForgeSpell extends AbstractSpell {
    public OrichalcumForgeSpell() { super(SpellType.ORICHALCUM_FORGE, MythologyType.ATLANTEAN, SpellCategory.RESOURCE, 200); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 3600, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 2400, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 2400, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 3600, 1));

        ItemStack copper = new ItemStack(Items.COPPER_INGOT, 8);
        ItemStack crystals = new ItemStack(Items.PRISMARINE_CRYSTALS, 6);
        ItemStack shards = new ItemStack(Items.PRISMARINE_SHARD, 10);
        world.spawnEntity(new ItemEntity(world, targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5, copper));
        world.spawnEntity(new ItemEntity(world, targetPos.getX() + 0.8, targetPos.getY() + 1.0, targetPos.getZ() + 0.2, crystals));
        world.spawnEntity(new ItemEntity(world, targetPos.getX() + 0.2, targetPos.getY() + 1.0, targetPos.getZ() + 0.8, shards));

        MythicParticleEffects.spawnMajor(world, ParticleTypes.CRIT,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            70, 1.8, 1.4, 1.8, 0.16);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.ENCHANT,
            targetPos.getX() + 0.5, targetPos.getY() + 1.3, targetPos.getZ() + 0.5,
            90, 2.2, 1.2, 2.2, 0.08);
        MythicParticleEffects.spawnNormal(world, ModParticles.DIVINE_SPARKLE,
            targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5,
            45, 2.0, 1.2, 2.0, 0.05);

        world.playSound(null, targetPos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.3f, 0.75f);
        world.playSound(null, targetPos, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 1.1f, 0.9f);
        sendMessage(caster, "mythic_age.spellbook.message.orichalcum_forge.success");
        return true;
    }
}
