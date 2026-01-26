package com.example.backend.repository;

import com.example.backend.entity.TeamMemberSchedule;
import com.example.backend.entity.TeamMember;
import java.time.LocalDate;
import java.util.List;
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
}
