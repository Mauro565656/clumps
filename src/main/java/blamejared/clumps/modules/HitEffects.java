package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;

import java.util.Random;

public class HitEffects extends ClumpsModule {
    private final Random random = new Random();

    @Override
    public String getName() { return "Hit Effects"; }
    @Override
    public String getDescription() { return "Custom particle burst on hit"; }
    @Override
    public String getCategory() { return "Visual"; }

    public void onHit(Entity target, Minecraft client) {
        if (!enabled || target == null || client.level == null) return;
        for (int i = 0; i < 15; i++) {
            double px = target.getX() + (random.nextDouble() - 0.5) * target.getBbWidth() * 1.2;
            double py = target.getY() + random.nextDouble() * target.getBbHeight();
            double pz = target.getZ() + (random.nextDouble() - 0.5) * target.getBbWidth() * 1.2;
            double vx = (random.nextDouble() - 0.5) * 0.4;
            double vy = random.nextDouble() * 0.35;
            double vz = (random.nextDouble() - 0.5) * 0.4;
            client.level.addParticle(ParticleTypes.CRIT, px, py, pz, vx, vy, vz);
            client.level.addParticle(ParticleTypes.ENCHANTED_HIT, px, py, pz, vx, vy, vz);
        }
    }
}
