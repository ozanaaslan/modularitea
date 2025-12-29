package com.github.ozanaaslan.modularitea.components;

import lombok.Builder;
import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class CommandManager {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Command {
        String name();
        String[] aliases() default {}; // Added: Supports multiple names for one command
        String description() default "No description provided";
        String permission() default "";
    }

    public interface CommandSender {
        void sendMessage(String message);
        boolean hasPermission(String permission);
    }

    @Getter @Builder
    private static class RegisteredCommand {
        private final String name;
        private final String description;
        private final Object instance;
        private final Method method;
        private final String permission;
        private final boolean passArgs; // Optimization: Pre-calculated parameter logic
    }

    private final Map<String, RegisteredCommand> registry = new ConcurrentHashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private volatile boolean running = true;

    public CommandManager() {
        // Automatically register the help command for this instance
        this.register(this);
    }

    /**
     * Optimized Registration: Pre-scans and caches execution strategies.
     */
    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Command.class)) continue;

            Command meta = method.getAnnotation(Command.class);
            method.setAccessible(true); // Do this once, not during execution

            RegisteredCommand rc = RegisteredCommand.builder()
                    .name(meta.name().toLowerCase())
                    .description(meta.description())
                    .instance(listener)
                    .method(method)
                    .permission(meta.permission())
                    .passArgs(method.getParameterCount() == 2)
                    .build();

            // Map the main name and all aliases to the same command object
            registry.put(rc.getName(), rc);
            for (String alias : meta.aliases()) {
                registry.put(alias.toLowerCase(), rc);
            }
        }
    }

    /**
     * High-speed Dispatcher: Minimal logic during the actual call.
     */
    public void execute(CommandSender sender, String input) {
        if (input == null || (input = input.trim()).isEmpty()) return;

        String[] split = input.split("\\s+");
        RegisteredCommand cmd = registry.get(split[0].toLowerCase());

        if (cmd == null) {
            sender.sendMessage("Unknown command. Type 'help' for a list of commands.");
            return;
        }

        if (!cmd.getPermission().isEmpty() && !sender.hasPermission(cmd.getPermission())) {
            sender.sendMessage("Access Denied.");
            return;
        }

        try {
            if (cmd.isPassArgs()) {
                String[] args = split.length > 1 ? Arrays.copyOfRange(split, 1, split.length) : new String[0];
                cmd.getMethod().invoke(cmd.getInstance(), sender, args);
            } else {
                cmd.getMethod().invoke(cmd.getInstance(), sender);
            }
        } catch (Exception e) {
            sender.sendMessage("Execution Error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    /**
     * Built-in Help Command: Dynamically reads the registry.
     */
    @Command(name = "help", aliases = {"?", "h"}, description = "Displays all available commands")
    private void helpCommand(CommandSender sender) {
        sender.sendMessage("=== Available Commands ===");
        // Use a Set to avoid listing aliases as separate commands
        new HashSet<>(registry.values()).forEach(cmd ->
                sender.sendMessage("- " + cmd.getName() + ": " + cmd.getDescription())
        );
    }

    public void startListening(String prefix, boolean daemon) {
        Thread thread = new Thread(() -> {
            Console console = new Console();
            while (running) {
                System.out.print(prefix + " > ");
                if (scanner.hasNextLine()) execute(console, scanner.nextLine());
                else break;
            }
        }, "CommandThread");
        thread.setDaemon(daemon);
        thread.start();
    }

    public void stop() { this.running = false; }

    private static class Console implements CommandSender {
        public void sendMessage(String msg) { System.out.println(msg); }
        public boolean hasPermission(String p) { return true; }
    }
}
