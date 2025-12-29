package com.github.ozanaaslan.modularitea;

import com.github.ozanaaslan.modularitea.components.*;
import lombok.Getter;

public abstract class AbstractModulariteaApplication implements IModulariteaEntrypoint {

    @Getter private static CommandManager commandManager;
    @Getter private static ModuleManager moduleManager;
    @Getter private static EventManager eventManager;
    @Getter private static ServiceManager serviceManager;
    @Getter private static TaskManager taskManager;

    @Getter private static AbstractModulariteaApplication instance;

    /**
     * The inherited main method.
     * When someone runs 'java ModApp', this code executes.
     */
    public static void main(String[] args) {
        try {
            // Determine the class name of the class that was actually launched
            String launchedClassName = System.getProperty("sun.java.command");
            // If the above fails in certain environments, we can use a helper or force a param

            Class<?> clazz = Class.forName(launchedClassName.split(" ")[0]);

            // Ensure it's actually an app we can run
            if (AbstractModulariteaApplication.class.isAssignableFrom(clazz)) {
                AbstractModulariteaApplication app = (AbstractModulariteaApplication) clazz.getDeclaredConstructor().newInstance();
                app.initialize();
            }
        } catch (Exception e) {
            System.err.println("Failed to bootstrap Modularitea: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void initialize() {
        instance = this;
        System.out.println("--- Booting Modularitea Kernel ---");

        // 1. Singleton/Manager Setup
        serviceManager = ServiceManager.getInstance();
        commandManager = new CommandManager();
        eventManager = new EventManager();
        taskManager = TaskManager.getInstance();
        moduleManager = new ModuleManager(); // Uses default 'modules' dir

        // 2. Self-Registration (The App itself can have @Commands, @Tasks, etc)
        intertwine(this);

        // 3. Load External JAR Modules
        moduleManager.loadModules();

        // 4. Intertwine every loaded Module
        moduleManager.getManifests().forEach(manifest -> intertwine(manifest.getModule().getInstance()));

        // 5. Fire Lifecycles
        moduleManager.invokePrimaries();

        // 6. Hand over control to the Developer's entrypoint
        this.entrypoint(this);

        // 7. Start the Console
        commandManager.startListening("Modularitea", false);
    }

    /**
     * Logic to connect any object to all framework managers automatically.
     */
    public void intertwine(Object obj) {
        serviceManager.inject(obj);      // Fill @InjectService fields
        serviceManager.registerBeans(obj); // Register factory methods
        commandManager.register(obj);    // Register @Command
        taskManager.register(obj);       // Register @Task
        eventManager.registerInstance(obj); // Register @EventBus
    }
}