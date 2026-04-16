package com.example.backend.repository;

import com.example.backend.entity.TeamMember;
import com.example.backend.entity.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    @Query("select tm from TeamMember tm left join fetch tm.geo where tm.team = :team order by tm.f_name asc, tm.l_name asc")
    List<TeamMember> findAllByTeamOrderByName(@Param("team") Team team);

    @Query("select tm from TeamMember tm left join fetch tm.geo where tm.tm_id = :id and tm.team = :team")
    Optional<TeamMember> findByIdAndTeam(@Param("id") Long id, @Param("team") Team team);

    @Query("""
            select tm
            from TeamMember tm
            left join fetch tm.geo
            where tm.team = :team
              and lower(trim(tm.email)) = lower(trim(:email))
            """)
    Optional<TeamMember> findByTeamAndNormalizedEmail(@Param("team") Team team, @Param("email") String email);

    boolean existsByEmailIgnoreCaseAndTeam(String email, Team team);

    @Query("select count(tm) > 0 from TeamMember tm where lower(tm.sys_id) = lower(:sysId) and tm.team = :team")
    boolean existsBySysIdIgnoreCaseAndTeam(@Param("sysId") String sysId, @Param("team") Team team);

    @Query("""
            select count(tm) > 0
            from TeamMember tm
            where tm.team = :team
              and lower(trim(tm.email)) = lower(trim(:email))
            """)
    boolean existsByTeamAndNormalizedEmail(@Param("email") String email, @Param("team") Team team);

    @Query("""
            select count(tm) > 0
            from TeamMember tm
            where tm.team = :team
              and lower(trim(tm.sys_id)) = lower(trim(:sysId))
            """)
    boolean existsByTeamAndNormalizedSysId(@Param("sysId") String sysId, @Param("team") Team team);

    long countByTeam(Team team);
}
