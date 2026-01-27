package com.example.backend.repository;

import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.ConfigurationItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CiUserMappingRepository extends JpaRepository<CiUserMapping, Long> {
    @Query(
            "select c from CiUserMapping c "
                    + "join fetch c.teamMember "
                    + "where c.configurationItem = :configurationItem "
                    + "order by c.sortOrder asc")
    List<CiUserMapping> findByConfigurationItemOrderBySortOrderAsc(
            @Param("configurationItem") ConfigurationItem configurationItem);
}
