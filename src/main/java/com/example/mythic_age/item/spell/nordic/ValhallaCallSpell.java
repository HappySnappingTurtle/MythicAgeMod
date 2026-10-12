package com.example.mythic_age.item.spell.nordic;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class ValhallaCallSpell extends AbstractSpell {
    public ValhallaCallSpell() { super(SpellType.VALHALLA_CALL, MythologyType.NORDIC, SpellCategory.SUMMONING, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Summon skeleton warriors as Einherjar (fallen warriors)
        for (int i = 0; i < 6; i++) {
            double angle = (i * 60.0) * Math.PI / 180.0;
            double x = targetPos.getX() + Math.cos(angle) * 4;
            double z = targetPos.getZ() + Math.sin(angle) * 4;

            SkeletonEntity warrior = new SkeletonEntity(EntityType.SKELETON, world);
            warrior.refreshPositionAfterTeleport(x, targetPos.getY(), z);

            // Equip with better gear
            warrior.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            warrior.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            warrior.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));

            // Enhance attributes
            warrior.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 1));
            warrior.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 1));
            warrior.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 0));

            world.spawnEntity(warrior);
        }

        // Grant player Einherjar blessing
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 12000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 1));

        MythicParticleEffects.spawnMajor(world, ParticleTypes.SOUL_FIRE_FLAME,
            targetPos.getX(), targetPos.getY() + 1, targetPos.getZ(),
            50, 5.0, 3.0, 5.0, 0.1);

        world.playSound(null, targetPos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.0f, 1.5f);
        sendMessage(caster, "mythic_age.spellbook.message.valhalla_call.success");

        return true;
    }
}
