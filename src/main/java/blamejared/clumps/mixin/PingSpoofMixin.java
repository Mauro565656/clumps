package blamejared.clumps.mixin;

import blamejared.clumps.ClumpsClient;
import blamejared.clumps.modules.PingSpoof;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class PingSpoofMixin {

    @Inject(method = "handleKeepAlive", at = @At("HEAD"), cancellable = true)
    private void clumps$delayKeepAlive(ClientboundKeepAlivePacket packet, CallbackInfo ci) {
        for (var m : ClumpsClient.modules) {
            if (m instanceof PingSpoof pingSpoof && m.enabled && pingSpoof.getDelay() > 0) {
                ci.cancel();
                long id = packet.getId();
                int delay = pingSpoof.getDelay();
                ClientCommonPacketListenerImpl self = (ClientCommonPacketListenerImpl)(Object) this;
                new Thread(() -> {
                    try {
                        Thread.sleep(delay);
                        self.send(new ServerboundKeepAlivePacket(id));
                    } catch (InterruptedException ignored) {}
                }).start();
                return;
            }
        }
    }
}
