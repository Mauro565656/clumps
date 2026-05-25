package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side crystal optimizer: discards attacked crystals at the END of the
 * tick so the attack packet is already queued before the entity is removed.
 *
 * The reference mod hooks into ServerboundInteractPacket send; we replicate
 * the same ordering by collecting crystals during AttackEntityCallback
 * (registered in ClumpsClient) then discarding them here in onTick, which
 * runs from END_CLIENT_TICK — after the packet has been sent.
 *
 * Also skips discard when the player has Weakness with no adequate Strength
 * (attack would deal 0 damage anyway, same check as the reference).
 */
public class CrystalOptimizer extends ClumpsModule {
    /** Crystals queued for removal — populated by ClumpsClient's AttackEntityCallback. */
    public final List<Entity> pendingDiscards = new ArrayList<>();

    @Override
    public String getName() { return "Crystal Optimizer"; }
    @Override
    public String getDescription() { return "Optimize crystal placement"; }
    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public void onTick(Minecraft client) {
        for (Entity e : pendingDiscards) {
            if (shouldDiscard(client, e)) e.discard();
        }
        pendingDiscards.clear();
    }

    private boolean shouldDiscard(Minecraft client, Entity e) {
        if (!(e instanceof EndCrystal)) return false;
        // Don't discard if player has Weakness and not enough Strength to overcome it
        var weakness = client.player.getEffect(MobEffects.WEAKNESS);
        var strength = client.player.getEffect(MobEffects.STRENGTH);
        if (weakness != null && (strength == null || strength.getAmplifier() <= weakness.getAmplifier())) {
            return false;
        }
        return true;
    }
}
