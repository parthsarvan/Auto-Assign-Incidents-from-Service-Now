package com.example.backend.service;

import com.example.backend.config.InciTeamEmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailNotificationSender {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationSender.class);
    private static final Pattern RECIPIENT_SPLIT = Pattern.compile("[,;\\s]+");

    private final InciTeamEmailProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public EmailNotificationSender(
            InciTeamEmailProperties properties,
            ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.properties = properties;
        this.mailSenderProvider = mailSenderProvider;
    }

    public boolean isProviderConfigured() {
        return properties.hasRequiredSettings() && mailSenderProvider.getIfAvailable() != null;
    }

    public EmailSendResult sendTextEmail(Collection<String> recipients, String subject, String body) {
        List<String> normalizedRecipients = normalizeRecipients(recipients);
        if (normalizedRecipients.isEmpty()) {
            return new EmailSendResult(false, "No email recipients were provided.", normalizedRecipients);
        }
        if (!isProviderConfigured()) {
            logger.info("Email delivery skipped because INCITEAM_EMAIL_ENABLED or SES SMTP settings are not configured.");
            return new EmailSendResult(false, "Email delivery is not configured.", normalizedRecipients);
        }
        validateSandboxRecipients(normalizedRecipients);

        try {
            JavaMailSender mailSender = mailSenderProvider.getObject();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getFrom().trim());
            if (StringUtils.hasText(properties.getReplyTo())) {
                helper.setReplyTo(properties.getReplyTo().trim());
            }
            helper.setTo(normalizedRecipients.toArray(String[]::new));
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            return new EmailSendResult(true, "Email sent.", normalizedRecipients);
        } catch (MessagingException | MailException ex) {
            logger.warn("Failed to send notification email: {}", ex.getMessage());
            throw new IllegalStateException("Failed to send notification email: " + ex.getMessage(), ex);
        }
    }

    public List<String> parseRecipients(String recipients) {
        if (!StringUtils.hasText(recipients)) {
            return List.of();
        }
        return normalizeRecipients(List.of(RECIPIENT_SPLIT.split(recipients)));
    }

    public void validateSandboxRecipients(Collection<String> recipients) {
        if (!properties.isSandboxMode()) {
            return;
        }
        Set<String> verifiedRecipients = properties.getVerifiedRecipients().stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeEmail)
                .collect(Collectors.toSet());
        List<String> unverifiedRecipients = normalizeRecipients(recipients).stream()
                .filter(recipient -> !verifiedRecipients.contains(recipient))
                .toList();
        if (!unverifiedRecipients.isEmpty()) {
            throw new IllegalArgumentException(
                    "SES sandbox mode only allows verified recipient email addresses: "
                            + String.join(", ", unverifiedRecipients));
        }
    }

    private List<String> normalizeRecipients(Collection<String> recipients) {
        if (recipients == null) {
            return List.of();
        }
        return recipients.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeEmail)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public record EmailSendResult(boolean sent, String message, List<String> recipients) {
    }
}
