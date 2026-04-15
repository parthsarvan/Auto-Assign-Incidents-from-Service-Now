package com.example.backend.repository;

import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMembership;
import com.example.backend.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {
    boolean existsByUserAndTeam(User user, Team team);
    Optional<TeamMembership> findByUserAndTeam(User user, Team team);
    long countByTeamAndRole(Team team, String role);

    @Query("select tm from TeamMembership tm join fetch tm.team t where tm.user = :user order by t.name asc")
    List<TeamMembership> findAllByUserWithTeam(@Param("user") User user);

    @Query(
            "select tm from TeamMembership tm "
                    + "join fetch tm.team t "
                    + "join fetch tm.user u "
                    + "where t.organization = :organization "
                    + "order by u.username asc, t.name asc")
    List<TeamMembership> findAllByOrganizationWithTeamAndUser(@Param("organization") com.example.backend.entity.Organization organization);
}
