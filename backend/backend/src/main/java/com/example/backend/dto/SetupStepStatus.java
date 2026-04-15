package com.example.backend.dto;

public class SetupStepStatus {
    private String key;
    private String label;
    private long count;
    private boolean complete;
    private boolean required;
    private String route;
    private String description;

    public SetupStepStatus() {}

    public SetupStepStatus(
            String key,
            String label,
            long count,
            boolean complete,
            boolean required,
            String route,
            String description) {
        this.key = key;
        this.label = label;
        this.count = count;
        this.complete = complete;
        this.required = required;
        this.route = route;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
