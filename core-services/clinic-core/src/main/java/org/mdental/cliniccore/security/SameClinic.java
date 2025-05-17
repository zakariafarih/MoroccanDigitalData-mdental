package org.mdental.cliniccore.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates a method should check that the current user belongs
 * to the same tenant/clinic as the one specified in the request.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SameClinic {
    /**
     * If true, will throw exception on validation failure.
     * If false, will return false or empty result.
     */
    boolean strict() default true;

    /**
     * Optional parameter name to use for clinic ID lookup.
     * If not specified, the first UUID parameter will be used.
     */
    String param() default "";
}