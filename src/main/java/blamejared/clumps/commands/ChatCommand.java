package blamejared.clumps.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

/**
 * Base class for dot-prefix chat commands (e.g. ".hclip 5", ".vclip 100").
 *
 * Commands are intercepted before the message is sent to the server — they
 * execute locally and never appear in chat.
 */
public abstract class ChatCommand {

    /** Prefix character(s) that mark a message as a command. */
    public static final String PREFIX = ".";

    protected final String name;
    protected final String description;
    protected final String usage;

    protected ChatCommand(String name, String description, String usage) {
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getUsage()       { return usage; }

    /**
     * Execute this command. {@code args} is the whitespace-split arguments
     * AFTER the command name (e.g. for ".hclip 5" args is ["5"]).
     *
     * @return true if the command handled the input, false to show usage.
     */
    public abstract boolean run(Minecraft client, String[] args);

    protected void info(Minecraft client, String message) {
        ChatOutput.print(client, "§b[Clumps]§r " + message);
    }

    protected void error(Minecraft client, String message) {
        ChatOutput.print(client, "§c[Clumps] " + message);
    }

    protected void printUsage(Minecraft client) {
        error(client, "Usage: " + PREFIX + name + " " + usage);
    }

    /**
     * Version-tolerant chat output helper. Minecraft's public chat-output API
     * has churned (displayClientMessage / sendSystemMessage / ChatComponent.addMessage
     * with 1, 3, or 4 arguments). Rather than target a specific signature, we
     * probe at runtime and cache whatever we find. Falls back to stdout.
     */
    static final class ChatOutput {

        private static boolean resolved = false;
        private static Method  targetMethod = null;
        private static Object  targetReceiverHolder = null; // "player" or "chat"

        public static void print(Minecraft client, String message) {
            if (client == null) {
                System.out.println("[Clumps] " + message);
                return;
            }
            Component text = Component.literal(message);
            try {
                resolve(client);
                if (targetMethod != null) {
                    Object receiver = "player".equals(targetReceiverHolder)
                            ? client.player
                            : (client.gui != null ? client.gui.getChat() : null);
                    if (receiver != null) {
                        Object[] args = buildArgs(targetMethod, text);
                        if (args != null) {
                            targetMethod.invoke(receiver, args);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {}
            // Last resort.
            System.out.println("[Clumps] " + message);
        }

        private static synchronized void resolve(Minecraft client) {
            if (resolved) return;
            resolved = true;

            // Try LocalPlayer-style methods first (most convenient semantics).
            if (client.player != null) {
                Class<?> pClass = client.player.getClass();
                // Common name forms across mapping eras.
                String[] playerNames = {
                        "displayClientMessage",
                        "sendSystemMessage",
                        "sendMessage",
                        "addChatMessage",
                };
                for (String n : playerNames) {
                    Method m = findMethodAcceptingComponent(pClass, n);
                    if (m != null) {
                        targetMethod = m;
                        targetReceiverHolder = "player";
                        return;
                    }
                }
            }

            // Fall back to ChatComponent.addMessage (single Component arg if available).
            if (client.gui != null && client.gui.getChat() != null) {
                Class<?> cClass = client.gui.getChat().getClass();
                Method m = findMethodAcceptingComponent(cClass, "addMessage");
                if (m != null) {
                    targetMethod = m;
                    targetReceiverHolder = "chat";
                }
            }
        }

        /**
         * Find a method on {@code cls} named {@code name} whose first parameter
         * is assignable from Component. Prefers methods with fewer parameters.
         */
        private static Method findMethodAcceptingComponent(Class<?> cls, String name) {
            Method best = null;
            int    bestArity = Integer.MAX_VALUE;
            for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (!m.getName().equals(name)) continue;
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 0) continue;
                    if (!params[0].isAssignableFrom(Component.class)
                            && !Component.class.isAssignableFrom(params[0])) continue;
                    if (params.length < bestArity) {
                        best = m;
                        bestArity = params.length;
                    }
                }
                if (best != null) break;
            }
            if (best != null) best.setAccessible(true);
            return best;
        }

        /**
         * Build an argument array for the chosen method. First arg is the
         * Component; remaining args are filled with safe defaults (false for
         * boolean, null for objects).
         */
        private static Object[] buildArgs(Method m, Component text) {
            Class<?>[] params = m.getParameterTypes();
            Object[]   out    = new Object[params.length];
            out[0] = text;
            for (int i = 1; i < params.length; i++) {
                Class<?> p = params[i];
                if (p == boolean.class) out[i] = Boolean.FALSE;
                else if (p == int.class)     out[i] = 0;
                else if (p == long.class)    out[i] = 0L;
                else if (p == float.class)   out[i] = 0f;
                else if (p == double.class)  out[i] = 0d;
                else if (p.isPrimitive())    return null;  // unsupported primitive
                else                         out[i] = null;
            }
            return out;
        }
    }
}
