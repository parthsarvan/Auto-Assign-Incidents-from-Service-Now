package com.example.backend.repository;

import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.ConfigurationItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiUserMappingRepository extends JpaRepository<CiUserMapping, Long> {
    List<CiUserMapping> findByConfigurationItemOrderBySortOrderAsc(ConfigurationItem configurationItem);
}
