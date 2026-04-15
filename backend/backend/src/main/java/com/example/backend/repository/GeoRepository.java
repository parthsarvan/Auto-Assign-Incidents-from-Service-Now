package com.example.backend.repository;

import com.example.backend.entity.Geo;
import com.example.backend.entity.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GeoRepository extends JpaRepository<Geo, Long> {
    List<Geo> findAllByTeamOrderByNameAsc(Team team);
    @Query("select g from Geo g where g.g_id = :id and g.team = :team")
    Optional<Geo> findByIdAndTeam(@Param("id") Long id, @Param("team") Team team);
    boolean existsByNameIgnoreCaseAndTeam(String name, Team team);
    @Query("""
            select count(g) > 0
            from Geo g
            where g.team = :team
              and lower(trim(g.name)) = lower(trim(:name))
            """)
    boolean existsByTeamAndNormalizedName(@Param("team") Team team, @Param("name") String name);
    long countByTeam(Team team);
}
