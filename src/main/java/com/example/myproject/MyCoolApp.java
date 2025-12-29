package com.example.myproject;

import com.github.ozanaaslan.modularitea.AbstractModulariteaApplication;

public class MyCoolApp extends AbstractModulariteaApplication {
    @Override
    public void entrypoint(AbstractModulariteaApplication app) {
        System.out.println("Hello from the custom app!");
    }
}