package com.example.backend.controller;

import com.example.backend.dto.GeoShiftMappingRequest;
import com.example.backend.entity.Geo;
import com.example.backend.entity.GeoShiftMapping;
import com.example.backend.entity.Shift;
import com.example.backend.repository.GeoRepository;
import com.example.backend.repository.GeoShiftMappingRepository;
import com.example.backend.repository.ShiftRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geo-shift-mappings")
public class GeoShiftMappingController {

    private final GeoShiftMappingRepository mappingRepository;
    private final GeoRepository geoRepository;
    private final ShiftRepository shiftRepository;

    public GeoShiftMappingController(
        GeoShiftMappingRepository mappingRepository,
        GeoRepository geoRepository,
        ShiftRepository shiftRepository
    ) {
        this.mappingRepository = mappingRepository;
        this.geoRepository = geoRepository;
        this.shiftRepository = shiftRepository;
    }

    @GetMapping
    public List<GeoShiftMapping> getAll() {
        return mappingRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody GeoShiftMappingRequest request) {
        Geo geo = geoRepository.findById(request.getGeoId()).orElse(null);
        Shift shift = shiftRepository.findById(request.getShiftId()).orElse(null);

        if (geo == null || shift == null) {
            return ResponseEntity.badRequest().body("Invalid geo or shift id");
        }

        GeoShiftMapping mapping = new GeoShiftMapping(geo, shift);
        return ResponseEntity.ok(mappingRepository.save(mapping));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!mappingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        mappingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
