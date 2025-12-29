package com.github.ozanaaslan.modularitea.demo.guiapp;

import com.github.ozanaaslan.modularitea.AbstractModulariteaApplication;
import javax.swing.*;
import java.awt.*;

public class NetworkApp extends AbstractModulariteaApplication {

    private JFrame frame;
    private JTextArea logArea;

    @Override
    public void entrypoint(AbstractModulariteaApplication app) {
        // Build the GUI on the Event Dispatch Thread (Swing Requirement)
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Modularitea Network Control");
            frame.setSize(500, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            logArea = new JTextArea();
            logArea.setBackground(Color.BLACK);
            logArea.setForeground(Color.GREEN);
            logArea.setEditable(false);
            frame.add(new JScrollPane(logArea), BorderLayout.CENTER);

            JButton btn = new JButton("Broadcast Ping");
            btn.addActionListener(e -> {
                // Testing Event Dispatching from a GUI button
                app.getEventManager().dispatch(new NetworkEvent("Manual Ping Sent!"));
            });
            frame.add(btn, BorderLayout.SOUTH);

            frame.setVisible(true);
            logArea.append("System Online. Waiting for Intertwined components...\n");
        });
        
        // Register our GUI logic so the framework can inject into it
        app.intertwine(new NetworkLogic(this));
    }

    // Helper for our logic class to write to the screen
    public void log(String msg) {
        if (logArea != null) SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }
}