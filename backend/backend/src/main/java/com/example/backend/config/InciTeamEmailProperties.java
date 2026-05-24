package com.example.backend.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "inciteam.email")
public class InciTeamEmailProperties {
    private boolean enabled;
    private String from;
    private String replyTo;
    private boolean sandboxMode = true;
    private List<String> verifiedRecipients = new ArrayList<>();
    private Smtp smtp = new Smtp();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public boolean isSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }

    public List<String> getVerifiedRecipients() {
        return verifiedRecipients;
    }

    public void setVerifiedRecipients(List<String> verifiedRecipients) {
        this.verifiedRecipients = verifiedRecipients != null ? verifiedRecipients : new ArrayList<>();
    }

    public Smtp getSmtp() {
        return smtp;
    }

    public void setSmtp(Smtp smtp) {
        this.smtp = smtp != null ? smtp : new Smtp();
    }

    public boolean hasRequiredSettings() {
        return enabled
                && StringUtils.hasText(from)
                && smtp != null
                && StringUtils.hasText(smtp.getHost())
                && StringUtils.hasText(smtp.getUsername())
                && StringUtils.hasText(smtp.getPassword());
    }

    public static class Smtp {
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private boolean auth = true;
        private boolean starttlsEnabled = true;
        private int connectionTimeoutMs = 10000;
        private int timeoutMs = 10000;
        private int writeTimeoutMs = 10000;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isAuth() {
            return auth;
        }

        public void setAuth(boolean auth) {
            this.auth = auth;
        }

        public boolean isStarttlsEnabled() {
            return starttlsEnabled;
        }

        public void setStarttlsEnabled(boolean starttlsEnabled) {
            this.starttlsEnabled = starttlsEnabled;
        }

        public int getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }

        public void setConnectionTimeoutMs(int connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getWriteTimeoutMs() {
            return writeTimeoutMs;
        }

        public void setWriteTimeoutMs(int writeTimeoutMs) {
            this.writeTimeoutMs = writeTimeoutMs;
        }
    }
}
