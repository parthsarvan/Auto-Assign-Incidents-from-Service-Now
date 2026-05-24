package com.example.backend.repository;

import com.example.backend.entity.Team;
import com.example.backend.entity.TeamNotificationSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamNotificationSettingsRepository extends JpaRepository<TeamNotificationSettings, Long> {
    Optional<TeamNotificationSettings> findByTeam(Team team);
}
