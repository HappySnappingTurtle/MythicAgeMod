package com.example.mythic_age.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class ShockwaveParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;
    private final float baseScale;

    protected ShockwaveParticle(
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
        this.baseScale = 0.32F + this.random.nextFloat() * 0.18F;
        this.scale = this.baseScale;
        this.maxAge = 16 + this.random.nextInt(6);
        this.gravityStrength = 0.0F;
        this.velocityMultiplier = 0.78F;
        this.velocityX = velocityX + (this.random.nextDouble() - 0.5D) * 0.04D;
        this.velocityY = velocityY * 0.1D;
        this.velocityZ = velocityZ + (this.random.nextDouble() - 0.5D) * 0.04D;
        this.setBoundingBoxSpacing(0.16F, 0.02F);
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.dead) {
            float life = (float) this.age / (float) this.maxAge;
            this.scale = this.baseScale * (1.0F + life * 2.7F);
            this.setColor(0.8F, 0.62F, 0.36F);
            this.setAlpha(0.78F - life * 0.72F);
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
            return new ShockwaveParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
