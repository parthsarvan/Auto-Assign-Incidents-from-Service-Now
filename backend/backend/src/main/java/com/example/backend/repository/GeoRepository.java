package com.example.backend.repository;

import com.example.backend.entity.Geo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeoRepository extends JpaRepository<Geo, Long> {}
