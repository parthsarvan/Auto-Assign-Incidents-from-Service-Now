package com.example.backend.repository;

import com.example.backend.entity.ServiceNowRunLogEntity;
import com.example.backend.entity.Team;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceNowRunLogRepository extends JpaRepository<ServiceNowRunLogEntity, Long> {
    List<ServiceNowRunLogEntity> findByTeamAndTimestampGreaterThanEqualOrderByTimestampDesc(
            Team team,
            Instant timestamp);

    @Modifying
    @Query("delete from ServiceNowRunLogEntity log where log.timestamp < :timestamp")
    int deleteByTimestampBefore(@Param("timestamp") Instant timestamp);
}
