package com.example.backend.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceBootstrapStartup {
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public WorkspaceBootstrapStartup(WorkspaceBootstrapService workspaceBootstrapService) {
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        workspaceBootstrapService.backfillDefaultWorkspace();
    }
}
