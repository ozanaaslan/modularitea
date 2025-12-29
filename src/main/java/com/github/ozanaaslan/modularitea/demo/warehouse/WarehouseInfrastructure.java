package com.github.ozanaaslan.modularitea.demo.warehouse;

import com.github.ozanaaslan.modularitea.components.ServiceManager.ServiceBean;

public class WarehouseInfrastructure {

    // TEST: Method-based Bean (Factory)
    @ServiceBean
    public StockDatabase provideDatabase() {
        System.out.println("[Bean] Initializing Stock Database...");
        return new StockDatabase("Main_Store");
    }

    // TEST: Field-based Bean
    @ServiceBean
    private final String location = "Berlin_Hub_01";
}

// Dummy object to represent a Service
class StockDatabase {
    private String name;
    public StockDatabase(String name) { this.name = name; }
    public String getName() { return name; }
}