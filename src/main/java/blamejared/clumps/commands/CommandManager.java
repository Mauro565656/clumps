package blamejared.clumps.commands;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Intercepts outgoing chat messages. If a message starts with {@link ChatCommand#PREFIX},
 * it's treated as a client command and is NOT sent to the server.
 *
 * Register commands with {@link #register(ChatCommand)} during client init,
 * then call {@link #install()} once.
 */
public final class CommandManager {

    private static final List<ChatCommand> COMMANDS = new ArrayList<>();

    private CommandManager() {}

    public static void register(ChatCommand command) {
        COMMANDS.add(command);
    }

    public static List<ChatCommand> getCommands() {
        return COMMANDS;
    }

    private static void chat(Minecraft client, String message) {
        ChatCommand.ChatOutput.print(client, message);
    }

    /** Install the chat interceptor. Call once during client init. */
    public static void install() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!blamejared.clumps.ClumpsConfig.chatCommandsEnabled) return true;
            if (message == null || !message.startsWith(ChatCommand.PREFIX)) return true;

            String body = message.substring(ChatCommand.PREFIX.length()).trim();
            if (body.isEmpty()) return true; // lone "." — let it pass through

            String[] parts = body.split("\\s+");
            String name = parts[0].toLowerCase();
            String[] args = new String[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, args.length);

            Minecraft client = Minecraft.getInstance();

            if (name.equals("help") || name.equals("commands") || name.equals("cmds")) {
                printHelp(client);
                return false;
            }

            for (ChatCommand cmd : COMMANDS) {
                if (cmd.getName().equalsIgnoreCase(name)) {
                    try {
                        boolean handled = cmd.run(client, args);
                        if (!handled) {
                            chat(client, "§cUsage: " + ChatCommand.PREFIX
                                    + cmd.getName() + " " + cmd.getUsage());
                        }
                    } catch (Throwable t) {
                        chat(client, "§cCommand error: " + t.getMessage());
                    }
                    return false;
                }
            }

            chat(client, "§cUnknown command: " + ChatCommand.PREFIX + name
                    + ". Try " + ChatCommand.PREFIX + "help");
            return false;
        });
    }

    private static void printHelp(Minecraft client) {
        chat(client, "§b=== Clumps Commands ===");
        for (ChatCommand cmd : COMMANDS) {
            chat(client, "§e" + ChatCommand.PREFIX + cmd.getName()
                    + " " + cmd.getUsage() + "§7 — " + cmd.getDescription());
        }
    }
}
