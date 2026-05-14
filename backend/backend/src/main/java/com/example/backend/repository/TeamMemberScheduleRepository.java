package com.example.backend.repository;

import com.example.backend.entity.TeamMemberSchedule;
import com.example.backend.entity.TeamMember;
import com.example.backend.entity.Team;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberScheduleRepository extends JpaRepository<TeamMemberSchedule, Long> {
    @Query(
            "select t from TeamMemberSchedule t "
                    + "join fetch t.geo "
                    + "join fetch t.shift "
                    + "where t.teamMember = :teamMember and :date between t.startDate and t.endDate")
    List<TeamMemberSchedule> findActiveSchedules(
            @Param("teamMember") TeamMember teamMember,
            @Param("date") LocalDate date);

    boolean existsByTeamMemberAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            TeamMember teamMember,
            LocalDate endDate,
            LocalDate startDate);

    @Query("""
            select count(t) > 0
            from TeamMemberSchedule t
            where t.teamMember = :teamMember
              and t.startDate <= :endDate
              and t.endDate >= :startDate
              and (:excludeId is null or t.tms_id <> :excludeId)
            """)
    boolean existsOverlappingSchedule(
            @Param("teamMember") TeamMember teamMember,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);

    @Query("""
            select t
            from TeamMemberSchedule t
            join fetch t.geo
            join fetch t.shift
            where t.teamMember = :teamMember
              and t.startDate <= :endDate
              and t.endDate >= :startDate
              and (:excludeId is null or t.tms_id <> :excludeId)
            """)
    List<TeamMemberSchedule> findOverlappingSchedules(
            @Param("teamMember") TeamMember teamMember,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);

    @Query(
            "select t from TeamMemberSchedule t "
                    + "join fetch t.teamMember tm "
                    + "join fetch t.geo "
                    + "join fetch t.shift "
                    + "where t.team = :team "
                    + "order by tm.f_name asc, tm.l_name asc, t.startDate asc")
    List<TeamMemberSchedule> findAllByTeamWithDetails(@Param("team") Team team);

    @Query(
            "select t from TeamMemberSchedule t "
                    + "join fetch t.teamMember "
                    + "join fetch t.geo "
                    + "join fetch t.shift "
                    + "where t.tms_id = :id and t.team = :team")
    Optional<TeamMemberSchedule> findByIdAndTeam(@Param("id") Long id, @Param("team") Team team);

    long countByTeam(Team team);
}
