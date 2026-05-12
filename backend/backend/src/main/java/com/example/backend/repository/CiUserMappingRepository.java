package com.example.backend.repository;

import com.example.backend.entity.CiUserMapping;
import com.example.backend.entity.ConfigurationItem;
import com.example.backend.entity.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CiUserMappingRepository extends JpaRepository<CiUserMapping, Long> {
    @Query(
            "select c from CiUserMapping c "
                    + "join fetch c.teamMember tm "
                    + "left join fetch tm.geo "
                    + "where c.configurationItem = :configurationItem "
                    + "order by c.sortOrder asc")
    List<CiUserMapping> findByConfigurationItemOrderBySortOrderAsc(
            @Param("configurationItem") ConfigurationItem configurationItem);

    @Query(
            "select c from CiUserMapping c "
                    + "join fetch c.configurationItem ci "
                    + "join fetch c.teamMember tm "
                    + "left join fetch tm.geo "
                    + "where c.configurationItem = :configurationItem and c.team = :team "
                    + "order by c.sortOrder asc")
    List<CiUserMapping> findByConfigurationItemAndTeamOrderBySortOrderAsc(
            @Param("configurationItem") ConfigurationItem configurationItem,
            @Param("team") Team team);

    @Query(
            "select c from CiUserMapping c "
                    + "join fetch c.configurationItem ci "
                    + "join fetch c.teamMember tm "
                    + "left join fetch tm.geo")
    List<CiUserMapping> findAllWithConfigurationItemAndTeamMember();

    @Query(
            "select c from CiUserMapping c "
                    + "join fetch c.configurationItem ci "
                    + "join fetch c.teamMember tm "
                    + "left join fetch tm.geo "
                    + "where c.team = :team "
                    + "order by c.sortOrder asc")
    List<CiUserMapping> findAllByTeamWithDetails(@Param("team") Team team);

    @Query(
            "select c from CiUserMapping c "
                    + "join fetch c.configurationItem ci "
                    + "join fetch c.teamMember tm "
                    + "left join fetch tm.geo "
                    + "where c.mapping_id = :id and c.team = :team")
    Optional<CiUserMapping> findByIdAndTeam(@Param("id") Long id, @Param("team") Team team);

    boolean existsByConfigurationItemAndTeamMemberAndTeam(
            ConfigurationItem configurationItem,
            com.example.backend.entity.TeamMember teamMember,
            Team team);

    long countByTeam(Team team);
}
