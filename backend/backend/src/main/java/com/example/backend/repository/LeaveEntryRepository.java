package com.example.backend.repository;

import com.example.backend.entity.LeaveEntry;
import com.example.backend.entity.TeamMember;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveEntryRepository extends JpaRepository<LeaveEntry, Long> {
    boolean existsByTeamMemberAndStartTsLessThanEqualAndEndTsGreaterThanEqual(
            TeamMember teamMember,
            Instant nowStart,
            Instant nowEnd);
}
