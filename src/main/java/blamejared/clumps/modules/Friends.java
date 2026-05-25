package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class Friends extends ClumpsModule {

    private final List<FriendData> friends = new ArrayList<>();

    @Override
    public String getName() { return "Friends"; }

    @Override
    public String getDescription() { return "Manage your friends list with per-player options"; }

    @Override
    public String getCategory() { return "Misc"; }

    public boolean isFriend(String username) {
        return friends.stream().anyMatch(f -> f.getName().equalsIgnoreCase(username));
    }

    public boolean isFriend(String username, boolean checkAttackable) {
        return friends.stream().anyMatch(f ->
                f.getName().equalsIgnoreCase(username) && f.isAttackable() == checkAttackable);
    }

    public FriendData getFriendData(String username) {
        return friends.stream()
                .filter(f -> f.getName().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    public List<FriendData> getFriends() {
        return friends;
    }

    public List<String> getFriendNames() {
        return friends.stream().map(FriendData::getName).toList();
    }

    public boolean addFriend(String name) {
        if (name == null || name.isBlank()) return false;
        if (isFriend(name)) return false;
        friends.add(new FriendData(name.trim()));
        return true;
    }

    public boolean removeFriend(String name) {
        return friends.removeIf(f -> f.getName().equalsIgnoreCase(name));
    }

    public void setFriendNames(List<String> names) {
        friends.clear();
        for (String name : names) {
            friends.add(new FriendData(name));
        }
    }

    public void setFromFriendData(List<FriendData> data) {
        friends.clear();
        friends.addAll(data);
    }

    @Override
    public void onTick(Minecraft client) {}
}
