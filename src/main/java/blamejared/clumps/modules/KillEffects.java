package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class KillEffects extends ClumpsModule {
    private final Random random = new Random();
    private final Set<Integer> tracked = new HashSet<>();

    @Override
    public String getName() { return "Kill Effects"; }
    @Override
    public String getDescription() { return "Magma block meteor on enemy death"; }
    @Override
    public String getCategory() { return "Visual"; }

    @Override
    public void onTick(Minecraft client) {
        if (!enabled || client.level == null || client.player == null) return;

        AABB area = new AABB(client.player.blockPosition()).inflate(64);
        for (Entity entity : client.level.getEntities(client.player, area)) {
            if (!(entity instanceof LivingEntity le)) continue;
            if (le == client.player || !le.isDeadOrDying()) {
                tracked.remove(le.getId());
                continue;
            }
            if (tracked.add(le.getId())) {
                spawnMagmaMeteor(le, client);
            }
        }
    }

    private void spawnMagmaMeteor(LivingEntity entity, Minecraft client) {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        var magmaState = Blocks.MAGMA_BLOCK.defaultBlockState();

        for (int i = 0; i < 30; i++) {
            double ty = y + 10 + random.nextDouble() * 6;
            double tx = x + (random.nextDouble() - 0.5) * 0.8;
            double tz = z + (random.nextDouble() - 0.5) * 0.8;
            client.level.addParticle(
                new BlockParticleOption(ParticleTypes.FALLING_DUST, magmaState),
                tx, ty, tz, 0, -0.4, 0
            );
        }

        for (int i = 0; i < 25; i++) {
            double ex = x + (random.nextDouble() - 0.5) * 2.5;
            double ey = y + random.nextDouble() * 1.5;
            double ez = z + (random.nextDouble() - 0.5) * 2.5;
            double vx = (random.nextDouble() - 0.5) * 0.5;
            double vy = random.nextDouble() * 0.4;
            double vz = (random.nextDouble() - 0.5) * 0.5;
            client.level.addParticle(
                new BlockParticleOption(ParticleTypes.BLOCK, magmaState),
                ex, ey, ez, vx, vy, vz
            );
        }
    }
}
