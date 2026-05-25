package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.Nametags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class NametagsMixin {

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void clumps$forceShowPlayerName(Entity entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) return;
        for (var m : ClumpsClient.modules) {
            if (m instanceof Nametags && m.enabled) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void clumps$addInfoToNametag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        Nametags nametags = null;
        for (var m : ClumpsClient.modules) {
            if (m instanceof Nametags n && m.enabled) { nametags = n; break; }
        }
        if (nametags == null) return;
        if (!(entity instanceof LivingEntity living)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == mc.player) return;

        if (state.nameTag == null) {
            if (entity instanceof Player player) {
                state.nameTag = player.getDisplayName();
            } else {
                return;
            }
        }

        MutableComponent tag = state.nameTag.copy();

        if (nametags.showHealth()) {
            float health = living.getHealth();
            float maxHealth = Math.max(1f, living.getMaxHealth());
            float pct = health / maxHealth;
            ChatFormatting col = pct > 0.666f ? ChatFormatting.GREEN
                    : pct > 0.333f ? ChatFormatting.GOLD
                    : ChatFormatting.RED;
            tag.append(Component.literal(" " + Math.round(health) + "/" + Math.round(maxHealth) + "hp").withStyle(col));
        }

        if (nametags.showPing() && entity instanceof Player p) {
            PlayerInfo info = mc.player.connection.getPlayerInfo(p.getUUID());
            if (info != null) {
                int ping = info.getLatency();
                ChatFormatting pingCol = ping < 100 ? ChatFormatting.GREEN
                        : ping < 200 ? ChatFormatting.YELLOW
                        : ping < 300 ? ChatFormatting.GOLD
                        : ChatFormatting.RED;
                tag.append(Component.literal(" " + ping + "ms").withStyle(pingCol));
            }
        }

        if (nametags.showGamemode() && entity instanceof Player p) {
            PlayerInfo info = mc.player.connection.getPlayerInfo(p.getUUID());
            if (info != null) {
                String gm = switch (info.getGameMode()) {
                    case CREATIVE  -> "C";
                    case SPECTATOR -> "Sp";
                    case ADVENTURE -> "Adv";
                    default        -> "S";
                };
                tag.append(Component.literal(" [" + gm + "]").withStyle(ChatFormatting.AQUA));
            }
        }

        if (nametags.showDistance()) {
            double dist = entity.distanceTo(mc.player);
            tag.append(Component.literal(String.format(" %.0fm", dist)).withStyle(ChatFormatting.GRAY));
        }

        if (nametags.showItems() && entity instanceof Player p) {
            ItemStack mainHand = p.getItemBySlot(EquipmentSlot.MAINHAND);
            ItemStack offHand = p.getItemBySlot(EquipmentSlot.OFFHAND);
            if (!mainHand.isEmpty()) {
                tag.append(Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY));
                tag.append(mainHand.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
                tag.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
            }
            if (!offHand.isEmpty()) {
                tag.append(Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY));
                tag.append(offHand.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
                tag.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        state.nameTag = tag;
    }
}
