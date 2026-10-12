package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.util.LightningStormManager;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class LightningStormSpell extends AbstractSpell {

    public LightningStormSpell() {
        super(SpellType.LIGHTNING_STORM, MythologyType.GREEK, SpellCategory.OFFENSIVE, 120);
    }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        BlockPos stormCenter = caster.getBlockPos();

        String stormId = "lightning_storm_" + caster.getUuid().toString() + "_" + System.currentTimeMillis();
        LightningStormManager.getInstance().startLightningStorm(world, stormCenter, stormId);

        for (int i = 0; i < 50; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 40.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 40.0;
            double offsetY = 15 + world.random.nextDouble() * 10;

            MythicParticleEffects.spawnAmbient(world, ParticleTypes.CLOUD,
                stormCenter.getX() + offsetX, stormCenter.getY() + offsetY, stormCenter.getZ() + offsetZ,
                1, 0, 0, 0, 0);
        }

        for (int i = 0; i < 5; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 20.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 20.0;
            BlockPos lightningPos = stormCenter.add((int) offsetX, 0, (int) offsetZ);

            LightningEntity lightning = new LightningEntity(net.minecraft.entity.EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAfterTeleport(
                lightningPos.getX() + 0.5,
                lightningPos.getY(),
                lightningPos.getZ() + 0.5);
            world.spawnEntity(lightning);

            MythicParticleEffects.spawnMajor(world, ParticleTypes.ELECTRIC_SPARK,
                lightningPos.getX() + 0.5, lightningPos.getY() + 5, lightningPos.getZ() + 0.5,
                15, 1.0, 3.0, 1.0, 0.2);
        }

        world.playSound(null, stormCenter, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
            SoundCategory.WEATHER, 2.0f, 0.8f);
        world.playSound(null, stormCenter, SoundEvents.ITEM_TRIDENT_THUNDER,
            SoundCategory.WEATHER, 1.5f, 1.0f);

        sendMessage(caster, "mythic_age.spellbook.message.lightning_storm.success");
        return true;
    }
}
