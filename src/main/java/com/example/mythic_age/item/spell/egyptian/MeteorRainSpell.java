package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.config.ModConfig;
import com.example.mythic_age.entity.MeteorEntity;
import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.util.GodPowerTaskScheduler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

public class MeteorRainSpell extends AbstractSpell {
    public MeteorRainSpell() { super(SpellType.METEOR, MythologyType.EGYPTIAN, SpellCategory.OFFENSIVE, 500); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        ModConfig.meteorDebug("开始施放陨石雨法术:");
        ModConfig.meteorDebug("  施法者: " + caster.getName().getString());
        ModConfig.meteorDebug("  目标位置: " + targetPos);
        ModConfig.meteorDebug("  世界: " + world.getClass().getSimpleName());

        // Enhanced meteor rain parameters
        int meteorCount = 15;
        int range = 50;
        int delayBetweenMeteors = 40; // 2 seconds between each meteor

        ModConfig.meteorDebug("  陨石数量: " + meteorCount);
        ModConfig.meteorDebug("  影响范围: " + range + " 格");
        ModConfig.meteorDebug("  生成间隔: " + delayBetweenMeteors + " ticks");

        // Use scheduler to delay meteor spawning for sequential fall effect
        for (int i = 0; i < meteorCount; i++) {
            final int index = i;
            final int delay = i * delayBetweenMeteors;

            // Pre-calculate meteor position
            final BlockPos meteorTarget = targetPos.add(
                world.random.nextInt(range * 2 + 1) - range,
                0,
                world.random.nextInt(range * 2 + 1) - range
            );

            // Immediate warning particle effect
            final int groundY = world.getTopY(Heightmap.Type.WORLD_SURFACE, meteorTarget.getX(), meteorTarget.getZ());
            for (int j = 0; j < 40; j++) {
                double x = meteorTarget.getX() + (world.random.nextDouble() - 0.5) * 12;
                double y = groundY + world.random.nextDouble() * 8;
                double z = meteorTarget.getZ() + (world.random.nextDouble() - 0.5) * 12;

                MythicParticleEffects.spawnAmbient(world, ParticleTypes.FLAME, x, y, z, 1, 0, 0.3, 0, 0.15);
                MythicParticleEffects.spawnAmbient(world, ParticleTypes.SMOKE, x, y, z, 1, 0, 0.2, 0, 0.1);
                MythicParticleEffects.spawnAmbient(world, ParticleTypes.LAVA, x, y, z, 1, 0, 0.15, 0, 0.05);
            }

            GodPowerTaskScheduler.schedule(world, delay, () -> {
                createDelayedMeteor(world, caster, meteorTarget, index);
            });
        }

        // Grant basic fire protection to caster to avoid self-damage
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 3600, 0));

        // Initial sky particle effect
        for (int i = 0; i < 100; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 80;
            double y = targetPos.getY() + 40 + world.random.nextDouble() * 20;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 80;

            MythicParticleEffects.spawnAmbient(world, ParticleTypes.LAVA, x, y, z, 1, 0, -1.0, 0, 0.3);
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.FLAME, x, y, z, 1, 0, -0.8, 0, 0.2);
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0, -0.5, 0, 0.1);
        }

        // Warning sound effect
        world.playSound(null, targetPos, SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
            SoundCategory.HOSTILE, 1.0f, 0.8f);

        sendMessage(caster, "mythic_age.spellbook.message.meteor.success");
        return true;
    }

    private void createDelayedMeteor(ServerWorld world, PlayerEntity caster, BlockPos meteorTarget, int index) {
        // Find ground height
        final int currentGroundY = world.getTopY(Heightmap.Type.WORLD_SURFACE, meteorTarget.getX(), meteorTarget.getZ());

        // Set high meteor spawn height for sufficient fall time
        double meteorX = meteorTarget.getX() + 0.5;
        double meteorY = currentGroundY + 60 + (index * 5);
        double meteorZ = meteorTarget.getZ() + 0.5;

        ModConfig.meteorDebug("  延迟创建陨石 #" + (index + 1) + ":");
        ModConfig.meteorDebug("    目标位置: " + meteorTarget);
        ModConfig.meteorDebug("    地面高度: " + currentGroundY);
        ModConfig.meteorDebug("    生成位置: (" + String.format("%.1f", meteorX) + ", " +
            String.format("%.1f", meteorY) + ", " +
            String.format("%.1f", meteorZ) + ")");

        // Create enhanced meteor entity
        MeteorEntity meteor = new MeteorEntity(world, meteorX, meteorY, meteorZ, caster);
        meteor.setExplosionPower(8.0f);
        boolean spawned = world.spawnEntity(meteor);

        ModConfig.meteorDebug("    实体生成结果: " + (spawned ? "成功" : "失败"));
        if (spawned) {
            ModConfig.meteorDebug("    实体ID: " + meteor.getId());
            ModConfig.meteorDebug("    实体UUID: " + meteor.getUuid());

            // Play individual meteor warning sound
            world.playSound(null, meteorTarget, SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.HOSTILE, 1.5f, 0.7f + (index * 0.1f));
        }

        // Enhanced warning particle effect when meteor spawns
        for (int j = 0; j < 50; j++) {
            double x = meteorTarget.getX() + (world.random.nextDouble() - 0.5) * 15;
            double y = currentGroundY + world.random.nextDouble() * 10;
            double z = meteorTarget.getZ() + (world.random.nextDouble() - 0.5) * 15;

            MythicParticleEffects.spawnAmbient(world, ParticleTypes.FLAME, x, y, z, 1, 0, 0.4, 0, 0.2);
        }
    }
}
