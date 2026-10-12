package com.example.mythic_age.item.spell;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.util.GodPowerTaskScheduler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import com.example.mythic_age.particle.SpellParticleEffect;
import com.example.mythic_age.particle.GreekParticleEffect;
import com.example.mythic_age.particle.EgyptianParticleEffect;
import com.example.mythic_age.particle.NordicParticleEffect;
import com.example.mythic_age.particle.ChineseParticleEffect;
import com.example.mythic_age.particle.AtlanteanParticleEffect;

public abstract class AbstractSpell implements Spell {

    protected final SpellType type;
    protected final MythologyType civilization;
    protected final SpellCategory category;
    protected final int defaultCooldown;

    protected AbstractSpell(SpellType type, MythologyType civilization, SpellCategory category, int defaultCooldown) {
        this.type = type;
        this.civilization = civilization;
        this.category = category;
        this.defaultCooldown = defaultCooldown;
    }

    @Override
    public SpellType getType() { return type; }

    @Override
    public MythologyType getCivilization() { return civilization; }

    @Override
    public SpellCategory getCategory() { return category; }

    @Override
    public int getDefaultCooldown() { return defaultCooldown; }

    @Override
    public final boolean cast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        getParticleEffect().playCast(world, caster.getBlockPos(), targetPos);
        boolean result = executeCast(world, caster, targetPos);
        if (result) {
            getParticleEffect().playEnd(world, targetPos);
        }
        return result;
    }

    protected abstract boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos);

    public SpellParticleEffect getParticleEffect() {
        return switch (civilization) {
            case GREEK -> GreekParticleEffect.INSTANCE;
            case EGYPTIAN -> EgyptianParticleEffect.INSTANCE;
            case NORDIC -> NordicParticleEffect.INSTANCE;
            case CHINESE -> ChineseParticleEffect.INSTANCE;
            case ATLANTEAN -> AtlanteanParticleEffect.INSTANCE;
        };
    }

    protected BlockPos getTargetPos(World world, PlayerEntity player) {
        return getTargetPos(world, player, 50);
    }

    protected BlockPos getTargetPos(World world, PlayerEntity player, int maxDistance) {
        Vec3d eyePos = player.getCameraPosVec(1.0F);
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance);

        BlockHitResult hitResult = world.raycast(new RaycastContext(
            eyePos, endPos,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            player));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }

        Vec3d fallback = eyePos.add(lookVec.x * 5, lookVec.y * 5, lookVec.z * 5);
        return BlockPos.ofFloored(fallback.x, fallback.y, fallback.z);
    }

    protected void sendMessage(PlayerEntity player, String key, Object... args) {
        player.sendMessage(Text.translatable(key, args), false);
    }

    protected void scheduleTask(ServerWorld world, int delayTicks, Runnable task) {
        GodPowerTaskScheduler.schedule(world, delayTicks, task);
    }
}
