package com.example.mythic_age.item.spell.chinese;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class RainPrayerSpell extends AbstractSpell {
    public RainPrayerSpell() { super(SpellType.RAIN_PRAYER, MythologyType.CHINESE, SpellCategory.UTILITY, 100); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // 祈雨 - 在目标区域降雨
        // 设置天气为雨天
        world.setWeather(0, 6000, true, false);

        // 在目标区域生成雨水效果
        for (int i = 0; i < 50; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 20;
            double y = targetPos.getY() + world.random.nextDouble() * 10;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 20;

            MythicParticleEffects.spawnAmbient(world, ParticleTypes.RAIN,
                x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // 给予玩家水之祝福
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 6000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 6000, 0));

        world.playSound(null, targetPos, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 1.0f, 1.0f);
        sendMessage(caster, "mythic_age.spellbook.message.rain_prayer.success");

        return true;
    }
}
