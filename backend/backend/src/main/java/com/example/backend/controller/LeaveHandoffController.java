package com.example.backend.controller;

import com.example.backend.dto.LeaveHandoffResponse;
import com.example.backend.service.LeaveHandoffService;
import com.example.backend.service.WorkspaceAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicenow")
public class LeaveHandoffController {
    private final LeaveHandoffService leaveHandoffService;
    private final WorkspaceAccessService workspaceAccessService;

    public LeaveHandoffController(
            LeaveHandoffService leaveHandoffService,
            WorkspaceAccessService workspaceAccessService) {
        this.leaveHandoffService = leaveHandoffService;
        this.workspaceAccessService = workspaceAccessService;
    }

    @GetMapping("/leave-handoff")
    public LeaveHandoffResponse getLeaveHandoff() {
        workspaceAccessService.requireCurrentTeamManager();
        return leaveHandoffService.getCurrentLeaveHandoff();
    }
}
