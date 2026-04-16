package com.example.backend.service;

import com.example.backend.entity.TeamMembership;
import com.example.backend.entity.User;
import com.example.backend.repository.TeamMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceAccessService {
    private final CurrentWorkspaceService currentWorkspaceService;
    private final TeamMembershipRepository teamMembershipRepository;

    public WorkspaceAccessService(
            CurrentWorkspaceService currentWorkspaceService,
            TeamMembershipRepository teamMembershipRepository) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    public boolean isGlobalAdmin(User user) {
        return user != null && "Admin".equalsIgnoreCase(user.getRole());
    }

    public String getCurrentTeamRole(User user) {
        if (user == null || user.getCurrentTeam() == null) {
            return null;
        }
        return teamMembershipRepository.findByUserAndTeam(user, user.getCurrentTeam())
                .map(TeamMembership::getRole)
                .orElse(isGlobalAdmin(user) ? "TEAM_ADMIN" : null);
    }

    public boolean canManageCurrentTeam(User user) {
        if (isGlobalAdmin(user)) {
            return true;
        }
        String teamRole = getCurrentTeamRole(user);
        return "TEAM_ADMIN".equalsIgnoreCase(teamRole) || "MANAGER".equalsIgnoreCase(teamRole);
    }

    public boolean isCurrentTeamAdmin(User user) {
        if (isGlobalAdmin(user)) {
            return true;
        }
        return "TEAM_ADMIN".equalsIgnoreCase(getCurrentTeamRole(user));
    }

    public void requireGlobalAdmin() {
        User user = currentWorkspaceService.getCurrentUser();
        if (!isGlobalAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organization admins can perform this action.");
        }
    }

    public void requireCurrentTeamManager() {
        User user = currentWorkspaceService.getCurrentUser();
        if (!canManageCurrentTeam(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You need TEAM_ADMIN or MANAGER access for the current team.");
        }
    }

    public boolean hasCurrentTeamManagerAccess() {
        User user = currentWorkspaceService.getCurrentUser();
        return canManageCurrentTeam(user);
    }

    public void requireCurrentTeamAdmin() {
        User user = currentWorkspaceService.getCurrentUser();
        if (!isCurrentTeamAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You need TEAM_ADMIN access for the current team.");
        }
    }
}
