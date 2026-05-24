package com.example.backend.dto;

import java.util.List;

public class NotificationTestEmailResponse {
    private boolean sent;
    private String message;
    private List<String> recipients;

    public NotificationTestEmailResponse() {
    }

    public NotificationTestEmailResponse(boolean sent, String message, List<String> recipients) {
        this.sent = sent;
        this.message = message;
        this.recipients = recipients;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }
}
