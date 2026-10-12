package com.example.mythic_age.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class MythicFlameParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float baseScale;

    protected MythicFlameParticle(
            ClientWorld world,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            SpriteProvider spriteProvider
    ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;
        this.baseScale = 0.18F + this.random.nextFloat() * 0.16F;
        this.scale = this.baseScale;
        this.maxAge = 18 + this.random.nextInt(8);
        this.gravityStrength = -0.02F;
        this.velocityMultiplier = 0.86F;
        this.velocityX = velocityX * 0.55D + (this.random.nextDouble() - 0.5D) * 0.035D;
        this.velocityY = velocityY * 0.45D + 0.035D + this.random.nextDouble() * 0.035D;
        this.velocityZ = velocityZ * 0.55D + (this.random.nextDouble() - 0.5D) * 0.035D;
        this.setBoundingBoxSpacing(0.08F, 0.08F);
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.dead) {
            float life = (float) this.age / (float) this.maxAge;
            this.scale = this.baseScale * (1.0F + life * 0.7F);
            this.setColor(1.0F, 0.36F + life * 0.2F, 0.08F);
            this.setAlpha(1.0F - life * 0.82F);
            this.setSpriteForAge(this.spriteProvider);
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(
                DefaultParticleType parameters,
                ClientWorld world,
                double x,
                double y,
                double z,
                double velocityX,
                double velocityY,
                double velocityZ
        ) {
            return new MythicFlameParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
