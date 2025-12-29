package com.github.ozanaaslan.modularitea.demo.moduletest;

import com.github.ozanaaslan.modularitea.AbstractModulariteaApplication;
import com.github.ozanaaslan.modularitea.components.CommandManager;
import com.github.ozanaaslan.modularitea.components.ServiceManager;

import javax.swing.*;
import java.awt.*;

public class ModApp extends AbstractModulariteaApplication {

    private JTextArea logArea;



    @Override
    public void entrypoint(AbstractModulariteaApplication app) {
        // Setup a simple GUI to see events/tasks in real-time
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Modularitea Stress Test");
            frame.setSize(600, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            logArea = new JTextArea();
            logArea.setBackground(Color.BLACK);
            logArea.setForeground(Color.CYAN);
            frame.add(new JScrollPane(logArea));
            frame.setVisible(true);
            logArea.append("Kernel Loaded. Waiting for Modules...\n");
        });
    }

    // A command to verify the main app is intertwined
    @CommandManager.Command(name = "ping")
    public void ping(CommandManager.CommandSender sender) {
        sender.sendMessage("Pong! Kernel is alive.");
        if (logArea != null) logArea.append("[CONSOLE] Ping received\n");
    }

    public void guiLog(String msg) {
        if (logArea != null) SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }
}