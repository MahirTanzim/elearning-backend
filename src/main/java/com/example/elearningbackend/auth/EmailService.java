package com.example.elearningbackend.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.mail.enabled}")
    private boolean enabled;

    @Value("${app.mail.from}")
    private String from;

    public void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("\n===== EMAIL (dev mode, not actually sent) =====\nTo: {}\nSubject: {}\n\n{}\n===============================================",
                to, subject, body);
            return;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("Mail is enabled but no mail sender is configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}