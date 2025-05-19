package org.mdental.authcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class RealmTemplateService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("__([A-Z_]+)__");

    @Value("classpath:templates/realm-template.json")
    private Resource templateResource;

    /**
     * Loads the realm template from classpath and substitutes placeholders
     *
     * @param variables Map of placeholder name to value
     * @return JSON string with placeholders substituted
     */
    public String loadAndProcessTemplate(Map<String, String> variables) {
        try {
            String template = StreamUtils.copyToString(templateResource.getInputStream(), StandardCharsets.UTF_8);

            return substituteVariables(template, variables);
        } catch (IOException e) {
            log.error("Failed to load realm template", e);
            throw new RuntimeException("Failed to load realm template", e);
        }
    }

    private String substituteVariables(String template, Map<String, String> variables) {
        StringBuilder result = new StringBuilder(template);

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String placeholder = matcher.group(0);
            String key = matcher.group(1);

            String replacement = variables.getOrDefault(key, "");

            // Replace all occurrences
            int start = 0;
            while (true) {
                start = result.indexOf(placeholder, start);
                if (start < 0) break;

                result.replace(start, start + placeholder.length(), replacement);
                start += replacement.length();
            }
        }

        return result.toString();
    }
}