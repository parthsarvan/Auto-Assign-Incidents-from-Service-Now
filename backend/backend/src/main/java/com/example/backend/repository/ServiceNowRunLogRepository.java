package com.example.backend.repository;

import com.example.backend.entity.ServiceNowRunLogEntity;
import com.example.backend.entity.Team;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceNowRunLogRepository extends JpaRepository<ServiceNowRunLogEntity, Long> {
    List<ServiceNowRunLogEntity> findTop100ByTeamOrderByTimestampDesc(Team team);
}
