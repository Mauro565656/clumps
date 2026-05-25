package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Friends;
import blamejared.clumps.modules.Nametags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(DebugRenderer.class)
public class NametagDebugRendererMixin {
    private static final int FRIEND_COLOR = 0xFF55FF55;
    private static final int OTHER_COLOR = 0xFFFFFFFF;
    private static final int SUB_COLOR = 0xFFD0D0D0;

    @Unique
    private static boolean clumps$nametagRendererAdded = false;

    @Inject(method = "refreshRendererList", at = @At("RETURN"))
    private void clumps$addNametagRenderer(CallbackInfo ci) {
        if (clumps$nametagRendererAdded) return;
        clumps$nametagRendererAdded = true;

        DebugRenderer self = (DebugRenderer) (Object) this;
        List<DebugRenderer.SimpleDebugRenderer> renderers = ((DebugRendererAccessor) self).clumps$getRenderers();
        renderers.add((camX, camY, camZ, debug, frustum, partialTick) -> {
            Nametags nametags = null;
            Friends friends = null;
            for (var module : ClumpsClient.modules) {
                if (module instanceof Nametags n && module.enabled) nametags = n;
                if (module instanceof Friends f) friends = f;
            }
            if (nametags == null) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            for (Player player : mc.level.players()) {
                if (player == mc.player || !player.isAlive()) continue;
                if (mc.player.distanceTo(player) > nametags.range()) continue;

                boolean friend = friends != null && friends.isFriend(player.getGameProfile().name());
                List<String> lines = buildLines(mc, nametags, player);
                int baseRow = lines.size() - 1;
                for (int i = 0; i < lines.size(); i++) {
                    int color = i == baseRow ? (friend ? FRIEND_COLOR : OTHER_COLOR) : SUB_COLOR;
                    try {
                        Gizmos.billboardTextOverMob(player, baseRow - i, lines.get(i), color, nametags.scale());
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private static List<String> buildLines(Minecraft mc, Nametags module, Player player) {
        List<String> lines = new ArrayList<>();
        try {
            if (module.showArmor()) {
                String armor = joinNonBlank(
                    stackText(player.getItemBySlot(EquipmentSlot.HEAD)),
                    stackText(player.getItemBySlot(EquipmentSlot.CHEST)),
                    stackText(player.getItemBySlot(EquipmentSlot.LEGS)),
                    stackText(player.getItemBySlot(EquipmentSlot.FEET))
                );
                if (!armor.isBlank()) lines.add(armor);
            }

            if (module.showItems()) {
                String hands = joinNonBlank(
                    "Main: " + stackText(player.getMainHandItem()),
                    "Off: " + stackText(player.getOffhandItem())
                );
                lines.add(hands);
            }
        } catch (Exception ignored) {}

        StringBuilder nameLine = new StringBuilder(player.getGameProfile().name());
        try {
            if (module.showHealth()) {
                float health = player.getHealth() + player.getAbsorptionAmount();
                nameLine.append(" ").append(Math.round(health)).append("hp");
            }
            if (module.showPing()) {
                PlayerInfo info = mc.player.connection.getPlayerInfo(player.getUUID());
                if (info != null) nameLine.append(" ").append(info.getLatency()).append("ms");
            }
            if (module.showDistance()) {
                nameLine.append(" ").append(Math.round(mc.player.distanceTo(player))).append("m");
            }
        } catch (Exception ignored) {}

        lines.add(nameLine.toString());
        return lines;
    }

    private static String stackText(ItemStack stack) {
        if (stack.isEmpty()) return "";
        StringBuilder out = new StringBuilder(stack.getHoverName().getString());
        if (stack.getCount() > 1) out.append(" x").append(stack.getCount());
        if (stack.isDamageableItem()) {
            out.append(" ").append(stack.getMaxDamage() - stack.getDamageValue()).append("/").append(stack.getMaxDamage());
        }
        if (stack.isEnchanted()) out.append(" ").append(ChatFormatting.AQUA).append("ench");
        return out.toString();
    }

    private static String joinNonBlank(String... values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            if (!out.isEmpty()) out.append(" | ");
            out.append(value);
        }
        return out.toString();
    }
}
