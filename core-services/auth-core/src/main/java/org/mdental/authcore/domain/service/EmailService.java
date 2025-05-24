package org.mdental.authcore.domain.service;

import java.util.Map;

public interface EmailService {
    /**
     * Send an email using a template.
     *
     * @param to the recipient email
     * @param subject the email subject
     * @param template the template name
     * @param templateVars the template variables
     */
    void sendEmail(String to, String subject, String template, Map<String, Object> templateVars);
}
