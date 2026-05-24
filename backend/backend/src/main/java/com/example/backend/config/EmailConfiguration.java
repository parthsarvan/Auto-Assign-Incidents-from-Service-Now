package com.example.backend.config;

import java.util.Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@EnableConfigurationProperties(InciTeamEmailProperties.class)
public class EmailConfiguration {
    @Bean
    @ConditionalOnProperty(name = "inciteam.email.enabled", havingValue = "true")
    public JavaMailSender inciTeamJavaMailSender(InciTeamEmailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        InciTeamEmailProperties.Smtp smtp = properties.getSmtp();
        sender.setHost(smtp.getHost());
        sender.setPort(smtp.getPort());
        sender.setUsername(smtp.getUsername());
        sender.setPassword(smtp.getPassword());

        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", Boolean.toString(smtp.isAuth()));
        mailProperties.put("mail.smtp.starttls.enable", Boolean.toString(smtp.isStarttlsEnabled()));
        mailProperties.put("mail.smtp.connectiontimeout", Integer.toString(smtp.getConnectionTimeoutMs()));
        mailProperties.put("mail.smtp.timeout", Integer.toString(smtp.getTimeoutMs()));
        mailProperties.put("mail.smtp.writetimeout", Integer.toString(smtp.getWriteTimeoutMs()));
        return sender;
    }
}
