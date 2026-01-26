package com.example.backend.repository;

import com.example.backend.entity.ConfigurationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfigurationItemRepository extends JpaRepository<ConfigurationItem, Long> {
    Optional<ConfigurationItem> findByName(String name);
}
