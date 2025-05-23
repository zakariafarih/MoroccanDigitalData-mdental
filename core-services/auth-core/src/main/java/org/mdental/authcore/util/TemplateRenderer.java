package org.mdental.authcore.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Utility for rendering Thymeleaf templates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateRenderer {
    private final TemplateEngine templateEngine;

    /**
     * Render a template with variables.
     *
     * @param templateName the template name
     * @param variables the template variables
     * @return the rendered template
     */
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);

        try {
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.error("Error rendering template {}: {}", templateName, e.getMessage(), e);
            return "Error rendering template. Please contact support.";
        }
    }
}
