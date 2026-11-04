package ru.aiforge.riftworld.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Кастомная частица парящего на ветру листа/лепестка для Фиолетовых холмов и Рощи Сизо.
 * Плавно кружится, покачивается в потоке тёплого ветра и отчётливо читается на фоне неба.
 */
public final class FloatingLeafParticle extends TextureSheetParticle {
    private final float rotSpeed;
    private final float swayPhase;
    private final float baseWindX;
    private final float baseWindZ;

    private FloatingLeafParticle(ClientLevel level, double x, double y, double z,
                                 double vx, double vy, double vz, SpriteSet sprites, float baseScale) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.quadSize = baseScale * (0.82F + this.random.nextFloat() * 0.52F);
        this.lifetime = 130 + this.random.nextInt(100);
        this.gravity = 0.0F;
        this.hasPhysics = true;
        this.friction = 0.985F;
        this.baseWindX = (float) (vx != 0.0 ? vx : (0.016 + this.random.nextDouble() * 0.014));
        this.baseWindZ = (float) (vz != 0.0 ? vz : (0.008 + (this.random.nextDouble() - 0.35) * 0.014));
        this.xd = this.baseWindX;
        this.yd = vy != 0.0 ? vy : (-0.005 - this.random.nextDouble() * 0.009);
        this.zd = this.baseWindZ;
        this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.oRoll = this.roll;
        this.rotSpeed = (this.random.nextFloat() - 0.5F) * 0.09F;
        this.swayPhase = this.random.nextFloat() * 6.283F;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float t = this.age * 0.075F + this.swayPhase;
        this.xd = this.baseWindX + Math.sin(t) * 0.010;
        this.zd = this.baseWindZ + Math.cos(t * 0.83F) * 0.010;
        this.yd = -0.0065 + Math.sin(t * 1.3F) * 0.0045;
        this.roll += this.rotSpeed + (float) Math.sin(t) * 0.025F;
        this.move(this.xd, this.yd, this.zd);
        if (this.onGround) {
            this.xd *= 0.6;
            this.zd *= 0.6;
            if (this.age < this.lifetime - 20) this.age = this.lifetime - 20;
        }
        if (this.age > this.lifetime - 18) {
            this.alpha = Math.max(0.0F, (this.lifetime - this.age) / 18.0F);
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class VioletProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public VioletProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new FloatingLeafParticle(level, x, y, z, vx, vy, vz, this.sprites, 0.24F);
        }
    }

    public static final class SizoProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SizoProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new FloatingLeafParticle(level, x, y, z, vx, vy, vz, this.sprites, 0.16F);
        }
    }
}
