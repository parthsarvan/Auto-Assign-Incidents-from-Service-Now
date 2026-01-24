package com.example.backend.controller;

import com.example.backend.entity.ConfigurationItem;
import com.example.backend.repository.ConfigurationItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuration-items")
public class ConfigurationItemController {

    private final ConfigurationItemRepository configurationItemRepository;

    public ConfigurationItemController(ConfigurationItemRepository configurationItemRepository) {
        this.configurationItemRepository = configurationItemRepository;
    }

    @GetMapping
    public List<ConfigurationItem> getAll() {
        return configurationItemRepository.findAll();
    }

    @PostMapping
    public ConfigurationItem create(@RequestBody ConfigurationItem configurationItem) {
        return configurationItemRepository.save(configurationItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!configurationItemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        configurationItemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
