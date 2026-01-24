package com.example.backend.controller;

import com.example.backend.entity.Geo;
import com.example.backend.repository.GeoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geos")
public class GeoController {

    private final GeoRepository geoRepository;

    public GeoController(GeoRepository geoRepository) {
        this.geoRepository = geoRepository;
    }

    @GetMapping
    public List<Geo> getAll() {
        return geoRepository.findAll();
    }

    @PostMapping
    public Geo create(@RequestBody Geo geo) {
        return geoRepository.save(geo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!geoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        geoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
