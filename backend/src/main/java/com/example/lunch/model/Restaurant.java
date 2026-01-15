package com.example.lunch.model;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class Restaurant {
    private String id;
    private String name;
    private List<MenuItem> menu;
    private String menuImageUrl;

    public Restaurant() {
    }

    public Restaurant(String name, List<MenuItem> menu) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.menu = menu;
    }

    public Restaurant(String id, String name, List<MenuItem> menu, String menuImageUrl) {
        this.id = id;
        this.name = name;
        this.menu = menu;
        this.menuImageUrl = menuImageUrl;
    }
}
