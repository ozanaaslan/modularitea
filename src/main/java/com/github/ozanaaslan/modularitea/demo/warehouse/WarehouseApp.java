package com.github.ozanaaslan.modularitea.demo.warehouse;

import com.github.ozanaaslan.modularitea.AbstractModulariteaApplication;
import com.github.ozanaaslan.modularitea.components.CommandManager;
import com.github.ozanaaslan.modularitea.components.ServiceManager;

public class WarehouseApp extends AbstractModulariteaApplication {

    // Test Field Injection in the Main Class
    @ServiceManager.InjectService
    private CommandManager console;

    @Override
    public void entrypoint(AbstractModulariteaApplication app) {

        // Manually register the local classes (JAR modules would be handled by the Kernel automatically)
        app.getServiceManager().registerBeans(new WarehouseInfrastructure());

        // This is the "Intertwine" call we discussed
        app.intertwine(new InventoryMonitor());

        System.out.println(">>> Warehouse Application Started!");
        
        // Dispatch a test event
        app.getEventManager().dispatch(new InventoryAlertEvent("Warehouse System Initialized"));

    }

    // A command inside the main class to test if the App itself is intertwined
    @CommandManager.Command(name = "info", description = "App Info")
    public void info(CommandManager.CommandSender sender) {
        sender.sendMessage("Warehouse v1.0 running on Modularitea!");
    }
}