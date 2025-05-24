package org.mdental.authcore.web.controller;

import lombok.RequiredArgsConstructor;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.service.UserService;
import org.mdental.commons.model.AuthPrincipal;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolver for the @AuthenticationUser annotation.
 */
@Component
@RequiredArgsConstructor
public class AuthenticationUserResolver implements HandlerMethodArgumentResolver {
    private final UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationUser.class)
                && parameter.getParameterType().equals(User.class);
    }

    @Override
    public User resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return userService.getUserById(principal.id());
        }
        return null;
    }
}