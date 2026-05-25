package blamejared.clumps;

import blamejared.clumps.gui.Theme;
import blamejared.clumps.modules.BlockESP;
import blamejared.clumps.modules.FriendData;
import blamejared.clumps.modules.Friends;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public final class ClumpsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean chatCommandsEnabled = true;

    private static int guiKeyCode = GLFW.GLFW_KEY_F8;

    private static final Path LEGACY_CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve(".clumps-client.json");

    private static final Path DATAPACK_CONFIG_PATH =
            FabricLoader.getInstance()
                    .getGameDir()
                    .resolve("datapacks")
                    .resolve("clumps-client")
                    .resolve("data")
                    .resolve("clumps")
                    .resolve("config")
                    .resolve("modules.json");

    private ClumpsConfig() {}

    public static int loadGuiKey() {
        Path configPath = resolveReadablePath();
        if (configPath == null) return GLFW.GLFW_KEY_F8;

        try {
            JsonObject root = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
            if (root.has("guiKey")) {
                guiKeyCode = InputConstants.getKey(root.get("guiKey").getAsString()).getValue();
            }
            if (root.has("theme")) {
                Theme.setCurrent(root.get("theme").getAsString());
            }
        } catch (IOException | RuntimeException ignored) {}

        return guiKeyCode;
    }

    public static void loadInto(List<ClumpsModule> modules) {
        Path configPath = resolveReadablePath();
        if (configPath == null) return;

        try {
            JsonObject root = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();

            if (root.has("chatCommandsEnabled")) {
                chatCommandsEnabled = root.get("chatCommandsEnabled").getAsBoolean();
            }

            if (root.has("theme")) {
                Theme.setCurrent(root.get("theme").getAsString());
            }

            JsonObject moduleStates =
                    root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();

            for (ClumpsModule module : modules) {
                if (!moduleStates.has(module.getId())) continue;

                JsonObject moduleState = moduleStates.getAsJsonObject(module.getId());

                if (moduleState.has("enabled")) {
                    module.enabled = moduleState.get("enabled").getAsBoolean();
                }

                if (module.keybind != null && moduleState.has("keybind")) {
                    module.keybind.setKey(
                            InputConstants.getKey(moduleState.get("keybind").getAsString())
                    );
                }

                if (moduleState.has("options")) {
                    JsonObject options = moduleState.getAsJsonObject("options");

                    for (Option<?> option : module.getOptions()) {
                        if (!options.has(option.getName())) continue;

                        JsonElement value = options.get(option.getName());

                        if (option instanceof Option.BoolOption boolOption) {
                            boolOption.setValue(value.getAsBoolean());
                        } else if (option instanceof Option.IntOption intOption) {
                            intOption.setValue(value.getAsInt());
                        } else if (option instanceof Option.DoubleOption dblOption) {
                            dblOption.setValue(value.getAsDouble());
                        }
                    }
                }

                if (module instanceof BlockESP blockESP && moduleState.has("customBlocks")) {
                    Type type = new TypeToken<List<String>>() {}.getType();
                    List<String> list = GSON.fromJson(moduleState.get("customBlocks"), type);
                    blockESP.setCustomBlockIds(list);
                }

                if (module instanceof Friends friendsModule && moduleState.has("friends")) {
                    JsonArray friendsArr = moduleState.getAsJsonArray("friends");
                    List<FriendData> friendDataList = new java.util.ArrayList<>();
                    for (int i = 0; i < friendsArr.size(); i++) {
                        JsonElement elem = friendsArr.get(i);
                        if (elem.isJsonObject()) {
                            JsonObject fObj = elem.getAsJsonObject();
                            String name = fObj.get("name").getAsString();
                            boolean attackable = fObj.has("attackable") && fObj.get("attackable").getAsBoolean();
                            boolean showTracers = !fObj.has("showTracers") || fObj.get("showTracers").getAsBoolean();
                            boolean showEsp = !fObj.has("showEsp") || fObj.get("showEsp").getAsBoolean();
                            friendDataList.add(new FriendData(name, attackable, showTracers, showEsp));
                        } else {
                            friendDataList.add(new FriendData(elem.getAsString()));
                        }
                    }
                    friendsModule.setFromFriendData(friendDataList);
                }
            }

            KeyMapping.resetMapping();

        } catch (IOException | RuntimeException ignored) {}
    }

    public static void save(Collection<ClumpsModule> modules) {
        JsonObject root = new JsonObject();
        root.addProperty("chatCommandsEnabled", chatCommandsEnabled);
        root.addProperty("guiKey", ClumpsClient.guiKey.saveString());
        root.addProperty("theme", Theme.currentName());

        JsonObject moduleStates = new JsonObject();
        root.add("modules", moduleStates);

        for (ClumpsModule module : modules) {
            JsonObject moduleState = new JsonObject();
            moduleState.addProperty("enabled", module.enabled);

            if (module.keybind != null) {
                moduleState.addProperty("keybind", module.keybind.saveString());
            }

            JsonObject options = new JsonObject();
            for (Option<?> option : module.getOptions()) {
                if (option instanceof Option.BoolOption boolOption) {
                    options.addProperty(option.getName(), boolOption.getValue());
                } else if (option instanceof Option.IntOption intOption) {
                    options.addProperty(option.getName(), intOption.getValue());
                } else if (option instanceof Option.DoubleOption dblOption) {
                    options.addProperty(option.getName(), dblOption.getValue());
                }
            }
            moduleState.add("options", options);

            if (module instanceof BlockESP blockESP) {
                moduleState.add("customBlocks", GSON.toJsonTree(blockESP.getCustomBlockIds()));
            }

            if (module instanceof Friends friendsModule) {
                JsonArray friendsArr = new JsonArray();
                for (FriendData fd : friendsModule.getFriends()) {
                    JsonObject fObj = new JsonObject();
                    fObj.addProperty("name", fd.getName());
                    fObj.addProperty("attackable", fd.isAttackable());
                    fObj.addProperty("showTracers", fd.showTracers());
                    fObj.addProperty("showEsp", fd.showEsp());
                    friendsArr.add(fObj);
                }
                moduleState.add("friends", friendsArr);
            }

            moduleStates.add(module.getId(), moduleState);
        }

        try {
            Files.createDirectories(DATAPACK_CONFIG_PATH.getParent());
            Files.writeString(DATAPACK_CONFIG_PATH, GSON.toJson(root));
        } catch (IOException ignored) {}
    }

    private static Path resolveReadablePath() {
        if (Files.exists(DATAPACK_CONFIG_PATH)) return DATAPACK_CONFIG_PATH;
        if (Files.exists(LEGACY_CONFIG_PATH)) return LEGACY_CONFIG_PATH;
        return null;
    }
}
