package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class LogoutSpots extends ClumpsModule {
    private final Option.BoolOption showBox = new Option.BoolOption("Show Box", true);
    private final Option.DoubleOption scale = new Option.DoubleOption("Scale", 1.0, 0.5, 3.0, 0.1);

    private final List<Entry> entries = new ArrayList<>();
    private final List<UUID> lastPlayers = new ArrayList<>();

    @Override
    public String getName() { return "Logout Spots"; }

    @Override
    public String getDescription() { return "Show where players logged out"; }

    @Override
    public String getCategory() { return "Visual"; }

    @Override
    public List<Option<?>> getOptions() { return Option.list(showBox, scale); }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.getConnection() == null) return;

        List<UUID> online = client.getConnection().getOnlinePlayers().stream().map(info -> info.getProfile().id()).toList();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player == client.player) continue;
            UUID uuid = player.getUUID();
            if (!online.contains(uuid) && !contains(uuid)) add(player);
        }

        for (Iterator<Entry> it = entries.iterator(); it.hasNext();) {
            Entry entry = it.next();
            if (online.contains(entry.uuid)) it.remove();
        }

        lastPlayers.clear();
        lastPlayers.addAll(online);
    }

    private boolean contains(UUID uuid) {
        for (Entry entry : entries) {
            if (entry.uuid.equals(uuid)) return true;
        }
        return false;
    }

    private void add(Player player) {
        entries.removeIf(entry -> entry.uuid.equals(player.getUUID()));
        entries.add(new Entry(player.getUUID(), player.getName().getString(), player.position(), player.getBoundingBox(), Math.round(player.getHealth() + player.getAbsorptionAmount())));
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public boolean showBox() {
        return showBox.getValue();
    }

    public float scale() {
        return scale.getValue().floatValue();
    }

    public record Entry(UUID uuid, String name, Vec3 pos, AABB box, int health) {}
}
