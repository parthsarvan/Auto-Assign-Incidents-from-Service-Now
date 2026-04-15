package com.example.backend.dto;

public class ServiceNowValidationIssue {
    private String type;
    private String localName;
    private String localSysId;
    private String message;

    public ServiceNowValidationIssue() {}

    public ServiceNowValidationIssue(String type, String localName, String localSysId, String message) {
        this.type = type;
        this.localName = localName;
        this.localSysId = localSysId;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getLocalSysId() {
        return localSysId;
    }

    public void setLocalSysId(String localSysId) {
        this.localSysId = localSysId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
