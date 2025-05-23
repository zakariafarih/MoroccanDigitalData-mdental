package org.mdental.commons.security;

import org.mdental.commons.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

/**

 Utility methods for handling Role objects and Spring Security authorities.
 */
public final class RoleUtils {

    private RoleUtils() {
// Prevent instantiation
    }

    /**

     Convert a set of application roles to Spring Security authorities
     @param roles Set of application roles, may be null
     @return List of Spring Security authorities (never null)
     */
    public static List<GrantedAuthority> toAuthorities(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>(roles.size());
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.asSpringRole()));
        }
        return authorities;
    }
}