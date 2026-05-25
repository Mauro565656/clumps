package blamejared.clumps;

import blamejared.clumps.gui.ModuleScreen;
import blamejared.clumps.gui.Theme;
import blamejared.clumps.modules.*;
import blamejared.clumps.commands.CommandManager;
import blamejared.clumps.commands.HClipCommand;
import blamejared.clumps.commands.VClipCommand;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClumpsClient implements ClientModInitializer {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("clumps", "clumps"));

    public static KeyMapping guiKey;
    public static final List<ClumpsModule> modules = new ArrayList<>();


    private static KeyMapping registerBind(String id, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clumps." + id,
                InputConstants.Type.KEYSYM,
                key,
                CATEGORY
        ));
    }

    public static void saveConfig() {
        ClumpsConfig.save(modules);
    }

    public static void setModuleEnabled(ClumpsModule module, boolean enabled, Minecraft client) {
        if (module.enabled == enabled) return;

        boolean wasEnabled = module.enabled;
        module.enabled = enabled;

        if (wasEnabled && !enabled) {
            if (module instanceof Flight f) {
                f.onDisable(client);
            }
            if (module instanceof Freecam fc) {
                fc.reset();
            }
            if (module instanceof AntiInvis ai) {
                ai.onDisable(client);
            }
        }

        saveConfig();
    }

    public static void toggleModule(ClumpsModule module, Minecraft client) {
        setModuleEnabled(module, !module.enabled, client);
    }

    @Override
    public void onInitializeClient() {
        int guiKeyCode = ClumpsConfig.loadGuiKey();
        guiKey = registerBind("gui", guiKeyCode);

        AutoTotem autoTotem = new AutoTotem();
        Anchorer anchorer = new Anchorer();
        Crystaler crystaler = new Crystaler();
        CrystalOptimizer crystalOpt = new CrystalOptimizer();
        PlayerESP playerEsp = new PlayerESP();
        BlockESP blockEsp = new BlockESP();

        ShieldDrainer shieldDrainer = new ShieldDrainer();
        ShieldSlammer shieldSlammer = new ShieldSlammer();
        ShieldDisabler shieldDisabler = new ShieldDisabler();
        MaceKiller maceKiller = new MaceKiller();
        MaceSwap maceSwap = new MaceSwap();
        AntiWeb antiWeb = new AntiWeb();
        AutoDrain autoDrain = new AutoDrain();
        AutoPot autoPot = new AutoPot();
        AutoPotRefill autoPotRefill = new AutoPotRefill();
        Criticals criticals = new Criticals();
        Hitbox hitbox = new Hitbox();
        Reach reach = new Reach();
        TriggerBot triggerBot = new TriggerBot();
        Nametags nametags = new Nametags();
        Freecam freecam = new Freecam();
        PingSpoof pingSpoof = new PingSpoof();
        Flight flight = new Flight();
        SpearKiller spearKiller = new SpearKiller();
        SpearLauncher spearLauncher = new SpearLauncher();
        AntiInvis antiInvis = new AntiInvis();
        Friends friends = new Friends();
        ClickCrystals clickCrystals = new ClickCrystals();
        AnchorSwitch anchorSwitch = new AnchorSwitch();
        StunSlam stunSlam = new StunSlam();
        Trajectories trajectories = new Trajectories();
        BetterTooltips betterTooltips = new BetterTooltips();
        LogoutSpots logoutSpots = new LogoutSpots();
        HitEffects hitEffects = new HitEffects();
        KillEffects killEffects = new KillEffects();

        autoTotem.keybind        = registerBind("auto_totem", GLFW.GLFW_KEY_UNKNOWN);
        anchorer.keybind         = registerBind("anchorer", GLFW.GLFW_KEY_UNKNOWN);
        crystaler.keybind        = registerBind("crystaler", GLFW.GLFW_KEY_UNKNOWN);
        crystalOpt.keybind       = registerBind("crystal_opt", GLFW.GLFW_KEY_UNKNOWN);
        playerEsp.keybind        = registerBind("player_esp", GLFW.GLFW_KEY_UNKNOWN);
        blockEsp.keybind         = registerBind("block_esp", GLFW.GLFW_KEY_UNKNOWN);
        spearLauncher.keybind    = registerBind("spear_launcher", GLFW.GLFW_KEY_UNKNOWN);
        antiInvis.keybind        = registerBind("anti_invis", GLFW.GLFW_KEY_UNKNOWN);
        shieldDrainer.keybind    = registerBind("shield_drainer", GLFW.GLFW_KEY_UNKNOWN);
        shieldSlammer.keybind    = registerBind("shield_slammer", GLFW.GLFW_KEY_UNKNOWN);
        shieldDisabler.keybind   = registerBind("shield_disabler", GLFW.GLFW_KEY_UNKNOWN);
        maceKiller.keybind       = registerBind("mace_killer", GLFW.GLFW_KEY_UNKNOWN);
        maceSwap.keybind         = registerBind("mace_swap", GLFW.GLFW_KEY_UNKNOWN);
        antiWeb.keybind          = registerBind("anti_web", GLFW.GLFW_KEY_UNKNOWN);
        autoDrain.keybind        = registerBind("auto_drain", GLFW.GLFW_KEY_UNKNOWN);
        autoPot.keybind          = registerBind("auto_pot", GLFW.GLFW_KEY_UNKNOWN);
        autoPotRefill.keybind    = registerBind("auto_pot_refill", GLFW.GLFW_KEY_UNKNOWN);
        criticals.keybind        = registerBind("criticals", GLFW.GLFW_KEY_UNKNOWN);
        hitbox.keybind           = registerBind("hitbox", GLFW.GLFW_KEY_UNKNOWN);
        reach.keybind            = registerBind("reach", GLFW.GLFW_KEY_UNKNOWN);
        triggerBot.keybind       = registerBind("trigger_bot", GLFW.GLFW_KEY_UNKNOWN);
        nametags.keybind         = registerBind("nametags", GLFW.GLFW_KEY_UNKNOWN);
        freecam.keybind          = registerBind("freecam", GLFW.GLFW_KEY_UNKNOWN);
        pingSpoof.keybind        = registerBind("ping_spoof", GLFW.GLFW_KEY_UNKNOWN);
        flight.keybind           = registerBind("flight", GLFW.GLFW_KEY_UNKNOWN);
        spearKiller.keybind      = registerBind("spear_killer", GLFW.GLFW_KEY_UNKNOWN);
        friends.keybind          = registerBind("friends", GLFW.GLFW_KEY_UNKNOWN);
        clickCrystals.keybind    = registerBind("click_crystals", GLFW.GLFW_KEY_UNKNOWN);
        anchorSwitch.keybind     = registerBind("anchor_switch", GLFW.GLFW_KEY_UNKNOWN);
        stunSlam.keybind         = registerBind("stun_slam", GLFW.GLFW_KEY_UNKNOWN);
        trajectories.keybind     = registerBind("trajectories", GLFW.GLFW_KEY_UNKNOWN);
        betterTooltips.keybind   = registerBind("better_tooltips", GLFW.GLFW_KEY_UNKNOWN);
        logoutSpots.keybind      = registerBind("logout_spots", GLFW.GLFW_KEY_UNKNOWN);
        hitEffects.keybind       = registerBind("hit_effects", GLFW.GLFW_KEY_UNKNOWN);
        killEffects.keybind      = registerBind("kill_effects", GLFW.GLFW_KEY_UNKNOWN);

        modules.addAll(List.of(
                autoTotem, anchorer, crystaler, crystalOpt,
                clickCrystals, anchorSwitch, stunSlam,
                playerEsp, blockEsp,
                shieldDrainer, shieldSlammer, shieldDisabler,
                maceKiller, maceSwap, antiWeb,
                autoDrain, autoPot, autoPotRefill,
                criticals, hitbox, reach, triggerBot,
                nametags, freecam, pingSpoof, flight, spearKiller,
                spearLauncher, antiInvis,
                trajectories, betterTooltips, friends, logoutSpots,
                hitEffects, killEffects
        ));

        ClumpsConfig.loadInto(modules);

        // Register dot-prefix chat commands.
        CommandManager.register(new HClipCommand());
        CommandManager.register(new VClipCommand());
        CommandManager.install();

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (crystalOpt.enabled && entity instanceof EndCrystal) {
                crystalOpt.pendingDiscards.add(entity);
            }
            if (clickCrystals.enabled && entity instanceof EndCrystal crystal) {
                clickCrystals.removeClientside(crystal);
            }
            hitEffects.onHit(entity, Minecraft.getInstance());
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            Minecraft mc = Minecraft.getInstance();
            if (anchorer.enabled && player.getItemInHand(hand).is(Items.RESPAWN_ANCHOR)) {

                BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());

                if (world.getBlockState(placePos).isAir()) {
                    anchorer.onBlockPlaced(placePos, mc);
                } else if (world.getBlockState(hitResult.getBlockPos()).is(Blocks.RESPAWN_ANCHOR)) {
                    // existing anchor
                } else {
                    anchorer.onBlockPlaced(placePos, mc);
                }
            }
            if (anchorSwitch.enabled && player.getItemInHand(hand).is(Items.RESPAWN_ANCHOR)) {
                anchorSwitch.onAnchorPlaced(mc);
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (guiKey.consumeClick()) {
                client.setScreen(new ModuleScreen());
            }

            for (ClumpsModule m : modules) {
                if (m.keybind != null) {
                    while (m.keybind.consumeClick()) {
                        toggleModule(m, client);
                    }
                }
            }

            for (ClumpsModule m : modules) {
                if (m.enabled) m.onTick(client);
            }
        });
    }
}
