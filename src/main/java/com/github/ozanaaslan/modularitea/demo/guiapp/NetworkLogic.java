package com.github.ozanaaslan.modularitea.demo.guiapp;

import com.github.ozanaaslan.modularitea.components.*;
import java.util.concurrent.TimeUnit;

public class NetworkLogic {

    private final NetworkApp ui;

    public NetworkLogic(NetworkApp ui) {
        this.ui = ui;
    }

    // TEST: Service Manager (Beans)
    @ServiceManager.ServiceBean
    public String getNetworkID() {
        return "NODE-DE-882";
    }

    // TEST: TaskManager (Background Logic)
    // Every 5 seconds, it "pings" the network and updates the GUI
    @TaskManager.Task(interval = 5, unit = TimeUnit.SECONDS)
    public void backgroundScan() {
        ui.log("[SCAN] Checking remote nodes...");
    }

    // TEST: EventManager (Reaction Logic)
    // When the button is pressed in the UI, this picks it up
    @EventManager.EventBus
    public void onNetworkEvent(NetworkEvent event) {
        ui.log("[EVENT] " + event.getMessage());
    }

    // TEST: CommandManager (Integration)
    // You can type 'alert' in the console to trigger a GUI change
    @CommandManager.Command(name = "alert", description = "Send alert to GUI")
    public void alertCmd(CommandManager.CommandSender sender) {
        ui.log("[CONSOLE] Admin sent a priority alert!");
        sender.sendMessage("Alert pushed to GUI.");
    }
}

// Simple Java 8 Event
class NetworkEvent extends EventManager.Event {
    private final String message;
    public NetworkEvent(String message) { this.message = message; }
    public String getMessage() { return message; }
}