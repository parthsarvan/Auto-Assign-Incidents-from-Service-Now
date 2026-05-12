package com.example.backend.repository;

import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.Organization;
import com.example.backend.entity.Team;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfigurationItemRepository extends JpaRepository<ConfigurationItem, Long> {
    Optional<ConfigurationItem> findByName(String name);
    Optional<ConfigurationItem> findByNameAndTeam(String name, Team team);
    @Query("""
            select ci
            from ConfigurationItem ci
            where ci.team = :team
              and lower(trim(ci.serviceNowSysId)) = lower(trim(:serviceNowSysId))
            """)
    Optional<ConfigurationItem> findByTeamAndNormalizedServiceNowSysId(
            @Param("team") Team team,
            @Param("serviceNowSysId") String serviceNowSysId);
    @Query("select ci from ConfigurationItem ci where ci.ci_id = :id and ci.team = :team")
    Optional<ConfigurationItem> findByIdAndTeam(@Param("id") Long id, @Param("team") Team team);
    List<ConfigurationItem> findAllByTeamOrderByNameAsc(Team team);
    boolean existsByNameIgnoreCaseAndTeam(String name, Team team);
    boolean existsByServiceNowSysIdIgnoreCaseAndTeam(String serviceNowSysId, Team team);
    @Query("""
            select count(ci) > 0
            from ConfigurationItem ci
            where ci.team = :team
              and lower(trim(ci.name)) = lower(trim(:name))
            """)
    boolean existsByTeamAndNormalizedName(@Param("team") Team team, @Param("name") String name);

    @Query("""
            select count(ci) > 0
            from ConfigurationItem ci
            where ci.team = :team
              and lower(trim(ci.serviceNowSysId)) = lower(trim(:serviceNowSysId))
            """)
    boolean existsByTeamAndNormalizedServiceNowSysId(
            @Param("team") Team team,
            @Param("serviceNowSysId") String serviceNowSysId);

    @Query("""
            select ci
            from ConfigurationItem ci
            join fetch ci.team t
            join fetch t.organization
            where t.organization = :organization
              and t <> :excludedTeam
              and lower(trim(ci.serviceNowSysId)) = lower(trim(:serviceNowSysId))
            order by t.name asc
            """)
    List<ConfigurationItem> findOrganizationCiOwnersByServiceNowSysId(
            @Param("organization") Organization organization,
            @Param("excludedTeam") Team excludedTeam,
            @Param("serviceNowSysId") String serviceNowSysId);

    @Query("""
            select ci
            from ConfigurationItem ci
            join fetch ci.team t
            join fetch t.organization
            where t.organization = :organization
              and t <> :excludedTeam
              and lower(trim(ci.name)) = lower(trim(:name))
            order by t.name asc
            """)
    List<ConfigurationItem> findOrganizationCiOwnersByName(
            @Param("organization") Organization organization,
            @Param("excludedTeam") Team excludedTeam,
            @Param("name") String name);

    long countByTeam(Team team);
}
