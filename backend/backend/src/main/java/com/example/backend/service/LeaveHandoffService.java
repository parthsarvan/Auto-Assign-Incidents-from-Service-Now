package com.example.backend.service;

import com.example.backend.dto.LeaveHandoffIncident;
import com.example.backend.dto.LeaveHandoffItem;
import com.example.backend.dto.LeaveHandoffResponse;
import com.example.backend.dto.ServiceNowIncident;
import com.example.backend.dto.ServiceNowReference;
import com.example.backend.entity.LeaveEntry;
import com.example.backend.entity.Team;
import com.example.backend.entity.TeamMember;
import com.example.backend.repository.LeaveEntryRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LeaveHandoffService {
    private final CurrentWorkspaceService currentWorkspaceService;
    private final LeaveEntryRepository leaveEntryRepository;
    private final ServiceNowIncidentClient incidentClient;
    private final OrganizationServiceNowConfigService organizationServiceNowConfigService;

    public LeaveHandoffService(
            CurrentWorkspaceService currentWorkspaceService,
            LeaveEntryRepository leaveEntryRepository,
            ServiceNowIncidentClient incidentClient,
            OrganizationServiceNowConfigService organizationServiceNowConfigService) {
        this.currentWorkspaceService = currentWorkspaceService;
        this.leaveEntryRepository = leaveEntryRepository;
        this.incidentClient = incidentClient;
        this.organizationServiceNowConfigService = organizationServiceNowConfigService;
    }

    public LeaveHandoffResponse getCurrentLeaveHandoff() {
        Team team = currentWorkspaceService.getCurrentTeam();
        Instant now = Instant.now();
        if (!organizationServiceNowConfigService.isConfiguredForTeam(team)) {
            return new LeaveHandoffResponse(now, 0, 0, List.of());
        }
        List<LeaveHandoffItem> items = leaveEntryRepository.findAllByTeamWithTeamMember(team).stream()
                .filter(leave -> isActiveLeave(leave, now))
                .map(this::toHandoffItem)
                .filter(item -> item.getIncidents() != null && !item.getIncidents().isEmpty())
                .sorted(Comparator.comparing(LeaveHandoffItem::getTeamMemberName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        int activeIncidentCount = items.stream()
                .mapToInt(item -> item.getIncidents().size())
                .sum();
        return new LeaveHandoffResponse(now, items.size(), activeIncidentCount, items);
    }

    private boolean isActiveLeave(LeaveEntry leave, Instant now) {
        return leave.getStartTs() != null
                && leave.getEndTs() != null
                && !leave.getStartTs().isAfter(now)
                && !leave.getEndTs().isBefore(now);
    }

    private LeaveHandoffItem toHandoffItem(LeaveEntry leave) {
        TeamMember member = leave.getTeamMember();
        List<LeaveHandoffIncident> incidents = member.getSys_id() == null || member.getSys_id().isBlank()
                ? List.of()
                : incidentClient.fetchActiveIncidentsAssignedTo(member.getSys_id()).stream()
                        .sorted(Comparator.comparing(
                                ServiceNowIncident::getSys_created_on,
                                Comparator.nullsLast(String::compareTo)))
                        .map(this::toHandoffIncident)
                        .toList();
        return new LeaveHandoffItem(
                fullName(member),
                member.getEmail(),
                leave.getStartTs(),
                leave.getEndTs(),
                leave.getReason(),
                incidents);
    }

    private LeaveHandoffIncident toHandoffIncident(ServiceNowIncident incident) {
        return new LeaveHandoffIncident(
                incident.getNumber(),
                incident.getSys_created_on(),
                incident.getPriority(),
                resolveDisplayValue(incident.getCmdb_ci()),
                resolveDisplayValue(incident.getAssignment_group()),
                incident.getShort_description());
    }

    private String fullName(TeamMember member) {
        return String.format("%s %s", member.getF_name(), member.getL_name()).trim();
    }

    private String resolveDisplayValue(ServiceNowReference reference) {
        if (reference == null) {
            return null;
        }
        if (reference.getDisplayValue() != null && !reference.getDisplayValue().isBlank()) {
            return reference.getDisplayValue();
        }
        return reference.getValue();
    }
}
