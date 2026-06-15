package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "items")
@Schema(description = "Item entity")
public class Item {

    @Id
    @Schema(description = "Unique identifier", example = "665f1a2b3c4d5e6f7a8b9c0d")
    private String id;

    @Schema(description = "Item name", example = "Laptop")
    private String name;

    @Schema(description = "Item description", example = "High-performance laptop")
    private String description;

    @Schema(description = "Item price", example = "999.99")
    private double price;

    public Item() {}

    public Item(String name, String description, double price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
