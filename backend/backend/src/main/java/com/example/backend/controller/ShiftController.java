package com.example.backend.controller;

import com.example.backend.entity.Shift;
import com.example.backend.repository.ShiftRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftRepository shiftRepository;

    public ShiftController(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    @GetMapping
    public List<Shift> getAll() {
        return shiftRepository.findAll();
    }

    @PostMapping
    public Shift create(@RequestBody Shift shift) {
        return shiftRepository.save(shift);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!shiftRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        shiftRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
