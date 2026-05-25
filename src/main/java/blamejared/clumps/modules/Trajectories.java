package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts projectile arc with proper physics: air drag, water drag, gravity,
 * entity collision, and bow charge power.
 */
public class Trajectories extends ClumpsModule {

    private final Option.IntOption   steps     = new Option.IntOption("Steps", 160, 10, 500, 10);
    private final Option.BoolOption  showHit   = new Option.BoolOption("Show Hit", true);
    private final Option.BoolOption  entities  = new Option.BoolOption("Entity Collision", true);

    @Override public String getName()     { return "Trajectories"; }
    @Override public String getDescription() { return "Show projectile trajectories"; }
    @Override     public String getCategory() { return "Visual"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(steps, showHit, entities); }

    @Override
    public void onTick(Minecraft client) { /* rendering in TrajectoriesMixin */ }

    public int getSteps()        { return steps.getValue(); }
    public boolean showHit()     { return showHit.getValue(); }
    public boolean checkEntities() { return entities.getValue(); }

    // ── Per-item simulation presets ───────────────────────────────────────

    public record SimPreset(double speed, double gravity, double airDrag, double waterDrag) {}

    private static final SimPreset BOW_SIM      = new SimPreset(3.0,  0.05, 0.99, 0.6);
    private static final SimPreset CROSSBOW_SIM = new SimPreset(3.15, 0.05, 0.99, 0.6);
    private static final SimPreset TRIDENT_SIM  = new SimPreset(2.5,  0.05, 0.99, 0.6);
    private static final SimPreset THROW_SIM    = new SimPreset(1.5,  0.03, 0.99, 0.8);
    private static final SimPreset POTION_SIM   = new SimPreset(0.5,  0.05, 0.99, 0.8);
    private static final SimPreset EXP_SIM      = new SimPreset(0.7,  0.07, 0.99, 0.8);

    public SimPreset getPreset(ItemStack stack, Minecraft mc) {
        Item item = stack.getItem();
        if (item instanceof ProjectileWeaponItem) {
            // Bow: scale speed by charge
            if (item instanceof BowItem) {
                float charge = BowItem.getPowerForTime(
                        mc.player != null && mc.player.isUsingItem()
                                ? mc.player.getTicksUsingItem() : 0);
                return new SimPreset(3.0 * charge, 0.05, 0.99, 0.6);
            }
            return CROSSBOW_SIM;
        }
        if (item instanceof TridentItem || stack.is(ItemTags.SPEARS)) return TRIDENT_SIM;
        if (item instanceof ThrowablePotionItem)  return POTION_SIM;
        if (item instanceof ExperienceBottleItem) return EXP_SIM;
        // Snowball, egg, ender pearl, fireball
        return THROW_SIM;
    }

    public static boolean isProjectileItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof ProjectileWeaponItem
            || item instanceof TridentItem
            || item instanceof SnowballItem
            || item instanceof EggItem
            || item instanceof EnderpearlItem
            || item instanceof ThrowablePotionItem
            || item instanceof ExperienceBottleItem
            || stack.is(ItemTags.SPEARS);
    }

    // ── Simulation result ─────────────────────────────────────────────────

    public record TrajectoryResult(List<Vec3> points, HitResult hit) {}

    public TrajectoryResult simulate(Minecraft mc, Player player, ItemStack stack) {
        List<Vec3> points = new ArrayList<>();
        if (mc.level == null) return new TrajectoryResult(points, null);

        SimPreset preset = getPreset(stack, mc);
        if (preset.speed() < 0.01) return new TrajectoryResult(points, null);

        Vec3 pos  = player.getEyePosition(1.0f);
        Vec3 vel  = player.getLookAngle().scale(preset.speed());
        points.add(pos);

        int maxSteps = getSteps();
        for (int i = 0; i < maxSteps; i++) {
            // Drag and gravity
            boolean inWater = mc.level.getFluidState(
                    new net.minecraft.core.BlockPos(
                            (int) Math.floor(pos.x),
                            (int) Math.floor(pos.y),
                            (int) Math.floor(pos.z)
                    )).isEmpty() == false;

            double drag = inWater ? preset.waterDrag() : preset.airDrag();
            vel = new Vec3(vel.x * drag, vel.y * drag - preset.gravity(), vel.z * drag);

            Vec3 next = pos.add(vel);

            // Block collision
            HitResult blockHit = mc.level.clip(new ClipContext(
                    pos, next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                points.add(blockHit.getLocation());
                return new TrajectoryResult(points, blockHit);
            }

            // Entity collision
            if (checkEntities()) {
                EntityHitResult ehr = findEntityHit(mc, player, pos, next);
                if (ehr != null) {
                    points.add(ehr.getLocation());
                    return new TrajectoryResult(points, ehr);
                }
            }

            pos = next;
            points.add(pos);
        }

        return new TrajectoryResult(points, null);
    }

    private EntityHitResult findEntityHit(Minecraft mc, Player player, Vec3 from, Vec3 to) {
        AABB searchBox = new AABB(from, to).inflate(1.0);
        Entity closest    = null;
        double closestDist = Double.MAX_VALUE;
        Vec3   closestHit  = null;

        for (Entity entity : mc.level.getEntities(player, searchBox)) {
            if (entity == player) continue;
            if (entity instanceof LivingEntity living && !living.isAlive()) continue;

            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            var result = box.clip(from, to);
            if (result.isPresent()) {
                double d = from.distanceToSqr(result.get());
                if (d < closestDist) {
                    closestDist = d;
                    closest     = entity;
                    closestHit  = result.get();
                }
            }
        }

        if (closest == null) return null;
        return new EntityHitResult(closest, closestHit);
    }
}
