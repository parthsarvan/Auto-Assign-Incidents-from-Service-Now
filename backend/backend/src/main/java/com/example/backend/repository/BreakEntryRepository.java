package com.example.backend.repository;

import com.example.backend.entity.BreakEntry;
import com.example.backend.entity.TeamMember;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreakEntryRepository extends JpaRepository<BreakEntry, Long> {
    boolean existsByTeamMemberAndStartTsLessThanEqualAndEndTsGreaterThanEqual(
            TeamMember teamMember,
            Instant nowStart,
            Instant nowEnd);
}
