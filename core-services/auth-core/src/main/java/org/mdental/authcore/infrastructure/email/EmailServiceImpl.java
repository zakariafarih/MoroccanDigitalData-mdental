package org.mdental.authcore.infrastructure.email;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.mdental.authcore.domain.event.EmailEvent;
import org.mdental.authcore.domain.service.EmailService;
import org.mdental.authcore.domain.service.OutboxService;
import org.mdental.authcore.util.TemplateRenderer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final OutboxService outboxService;
    private final TemplateRenderer templateRenderer;

    @Override
    public void sendEmail(String to,
                          String subject,
                          String template,
                          Map<String, Object> templateVars) {
        log.info("Sending email with template '{}' to: {}", template, to);

        String htmlContent = templateRenderer.renderTemplate(template, templateVars);
        Map<String, Object> emailData = Map.of(
                "to",           to,
                "subject",      subject,
                "htmlContent",  htmlContent,
                "templateName", template,
                "timestamp",    Instant.now()
        );

        outboxService.saveEvent(
                "Email",
                UUID.randomUUID(),
                EmailEvent.EMAIL_REQUESTED.name(),
                null,
                emailData
        );
    }
}
