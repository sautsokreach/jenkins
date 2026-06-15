package com.example.demo.controller;

import com.example.demo.model.Item;
import com.example.demo.repository.ItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@Tag(name = "Items", description = "CRUD operations for items")
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @Operation(summary = "Get all items")
    @GetMapping
    public List<Item> getAll() {
        return itemRepository.findAll();
    }

    @Operation(summary = "Get item by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Item> getById(@PathVariable String id) {
        return itemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new item")
    @PostMapping
    public Item create(@RequestBody Item item) {
        return itemRepository.save(item);
    }

    @Operation(summary = "Update an existing item")
    @PutMapping("/{id}")
    public ResponseEntity<Item> update(@PathVariable String id, @RequestBody Item updated) {
        return itemRepository.findById(id).map(item -> {
            item.setName(updated.getName());
            item.setDescription(updated.getDescription());
            item.setPrice(updated.getPrice());
            return ResponseEntity.ok(itemRepository.save(item));
        }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete an item")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!itemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
