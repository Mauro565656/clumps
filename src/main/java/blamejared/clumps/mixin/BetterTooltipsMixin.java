package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.BetterTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Mixin(ItemStack.class)
public class BetterTooltipsMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void clumps$appendTooltip(
            Item.TooltipContext context, @Nullable Player player, TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir) {

        BetterTooltips bt = null;
        for (var m : ClumpsClient.modules) {
            if (m instanceof BetterTooltips b && m.enabled) { bt = b; break; }
        }
        if (bt == null) return;

        ItemStack self = (ItemStack) (Object) this;
        List<Component> lines = cir.getReturnValue();

        // Food stats
        if (bt.showFood()) {
            FoodProperties food = self.get(DataComponents.FOOD);
            if (food != null) {
                lines.add(Component.literal("Food: +" + food.nutrition() + " hunger, +"
                        + String.format("%.1f", food.saturation()) + " sat")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }

        // Durability
        if (bt.showDurability()) {
            Integer maxDmg = self.get(DataComponents.MAX_DAMAGE);
            if (maxDmg != null && maxDmg > 0) {
                int dur = maxDmg - self.getDamageValue();
                ChatFormatting col = dur > maxDmg * 0.5f ? ChatFormatting.GREEN
                                   : dur > maxDmg * 0.2f ? ChatFormatting.YELLOW
                                   : ChatFormatting.RED;
                lines.add(Component.literal("Durability: " + dur + " / " + maxDmg).withStyle(col));
            }
        }

        // Enchantment max levels
        if (bt.showEnchantMax()) {
            ItemEnchantments enchants = self.get(DataComponents.ENCHANTMENTS);
            if (enchants != null && !enchants.isEmpty()) {
                for (var entry : enchants.entrySet()) {
                    int level    = entry.getIntValue();
                    int maxLevel = entry.getKey().value().getMaxLevel();
                    if (level < maxLevel) {
                        var enchId = entry.getKey().unwrapKey()
                                .map(k -> k.identifier().getPath()).orElse("?");
                        lines.add(Component.literal(
                                "  " + enchId + ": " + level + "/" + maxLevel + " max")
                                .withStyle(ChatFormatting.DARK_AQUA));
                    }
                }
            }
        }

        if (bt.showDataSize()) {
            int bytes = self.getComponentsPatch().toString().getBytes(StandardCharsets.UTF_8).length;
            lines.add(Component.literal("Data Size: " + formatBytes(bytes)).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L) return String.format("%.1fmb", bytes / 1024.0 / 1024.0);
        if (bytes >= 1024L) return String.format("%.1fkb", bytes / 1024.0);
        return bytes + "b";
    }
}
