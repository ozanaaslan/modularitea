package com.github.ozanaaslan.modularitea.demo.warehouse;

import com.github.ozanaaslan.modularitea.components.*;

import java.util.concurrent.TimeUnit;

public class InventoryMonitor {

    // TEST: Dependency Injection (Injecting the Beans from above)
    @ServiceManager.InjectService
    private StockDatabase db;

    @ServiceManager.InjectService
    private String location;

    // TEST: TaskManager (Scheduled Logic)
    @TaskManager.Task(interval = 5, unit = TimeUnit.SECONDS)
    public void stockCheck() {
        System.out.println("[Task] Auto-scanning " + location + " inventory...");
    }

    // TEST: EventManager (Reactive Logic)
    @EventManager.EventBus
    public void onAlert(InventoryAlertEvent event) {
        System.out.println("[Event] ALERT RECEIVED: " + event.getMsg());
    }

    // TEST: CommandManager (User Interface)
    @CommandManager.Command(name = "status", description = "Check Warehouse Status")
    public void checkStatus(CommandManager.CommandSender sender) {
        sender.sendMessage("Warehouse: " + location);
        sender.sendMessage("Database: " + (db != null ? db.getName() : "OFFLINE"));
    }
}

// Custom Event
class InventoryAlertEvent extends EventManager.Event {
    private String msg;
    public InventoryAlertEvent(String msg) { this.msg = msg; }
    public String getMsg() { return msg; }
}