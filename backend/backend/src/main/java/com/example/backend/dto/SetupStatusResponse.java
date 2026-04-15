package com.example.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class SetupStatusResponse {
    private boolean brandNew;
    private boolean ready;
    private int completedSteps;
    private int totalSteps;
    private List<SetupStepStatus> steps = new ArrayList<>();

    public SetupStatusResponse() {}

    public SetupStatusResponse(
            boolean brandNew,
            boolean ready,
            int completedSteps,
            int totalSteps,
            List<SetupStepStatus> steps) {
        this.brandNew = brandNew;
        this.ready = ready;
        this.completedSteps = completedSteps;
        this.totalSteps = totalSteps;
        this.steps = steps;
    }

    public boolean isBrandNew() {
        return brandNew;
    }

    public void setBrandNew(boolean brandNew) {
        this.brandNew = brandNew;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(int completedSteps) {
        this.completedSteps = completedSteps;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public List<SetupStepStatus> getSteps() {
        return steps;
    }

    public void setSteps(List<SetupStepStatus> steps) {
        this.steps = steps;
    }
}
