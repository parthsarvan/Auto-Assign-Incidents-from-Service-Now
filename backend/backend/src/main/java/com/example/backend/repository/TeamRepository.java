package com.example.backend.repository;

import com.example.backend.entity.Organization;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByOrganizationAndName(Organization organization, String name);
    List<Team> findAllByOrganizationOrderByNameAsc(Organization organization);
    List<Team> findAllByUnsupportedCiFallbackTeamMember(TeamMember teamMember);
    Optional<Team> findByJoinCode(String joinCode);
    boolean existsByJoinCode(String joinCode);

    @Query("""
            select count(t) > 0
            from Team t
            where t.organization = :organization
              and lower(trim(t.name)) = lower(trim(:name))
            """)
    boolean existsByOrganizationAndNormalizedName(
            @Param("organization") Organization organization,
            @Param("name") String name);
}
