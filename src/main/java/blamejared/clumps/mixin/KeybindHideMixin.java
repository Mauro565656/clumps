package blamejared.clumps.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

/**
 * Strips Clumps keybinds from the array that KeyBindsList sorts and iterates
 * in its constructor, so they never appear in the vanilla controls screen.
 * The keybinds still work; they are just hidden from the controls menu.
 */
@Mixin(KeyBindsList.class)
public class KeybindHideMixin {

    private static final Identifier CLUMPS_CAT_ID = Identifier.fromNamespaceAndPath("clumps", "clumps");

    @ModifyVariable(
        method = "<init>",
        at = @At(value = "STORE"),
        ordinal = 0,
        require = 0
    )
    private KeyMapping[] clumps$filterClumpsKeys(KeyMapping[] mappings) {
        return Arrays.stream(mappings)
            .filter(km -> {
                KeyMapping.Category cat = km.getCategory();
                return cat == null || !CLUMPS_CAT_ID.equals(cat.id());
            })
            .toArray(KeyMapping[]::new);
    }
}
