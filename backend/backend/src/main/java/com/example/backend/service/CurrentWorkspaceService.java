package com.example.backend.service;

import com.example.backend.entity.Team;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import java.util.function.Supplier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentWorkspaceService {
    private static final ThreadLocal<Team> TEAM_OVERRIDE = new ThreadLocal<>();

    private final UserRepository userRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public CurrentWorkspaceService(
            UserRepository userRepository,
            WorkspaceBootstrapService workspaceBootstrapService) {
        this.userRepository = userRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("No authenticated user is available.");
        }
        User user = userRepository.findByNormalizedUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user could not be resolved."));
        workspaceBootstrapService.ensureWorkspaceForUser(user);
        return userRepository.findById(user.getU_id()).orElse(user);
    }

    public Team getCurrentTeam() {
        Team overriddenTeam = TEAM_OVERRIDE.get();
        if (overriddenTeam != null) {
            return overriddenTeam;
        }
        try {
            User user = getCurrentUser();
            if (user.getCurrentTeam() == null) {
                return workspaceBootstrapService.getDefaultTeam();
            }
            return user.getCurrentTeam();
        } catch (IllegalStateException ex) {
            return workspaceBootstrapService.getDefaultTeam();
        }
    }

    public void runInTeam(Team team, Runnable action) {
        Team previousTeam = TEAM_OVERRIDE.get();
        TEAM_OVERRIDE.set(team);
        try {
            action.run();
        } finally {
            restoreOverride(previousTeam);
        }
    }

    public <T> T supplyInTeam(Team team, Supplier<T> action) {
        Team previousTeam = TEAM_OVERRIDE.get();
        TEAM_OVERRIDE.set(team);
        try {
            return action.get();
        } finally {
            restoreOverride(previousTeam);
        }
    }

    private void restoreOverride(Team previousTeam) {
        if (previousTeam == null) {
            TEAM_OVERRIDE.remove();
        } else {
            TEAM_OVERRIDE.set(previousTeam);
        }
    }
}
