package org.mdental.authcore.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.event.EmailEvent;
import org.mdental.authcore.util.TemplateRenderer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending emails.
 * This implementation publishes email events to the outbox.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final OutboxService outboxService;
    private final TemplateRenderer templateRenderer;

    /**
     * Send an email using a template.
     *
     * @param to the recipient email
     * @param subject the email subject
     * @param template the template name
     * @param templateVars the template variables
     */
    public void sendEmail(String to, String subject, String template, Map<String, Object> templateVars) {
        log.info("Sending email with template '{}' to: {}", template, to);

        // Render template to HTML
        String htmlContent = templateRenderer.renderTemplate(template, templateVars);

        Map<String, Object> emailData = Map.of(
                "to", to,
                "subject", subject,
                "htmlContent", htmlContent,
                "templateName", template,
                "timestamp", Instant.now()
        );

        // Publish email event to outbox
        outboxService.saveEvent(
                "Email",
                UUID.randomUUID(),
                EmailEvent.EMAIL_REQUESTED.name(),
                null,
                emailData
        );
    }
}