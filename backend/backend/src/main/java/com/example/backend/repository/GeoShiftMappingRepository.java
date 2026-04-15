package com.example.backend.repository;

import com.example.backend.entity.GeoShiftMapping;
import com.example.backend.entity.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GeoShiftMappingRepository extends JpaRepository<GeoShiftMapping, Long> {
    @Query("select gsm from GeoShiftMapping gsm join fetch gsm.geo join fetch gsm.shift")
    List<GeoShiftMapping> findAllWithGeoAndShift();

    @Query("select gsm from GeoShiftMapping gsm join fetch gsm.geo join fetch gsm.shift where gsm.team = :team")
    List<GeoShiftMapping> findAllByTeamWithGeoAndShift(Team team);

    @Query("select gsm from GeoShiftMapping gsm join fetch gsm.geo join fetch gsm.shift where gsm.gsm_id = :id and gsm.team = :team")
    Optional<GeoShiftMapping> findByIdAndTeam(@Param("id") Long id, @Param("team") Team team);

    boolean existsByGeoAndShiftAndTeam(com.example.backend.entity.Geo geo, com.example.backend.entity.Shift shift, Team team);

    long countByTeam(Team team);
}
