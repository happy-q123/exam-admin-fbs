package com.domain.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 基于访问令牌中的业务角色做方法级授权。
 * 角色由认证服务签发，不能以请求参数或前端菜单作为权限依据。
 */
public class RoleGuard {

    public boolean isTeacherOrAdmin(Authentication authentication) {
        Set<String> roles = rolesOf(authentication);
        return roles.contains("teacher") || roles.contains("admin");
    }

    public boolean isAdmin(Authentication authentication) {
        return rolesOf(authentication).contains("admin");
    }

    private Set<String> rolesOf(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return Collections.emptySet();
        }
        Set<String> roles = new LinkedHashSet<>();
        Object role = jwtAuthenticationToken.getToken().getClaims().get("role");
        addRoles(roles, role);
        addRoles(roles, jwtAuthenticationToken.getToken().getClaims().get("roles"));
        addRoles(roles, jwtAuthenticationToken.getToken().getClaims().get("authorities"));
        return roles;
    }

    private void addRoles(Set<String> roles, Object value) {
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> addRoles(roles, item));
            return;
        }
        if (value == null) {
            return;
        }
        String normalized = String.valueOf(value).trim().replaceFirst("^ROLE_", "");
        if (!normalized.isBlank()) {
            roles.add(normalized.toLowerCase());
        }
    }

    @SuppressWarnings("unused")
    private String roleOf(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Object role = jwtAuthenticationToken.getToken().getClaims().get("role");
            return role == null ? "" : String.valueOf(role).replaceFirst("^ROLE_", "");
        }
        return "";
    }
}
