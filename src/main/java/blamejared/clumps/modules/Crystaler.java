package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public class Crystaler extends ClumpsModule {
    private final Option.IntOption placeDelay = new Option.IntOption("Place Delay", 0, 0, 20, 1);
    private final Option.IntOption breakDelay = new Option.IntOption("Break Delay", 0, 0, 20, 1);

    private int placeClock = 0;
    private int breakClock = 0;

    @Override
    public String getName() { return "Crystaler"; }
    @Override
    public String getDescription() { return "Auto-place and break crystals"; }
    @Override
    public String getCategory() { return "Combat"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(placeDelay, breakDelay); }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) return;
        if (!client.options.keyUse.isDown()) {
            placeClock = 0;
            breakClock = 0;
            return;
        }

        boolean cantPlace = placeClock > 0;
        boolean cantBreak = breakClock > 0;
        if (cantPlace) placeClock--;
        if (cantBreak) breakClock--;

        if (client.hitResult instanceof EntityHitResult ehr) {
            if (ehr.getEntity() instanceof EndCrystal crystal && !cantBreak) {
                client.gameMode.attack(client.player, crystal);
                client.player.swing(InteractionHand.MAIN_HAND);
                breakClock = Math.max(1, breakDelay.getValue());
            }
        } else if (client.hitResult instanceof BlockHitResult bhr && !cantPlace) {
            var state = client.level.getBlockState(bhr.getBlockPos());
            if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) {
                if (client.player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.END_CRYSTAL)) {
                    client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, bhr);
                    placeClock = Math.max(1, placeDelay.getValue());
                }
            }
        }
    }
}
