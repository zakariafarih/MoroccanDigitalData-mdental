package org.mdental.authcore.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for rendering Thymeleaf templates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateRenderer {
    private final TemplateEngine templateEngine;

    private static final String DEFAULT_LAYOUT = "email-layout";

    /**
     * Render a template with variables.
     *
     * @param templateName the template name (used for specific content fragments)
     * @param variables the template variables
     * @return the rendered template
     */
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();

        // Create deep copy to avoid modifying the original
        Map<String, Object> templateVars = new HashMap<>(variables);

        // If no subject provided, use a default
        if (!templateVars.containsKey("subject")) {
            templateVars.put("subject", "MDental Notification");
        }

        // Render content body fragment if template is specified and not the layout itself
        if (!DEFAULT_LAYOUT.equals(templateName)) {
            // Generate body content from specific template
            String bodyContent = renderFragment(templateName, templateVars);
            templateVars.put("body", bodyContent);

            // Use the layout template instead
            templateName = DEFAULT_LAYOUT;
        }

        templateVars.forEach(context::setVariable);

        try {
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.error("Error rendering template {}: {}", templateName, e.getMessage(), e);
            return "Error rendering template. Please contact support.";
        }
    }

    /**
     * Render a specific template fragment for the body content.
     *
     * @param templateName the template name
     * @param variables the template variables
     * @return the rendered fragment
     */
    private String renderFragment(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);

        try {
            return templateEngine.process("fragments/" + templateName, context);
        } catch (Exception e) {
            log.error("Error rendering template fragment {}: {}", templateName, e.getMessage(), e);
            return "Error rendering email content.";
        }
    }
}