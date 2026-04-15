package com.example.backend.repository;

import com.example.backend.entity.BreakEntry;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BreakEntryRepository extends JpaRepository<BreakEntry, Long> {
    boolean existsByTeamMemberAndStartTsLessThanEqualAndEndTsGreaterThanEqual(
            TeamMember teamMember,
            Instant nowStart,
            Instant nowEnd);

    @Query(
            "select b from BreakEntry b "
                    + "join fetch b.teamMember tm "
                    + "left join fetch tm.geo "
                    + "where b.team = :team "
                    + "order by b.startTs desc")
    List<BreakEntry> findAllByTeamWithTeamMember(@Param("team") Team team);

    @Query(
            "select b from BreakEntry b "
                    + "join fetch b.teamMember tm "
                    + "left join fetch tm.geo "
                    + "where b.break_id = :id and b.team = :team")
    Optional<BreakEntry> findByIdAndTeam(@Param("id") Long id, @Param("team") Team team);
}
