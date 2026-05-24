package com.example.backend.repository;

import com.example.backend.entity.LeaveEntry;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveEntryRepository extends JpaRepository<LeaveEntry, Long> {
    boolean existsByTeamMemberAndStartTsLessThanEqualAndEndTsGreaterThanEqual(
            TeamMember teamMember,
            Instant nowStart,
            Instant nowEnd);

    @Query(
            "select l from LeaveEntry l "
                    + "join fetch l.teamMember tm "
                    + "left join fetch tm.geo "
                    + "where l.team = :team "
                    + "order by l.startTs desc")
    List<LeaveEntry> findAllByTeamWithTeamMember(@Param("team") Team team);

    @Query(
            "select l from LeaveEntry l "
                    + "join fetch l.teamMember tm "
                    + "left join fetch tm.geo "
                    + "where l.leave_id = :id and l.team = :team")
    Optional<LeaveEntry> findByIdAndTeam(@Param("id") Long id, @Param("team") Team team);

    long deleteByTeamMember(TeamMember teamMember);
}
