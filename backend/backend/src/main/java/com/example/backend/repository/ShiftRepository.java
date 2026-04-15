package com.example.backend.repository;

import com.example.backend.entity.Shift;
import com.example.backend.entity.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findAllByTeamOrderByNameAsc(Team team);
    @Query("select s from Shift s where s.s_id = :id and s.team = :team")
    Optional<Shift> findByIdAndTeam(@Param("id") Long id, @Param("team") Team team);
    boolean existsByNameIgnoreCaseAndTeam(String name, Team team);
    @Query("""
            select count(s) > 0
            from Shift s
            where s.team = :team
              and lower(trim(s.name)) = lower(trim(:name))
            """)
    boolean existsByTeamAndNormalizedName(@Param("team") Team team, @Param("name") String name);
    long countByTeam(Team team);
}
